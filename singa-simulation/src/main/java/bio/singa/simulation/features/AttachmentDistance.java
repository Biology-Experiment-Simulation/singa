package bio.singa.simulation.features;

import bio.singa.features.model.Evidence;
import bio.singa.features.model.QuantitativeFeature;
import tec.units.indriya.quantity.Quantities;

import javax.measure.Quantity;
import javax.measure.quantity.Length;
import java.util.List;

import static tec.units.indriya.unit.MetricPrefix.NANO;
import static tec.units.indriya.unit.Units.METRE;

/**
 * @author cl
 */
public class AttachmentDistance extends QuantitativeFeature<Length> {

    /**
     * Size of the dynein complex fom vesicle surface to microtubule.
     */
    public static final AttachmentDistance DEFAULT_DYNEIN_ATTACHMENT_DISTANCE = new AttachmentDistance(Quantities.getQuantity(61, NANO(METRE)), DefaultFeatureSources.JHA2015);

    public AttachmentDistance(Quantity<Length> quantity, List<Evidence> evidence) {
        super(quantity, evidence);
    }

    public AttachmentDistance(Quantity<Length> quantity, Evidence evidence) {
        super(quantity, evidence);
    }

    public AttachmentDistance(Quantity<Length> quantity) {
        super(quantity);
    }

}
