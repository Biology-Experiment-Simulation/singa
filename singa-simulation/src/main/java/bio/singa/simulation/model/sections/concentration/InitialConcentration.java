package bio.singa.simulation.model.sections.concentration;

import bio.singa.features.model.Evidence;
import bio.singa.simulation.model.simulation.Simulation;
import bio.singa.simulation.model.simulation.Updatable;

/**
 * @author cl
 */
public interface InitialConcentration  {

    default void initialize(Simulation simulation) {
        for (Updatable updatable : simulation.getUpdatables()) {
            initialize(updatable);
        }
    }

    void initialize(Updatable updatable);

    Evidence getEvidence();

}
