package bio.singa.simulation.model.simulation;

import bio.singa.features.formatter.TimeFormatter;
import bio.singa.features.quantities.MolarConcentration;
import bio.singa.features.units.UnitRegistry;
import bio.singa.simulation.model.modules.UpdateModule;
import bio.singa.simulation.model.modules.concentration.LocalError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tec.units.indriya.AbstractUnit;
import tec.units.indriya.quantity.Quantities;

import javax.measure.Quantity;
import javax.measure.quantity.Frequency;
import javax.measure.quantity.Time;
import java.util.*;
import java.util.concurrent.CountDownLatch;

import static bio.singa.simulation.model.modules.concentration.ModuleState.SUCCEEDED_WITH_PENDING_CHANGES;

/**
 * @author cl
 */
public class UpdateScheduler {

    private static final Logger logger = LoggerFactory.getLogger(UpdateScheduler.class);

    /**
     * The default value where errors are considered too large and the time step is reduced.
     */
    private static final double DEFAULT_RECALCULATION_CUTOFF = 0.01;

    private Simulation simulation;
    private List<Updatable> updatables;

    private Iterator<UpdateModule> moduleIterator;
    private double recalculationCutoff = DEFAULT_RECALCULATION_CUTOFF;

    private boolean timeStepRescaled;
    private boolean timeStepAlteredInThisEpoch;
    private long timestepsDecreased = 0;
    private long timestepsIncreased = 0;
    private LocalError largestLocalError;
    private UpdateModule localErrorModule;
    private double localErrorUpdate;

    private double largestGlobalError;

    private CountDownLatch countDownLatch;

    private final Deque<UpdateModule> modules;
    private final List<Thread> threads;

    private double previousError;
    private Quantity<Time> previousTimeStep;
    private Quantity<Frequency> accuracyGain;

    private volatile boolean interrupted;
    private boolean globalErrorAcceptable;
    private boolean calculateGlobalError;

    public UpdateScheduler(Simulation simulation) {
        this.simulation = simulation;
        modules = new ArrayDeque<>(simulation.getModules());
        threads = Collections.synchronizedList(new ArrayList<>());
        largestLocalError = LocalError.MINIMAL_EMPTY_ERROR;
    }

    public double getRecalculationCutoff() {
        return recalculationCutoff;
    }

    public void setRecalculationCutoff(double recalculationCutoff) {
        this.recalculationCutoff = recalculationCutoff;
    }

    public long getTimestepsDecreased() {
        return timestepsDecreased;
    }

    public long getTimestepsIncreased() {
        return timestepsIncreased;
    }

