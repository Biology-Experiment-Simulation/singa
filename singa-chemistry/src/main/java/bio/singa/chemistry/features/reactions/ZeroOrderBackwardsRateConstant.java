package bio.singa.chemistry.features.reactions;

import bio.singa.features.model.Evidence;

import javax.measure.Quantity;
import javax.measure.Unit;

/**
 * @author cl
 */
public class ZeroOrderBackwardsRateConstant extends ZeroOrderRateConstant implements BackwardsRateConstant<ZeroOrderRate> {

    public ZeroOrderBackwardsRateConstant(Quantity<ZeroOrderRate> zeroOrderRateQuantity, Evidence evidence) {
        super(zeroOrderRateQuantity, evidence);
    }

    public ZeroOrderBackwardsRateConstant(double value, Unit<ZeroOrderRate> unit, Evidence evidence) {
        super(value, unit, evidence);
    }

    public ZeroOrderBackwardsRateConstant(Quantity<ZeroOrderRate> quantity) {
        super(quantity);
    }

}
