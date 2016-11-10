package de.bioforscher.chemistry.physical.proteins;

import de.bioforscher.chemistry.physical.atoms.Atom;
import de.bioforscher.chemistry.physical.atoms.AtomName;
import jersey.repackaged.com.google.common.base.Predicates;

import java.util.EnumMap;
import java.util.stream.Collectors;

import static de.bioforscher.chemistry.descriptive.elements.ElementProvider.HYDROGEN;
import static de.bioforscher.chemistry.physical.atoms.AtomName.*;

/**
 * The residue factory is used to create residues from a set of Atoms with their AtomNames. This also connects the atoms
 * in the residues, where possible. No distance criterion is used but the knowledge of the residues and the usually
 * connected atoms. Different options can be set.
 */
public class ResidueFactory {

    /**
     * the factory used to parse all residues.
     */
    private static final ResidueFactory factory = new ResidueFactory();

    /**
     * Tries to saturate the residue with hydrogen atoms if they are in the given map of atoms.
     */
    private boolean connectHydrogens = false;

    /**
     * Omits all hydrogen (and eventually deuterium) atoms. Those atoms are not added to the resulting residue.
     */
    private boolean omitHydrogens = false;

    private ResidueFactory() {
    }

    public static Residue createResidueFromAtoms(int identifier, ResidueType residueType, EnumMap<AtomName, Atom> atoms) {

        // create new residue
        Residue residue = new Residue(identifier, residueType);
        // and add atoms
        if (factory.omitHydrogens) {
            // without hydrogens
            residue.addAllNodes(atoms.values().stream()
                    .filter(atom -> !atom.isHydrogen())
                    .collect(Collectors.toList()));
        } else {
            // all
            residue.addAllNodes(atoms.values());
        }
        // connect backbone atoms first
        connectBackboneAtoms(residue, atoms);

        // TODO maybe order by relative occurrence to speedup
        switch (residueType) {
            case ALANINE: {
                residue.connect(atoms.get(CA), atoms.get(CB));
                break;
            }
            case ARGININE: {
                connectInOrder(residue, atoms,
                        CA, CB, CG, CD, NE, CZ, NH1);
                residue.connect(atoms.get(CZ), atoms.get(NH2));
                break;
            }
            case ASPARAGINE: {
                connectInOrder(residue, atoms,
                        CA, CB, CG, OD1);
                residue.connect(atoms.get(CG), atoms.get(ND2));
                break;
            }
            case ASPARTIC_ACID: {
                connectInOrder(residue, atoms,
                        CA, CB, CG, OD1);
                residue.connect(atoms.get(CG), atoms.get(OD2));
                break;
            }
            case CYSTEINE: {
                connectInOrder(residue, atoms,
                        CA, CB, SG);
                break;
            }
            case GLUTAMINE: {
                connectInOrder(residue, atoms,
                        CA, CB, CG, CD, OE1);
                residue.connect(atoms.get(CD), atoms.get(NE2));
                break;
            }
            case GLUTAMIC_ACID: {
                connectInOrder(residue, atoms,
                        CA, CB, CG, CD, OE1);
                residue.connect(atoms.get(CD), atoms.get(OE2));
                break;
            }
            case GLYCINE: {
                // nothing ...
            }
            case HISTIDINE: {
                connectInOrder(residue, atoms,
                        CA, CB, CG, CD2, NE2, CE1, ND1, CG);
                break;
            }
            case ISOLEUCINE: {
                connectInOrder(residue, atoms,
                        CA, CB, CG1, CD1);
                residue.connect(atoms.get(CB), atoms.get(CG2));
                break;
            }
            case LEUCINE: {
                connectInOrder(residue, atoms,
                        CA, CB, CG, CD1);
                residue.connect(atoms.get(CG), atoms.get(CD2));
                break;
            }
            case LYSINE: {
                connectInOrder(residue, atoms,
                        CA, CB, CG, CD, CE, NZ);
                break;
            }
            case METHIONINE: {
                connectInOrder(residue, atoms,
                        CA, CB, CG, SD, CE);
                break;
            }
            case PHENYLALANINE: {
                connectInOrder(residue, atoms,
                        CA, CB, CG, CD2, CE2, CZ, CE1, CD1, CG);
                break;
            }
            case PROLINE: {
                connectInOrder(residue, atoms,
                        CA, CB, CG, CD, N, CA);
                break;
            }
            case SERINE: {
                connectInOrder(residue, atoms,
                        CA, CB, OG);
                break;
            }
            case THREONINE: {
                connectInOrder(residue, atoms,
                        CA, CB, OG1);
                residue.connect(atoms.get(CB), atoms.get(CG2));
                break;
            }
            case TRYPTOPHAN: {
                connectInOrder(residue, atoms,
                        CA, CB, CG, CD, CD1, CE2, CE2, CZ2, CH2, CZ2, CE3, CD2, CG);
                residue.connect(atoms.get(CD2), atoms.get(CE2));
                break;
            }
            case TYROSINE: {
                connectInOrder(residue, atoms,
                        CA, CB, CG, CD1, CE1, CZ, CE2, CD2, CG);
                residue.connect(atoms.get(CZ), atoms.get(OH));
                break;
            }
            case VALINE: {
                connectInOrder(residue, atoms,
                        CA, CB, CG1);
                residue.connect(atoms.get(CB), atoms.get(CG2));
                break;
            }

            default: {
                break;
            }
        }
        return residue;
    }