    public void nextEpoch() {
        // initialize fields
        timeStepAlteredInThisEpoch = false;
        previousError = 0;
        largestLocalError = LocalError.MINIMAL_EMPTY_ERROR;
        previousTimeStep = UnitRegistry.getTime();
        simulation.collectUpdatables();
        updatables = simulation.getUpdatables();
        moduleIterator = modules.iterator();
        globalErrorAcceptable = true;
        calculateGlobalError = true;

        for (Updatable updatable : updatables) {
            updatable.getConcentrationManager().backupConcentrations();
        }

        // until all models passed
        do {
            if (timeStepRescaled || interrupted || !globalErrorAcceptable) {
                largestLocalError = LocalError.MINIMAL_EMPTY_ERROR;
                resetCalculation();
            }
            timeStepRescaled = false;
            interrupted = false;

            countDownLatch = new CountDownLatch(getNumberOfModules());
            logger.debug("Starting with latch at {}.", countDownLatch.getCount());

            int i = 0;

            synchronized (threads) {
                while (moduleIterator.hasNext()) {
                    i++;
                    UpdateModule module = moduleIterator.next();
                    Thread thread = new Thread(module, "Module " + module.toString() + " (Thread " + i + ")");
                    if (!interrupted) {
                        threads.add(thread);
                        thread.start();
                    } else {
                        logger.debug("Skipping module {}, decreasing latch to {}.", i, countDownLatch.getCount());
                        countDownLatch.countDown();
                    }
                }
            }

            try {
                countDownLatch.await();
                threads.clear();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (!interrupted) {
                // perform only if every module passed individually
                // evaluate total concentration change
                evaluateGlobalNumericalAccuracy();
            }

            // evaluate total spatial displacement
            spatialDisplacementIsValid();

        } while (recalculationRequired());
        // System.out.println("accepted local error: "+largestLocalError.getValue());
        // resolve pending changes
        for (UpdateModule updateModule : modules) {
            if (updateModule.getState().equals(SUCCEEDED_WITH_PENDING_CHANGES)) {
                updateModule.onCompletion();
            }
        }

        logger.debug("Finished processing modules for epoch {}.", simulation.getEpoch());
        // wrap up
        determineAccuracyGain();
        finalizeDeltas();
        modules.forEach(UpdateModule::resetState);
    }

    public void evaluateGlobalNumericalAccuracy() {
        if (calculateGlobalError) {
            // calculate half step concentrations for subsequent evaluation
            // for each node
            for (Updatable updatable : updatables) {
                // calculate interim container (added current updates with checked local error)
                // set half step concentrations y(t+1/2dt) for interim containers
                // backup current concentrations and set current concentration to interim concentrations
                updatable.getConcentrationManager().setInterimAndUpdateCurrentConcentrations();
            }
            globalErrorAcceptable = false;
            calculateGlobalError = false;
        } else {
            // evaluate global numerical accuracy
            // for each node
            double largestGlobalError = 0.0;
            for (Updatable updatable : updatables) {
                // determine full concentrations with full update and 2 * half update
                updatable.getConcentrationManager().determineComparisionConcentrations();
                // determine error between both
                double globalError = updatable.getConcentrationManager().determineGlobalNumericalError();
                if (largestGlobalError < globalError) {
                    largestGlobalError = globalError;
                }
            }
            // set interim check false if global error is to large and true if you can continue
            if (largestGlobalError > recalculationCutoff) {
                // System.out.println("rejected global error: "+largestGlobalError+" @ "+TimeFormatter.formatTime(UnitRegistry.getTime()));
                simulation.getScheduler().decreaseTimeStep();
                globalErrorAcceptable = false;
                calculateGlobalError = true;
            } else {
                // System.out.println("accepted global error: "+largestGlobalError+ " @ "+TimeFormatter.formatTime(UnitRegistry.getTime()));
                globalErrorAcceptable = true;
            }
            if (getLargestLocalError().getValue() != LocalError.MINIMAL_EMPTY_ERROR.getValue()) {
                logger.debug("Largest local error : {} ({}, {}, {})", getLargestLocalError().getValue(), getLargestLocalError().getChemicalEntity(), getLargestLocalError().getUpdatable().getStringIdentifier(), getLocalErrorModule());
            } else {
                logger.debug("Largest local error : minimal");
            }
            logger.debug("Largest global error: {} ", largestGlobalError);
            this.largestGlobalError = largestGlobalError;
        }

    }

    /**
     * Recalculations are required if:
     * <ul>
     * <li>the time step was rescaled during this calculation</li>
     * <li>any module was interrupted during this calculation</li>
     * <li>the global error was larger than the recalculation cutoff</li>
     * </ul>
     *
     * @return true, if a recalculation is required
     */
    public boolean recalculationRequired() {
        return timeStepRescaled || interrupted || !globalErrorAcceptable;
    }

    public LocalError getLargestLocalError() {
        return largestLocalError;
    }

    public void setLargestLocalError(LocalError localError, UpdateModule associatedModule, double associatedConcentration) {
        if (localError.getValue() > largestLocalError.getValue()) {
            largestLocalError = localError;
            localErrorModule = associatedModule;
            localErrorUpdate = associatedConcentration;
        }
    }

    public UpdateModule getLocalErrorModule() {
        return localErrorModule;
    }

    public double getLocalErrorUpdate() {
        return MolarConcentration.concentrationToMolecules(localErrorUpdate).getValue().doubleValue();
    }

    public double getLargestGlobalError() {
        return largestGlobalError;
    }

    public boolean timeStepWasRescaled() {
        return timeStepRescaled;
    }

    public boolean timeStepWasAlteredInThisEpoch() {
        return timeStepAlteredInThisEpoch;
    }

    public Quantity<Frequency> getAccuracyGain() {
        return accuracyGain;
    }

    public void increaseTimeStep() {
        UnitRegistry.setTime(UnitRegistry.getTime().multiply(1.1));
        logger.debug("Increasing time step to {}.", TimeFormatter.formatTime(UnitRegistry.getTime()));
        timestepsIncreased++;
    }

    public synchronized void decreaseTimeStep() {
        // if time step is rescaled for the very fist time this epoch remember the initial error and time step
        if (!timeStepWasAlteredInThisEpoch()) {
            previousError = largestLocalError.getValue();
            previousTimeStep = UnitRegistry.getTime();
        }
        UnitRegistry.setTime(UnitRegistry.getTime().multiply(0.9));
        logger.debug("Decreasing time step to {}.", TimeFormatter.formatTime(UnitRegistry.getTime()));
        timestepsDecreased++;
        timeStepRescaled = true;
        timeStepAlteredInThisEpoch = true;
    }

    private void finalizeDeltas() {
        for (Updatable updatable : updatables) {
            updatable.getConcentrationManager().shiftDeltas();
        }
        // potential updates for vesicles are already set during displacement evaluation
    }

    private void determineAccuracyGain() {
        if (timeStepWasAlteredInThisEpoch()) {
            final double errorDelta = previousError - largestLocalError.getValue();
            final Quantity<Time> timeDelta = previousTimeStep.subtract(UnitRegistry.getTime());
            accuracyGain = Quantities.getQuantity(errorDelta, AbstractUnit.ONE).divide(timeDelta).asType(Frequency.class);
        }
    }

    public CountDownLatch getCountDownLatch() {
        return countDownLatch;
    }

    public void addModule(UpdateModule module) {
        modules.add(module);
    }

    public int getNumberOfModules() {
        return modules.size();
    }

    /**
     * Returns true if the calling module was the first to call the method, therefore being allowed to optimize the
     * time step.
     *
     * @param callingThread The calling thread.
     * @param callingModule The associated module.
     * @return True if the calling module was the first to call the method.
     */
    public synchronized boolean interruptAllBut(Thread callingThread, UpdateModule callingModule) {
        // interrupt all threads but the calling thread and count down their latch
        if (!interrupted) {
            logger.debug("Module {} triggered interrupt.", callingModule);
            interrupted = true;
            List<String> interruptedThreads = new ArrayList<>();
            synchronized (threads) {
                for (Thread thread : threads) {
                    if (thread != callingThread) {
                        if (thread.isAlive()) {
                            thread.interrupt();
                            // countDownLatch.countDown();
                            interruptedThreads.add(thread.getName());
                        }
                    }
                }
            }
            // prioritize(callingModule);
            logger.debug("{} interrupted {}", callingThread.getName(), interruptedThreads);
            return true;
        }
        logger.debug("Module {} tried to interrupt, but interruption was already in progress.", callingModule);
        return false;
    }

    private void prioritize(UpdateModule module) {
        // remove the module from wherever it is
        modules.remove(module);
        // and add it to the front
        modules.addFirst(module);
    }

    public void resetCalculation() {
        logger.debug("Resetting calculations.");
        // reset states
        modules.forEach(UpdateModule::resetState);
        // clear deltas that have previously been calculated
        updatables.forEach(updatable -> updatable.getConcentrationManager().clearPotentialDeltas());
        if (calculateGlobalError) {
            updatables.forEach(updatable -> updatable.getConcentrationManager().revertToOriginalConcentrations());
        }
        // start from the beginning
        moduleIterator = modules.iterator();
    }

    private boolean spatialDisplacementIsValid() {
        if (simulation.getVesicleLayer().getVesicles().isEmpty()) {
            return true;
        }
        if (!simulation.getVesicleLayer().deltasAreBelowDisplacementCutoff()) {
            decreaseTimeStep();
            simulation.getVesicleLayer().clearUpdates();
            modules.forEach(UpdateModule::resetState);
            return false;
        }
        return true;
    }

}
