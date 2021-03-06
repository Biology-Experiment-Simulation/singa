package bio.singa.simulation.features;

import bio.singa.features.model.Evidence;
import bio.singa.features.model.ScalableQuantitativeFeature;
import tec.units.indriya.quantity.Quantities;
import tec.units.indriya.unit.ProductUnit;

import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.quantity.Speed;

import java.util.List;

import static tec.units.indriya.unit.MetricPrefix.NANO;
import static tec.units.indriya.unit.Units.METRE;
import static tec.units.indriya.unit.Units.SECOND;

/**
 * @author cl
 */
public class MotorMovementVelocity extends ScalableQuantitativeFeature<Speed> {

    private static final Unit<Speed> NANOMETRE_PER_SECOND = new ProductUnit<>(NANO(METRE).divide(SECOND));

    /**
     * Average lateral displacement velocity after scission for 11 seconds.
     */
    public static final MotorMovementVelocity DEFAULT_MOTOR_VELOCITY = new MotorMovementVelocity(Quantities.getQuantity(800.0, NANOMETRE_PER_SECOND), DefaultFeatureSources.EHRLICH2004);

    public MotorMovementVelocity(Quantity<Speed> quantity, List<Evidence> evidence) {
        super(quantity, evidence);
    }

    public MotorMovementVelocity(Quantity<Speed> quantity, Evidence evidence) {
        super(quantity, evidence);
    }

    public MotorMovementVelocity(Quantity<Speed> quantity) {
        super(quantity);
    }

}
