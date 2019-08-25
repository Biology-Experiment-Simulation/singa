package bio.singa.structure.model.mmcif;

import bio.singa.structure.model.families.LigandFamily;
import bio.singa.structure.model.identifiers.LeafIdentifier;
import bio.singa.structure.model.interfaces.Ligand;
import org.rcsb.cif.model.Block;

/**
 * @author cl
 */
public class MmcifLigand extends MmcifLeafSubstructure<LigandFamily> implements Ligand {

    public MmcifLigand(Block data, LeafIdentifier leafIdentifier, LigandFamily structuralFamily, int initialIndex, int endIndex) {
        super(data, leafIdentifier, structuralFamily, initialIndex, endIndex);
    }
}
