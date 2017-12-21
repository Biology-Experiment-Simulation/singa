package de.bioforscher.singa.simulation.modules.model;

import de.bioforscher.singa.chemistry.descriptive.entities.ChemicalEntity;
import de.bioforscher.singa.features.model.ScalableFeature;
import de.bioforscher.singa.simulation.model.compartments.CellSection;
import de.bioforscher.singa.simulation.model.concentrations.ConcentrationContainer;
import de.bioforscher.singa.simulation.model.concentrations.Delta;
import de.bioforscher.singa.simulation.model.graphs.AutomatonGraph;
import de.bioforscher.singa.simulation.model.graphs.AutomatonNode;
import tec.units.ri.quantity.Quantities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import static de.bioforscher.singa.features.units.UnitProvider.MOLE_PER_LITRE;

/**
 * @author cl
 */
public abstract class AbstractNeighbourIndependentModule implements Module {

    protected boolean halfTime;

    private Simulation simulation;
    private Map<Function<ConcentrationContainer, Delta>, Predicate<ConcentrationContainer>> deltaFunctions;

    private Predicate<AutomatonNode> conditionalApplication;

    private LocalError largestLocalError;

    private AutomatonNode currentNode;
    private ChemicalEntity currentChemicalEntity;
    private CellSection currentCellSection;
    private List<Delta> currentFullDeltas;
    private List<Delta> currentHalfDeltas;
    private ConcentrationContainer currentHalfConcentrations;

    public AbstractNeighbourIndependentModule() {
        this.deltaFunctions = new HashMap<>();
        this.currentFullDeltas = new ArrayList<>();
        this.currentHalfDeltas = new ArrayList<>();
        this.largestLocalError = LocalError.MINIMAL_EMPTY_ERROR;
    }

    public AbstractNeighbourIndependentModule(Simulation simulation) {
        this();
        this.simulation = simulation;
    }

    public Simulation getSimulation() {
        return simulation;
    }

    public void setSimulation(Simulation simulation) {
        this.simulation = simulation;
    }

    public void addDeltaFunction(Function<ConcentrationContainer, Delta> deltaFunction, Predicate<ConcentrationContainer> predicate) {
        this.deltaFunctions.put(deltaFunction, predicate);
    }

    public AutomatonNode getCurrentNode() {
        return this.currentNode;
    }

    public ChemicalEntity getCurrentChemicalEntity() {
        return this.currentChemicalEntity;
    }

    public CellSection getCurrentCellSection() {
        return this.currentCellSection;
    }

    public void onlyApplyIf(Predicate<AutomatonNode> predicate) {
        this.conditionalApplication = predicate;
    }

    public void applyAlways() {
        this.conditionalApplication = bioNode -> true;
    }

    protected <FeatureContent> FeatureContent getFeature(ChemicalEntity<?> entity, Class<? extends ScalableFeature<FeatureContent>> featureClass) {
        ScalableFeature<FeatureContent> feature = entity.getFeature(featureClass);
        if (this.halfTime) {
            return feature.getHalfScaledQuantity();
        }
        return feature.getScaledQuantity();
    }

    public void determineAllDeltas() {
        AutomatonGraph graph = this.simulation.getGraph();
        // determine deltas
        for (AutomatonNode node : graph.getNodes()) {
            if (this.conditionalApplication.test(node)) {
                determineDeltasForNode(node);
            }
        }
    }

    public LocalError determineDeltasForNode(AutomatonNode node) {
        this.currentNode = node;
        ConcentrationContainer fullConcentrations = node.getConcentrationContainer();
        this.currentHalfConcentrations = fullConcentrations.getCopy();
        return determineDeltas(fullConcentrations);
    }

