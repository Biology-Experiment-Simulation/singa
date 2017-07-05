package de.bioforscher.singa.chemistry.descriptive.features.diffusivity;

import de.bioforscher.singa.chemistry.descriptive.features.molarmass.MolarMass;
import de.bioforscher.singa.features.model.Correlation;
import de.bioforscher.singa.features.model.FeatureOrigin;
import de.bioforscher.singa.features.model.Featureable;
import de.bioforscher.singa.features.parameters.EnvironmentalParameters;
import tec.units.ri.quantity.Quantities;

import javax.measure.Quantity;
import javax.measure.quantity.Dimensionless;

import static de.bioforscher.singa.chemistry.descriptive.features.diffusivity.Diffusivity.SQUARE_CENTIMETER_PER_SECOND;
import static tec.units.ri.AbstractUnit.ONE;

/**
 * @author cl
 */
public class YoungCorrelation implements Correlation<Diffusivity> {

    private static final FeatureOrigin origin = new FeatureOrigin(FeatureOrigin.OriginType.PREDICTION,
            "Young Correlation",
            "Young, M. E., P. A. Carroad, and R. L. Bell. \"Estimation of diffusion coefficients " +
                    "of proteins.\" Biotechnology and Bioengineering 22.5 (1980): 947-955.");

    /**
     * Diffusion calculation coefficient [dimensionless] (8.34e-8 = 0.0000000834)
     */
    private static final Quantity<Dimensionless> YOUNG_COEFFICIENT = Quantities.getQuantity(8.34e-8, ONE);

    /**
     * Estimates the diffusion coefficient. Always returns cm^2/s.
     *
     * @param featureable The entity to be annotated.
     * @param <FeaturableType> The type of the feature.
     * @return The Diffusivity of the entity in cm^2/s.
     */
    @Override
    public  <FeaturableType extends Featureable> Diffusivity predict(FeaturableType featureable) {
        final double molarMass = featureable.getFeature(MolarMass.class).getValue().doubleValue();
        // D = coefficient * (T/n*M^1/3)
        final double diffusivity = YOUNG_COEFFICIENT.getValue().doubleValue()
                * (EnvironmentalParameters.getInstance().getSystemTemperature().getValue().doubleValue()
                / (EnvironmentalParameters.getInstance().getSystemViscosity().getValue().doubleValue() * Math.cbrt(molarMass)));
        final Quantity<Diffusivity> quantity = Quantities.getQuantity(diffusivity, SQUARE_CENTIMETER_PER_SECOND);
        return new Diffusivity(quantity, origin);
    }


}