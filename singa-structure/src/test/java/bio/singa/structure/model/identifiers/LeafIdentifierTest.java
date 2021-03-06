package bio.singa.structure.model.identifiers;


import bio.singa.structure.model.interfaces.LeafSubstructure;
import bio.singa.structure.model.interfaces.Structure;
import bio.singa.structure.parser.pdb.structures.StructureParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LeafIdentifierTest {

    private static LeafSubstructure<?> leaf;
    private static Structure structure;

    @BeforeAll
    static void initialize() {
        structure = StructureParser.pdb()
                .pdbIdentifier("2w0l")
                .parse();
        leaf = structure.getAllLeafSubstructures().get(26);
    }

    @Test
    void fromString() {
        LeafIdentifier leafIdentifier = LeafIdentifier.fromString("2w0l-1-A-27A");
        assertEquals(leaf.getIdentifier(), leafIdentifier);
    }

    @Test
    void fromSimpleString() {
        LeafIdentifier leafIdentifier = LeafIdentifier.fromSimpleString("A-27A");
        assertEquals(leaf.getChainIdentifier(), leafIdentifier.getChainIdentifier());
        assertEquals(leaf.getIdentifier().getSerial(), leafIdentifier.getSerial());
        assertEquals(leaf.getIdentifier().getInsertionCode(), leafIdentifier.getInsertionCode());
    }
}
