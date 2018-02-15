package de.bioforscher.singa.chemistry.descriptive.features.reactions;

import de.bioforscher.singa.features.model.AbstractFeature;
import de.bioforscher.singa.features.model.FeatureOrigin;
import de.bioforscher.singa.features.model.ScalableFeature;
import tec.uom.se.quantity.Quantities;
import tec.uom.se.unit.ProductUnit;

import javax.measure.Quantity;
import javax.measure.quantity.Frequency;
import javax.measure.quantity.Length;
import javax.measure.quantity.Time;

import static tec.uom.se.AbstractUnit.ONE;
import static tec.uom.se.unit.Units.HERTZ;

/**
 * @author cl
 */
public class RateConstant extends AbstractFeature<Quantity<Frequency>> implements ScalableFeature<Quantity<Frequency>> {

    public static final String SYMBOL = "k";

    private Quantity<Frequency> scaledQuantity;
    private Quantity<Frequency> halfScaledQuantity;

    public RateConstant(Quantity<Frequency> frequencyQuantity, FeatureOrigin featureOrigin) {
        super(frequencyQuantity, featureOrigin);
    }

    public RateConstant(double frequency, FeatureOrigin featureOrigin) {
        super(Quantities.getQuantity(frequency, HERTZ), featureOrigin);
    }

    @Override
    public void scale(Quantity<Time> time, Quantity<Length> space) {
        // transform to specified unit
        Quantity<Frequency> scaledQuantity = getFeatureContent()
                .to(new ProductUnit<>(ONE.divide(time.getUnit())));
        // transform to specified amount
        this.scaledQuantity = scaledQuantity.multiply(time.getValue().doubleValue());
        // and half
        halfScaledQuantity = scaledQuantity.multiply(time.multiply(0.5).getValue().doubleValue());
    }

    @Override
    public Quantity<Frequency> getScaledQuantity() {
        return scaledQuantity;
    }

    @Override
    public Quantity<Frequency> getHalfScaledQuantity() {
        return halfScaledQuantity;
    }

    @Override
    public String getSymbol() {
        return SYMBOL;
    }
}
