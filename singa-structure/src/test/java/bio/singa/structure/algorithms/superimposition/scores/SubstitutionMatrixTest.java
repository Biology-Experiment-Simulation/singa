package bio.singa.structure.algorithms.superimposition.scores;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author cl
 */
public class SubstitutionMatrixTest {

    @Test
    public void getMatrix() {
        assertNotNull(SubstitutionMatrix.BLOSUM_45.getMatrix());
    }

    @Test
    public void getAllMatrices() {
        Set<String> stringComparison = new HashSet<>();
        for (SubstitutionMatrix substitutionMatrix : SubstitutionMatrix.values()) {
            stringComparison.add(substitutionMatrix.getMatrix().getStringRepresentation());
        }
        assertEquals(SubstitutionMatrix.values().length, stringComparison.size());
    }
}