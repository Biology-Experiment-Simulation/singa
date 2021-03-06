package bio.singa.simulation.model.sections;

import bio.singa.features.identifiers.GoTerm;
import bio.singa.simulation.model.simulation.Updatable;

import java.util.Objects;

/**
 * A cell subsection organizes the contents of a {@link Updatable}. Each subsection has its own {@link ConcentrationPool}
 * and behaves independently of other subsections in the same {@link CellRegion}.
 *
 * @author cl
 */
public class CellSubsection {

    /**
     * A general membrane subsection.
     */
    public static CellSubsection MEMBRANE = new CellSubsection("MEM");

    /**
     * A cellular subsection "A".
     */
    public static CellSubsection SECTION_A = new CellSubsection("SA");

    /**
     * A cellular subsection "B".
     */
    public static CellSubsection SECTION_B = new CellSubsection("SB");

    /**
     * The identifier.
     */
    private String identifier;

    private GoTerm goTerm;

    /**
     * Creates a new cell subsection with the given identifier and dynamic preferred concentration unit.
     *
     * @param identifier The identifier.
     */
    public CellSubsection(String identifier) {
        this.identifier = identifier;
    }

    public CellSubsection(String identifier, GoTerm goTerm) {
        this.identifier = identifier;
        this.goTerm = goTerm;
    }

    /**
     * Returns the identifier.
     *
     * @return The identifier.
     */
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public GoTerm getGoTerm() {
        return goTerm;
    }

    public void setGoTerm(GoTerm goTerm) {
        this.goTerm = goTerm;
    }

    @Override
    public String toString() {
        return identifier;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CellSubsection that = (CellSubsection) o;
        return Objects.equals(identifier, that.identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier);
    }

}
