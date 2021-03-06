package bio.singa.simulation.features.variation;

import bio.singa.chemistry.entities.ChemicalEntity;
import bio.singa.features.quantities.MolarConcentration;
import bio.singa.simulation.model.sections.CellRegion;
import bio.singa.simulation.model.sections.CellSubsection;
import bio.singa.simulation.model.sections.concentration.SectionConcentration;

import javax.measure.Quantity;

/**
 * @author cl
 */
public class ConcentrationVariation extends Variation<MolarConcentration> {

    private final CellRegion cellRegion;

    private final CellSubsection subsection;

    private final ChemicalEntity entity;

    public ConcentrationVariation(CellRegion cellRegion, CellSubsection subsection, ChemicalEntity entity) {
        this.cellRegion = cellRegion;
        this.subsection = subsection;
        this.entity = entity;
    }

    public ConcentrationVariation(CellSubsection subsection, ChemicalEntity entity) {
        cellRegion = null;
        this.subsection = subsection;
        this.entity = entity;
    }

    public CellRegion getCellRegion() {
        return cellRegion;
    }

    public CellSubsection getSubsection() {
        return subsection;
    }

    public ChemicalEntity getEntity() {
        return entity;
    }

    @Override
    public SectionConcentration create(Object concentration) {
        return new SectionConcentration(cellRegion, subsection, entity, ((Quantity) concentration).asType(MolarConcentration.class));
    }

    @Override
    public String toString() {
        return "Concentration:" + (cellRegion == null ? "" : " R = " + cellRegion.getIdentifier()) +
                " S = " + subsection.getIdentifier() +
                " E = " + entity.getIdentifier();
    }

}
