package bio.singa.simulation.model.sections;

import bio.singa.chemistry.entities.ChemicalEntity;
import bio.singa.chemistry.entities.ComplexEntity;
import bio.singa.features.quantities.MolarConcentration;
import bio.singa.features.units.UnitRegistry;
import bio.singa.simulation.model.modules.concentration.imlementations.reactions.behaviors.reactants.DynamicChemicalEntity;
import bio.singa.simulation.model.modules.concentration.imlementations.reactions.behaviors.reactants.EntityReducer;

import javax.measure.Quantity;
import java.util.*;

import static bio.singa.simulation.model.sections.CellTopology.INNER;
import static bio.singa.simulation.model.sections.CellTopology.OUTER;

/**
 * The concentration container manages the concentrations of one updatable. {@link MolarConcentration}s of
 * {@link ChemicalEntity}s can be set and got by the {@link CellSubsection} or {@link CellTopology}.
 * If no relevant concentration was registered an empty concentration will be returned.
 *
 * @author cl
 */
public class ConcentrationContainer {

    /**
     * The mapping of topology to subsection.
     */
    private Map<CellTopology, CellSubsection> subsectionTopology;

    /**
     * The mapping of the subsection to the corresponding concentration pool.
     */
    private Map<CellSubsection, ConcentrationPool> concentrations;

    /**
     * Creates a new concentration container.
     */
    public ConcentrationContainer() {
        subsectionTopology = new HashMap<>();
        concentrations = new HashMap<>();
    }

    /**
     * Initializes a new concentration pool for the corresponding subsection and topology.
     *
     * @param subsection The subsection.
     * @param topology The topology.
     */
    public void initializeSubsection(CellSubsection subsection, CellTopology topology) {
        putSubsectionPool(subsection, topology, new ConcentrationPool());
    }

    /**
     * Adds a concentration pool, referenced to the subsection and topology.
     *
     * @param subsection The subsection.
     * @param topology The topology.
     * @param concentrationPool The concentration pool.
     */
    public void putSubsectionPool(CellSubsection subsection, CellTopology topology, ConcentrationPool concentrationPool) {
        subsectionTopology.put(topology, subsection);
        concentrations.put(subsection, concentrationPool);
    }

    /**
     * Removes a subsection, the corresponding topology and concentration pool from the container.
     *
     * @param subsection The subsection to remove.
     */
    public void removeSubsection(CellSubsection subsection) {
        concentrations.remove(subsection);
        subsectionTopology.remove(getTopologyFromSubsection(subsection));
    }

