package de.bioforscher.singa.simulation.modules.transport;

import de.bioforscher.singa.chemistry.descriptive.entities.ChemicalEntity;
import de.bioforscher.singa.chemistry.descriptive.features.permeability.MembranePermeability;
import de.bioforscher.singa.features.parameters.EnvironmentalParameters;
import de.bioforscher.singa.simulation.model.compartments.NodeState;
import de.bioforscher.singa.simulation.model.concentrations.ConcentrationContainer;
import de.bioforscher.singa.simulation.model.concentrations.MembraneContainer;
import de.bioforscher.singa.simulation.modules.model.AbstractNeighbourIndependentModule;
import de.bioforscher.singa.simulation.modules.model.Delta;
import de.bioforscher.singa.simulation.modules.model.Simulation;
import tec.uom.se.quantity.Quantities;

public class MembraneDiffusion extends AbstractNeighbourIndependentModule {

    private ChemicalEntity cargo;

    public MembraneDiffusion(Simulation simulation, ChemicalEntity cargo) {
        super(simulation);
        this.cargo = cargo;
        // apply this module only to membranes
        onlyApplyIf(node -> node.getState().equals(NodeState.MEMBRANE));
        // change of inner phase
        addDeltaFunction(this::calculateInnerPhaseDelta, this::onlyInnerPhase);
        // change of outer phase
        addDeltaFunction(this::calculateOuterPhaseDelta, this::onlyOuterPhase);
    }

    private Delta calculateOuterPhaseDelta(ConcentrationContainer concentrationContainer) {
        final ChemicalEntity entity = getCurrentChemicalEntity();
        final MembraneContainer membraneContainer = (MembraneContainer) concentrationContainer;
        double value;
        if (entity.equals(cargo)) {
            final double permeability = getFeature(cargo, MembranePermeability.class).getValue().doubleValue();
            value = getCargoDelta(membraneContainer) * permeability;
        } else {
            value = 0.0;
        }
        return new Delta(this, membraneContainer.getOuterPhaseSection(), entity, Quantities.getQuantity(value, EnvironmentalParameters.getTransformedMolarConcentration()));
    }

    private Delta calculateInnerPhaseDelta(ConcentrationContainer concentrationContainer) {
        final ChemicalEntity entity = getCurrentChemicalEntity();
        final MembraneContainer membraneContainer = (MembraneContainer) concentrationContainer;
        double value;
        if (entity.equals(cargo)) {
            final double permeability = getFeature(cargo, MembranePermeability.class).getValue().doubleValue();
            value = getCargoDelta(membraneContainer) * permeability;
        } else {
            value = 0.0;
        }
        return new Delta(this, membraneContainer.getInnerPhaseSection(), entity, Quantities.getQuantity(value, EnvironmentalParameters.getTransformedMolarConcentration()));
    }

    private double getCargoDelta(MembraneContainer container) {
        // sum outer solutes
        double outerConcentration = container.getOuterPhaseConcentration(cargo).getValue().doubleValue();
        // sum inner solutes
        double innerConcentration = container.getInnerPhaseConcentration(cargo).getValue().doubleValue();
        // return delta
        return isInnerPhase(container) ? outerConcentration - innerConcentration : innerConcentration - outerConcentration;
    }

    /**
     * Only apply, if this is the outer phase, the outer phase contains the cargo and the inner layer contains the
     * transporter.
     *
     * @param concentrationContainer
     * @return
     */
    private boolean onlyOuterPhase(ConcentrationContainer concentrationContainer) {
        MembraneContainer container = (MembraneContainer) concentrationContainer;
        return isOuterPhase(container) && isCargo();
    }

    /**
     * Only apply, if this is the inner phase, the outer phase contains the cargo and the inner layer contains the
     * transporter.
     *
     * @param concentrationContainer
     * @return
     */
    private boolean onlyInnerPhase(ConcentrationContainer concentrationContainer) {
        MembraneContainer container = (MembraneContainer) concentrationContainer;
        return isInnerPhase(container) && isCargo();
    }

    private boolean isOuterPhase(MembraneContainer container) {
        return getCurrentCellSection().equals(container.getOuterPhaseSection());
    }

    private boolean isInnerPhase(MembraneContainer container) {
        return getCurrentCellSection().equals(container.getInnerPhaseSection());
    }

    private boolean isCargo() {
        return getCurrentChemicalEntity().equals(cargo);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()+" ("+cargo.getName()+")";
    }

}