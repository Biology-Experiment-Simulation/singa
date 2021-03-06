package bio.singa.simulation.model.modules.qualitative.implementations;

import bio.singa.chemistry.entities.ChemicalEntity;
import bio.singa.simulation.features.AppliedVesicleState;
import bio.singa.simulation.features.Cargoes;
import bio.singa.simulation.features.Ratio;
import bio.singa.simulation.features.RequiredVesicleState;
import bio.singa.simulation.model.agents.pointlike.Vesicle;
import bio.singa.simulation.model.modules.concentration.ModuleState;
import bio.singa.simulation.model.modules.qualitative.QualitativeModule;
import bio.singa.simulation.model.sections.CellTopology;

import java.util.ArrayList;
import java.util.List;

/**
 * @author cl
 */
public class ConcentrationStateChange extends QualitativeModule {

    private List<Vesicle> changingVesicles;

    public ConcentrationStateChange() {
        changingVesicles = new ArrayList<>();

        // in storage
        getRequiredFeatures().add(RequiredVesicleState.class);
        // unattached
        getRequiredFeatures().add(AppliedVesicleState.class);

        // first: AQP2  second: AQP2P
        getRequiredFeatures().add(Cargoes.class);
        // 3/4
        getRequiredFeatures().add(Ratio.class);

    }

    @Override
    public void calculateUpdates() {
        String requiredState = getFeature(RequiredVesicleState.class).getContent();
        List<ChemicalEntity> content = getFeature(Cargoes.class).getContent();
        ChemicalEntity firstEntity = content.get(0);
        ChemicalEntity secondEntity = content.get(1);
        double ratio = getFeature(Ratio.class).getContent().getValue().doubleValue();
        for (Vesicle vesicle : simulation.getVesicleLayer().getVesicles()) {
            if (vesicle.getState().equals(requiredState)) {
                // if c(AQP2)/c(AQP2P) >= 3/4
                double firstConcentration = vesicle.getConcentrationContainer().get(CellTopology.MEMBRANE, firstEntity);
                double secondConcentration = vesicle.getConcentrationContainer().get(CellTopology.MEMBRANE, secondEntity);
                if (firstConcentration/secondConcentration < ratio) {
                    changingVesicles.add(vesicle);
                }
            }
        }
        state = ModuleState.SUCCEEDED_WITH_PENDING_CHANGES;
    }

    @Override
    public void optimizeTimeStep() {

    }

    @Override
    public void onReset() {
        changingVesicles.clear();
    }

    @Override
    public void onCompletion() {
        String appliedState = getFeature(AppliedVesicleState.class).getContent();
        for (Vesicle changingVesicle : changingVesicles) {
            changingVesicle.setState(appliedState);
        }
    }

}
