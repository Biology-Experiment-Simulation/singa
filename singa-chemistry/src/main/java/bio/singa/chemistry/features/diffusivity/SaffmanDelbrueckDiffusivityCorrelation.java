package bio.singa.chemistry.features.diffusivity;

import bio.singa.chemistry.features.structure3d.Radius;
import bio.singa.features.model.Correlation;
import bio.singa.features.model.Evidence;
import bio.singa.features.model.Featureable;
import bio.singa.features.parameters.Environment;
import bio.singa.features.quantities.DynamicViscosity;
import bio.singa.features.quantities.NaturalConstants;
import tech.units.indriya.quantity.Quantities;

import javax.measure.Quantity;
import javax.measure.quantity.Length;

import static java.lang.Math.PI;
import static java.lang.Math.log;
import static tech.units.indriya.unit.MetricPrefix.KILO;
import static tech.units.indriya.unit.MetricPrefix.NANO;
import static tech.units.indriya.unit.Units.*;

/**
 * Dsd = (kb * t)/(4 * pi * um * h) * (ln((2 * ((h * um) / (2 uf)))) / a) - em)
 *
 * where
 * kb is the {@link NaturalConstants#BOLTZMANN_CONSTANT}
 * em is the {@link NaturalConstants#EULER_MASCHERONI_CONSTANT}
 * t is the System Temperature {@link Environment#getTemperature()}
 * pi is {@link Math#PI}
 * um is the viscosity of the membrane
 * uf is the viscosity of the bulk fluid
 * h is the membrane thickness
 * a is the radius of the protein complex
 *
 *
 * @author cl
 */
public class SaffmanDelbrueckDiffusivityCorrelation implements Correlation<MembraneDiffusivity> {

    private static Evidence parameters = new Evidence(Evidence.SourceType.LITERATURE, "Ramadurai 2009", "Ramadurai, Sivaramakrishnan, et al. \"Lateral diffusion of membrane proteins.\" Journal of the American Chemical Society 131.35 (2009): 12650-12656.", "general parameters for the correlation");
    private static Evidence method = new Evidence(Evidence.SourceType.LITERATURE, "Saffman 1975", "Saffman, P. G., and M. Delbrück. \"Brownian motion in biological membranes.\" Proceedings of the National Academy of Sciences 72.8 (1975): 3111-3113.", "correlation method");

    private static Quantity<Length> membraneThickness = Quantities.getQuantity(3.8, NANO(METRE));
    private static Quantity<DynamicViscosity> membraneViscosity = Quantities.getQuantity(0.08, PASCAL.multiply(SECOND).asType(DynamicViscosity.class));
    private static Quantity<DynamicViscosity> bulkViscosity = Quantities.getQuantity(1.003, PASCAL.multiply(SECOND).asType(DynamicViscosity.class));

    @Override
    public <FeatureableType extends Featureable> MembraneDiffusivity predict(FeatureableType featureable) {
        double kb = NaturalConstants.BOLTZMANN_CONSTANT.getValue().doubleValue();
        double em = NaturalConstants.EULER_MASCHERONI_CONSTANT.getValue().doubleValue();
        double t = Environment.getTemperature().getValue().doubleValue();
        double um = membraneViscosity.getValue().doubleValue();
        double uf = bulkViscosity.getValue().doubleValue();
        double h = membraneThickness.to(METRE).getValue().doubleValue();
        // somewhere this is a factor 1000 astray
        Radius radius = featureable.getFeature(Radius.class);
        double a = radius.getContent().to(KILO(METRE)).getValue().doubleValue();

        double lsd = log((h * um) / (a * uf));
        double blzTerm = (kb * t) / (4 * PI * um * h);
        double dsd = blzTerm * (lsd - em);

        MembraneDiffusivity diffusivity = new MembraneDiffusivity(Quantities.getQuantity(dsd, Diffusivity.SQUARE_METRE_PER_SECOND));
        diffusivity.addEvidence(parameters);
        diffusivity.addEvidence(method);
        diffusivity.addEvidence(radius.getPrimaryEvidence());
        return diffusivity;
    }
}