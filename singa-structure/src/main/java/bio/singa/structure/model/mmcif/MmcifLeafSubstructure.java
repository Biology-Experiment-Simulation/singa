package bio.singa.structure.model.mmcif;

import bio.singa.structure.model.families.StructuralFamily;
import bio.singa.structure.model.identifiers.LeafIdentifier;
import bio.singa.structure.model.interfaces.Atom;
import bio.singa.structure.model.interfaces.LeafSubstructure;
import org.rcsb.cif.model.Block;

import java.util.List;
import java.util.Optional;

/**
 * @author cl
 */
public class MmcifLeafSubstructure<FamilyType extends StructuralFamily> implements LeafSubstructure<FamilyType> {

    private Block data;
    private LeafIdentifier leafIdentifier;
    private FamilyType structuralFamily;
    private int initialIndex;
    private int endINdex;

    public MmcifLeafSubstructure(Block data, LeafIdentifier leafIdentifier, FamilyType structuralFamily, int initialIndex, int endIndex) {
        this.data = data;
        this.leafIdentifier = leafIdentifier;
        this.structuralFamily = structuralFamily;
        this.initialIndex = initialIndex;
        this.endINdex = endIndex;
    }

    @Override
    public LeafIdentifier getIdentifier() {
        return leafIdentifier;
    }

    @Override
    public boolean containsAtomWithName(String atomName) {
        return false;
    }

    @Override
    public Optional<Atom> getAtomByName(String atomName) {
        return Optional.empty();
    }

    @Override
    public boolean isAnnotatedAsHeteroAtom() {
        return false;
    }

    @Override
    public String getThreeLetterCode() {
        return null;
    }

    @Override
    public <LeafImplementation extends LeafSubstructure> LeafImplementation getCopy() {
        return null;
    }

    @Override
    public List<Atom> getAllAtoms() {
        return null;
    }

    @Override
    public Optional<Atom> getAtom(Integer atomIdentifier) {
        return Optional.empty();
    }

    @Override
    public void removeAtom(Integer atomIdentifier) {

    }

    @Override
    public FamilyType getFamily() {
        return null;
    }
}
