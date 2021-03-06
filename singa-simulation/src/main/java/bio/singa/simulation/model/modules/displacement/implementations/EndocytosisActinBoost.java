package bio.singa.simulation.model.modules.displacement.implementations;

import bio.singa.features.parameters.Environment;
import bio.singa.features.quantities.MolarConcentration;
import bio.singa.features.units.UnitRegistry;
import bio.singa.mathematics.vectors.Vector2D;
import bio.singa.simulation.features.ActinBoostVelocity;
import bio.singa.simulation.features.BoostMediatingEntity;
import bio.singa.simulation.model.agents.pointlike.Vesicle;
import bio.singa.simulation.model.agents.pointlike.VesicleStateRegistry;
import bio.singa.simulation.model.modules.displacement.DisplacementBasedModule;
import bio.singa.simulation.model.modules.displacement.DisplacementDelta;
import bio.singa.simulation.model.sections.CellTopology;
import tec.units.indriya.quantity.Quantities;

import javax.measure.Quantity;
import javax.measure.quantity.Length;

/**
 * @author cl
 */
public class EndocytosisActinBoost extends DisplacementBasedModule {

    private double scaledVelocity;

    public EndocytosisActinBoost() {
        // delta function
        addDeltaFunction(this::calculateDisplacement, vesicle -> vesicle.getState().equals(VesicleStateRegistry.ACTIN_PROPELLED));
        // feature
        getRequiredFeatures().add(ActinBoostVelocity.class);
        getRequiredFeatures().add(BoostMediatingEntity.class);
    }

    @Override
    public void calculateUpdates() {
        scaledVelocity = getScaledFeature(ActinBoostVelocity.class) * 2.0 / 60.0;
        super.calculateUpdates();
    }

    public DisplacementDelta calculateDisplacement(Vesicle vesicle) {
        BoostMediatingEntity decayingEntity = getFeature(BoostMediatingEntity.class);
        // calculate speed based on clathrins available
        double pullingEntity = MolarConcentration.concentrationToMolecules(vesicle.getConcentrationContainer().get(CellTopology.MEMBRANE, decayingEntity.getContent())).getValue().doubleValue();
        if (pullingEntity < 1) {
            vesicle.setState(VesicleStateRegistry.UNATTACHED);
        }
        double systemSpeed = scaledVelocity * pullingEntity;
        Quantity<Length> distance = Quantities.getQuantity(systemSpeed, UnitRegistry.getSpaceUnit());
        // determine direction
        Vector2D centre = simulation.getMembraneLayer().getMicrotubuleOrganizingCentre().getCircleRepresentation().getMidpoint();
        Vector2D direction = centre.subtract(vesicle.getPosition()).normalize();
        // determine delta
        Vector2D delta = direction.multiply(Environment.convertSystemToSimulationScale(distance));
        return new DisplacementDelta(this, delta);
    }

}