    /**
     * Returns the topology referenced to the subsection and null otherwise.
     *
     * @param subsection The subsection.
     * @return the topology referenced to the subsection and null otherwise.
     */
    private CellTopology getTopologyFromSubsection(CellSubsection subsection) {
        for (Map.Entry<CellTopology, CellSubsection> entry : subsectionTopology.entrySet()) {
            if (Objects.equals(subsection, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Removes a subsection, the given topology and concentration pool from the container.
     *
     * @param topology The topology to remove.
     */
    public void removeSubsection(CellTopology topology) {
        CellSubsection subsection = subsectionTopology.get(topology);
        concentrations.remove(subsection);
        subsectionTopology.remove(getTopologyFromSubsection(subsection));
    }

    public Map<CellSubsection, ConcentrationPool> getConcentrations() {
        return concentrations;
    }

    /**
     * Returns all subsections, referenced in this container.
     *
     * @return All subsections, referenced in this container.
     */
    public Set<CellSubsection> getReferencedSubsections() {
        return concentrations.keySet();
    }

    /**
     * Returns all concentration pools in this container.
     *
     * @return All concentration pools in this container.
     */
    public Collection<ConcentrationPool> getPoolsOfConcentration() {
        return concentrations.values();
    }

    /**
     * Returns all entities referenced in any concentration pool.
     *
     * @return All entities referenced in any concentration pool.
     */
    public Set<ChemicalEntity> getReferencedEntities() {
        Set<ChemicalEntity> chemicalEntities = new HashSet<>();
        for (ConcentrationPool concentrationPool : concentrations.values()) {
            chemicalEntities.addAll(concentrationPool.getReferencedEntities());
        }
        return chemicalEntities;
    }

    public Optional<ChemicalEntity> containsHiddenEntity(CellTopology topology, ChemicalEntity entity) {
        for (ChemicalEntity chemicalEntity : getPool(topology).getValue().getReferencedEntities()) {
            if (entity.equals(chemicalEntity)) {
                return Optional.of(chemicalEntity);
            }
            if (chemicalEntity instanceof ComplexEntity) {
                for (ChemicalEntity associatedChemicalEntity : ((ComplexEntity) chemicalEntity).getAllData()) {
                    if (entity.equals(associatedChemicalEntity)) {
                        return Optional.of(chemicalEntity);
                    }
                }
            }
        }
        return Optional.empty();
    }

    public boolean containsEntity(CellTopology topology, ChemicalEntity entity) {
        for (ChemicalEntity chemicalEntity : getPool(topology).getValue().getReferencedEntities()) {
            if (entity.equals(chemicalEntity)) {
                return true;
            }
        }
        return false;
    }

    public boolean containsEntity(CellSubsection subsection, ChemicalEntity entity) {
        for (ChemicalEntity chemicalEntity : getPool(subsection).getValue().getReferencedEntities()) {
            if (entity.equals(chemicalEntity)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the topology and concentration pool for the subsection.
     *
     * @param subsection The subsection.
     * @return The topology and concentration pool for the subsection.
     */
    public Map.Entry<CellTopology, ConcentrationPool> getPool(CellSubsection subsection) {
        CellTopology topology = getTopologyFromSubsection(subsection);
        if (topology != null) {
            return new AbstractMap.SimpleEntry<>(topology, concentrations.get(subsection));
        }
        return null;
    }

    /**
     * Returns the subsection and concentration pool for the topology.
     *
     * @param topology Th topology.
     * @return The subsection and concentration pool for the topology.
     */
    public Map.Entry<CellSubsection, ConcentrationPool> getPool(CellTopology topology) {
        CellSubsection subsection = subsectionTopology.get(topology);
        if (subsection != null) {
            return new AbstractMap.SimpleEntry<>(subsection, concentrations.get(subsection));
        }
        return null;
    }

    /**
     * Returns the concentration of the entity in the corresponding subsection.
     *
     * @param subsection The subsection.
     * @param entity The entity.
     * @return The concentration of the entity in the corresponding subsection.
     */
    public double get(CellSubsection subsection, ChemicalEntity entity) {
        ConcentrationPool concentrationPool = concentrations.get(subsection);
        if (concentrationPool == null) {
            return 0.0;
        }
        return concentrationPool.get(entity);
    }

    /**
     * Returns the concentration of the entity in the corresponding topology.
     *
     * @param topology The topology.
     * @param entity The entity.
     * @return The concentration of the entity in the corresponding topology.
     */
    public double get(CellTopology topology, ChemicalEntity entity) {
        CellSubsection subsection = subsectionTopology.get(topology);
        if (subsection == null) {
            return 0.0;
        }
        return get(subsection, entity);
    }

    public double sumOf(DynamicChemicalEntity dynamicEntity) {
        double sum = 0.0;
        for (CellTopology topology : dynamicEntity.getPossibleTopologies()) {
            if (getPool(topology) != null) {
                Set<ChemicalEntity> originalEntities = getPool(topology).getValue().getReferencedEntities();
                List<ChemicalEntity> reducedEntities = EntityReducer.apply(originalEntities, dynamicEntity.getComposition());
                for (ChemicalEntity entity : reducedEntities) {
                    sum += get(topology, entity);
                }
            }
        }
        return sum;
    }


    /**
     * Sets the concentration of the given entity in the given subsection.
     *
     * @param subsection The subsection.
     * @param entity The entity.
     * @param concentration The concentration.
     */
    public void set(CellSubsection subsection, ChemicalEntity entity, double concentration) {
        concentrations.get(subsection).set(entity, concentration);
    }

    public void initialize(CellSubsection subsection, ChemicalEntity entity, Quantity<MolarConcentration> concentration) {
        concentrations.get(subsection).set(entity, concentration.to(UnitRegistry.getConcentrationUnit()).getValue().doubleValue());
    }

    /**
     * Sets the concentration of the given entity in the given topology.
     *
     * @param topology The topology.
     * @param entity The entity.
     * @param concentration The concentration.
     */
    public void set(CellTopology topology, ChemicalEntity entity, double concentration) {
        set(subsectionTopology.get(topology), entity, concentration);
    }

    public void initialize(CellTopology topology, ChemicalEntity entity, Quantity<MolarConcentration> concentration) {
        initialize(subsectionTopology.get(topology), entity, concentration);
    }

    /**
     * Returns the subsection corresponding to the topology.
     *
     * @param topology The topology.
     * @return The subsection.
     */
    public CellSubsection getSubsection(CellTopology topology) {
        return subsectionTopology.get(topology);
    }

    /**
     * Returns the inner subsection.
     *
     * @return The inner subsection.
     */
    public CellSubsection getInnerSubsection() {
        return subsectionTopology.get(INNER);
    }

    /**
     * Returns the outer subsection.
     *
     * @return The outer subsection.
     */
    public CellSubsection getOuterSubsection() {
        return subsectionTopology.get(OUTER);
    }

    /**
     * Returns the membrane subsection.
     *
     * @return The membrane subsection.
     */
    public CellSubsection getMembraneSubsection() {
        return subsectionTopology.get(CellTopology.MEMBRANE);
    }

    /**
     * Returns a empty copy of this container, keeping subsections and cell topologies associated.
     *
     * @return A empty copy of this container.
     */
    public ConcentrationContainer emptyCopy() {
        ConcentrationContainer concentrationContainer = new ConcentrationContainer();
        for (Map.Entry<CellTopology, CellSubsection> entry : subsectionTopology.entrySet()) {
            concentrationContainer.initializeSubsection(entry.getValue(), entry.getKey());
        }
        return concentrationContainer;
    }

    /**
     * Returns a full copy of this container, keeping concentrations.
     *
     * @return A full copy of this container.
     */
    public ConcentrationContainer fullCopy() {
        ConcentrationContainer concentrationContainer = new ConcentrationContainer();
        for (Map.Entry<CellTopology, CellSubsection> entry : subsectionTopology.entrySet()) {
            CellTopology topology = entry.getKey();
            CellSubsection subsection = subsectionTopology.get(topology);
            concentrationContainer.putSubsectionPool(subsection, topology, getPool(subsection).getValue().fullCopy());
        }
        return concentrationContainer;
    }

}
