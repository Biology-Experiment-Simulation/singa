package bio.singa.structure.model.mmtf;

import bio.singa.mathematics.vectors.Vector3D;
import bio.singa.structure.model.families.AminoAcidFamily;
import bio.singa.structure.model.identifiers.LeafIdentifier;
import bio.singa.structure.model.interfaces.AminoAcid;
import bio.singa.structure.model.interfaces.Atom;
import bio.singa.structure.model.oak.AtomName;
import org.rcsb.mmtf.api.StructureDataInterface;

import java.util.HashSet;
import java.util.Optional;

/**
 * The implementation of {@link AminoAcid}s for mmtf structures.
 *
 * @author cl
 */
public class MmtfAminoAcid extends MmtfLeafSubstructure<AminoAcidFamily> implements AminoAcid {

    /**
     * The DSSP-annotated secondary structure
     */
    private final MmtfSecondaryStructure secondaryStructure;

    /**
     * Creates a new {@link MmtfAminoAcid}.
     *
     * @param data The original data.
     * @param family The leaf family (e.g. {@link AminoAcidFamily#ALANINE}).
     * @param leafIdentifier The leaf identifier.
     * @param internalGroupIndex The index of this leaf in the data array.
     * @param atomStartIndex The index of the first atom that belong to this leaf.
     * @param atomEndIndex The index of the last atom that belong to this leaf.
     */
    MmtfAminoAcid(StructureDataInterface data, byte[] bytes, AminoAcidFamily family, MmtfSecondaryStructure secondaryStructure, LeafIdentifier leafIdentifier, int internalGroupIndex, int atomStartIndex, int atomEndIndex) {
        super(data, bytes, family, leafIdentifier, internalGroupIndex, atomStartIndex, atomEndIndex);
        this.secondaryStructure = secondaryStructure;
    }

    /**
     * A copy constructor that passes all attributes of the given {@link MmtfLeafSubstructure} to a new instance.
     *
     * @param mmtfLeafSubstructure The {@link MmtfLeafSubstructure} to copy.
     */
    private MmtfAminoAcid(MmtfAminoAcid mmtfLeafSubstructure) {
        super(mmtfLeafSubstructure);
        family = mmtfLeafSubstructure.family;
        secondaryStructure = mmtfLeafSubstructure.secondaryStructure;
        exchangeableFamilies = new HashSet<>(mmtfLeafSubstructure.exchangeableFamilies);
    }

    @Override
    public boolean isAnnotatedAsHeteroAtom() {
        return false;
    }

    public MmtfSecondaryStructure getSecondaryStructure() {
        return secondaryStructure;
    }

    @Override
    public Vector3D getPosition() {
        Optional<Atom> optionalAlphaCarbon = getAtomByName(AtomName.CA.getName());
        return optionalAlphaCarbon.map(Atom::getPosition).orElseGet(super::getPosition);
    }

    @Override
    public AminoAcid getCopy() {
        return new MmtfAminoAcid(this);
    }

}
