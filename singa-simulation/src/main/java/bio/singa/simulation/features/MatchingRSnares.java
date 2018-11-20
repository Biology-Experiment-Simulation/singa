package bio.singa.simulation.features;

import bio.singa.chemistry.features.MultiEntityFeature;
import bio.singa.chemistry.entities.ChemicalEntity;
import bio.singa.features.model.FeatureOrigin;

import java.util.Set;

/**
 * @author cl
 */
public class MatchingRSnares extends MultiEntityFeature {

    private static final String SYMBOL = "es_RSnares";

    public MatchingRSnares(Set<ChemicalEntity> chemicalEntities, FeatureOrigin featureOrigin) {
        super(chemicalEntities, featureOrigin);
    }

    @Override
    public String getSymbol() {
        return SYMBOL;
    }

}
