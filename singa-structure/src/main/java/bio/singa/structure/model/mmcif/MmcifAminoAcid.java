package bio.singa.structure.model.mmcif;

import bio.singa.structure.model.families.AminoAcidFamily;
import bio.singa.structure.model.identifiers.LeafIdentifier;
import bio.singa.structure.model.interfaces.AminoAcid;
import org.rcsb.cif.model.Block;

/**
 * @author cl
 */
public class MmcifAminoAcid extends MmcifLeafSubstructure<AminoAcidFamily> implements AminoAcid {

    public MmcifAminoAcid(Block data, LeafIdentifier leafIdentifier, AminoAcidFamily structuralFamily, int initialIndex, int endIndex) {
        super(data, leafIdentifier, structuralFamily, initialIndex, endIndex);
    }
}
