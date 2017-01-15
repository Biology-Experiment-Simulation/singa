package de.bioforscher.chemistry.descriptive.molecules;

import de.bioforscher.chemistry.descriptive.elements.Element;
import de.bioforscher.chemistry.descriptive.estimations.MoleculePathFinder;
import de.bioforscher.chemistry.parser.smiles.SmilesParser;
import de.bioforscher.mathematics.geometry.faces.Rectangle;
import de.bioforscher.mathematics.graphs.model.AbstractGraph;
import de.bioforscher.mathematics.vectors.Vector2D;
import de.bioforscher.mathematics.vectors.Vectors;


import java.util.*;
import java.util.function.Predicate;

import static de.bioforscher.chemistry.descriptive.elements.ElementProvider.*;

/**
 * @author cl
 */
public class MoleculeGraph extends AbstractGraph<MoleculeAtom, MoleculeBond, Vector2D> {

    // TODO add a reference of this to Species

    public static Predicate<MoleculeAtom> isElement(Element element) {
        return atom -> atom.getElement().getProtonNumber() == element.getProtonNumber();
    }

    public static Predicate<MoleculeAtom> isOneOfElements(final Set<Element> elements) {
        return atom -> elements.contains(atom.getElement());
    }

    public int addNextAtom(char elementSymbol) {
        return addNextAtom(String.valueOf(elementSymbol));
    }

    public int addNextAtom(String elementSymbol) {
        return addNextAtom(getElementBySymbol(elementSymbol).orElseThrow(() -> new IllegalArgumentException("The symbol " + elementSymbol + " represents no valid element.")));
    }

    public int addNextAtom(Element element) {
        MoleculeAtom atom = new MoleculeAtom(nextNodeIdentifier(),
                Vectors.generateRandomVectorInRectangle(new Rectangle(100, 100)), element);
        addNode(atom);
        return atom.getIdentifier();
    }

    public int addNextAtom(Element element, int charge) {
        Element ion = element.asIon(charge);
        return addNextAtom(ion);
    }

    public int addNextAtom(Element element, int charge, int numberOfNeutrons) {
        Element ion = element.asIon(charge);
        ion.asIsotope(numberOfNeutrons);
        return addNextAtom(ion);
    }

    @Override
    public int addEdgeBetween(int identifier, MoleculeAtom source, MoleculeAtom target) {
        return addEdgeBetween(new MoleculeBond(identifier), source, target);
    }

    @Override
    public int addEdgeBetween(MoleculeAtom source, MoleculeAtom target) {
        return addEdgeBetween(nextEdgeIdentifier(), source, target);
    }

    public int addEdgeBetween(MoleculeAtom source, MoleculeAtom target, MoleculeBondType bondType) {
        MoleculeBond bond = new MoleculeBond(nextEdgeIdentifier());
        bond.setType(bondType);
        return addEdgeBetween(bond, source, target);
    }

    public MoleculeBond getEdgeBetween(MoleculeAtom source, MoleculeAtom target) {
        return this.getEdges().stream()
                .filter(bond -> bond.containsNode(source) && bond.containsNode(target))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Could not find any edge connecting " + source + " and " + target + "."));
    }

    public int countAtomsOfElement(Element element) {
        return (int) this.getNodes().stream()
                .filter(isElement(element))
                .count();
    }

    public List<LinkedList<MoleculeAtom>> findPathOfElements(LinkedList<Element> path) {
        return MoleculePathFinder.findPathInMolecule(this, path);
    }

    public List<LinkedList<MoleculeAtom>> findMultiPathOfElements(LinkedList<Set<Element>> path) {
        return MoleculePathFinder.findMultiPathInMolecule(this, path);
    }

    public void replaceAromaticsWithDoubleBonds() {
        // get all aromatic paths in the molecule
        List<LinkedList<MoleculeBond>> aromaticPaths = MoleculePathFinder.findAromaticPath(this);
        // replace every second bond with a double bond
        for (List<MoleculeBond> path : aromaticPaths) {
            for (int i = 0; i < path.size(); i++) {
                if (i % 2  == 0) {
                    path.get(i).setType(MoleculeBondType.DOUBLE_BOND);
                } else {
                    path.get(i).setType(MoleculeBondType.SINGLE_BOND);
                }
            }
        }
    }

}
