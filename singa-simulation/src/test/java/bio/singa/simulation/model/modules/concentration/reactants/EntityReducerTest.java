package bio.singa.simulation.model.modules.concentration.reactants;

import bio.singa.chemistry.entities.ChemicalEntity;
import bio.singa.chemistry.entities.SmallMolecule;
import bio.singa.chemistry.entities.ComplexEntity;
import bio.singa.simulation.model.modules.concentration.imlementations.reactions.behaviors.reactants.EntityReducer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author cl
 */
class EntityReducerTest {

    @Test
    @DisplayName("entity collection reduction")
    void shouldReduceCorrectly() {

        SmallMolecule a = SmallMolecule.create("A").build();
        SmallMolecule b = SmallMolecule.create("B").build();
        SmallMolecule c = SmallMolecule.create("C").build();
        SmallMolecule d = SmallMolecule.create("D").build();
        SmallMolecule e = SmallMolecule.create("E").build();

        ComplexEntity ab = ComplexEntity.from(a, b);
        ComplexEntity abc = ComplexEntity.from(ab, c);
        ComplexEntity abcd = ComplexEntity.from(abc, d);
        ComplexEntity abcde = ComplexEntity.from(abcd, e);

        List<ChemicalEntity> firstTest = new ArrayList<>();
        firstTest.add(a);
        firstTest.add(ab);
        firstTest.add(abc);
        firstTest.add(abcde);
        firstTest.add(b);

        List<ChemicalEntity> firstResult = EntityReducer.apply(firstTest, EntityReducer.hasPart(ab), EntityReducer.hasNotPart(e));
        assertTrue(firstResult.contains(ab));
        assertTrue(firstResult.contains(abc));
        assertFalse(firstResult.contains(a));
        assertFalse(firstResult.contains(abcde));
        assertFalse(firstResult.contains(b));

    }
}