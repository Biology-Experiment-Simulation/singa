package de.bioforscher.singa.simulation.model.concentrations;

import de.bioforscher.singa.chemistry.descriptive.entities.ChemicalEntity;
import de.bioforscher.singa.features.quantities.MolarConcentration;
import de.bioforscher.singa.features.units.UnitProvider;
import de.bioforscher.singa.simulation.model.compartments.CellSection;
import tec.units.ri.quantity.Quantities;

import javax.measure.Quantity;
import java.util.*;

/**
 * @author cl
 */
public class MultiConcentrationContainer implements ConcentrationContainer {

    private final Set<ChemicalEntity<?>> referencedEntities;
    private final Map<CellSection, Map<ChemicalEntity<?>, Quantity<MolarConcentration>>> concentrations;

    public MultiConcentrationContainer(Set<CellSection> cellSections) {
        referencedEntities = new HashSet<>();
        concentrations = new HashMap<>();
        cellSections.forEach(compartment -> concentrations.put(compartment, new HashMap<>()));
    }

    public MultiConcentrationContainer(MultiConcentrationContainer multiConcentrationContainer) {
        referencedEntities = multiConcentrationContainer.referencedEntities;
        concentrations = new HashMap<>();
        Set<CellSection> cellSections = multiConcentrationContainer.concentrations.keySet();
        cellSections.forEach(compartment -> concentrations.put(compartment, new HashMap<>()));
    }

    @Override
    public Quantity<MolarConcentration> getConcentration(ChemicalEntity chemicalEntity) {
        return Quantities.getQuantity(concentrations.keySet().stream()
                .mapToDouble(identifier -> getAvailableConcentration(identifier, chemicalEntity).getValue().doubleValue())
                .average().orElse(0.0), UnitProvider.MOLE_PER_LITRE);
    }

    @Override
    public Map<ChemicalEntity<?>, Quantity<MolarConcentration>> getAllConcentrationsForSection(CellSection cellSection) {
        if (concentrations.containsKey(cellSection)) {
            return concentrations.get(cellSection);
        }
        return Collections.emptyMap();
    }

    @Override
    public Quantity<MolarConcentration> getAvailableConcentration(CellSection cellSection, ChemicalEntity chemicalEntity) {
        if (!concentrations.containsKey(cellSection)) {
            Quantities.getQuantity(0.0, UnitProvider.MOLE_PER_LITRE);
        }
        return concentrations.get(cellSection).get(chemicalEntity);
    }

    @Override
    public void setConcentration(ChemicalEntity chemicalEntity, Quantity<MolarConcentration> concentration) {
        concentrations.keySet().forEach(compartment -> setAvailableConcentration(compartment, chemicalEntity, concentration));
    }

    @Override
    public void setAvailableConcentration(CellSection cellSection, ChemicalEntity chemicalEntity, Quantity<MolarConcentration> concentration) {
        concentrations.get(cellSection).put(chemicalEntity, concentration);
        referencedEntities.add(chemicalEntity);
    }

    @Override
    public Set<ChemicalEntity<?>> getAllReferencedEntities() {
        return referencedEntities;
    }

    @Override
    public Set<CellSection> getAllReferencedSections() {
        return concentrations.keySet();
    }

    @Override
    public Map<ChemicalEntity<?>, Quantity<MolarConcentration>> getAllConcentrations() {
        Map<ChemicalEntity<?>, Quantity<MolarConcentration>> result = new HashMap<>();
        for (ChemicalEntity<?> chemicalEntity : referencedEntities) {
            result.put(chemicalEntity, getConcentration(chemicalEntity));
        }
        return result;
    }

    @Override
    public MultiConcentrationContainer getCopy() {
        return new MultiConcentrationContainer(this);
    }
}
