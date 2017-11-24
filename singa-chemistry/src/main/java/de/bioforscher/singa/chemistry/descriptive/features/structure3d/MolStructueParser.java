package de.bioforscher.singa.chemistry.descriptive.features.structure3d;

import de.bioforscher.singa.core.utility.Pair;
import de.bioforscher.singa.mathematics.vectors.Vector3D;
import de.bioforscher.singa.structure.elements.Element;
import de.bioforscher.singa.structure.elements.ElementProvider;
import de.bioforscher.singa.structure.model.families.LigandFamily;
import de.bioforscher.singa.structure.model.identifiers.LeafIdentifier;
import de.bioforscher.singa.structure.model.interfaces.Ligand;
import de.bioforscher.singa.structure.model.oak.BondType;
import de.bioforscher.singa.structure.model.oak.OakAtom;
import de.bioforscher.singa.structure.model.oak.OakBond;
import de.bioforscher.singa.structure.model.oak.OakLigand;

import java.util.*;

/**
 * according to specification http://www.daylight.com/meetings/mug05/Kappler/ctfile.pdf
 *
 * @author cl
 */
public class MolStructueParser {

    private List<String> lines;

    private String headerInformation;
    private int atomCount;
    private int bondCount;

    private List<OakAtom> atoms;
    private Map<Pair<Integer>, BondType> bonds;

    public MolStructueParser(List<String> lines) {
        this.lines = lines;
        atoms = new ArrayList<>();
        bonds = new HashMap<>();
    }

    public void parseCountsLine() {
        // 0.........1.........2.........3........
        // 012345678901234567890123456789012345678
        // aaabbblllfffcccsssxxxrrrpppiiimmmvvvvvv
        String line = lines.get(3);
        atomCount = Integer.valueOf(line.substring(0, 3).trim());
        bondCount = Integer.valueOf(line.substring(3, 6).trim());
    }

    public void parseAtomBlock() {
        // 0.........1.........2.........3.........4.........5.........6........
        // 012345678901234567890123456789012345678901234567890123456789012345678
        // xxxxx.xxxxyyyyy.yyyyzzzzz.zzzz aaaddcccssshhhbbbvvvHHHrrriiimmmnnneee
        for (int blockIndex = 0; blockIndex < atomCount; blockIndex++) {
            String line = lines.get(blockIndex + 4);
            // acquire data
            final double x = Double.valueOf(line.substring(0, 10));
            final double y = Double.valueOf(line.substring(10, 20));
            final double z = Double.valueOf(line.substring(20, 30));
            final Optional<Element> optinalElement = ElementProvider.getElementBySymbol(line.substring(31, 34).trim());
            final int charge = Integer.valueOf(line.substring(36, 39).trim());
            // create entities
            final Element element = optinalElement.orElse(ElementProvider.UNKOWN);
            final OakAtom atom = new OakAtom(blockIndex, element, element.getSymbol(), new Vector3D(x, y, z));
            atoms.add(atom);
        }

    }

    public void parseBondBlock() {
        // 0.........1.........2
        // 012345678901234567890
        // 111222tttsssxxxrrrccc
        for (int blockIndex = 0; blockIndex < bondCount; blockIndex++) {
            String line = lines.get(blockIndex + 4 + atomCount);
            // acquire data
            final int first = Integer.valueOf(line.substring(0, 3).trim());
            final int second = Integer.valueOf(line.substring(3, 6).trim());
            final int typeInt = Integer.valueOf(line.substring(6, 9).trim());
            BondType type;
            switch (typeInt) {
                case 2:
                case 4:
                    type = BondType.DOUBLE_BOND;
                    break;
                case 3:
                    type = BondType.TRIPLE_BOND;
                case 1:
                default:
                    type = BondType.SINGLE_BOND;
                    break;
            }

            bonds.put(new Pair<>(first, second), type);
        }
    }

    public Ligand parse() {
        parseCountsLine();
        parseAtomBlock();
        parseBondBlock();
        // create structure;
        OakLigand ligand = new OakLigand(LeafIdentifier.DEFAULT_LEAF_IDENTIFIER, new LigandFamily("UNK", "?"));
        atoms.forEach(ligand::addAtom);
        int bondCounter = 0;
        for (Map.Entry<Pair<Integer>, BondType> bond : bonds.entrySet()) {
            ligand.addBondBetween(new OakBond(bondCounter, bond.getValue()), atoms.get(bond.getKey().getFirst()-1),
                    atoms.get(bond.getKey().getSecond()-1));
            bondCounter++;
        }
        return ligand;
    }
}
