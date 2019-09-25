package bio.singa.simulation.model.sections.nconcentrations;

import bio.singa.simulation.model.simulation.Updatable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author cl
 */
public class NodeIdentifierCondition extends AbstractConcentrationCondition {

    public static NodeIdentifierCondition forIdentifiers(List<String> identifiers) {
        return new NodeIdentifierCondition(identifiers);
    }

    public static NodeIdentifierCondition forIdentifier(String identifier) {
        return forIdentifiers(Collections.singletonList(identifier));
    }

    public static NodeIdentifierCondition forIdentifiers(String... identifiers) {
        return forIdentifiers(Arrays.asList(identifiers));
    }

    private List<String> identifiers;

    private NodeIdentifierCondition(List<String> identifiers) {
        super(10);
        this.identifiers = identifiers;
    }

    @Override
    public boolean test(Updatable updatable) {
        return identifiers.contains(updatable.getStringIdentifier());
    }

    @Override
    public String toString() {
        return "updatable has one of identifiers: " + identifiers;
    }

}