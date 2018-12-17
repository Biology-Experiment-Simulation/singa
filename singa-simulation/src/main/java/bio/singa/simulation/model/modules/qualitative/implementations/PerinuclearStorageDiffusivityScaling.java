package bio.singa.simulation.model.modules.qualitative.implementations;

import bio.singa.chemistry.features.diffusivity.Diffusivity;
import bio.singa.simulation.features.ModifiedDiffusivity;
import bio.singa.simulation.model.agents.pointlike.Vesicle;
import bio.singa.simulation.model.agents.pointlike.VesicleStateRegistry;
import bio.singa.simulation.model.modules.concentration.ModuleState;
import bio.singa.simulation.model.modules.qualitative.QualitativeModule;

import javax.measure.Quantity;
import java.util.ArrayList;
import java.util.List;

/**
 * @author cl
 */
public class PerinuclearStorageDiffusivityScaling extends QualitativeModule {

    List<Vesicle> storedVesicles;

    public PerinuclearStorageDiffusivityScaling() {
        storedVesicles = new ArrayList<>();
        // features
        getRequiredFeatures().add(ModifiedDiffusivity.class);
    }

    @Override
    public void calculateUpdates() {
        double scaledDiffusivity = getScaledFeature(ModifiedDiffusivity.class);
        for (Vesicle vesicle : simulation.getVesicleLayer().getVesicles()) {
            if (vesicle.getState().equals(VesicleStateRegistry.IN_STORAGE)) {
                if (scaledDiffusivity != vesicle.getFeature(Diffusivity.class).getScaledQuantity()) {
                    storedVesicles.add(vesicle);
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
        storedVesicles.clear();
    }

    @Override
    public void onCompletion() {
        Quantity<Diffusivity> diffusivity = getFeature(ModifiedDiffusivity.class).getContent();
        for (Vesicle storedVesicle : storedVesicles) {
            storedVesicle.getFeature(Diffusivity.class).setContent(diffusivity);
        }
    }
}