    /**
     * Connects the atoms in the given order.
     *
     * @param residue The residue to connect in.
     * @param atoms The atoms to take from.
     * @param names The names that should be connected.
     */
    private static void connectInOrder(Residue residue, EnumMap<AtomName, Atom> atoms, AtomName... names) {
        if (names.length < 2) {
            throw new IllegalArgumentException("Two or more atom names are required in order to connect them.");
        }
        for (int i = 1; i < names.length; i++) {
            residue.connect(atoms.get(names[i - 1]), atoms.get(names[i]));
        }
    }

    /**
     * Connects the backbone atoms N-to-CA-to-C-to-O.
     * @param residue The residue to connect in.
     * @param atoms The atoms to take from.
     */
    private static void connectBackboneAtoms(Residue residue, EnumMap<AtomName, Atom> atoms) {
        residue.connect(atoms.get(N), atoms.get(CA));
        residue.connect(atoms.get(CA), atoms.get(C));
        residue.connect(atoms.get(C), atoms.get(O));
    }

    /**
     * Connects the N terminal Hydrogens N-to-H and N-to-H2.
     * @param residue The residue to connect in.
     * @param atoms The atoms to take from.
     */
    private static void connectNTerminalAtoms(Residue residue, EnumMap<AtomName, Atom> atoms) {
        if (factory.connectHydrogens) {
            residue.connect(atoms.get(N), atoms.get(H));
            residue.connect(atoms.get(N), atoms.get(H2));
        }
    }

    /**
     * Connects the C terminal Atoms C-to-OXT-to-HXT.
     * @param residue The residue to connect in.
     * @param atoms The atoms to take from.
     */
    private static void connectCTerminaAtoms(Residue residue, EnumMap<AtomName, Atom> atoms) {
        residue.connect(atoms.get(C), atoms.get(OXT));
        if (factory.connectHydrogens) {
            residue.connect(atoms.get(OXT), atoms.get(HXT));
        }
    }

    private static void saturateCarbon(Residue residue, EnumMap<AtomName, Atom> atoms, AtomName anyCarbon) {
        final int currentSaturation = residue.getAtomByName(anyCarbon).getDegree();

        switch (anyCarbon) {
            case CA: {


                break;
            }

        }


    }

    /**
     * Sets to omit all hydrogen (and deuterium) atoms. Those atoms are not added to the resulting residue.
     * @param omitHydrogens True, if no hydrogen should be parsed, false otherwise.
     */
    public static void setToOmitHydrogens(boolean omitHydrogens) {
        factory.omitHydrogens = omitHydrogens;
    }

}



