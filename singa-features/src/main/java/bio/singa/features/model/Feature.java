package bio.singa.features.model;

import java.util.List;

/**
 *
 * @author cl
 */
public interface Feature<FeatureContent> {

    FeatureContent getContent();

    Evidence getPrimaryEvidence();

    List<Evidence> getAllEvidence();

    void addEvidence(Evidence evidence);

    String getDescriptor();

}
