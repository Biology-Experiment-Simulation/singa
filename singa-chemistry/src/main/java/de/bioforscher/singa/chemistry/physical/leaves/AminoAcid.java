package de.bioforscher.singa.chemistry.physical.leaves;

import de.bioforscher.singa.chemistry.physical.atoms.Atom;
import de.bioforscher.singa.chemistry.physical.atoms.AtomName;
import de.bioforscher.singa.chemistry.physical.families.AminoAcidFamily;
import de.bioforscher.singa.chemistry.physical.model.LeafIdentifier;
import de.bioforscher.singa.mathematics.vectors.Vector3D;

/**
 * A residue is a grouping element that should only contain atoms. Each and every residue has a associate ResidueType,
 * that determines the amino acid (and the overarching features).
 *
 * @author cl
 */
public class AminoAcid extends LeafSubstructure<AminoAcid, AminoAcidFamily> {

    private final boolean modified;
    private final String modifiedName;

    /**
     * Creates a new AminoAcid.
     *
     * @param leafIdentifier The pdbIdentifier.
     * @param family         The ResidueType.
     */
    public AminoAcid(LeafIdentifier leafIdentifier, AminoAcidFamily family) {
        super(leafIdentifier, family);
        this.modified = false;
        this.modifiedName = null;
    }

    public AminoAcid(int identifer, AminoAcidFamily family) {
        this(new LeafIdentifier(identifer), family);
    }

    /**
     * Creates a new modified AminoAcid with a pdbIdentifier and ResidueType.
     *
     * @param leafIdentifier The pdbIdentifier.
     * @param family         The ResidueType.
     *                       @param modifiedName The name of the modified amino acid.
     */
    public AminoAcid(LeafIdentifier leafIdentifier, AminoAcidFamily family, String modifiedName) {
        super(leafIdentifier, family);
        this.modified = true;
        this.modifiedName = modifiedName;
    }

    /**
     * This is a copy constructor. Creates a new aminoAcid with the same attributes as the given aminoAcid. This
     * also recursively creates copies of all the underlying substructures and atoms. The neighbours of this
     * substructure are NOT copied. Due to the nature of this operation it would be bad to keep a part of the relations
     * to the lifecycle of the substructure to copy. If you want to keep the neighbouring substructures, copy the
     * superordinate substructure that contains this substructure and it will also traverse and copy the neighbouring
     * substructures.
     *
     * @param aminoAcid The aminoAcid to copy
     */
    public AminoAcid(AminoAcid aminoAcid) {
        super(aminoAcid);
        this.modified = aminoAcid.modified;
        this.modifiedName = aminoAcid.modifiedName;
    }

    /**
     * Returns the one letter code of this residue.
     *
     * @return The one letter code of this residue.
     */
    public String getOneLetterCode() {
        return getFamily().getOneLetterCode();
    }

    /**
     * Returns the three letter code of this residue.
     *
     * @return The three letter code of this residue.
     */
    public String getThreeLetterCode() {
        return this.modified ? this.modifiedName : getFamily().getThreeLetterCode();
    }

    public boolean isModified() {
        return this.modified;
    }

    /**
     * Returns the {@link AtomName#CA alpha carbon} (carbon with the side cain attached).
     *
     * @return The C alpha carbon.
     */
    public Atom getAlphaCarbon() {
        return getAtomByName(AtomName.CA);
    }

    /**
     * Returns the {@link AtomName#CB beta carbon} (first carbon of the side cain).
     *
     * @return The C beta carbon.
     */
    public Atom getBetaCarbon() {
        return getAtomByName(AtomName.CB);
    }

    /**
     * Returns the {@link AtomName#C backbone carbon} (carbon in the backbone).
     *
     * @return The backbone carbon.
     */
    public Atom getBackboneCarbon() {
        return getAtomByName(AtomName.C);
    }

    /**
     * Returns the {@link AtomName#N backbone nitrogen} (nitrogen in the backbone).
     *
     * @return The backbone nitrogen.
     */
    public Atom getBackboneNitrogen() {
        return getAtomByName(AtomName.N);
    }

    /**
     * Returns the {@link AtomName#O backbone oxygen} (oxygen in the backbone).
     *
     * @return The backbone oxygen.
     */
    public Atom getBackboneOxygen() {
        return getAtomByName(AtomName.O);
    }

    /**
     * Returns a copy of this residue.
     *
     * @return A copy of this residue.
     */
    @Override
    public AminoAcid getCopy() {
        return new AminoAcid(this);
    }

    /**
     * Returns the name (i.e. the three letter code) of this residue.
     *
     * @return The three letter code.
     */
    @Override
    public String getName() {
        return this.getThreeLetterCode();
    }

    /**
     * Moves all atoms in this residue, such that the centroid of this residue is at the specified position.
     *
     * @param position The new centroid position.
     */
    @Override
    public void setPosition(Vector3D position) {
        //FIXME not yet implemented
        throw new UnsupportedOperationException();
    }
}