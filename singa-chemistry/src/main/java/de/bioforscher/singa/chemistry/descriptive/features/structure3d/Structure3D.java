package de.bioforscher.singa.chemistry.descriptive.features.structure3d;

import de.bioforscher.singa.features.model.AbstractFeature;
import de.bioforscher.singa.features.model.FeatureOrigin;
import de.bioforscher.singa.structure.model.interfaces.AtomContainer;

/**
 * @author cl
 */
public class Structure3D extends AbstractFeature<AtomContainer> {

public static final String SYMBOL = "Structure3D";

    public Structure3D(AtomContainer structure, FeatureOrigin featureOrigin) {
        super(structure, featureOrigin);
    }

    @Override
    public String getSymbol() {
        return SYMBOL;
    }
    
}
