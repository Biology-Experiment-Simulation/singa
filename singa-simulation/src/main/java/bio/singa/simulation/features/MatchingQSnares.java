package bio.singa.simulation.features;

import bio.singa.chemistry.entities.ChemicalEntity;
import bio.singa.features.model.Evidence;

import java.util.List;
import java.util.Set;

/**
 * @author cl
 */
public class MatchingQSnares extends MultiEntityFeature {

    public MatchingQSnares(Set<ChemicalEntity> chemicalEntities, List<Evidence> evidence) {
        super(chemicalEntities, evidence);
    }

    public MatchingQSnares(Set<ChemicalEntity> chemicalEntities, Evidence evidence) {
        super(chemicalEntities, evidence);
    }

    public MatchingQSnares(Set<ChemicalEntity> chemicalEntities) {
        super(chemicalEntities);
    }

}
