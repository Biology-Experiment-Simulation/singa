package bio.singa.chemistry.annotations;

/**
 * The {@link AnnotationType} provides a primary identification criteria for {@link Annotation}s.
 *
 * @author cl
 */
public enum AnnotationType {

    NOTE("Note"),
    AMINO_ACID_SEQUENCE("Sequence"),
    NAME("Additional Name"),
    ORGANISM("Organism"),
    GO_TERM("GO"),
    ADDITIONAL_IDENTIFIER("Additional Identifier");

    private final String outputString;

    AnnotationType(String outputString) {
        this.outputString = outputString;
    }

    @Override
    public String toString() {
        return outputString;
    }
}
