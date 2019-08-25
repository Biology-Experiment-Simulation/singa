package bio.singa.structure.model.mmcif;

import bio.singa.structure.model.families.NucleotideFamily;
import bio.singa.structure.model.identifiers.LeafIdentifier;
import bio.singa.structure.model.interfaces.Nucleotide;
import org.rcsb.cif.model.Block;

/**
 * @author cl
 */
public class MmcifNucleotide extends MmcifLeafSubstructure<NucleotideFamily> implements Nucleotide {

    public MmcifNucleotide(Block data, LeafIdentifier leafIdentifier, NucleotideFamily structuralFamily, int initialIndex, int endIndex) {
        super(data, leafIdentifier, structuralFamily, initialIndex, endIndex);
    }

}
