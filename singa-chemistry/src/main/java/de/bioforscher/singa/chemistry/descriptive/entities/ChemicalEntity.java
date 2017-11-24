package de.bioforscher.singa.chemistry.descriptive.entities;

import de.bioforscher.singa.chemistry.descriptive.annotations.Annotatable;
import de.bioforscher.singa.chemistry.descriptive.annotations.Annotation;
import de.bioforscher.singa.chemistry.descriptive.annotations.AnnotationType;
import de.bioforscher.singa.chemistry.descriptive.features.ChemistryFeatureContainer;
import de.bioforscher.singa.chemistry.descriptive.features.diffusivity.Diffusivity;
import de.bioforscher.singa.chemistry.descriptive.features.structure3d.Structure3D;
import de.bioforscher.singa.core.identifier.model.Identifiable;
import de.bioforscher.singa.core.identifier.model.Identifier;
import de.bioforscher.singa.core.utility.Nameable;
import de.bioforscher.singa.features.model.Feature;
import de.bioforscher.singa.features.model.FeatureContainer;
import de.bioforscher.singa.features.model.Featureable;
import de.bioforscher.singa.structure.features.molarmass.MolarMass;
import de.bioforscher.singa.structure.features.molarvolume.MolarVolume;

import java.util.*;

/**
 * Chemical Entity is an abstract class that provides the common features of all chemical substances on a descriptive
 * level. Each chemical entity should be identifiable by an
 * {@link Identifier}. Chemical entities can be annotated, posses a {@link MolarMass} and a name.
 *
 * @param <IdentifierType> The Type of the {@link Identifier}, that identifies this entity.
 * @author cl
 * @see <a href="https://de.wikipedia.org/wiki/Simplified_Molecular_Input_Line_Entry_Specification">Wikipedia:
 * SMILES</a>
 */
public abstract class ChemicalEntity<IdentifierType extends Identifier> implements Identifiable<IdentifierType>,
        Nameable, Annotatable, Featureable {

    /**
     * The distinct {@link Identifier} by which this entity is identified.
     */
    private final IdentifierType identifier;

    /**
     * The name by which this entity is referenced.
     */
    private String name = "Unnamed chemical entity";

    /**
     * All annotations of this entity.
     */
    private List<Annotation> annotations;

    private FeatureContainer features;

    protected static final Set<Class<? extends Feature>> availableFeatures = new HashSet<>();

    static {
        availableFeatures.add(Diffusivity.class);
        availableFeatures.add(MolarMass.class);
        availableFeatures.add(Structure3D.class);
        availableFeatures.add(MolarVolume.class);
    }

    /**
     * Creates a new Chemical Entity with the given pdbIdentifier.
     *
     * @param identifier The pdbIdentifier.
     */
    protected ChemicalEntity(IdentifierType identifier) {
        this.identifier = identifier;
        annotations = new ArrayList<>();
        features = new ChemistryFeatureContainer();
    }

    @Override
    public IdentifierType getIdentifier() {
        return identifier;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Sets the name.
     *
     * @param name The name.
     */
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public List<Annotation> getAnnotations() {
        return annotations;
    }

    /**
     * Adds an additional name as an annotation to this chemical entity.
     *
     * @param additionalName An alternative name.
     */
    public void addAdditionalName(String additionalName) {
        addAnnotation(new Annotation<>(AnnotationType.ADDITIONAL_NAME, additionalName));
    }

    /**
     * Gets all additional names for the Annotations as a List of Strings.
     *
     * @return All alternative names.
     */
    public List<String> getAdditionalNames() {
        return getContentOfAnnotations(String.class, AnnotationType.ADDITIONAL_NAME);
    }

    public void addAdditionalIdentifiers(Collection<Identifier> identifiers) {
        identifiers.forEach(this::addAdditionalIdentifier);
    }

    public void addAdditionalIdentifier(Identifier identifier) {
        addAnnotation(new Annotation<>(AnnotationType.ADDITIONAL_IDENTIFIER, identifier));
    }

    public List<Identifier> getAdditionalIdentifiers() {
        return getContentOfAnnotations(Identifier.class, AnnotationType.ADDITIONAL_IDENTIFIER);
    }

    @Override
    public Collection<Feature<?>> getFeatures() {
        return features.getAllFeatures();
    }

    @Override
    public <FeatureType extends Feature> FeatureType getFeature(Class<FeatureType> featureTypeClass) {
        return features.getFeature(featureTypeClass);
    }

    @Override
    public <FeatureType extends Feature> void setFeature(Class<FeatureType> featureTypeClass) {
        features.setFeature(featureTypeClass, this);
    }

    @Override
    public <FeatureType extends Feature> void setFeature(FeatureType feature) {
        features.setFeature(feature);
    }

    @Override
    public <FeatureType extends Feature> boolean hasFeature(Class<FeatureType> featureTypeClass) {
        return features.hasFeature(featureTypeClass);
    }

    @Override
    public Set<Class<? extends Feature>> getAvailableFeatures() {
        return availableFeatures;
    }

    public List<Identifier> getAllIdentifiers() {
        List<Identifier> identifiers = getAdditionalIdentifiers();
        identifiers.add(identifier);
        return identifiers;
    }

    @Override
    public String toString() {
        return "ChemicalEntity{" +
                "identifier=" + identifier +
                ", name='" + name + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ChemicalEntity<?> that = (ChemicalEntity<?>) o;

        return identifier != null ? identifier.equals(that.identifier) : that.identifier == null;
    }

    @Override
    public int hashCode() {
        return identifier != null ? identifier.hashCode() : 0;
    }

    public static abstract class Builder<TopLevelType extends ChemicalEntity<?>, BuilderType extends Builder, IdentifierType extends Identifier> {

        TopLevelType topLevelObject;
        BuilderType builderObject;

        public Builder(IdentifierType identifier) {
            topLevelObject = createObject(identifier);
            builderObject = getBuilder();
        }

        protected abstract TopLevelType createObject(IdentifierType primaryIdentifer);

        protected abstract BuilderType getBuilder();

        public BuilderType name(String name) {
            topLevelObject.setName(name);
            return builderObject;
        }

        public BuilderType assignFeature(Feature feature) {
            topLevelObject.setFeature(feature);
            return builderObject;
        }

        public BuilderType assignFeature(Class<? extends Feature> feature) {
            topLevelObject.setFeature(feature);
            return builderObject;
        }

        public BuilderType additionalIdentifier(Identifier identifier) {
            topLevelObject.addAdditionalIdentifier(identifier);
            return builderObject;
        }

        public BuilderType additionalIdentifiers(Collection<Identifier> identifiers) {
            topLevelObject.addAdditionalIdentifiers(identifiers);
            return builderObject;
        }

        public BuilderType annotation(Annotation annotation) {
            topLevelObject.addAnnotation(annotation);
            return builderObject;
        }

        public TopLevelType build() {
            return topLevelObject;
        }
    }

}