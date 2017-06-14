package de.bioforscher.singa.chemistry.physical.atoms.representations;


import de.bioforscher.singa.chemistry.descriptive.elements.ElementProvider;
import de.bioforscher.singa.chemistry.physical.atoms.Atom;
import de.bioforscher.singa.chemistry.physical.atoms.UncertainAtom;
import de.bioforscher.singa.chemistry.physical.leafes.LeafSubstructure;
import de.bioforscher.singa.mathematics.vectors.Vectors3D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

import static de.bioforscher.singa.chemistry.physical.model.StructuralEntityFilter.AtomFilter;

/**
 * This abstract implementation provide a fallback solution to compute the centroid of a {@link LeafSubstructure},
 * iff the specified representation fails.
 *
 * @author fk
 */
public abstract class AbstractRepresentationScheme implements RepresentationScheme {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractRepresentationScheme.class);

    /**
     * Determines the centroid (<b>EXCLUDING</b> hydrogen atoms) of the given {@link LeafSubstructure}.
     * @param leafSubstructure The {@link LeafSubstructure} for which the centroid should be computed.
     * @return The centroid as an {@link UncertainAtom}.
     */
    Atom determineCentroid(LeafSubstructure<?, ?> leafSubstructure) {
        logger.warn("obtaining centroid representation for ", leafSubstructure);
        return new UncertainAtom(leafSubstructure.getAllAtoms().get(0).getIdentifier(),
                ElementProvider.UNKOWN,
                RepresentationSchemeType.CENTROID.getAtomNameString(),
                Vectors3D.getCentroid(leafSubstructure.getAllAtoms().stream()
                        .filter(AtomFilter.isHydrogen().negate())
                        .map(Atom::getPosition)
                        .collect(Collectors.toList())));
    }
}