    public LocalError determineDeltas(ConcentrationContainer concentrationContainer) {
        this.halfTime = false;
        for (CellSection cellSection : this.currentNode.getAllReferencedSections()) {
            this.currentCellSection = cellSection;
            for (ChemicalEntity chemicalEntity : this.currentNode.getAllReferencedEntities()) {
                this.currentChemicalEntity = chemicalEntity;
                determineFullDeltas(concentrationContainer);
            }
        }

        this.halfTime = true;
        for (CellSection cellSection : this.currentNode.getAllReferencedSections()) {
            this.currentCellSection = cellSection;
            for (ChemicalEntity chemicalEntity : this.currentNode.getAllReferencedEntities()) {
                this.currentChemicalEntity = chemicalEntity;
                determineHalfDeltas(concentrationContainer);

            }
        }
        // examine local errors
        examineLocalError();
        return this.largestLocalError;
    }

    private void determineFullDeltas(ConcentrationContainer concentrationContainer) {
        // determine full step deltas and half step concentrations
        for (Map.Entry<Function<ConcentrationContainer, Delta>, Predicate<ConcentrationContainer>> entry : this.deltaFunctions.entrySet()) {
            if (entry.getValue().test(concentrationContainer)) {
                Delta fullDelta = entry.getKey().apply(concentrationContainer);
                setHalfStepConcentration(fullDelta);
                this.currentFullDeltas.add(fullDelta);
            }
        }
    }

    private void determineHalfDeltas(ConcentrationContainer concentrationContainer) {
        // determine half step deltas
        for (Map.Entry<Function<ConcentrationContainer, Delta>, Predicate<ConcentrationContainer>> entry : this.deltaFunctions.entrySet()) {
            if (entry.getValue().test(concentrationContainer)) {
                Delta halfDelta = entry.getKey().apply(this.currentHalfConcentrations).multiply(2.0);
                this.currentHalfDeltas.add(halfDelta);
            }
        }
        // and register potential deltas at node
        this.currentNode.addPotentialDeltas(this.currentHalfDeltas);
    }

    private void examineLocalError() {
        // only if there is any change there can be a local error
        // careful we rely on putting the deltas in the same order as they are referenced in the list of delta functions
        double temporaryLargestLocalError = -Double.MAX_VALUE;
        for (int i = 0; i < this.currentFullDeltas.size(); i++) {
            double fullDelta = this.currentFullDeltas.get(i).getQuantity().getValue().doubleValue();
            double halfDelta = this.currentHalfDeltas.get(i).getQuantity().getValue().doubleValue();
            double localError = 0.0;
            // if there is no change, there is no error
            if (fullDelta != 0.0 && halfDelta != 0) {
                // calculate error
                localError = Math.abs(1 - (fullDelta / halfDelta));
            }
            // determine the largest error in the current deltas
            if (temporaryLargestLocalError < localError) {
                temporaryLargestLocalError = localError;
            }
        }
        // compare to current maximum
        if (this.largestLocalError.getValue() < temporaryLargestLocalError) {
            // set if this is larger
            this.largestLocalError = new LocalError(this.currentNode, this.currentChemicalEntity, temporaryLargestLocalError);
        }
        // clear used deltas
        this.currentFullDeltas.clear();
        this.currentHalfDeltas.clear();
    }

    private void setHalfStepConcentration(Delta fullDelta) {
        final double fullConcentration = this.currentNode.getAvailableConcentration(this.currentChemicalEntity, this.currentCellSection).getValue().doubleValue();
        final double halfStepConcentration = fullConcentration + 0.5 * fullDelta.getQuantity().getValue().doubleValue();
        this.currentHalfConcentrations.setAvailableConcentration(this.currentCellSection, this.currentChemicalEntity, Quantities.getQuantity(halfStepConcentration, MOLE_PER_LITRE));
    }

    @Override
    public LocalError getLargestLocalError() {
        return this.largestLocalError;
    }

    @Override
    public void resetLargestLocalError() {
        this.largestLocalError = LocalError.MINIMAL_EMPTY_ERROR;
    }
}