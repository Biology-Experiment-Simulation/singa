package bio.singa.chemistry.features.logp;

import bio.singa.chemistry.features.FeatureProviderRegistry;
import bio.singa.features.model.AbstractFeature;
import bio.singa.features.model.Evidence;

/**
 * @author cl
 */
public class LogP extends AbstractFeature<Double> {

    public LogP(Double value, Evidence evidence) {
        super(value, evidence);
    }

    public static void register() {
        FeatureProviderRegistry.addProviderForFeature(LogP.class, LogPProvider.class);
    }

}
