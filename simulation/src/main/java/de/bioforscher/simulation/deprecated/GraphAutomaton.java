package de.bioforscher.simulation.deprecated;

import de.bioforscher.chemistry.descriptive.ChemicalEntity;
import de.bioforscher.core.events.UpdateEventEmitter;
import de.bioforscher.core.events.UpdateEventListener;
import de.bioforscher.simulation.model.AutomatonGraph;
import de.bioforscher.simulation.model.BioNode;
import de.bioforscher.simulation.model.NodeUpdatedEvent;
import de.bioforscher.simulation.modules.diffusion.FreeDiffusion;
import de.bioforscher.simulation.parser.EpochUpdateWriter;
import de.bioforscher.simulation.util.BioGraphUtilities;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This class is used to simulate diffusion and reactions.
 *
 * @author Christoph Leberecht
 * @deprecated
 */
public class GraphAutomaton implements UpdateEventEmitter<NodeUpdatedEvent> {

    private AutomatonGraph graph;
    private FreeDiffusion diffusion;

    private List<Reaction> reactions;
    private Map<String, ChemicalEntity> species;

    private CopyOnWriteArrayList<UpdateEventListener<NodeUpdatedEvent>> listeners;

    private EpochUpdateWriter updateWriter;

    public int epoch;

    public GraphAutomaton(AutomatonGraph graph) {
        this.graph = graph;
        this.diffusion = new FreeDiffusion();
        this.reactions = new ArrayList<>();
        this.listeners = new CopyOnWriteArrayList<>();
        initializeChemicalEntitiesFromGraph(this.graph);
        initialize();
    }

    private void initializeChemicalEntitiesFromGraph(AutomatonGraph graph) {
        this.species = BioGraphUtilities.generateMapOfEntities(graph);
    }

    public void initialize() {
        // setup listener to write results
        // ZonedDateTime now = ZonedDateTime.now();
        // DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd
        // HH-mm-ss");
        // try {
        // this.updateWriter = new EpochUpdateWriter(Paths.get("data/"),
        // Paths.get("Simulation " + dtf.format(now)),
        // BioGraphUtilities.generateMapOfEntities(this.graph));
        // } catch (IOException e) {
        // e.printStackTrace();
        // }
        // this.addEventListener(this.updateWriter);
    }

    /**
     * This method must be called after the nodes that have to be observed have
     * been chosen. Than the files to write are initialized and are ready to
     * receive updates.
     */
    public void activateWriteObservedNodesToFiles() {
        this.graph.getNodes().stream().filter(BioNode::isObserved).forEach(node -> {
            try {
                this.updateWriter.addNodeToObserve(node);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public AutomatonGraph getGraph() {
        return this.graph;
    }

    public void setGraph(AutomatonGraph graph) {
        this.graph = graph;
    }

    /**
     * Calculates the next state of the system.
     *
     * @return
     */
    public AutomatonGraph next() {

        this.diffusion.applyTo(this.graph);

        // update
        for (BioNode node : this.graph.getNodes()) {
            //    node.setConcentrations(nextConcentrations.get(node.getIdentifier()));
            // applyTo immediate updates
            for (Reaction reaction : this.reactions) {
                reaction.updateConcentrations(node);
            }
            if (node.isObserved()) {
                emitNextEpochEvent(node);
            }
        }

        this.epoch++;
        return this.graph;

    }

    private void emitNextEpochEvent(BioNode node) {
        NodeUpdatedEvent event = new NodeUpdatedEvent(this.epoch, node);
        emitEvent(event);
    }


    public List<Reaction> getReactions() {
        return this.reactions;
    }

    @Override
    public CopyOnWriteArrayList<UpdateEventListener<NodeUpdatedEvent>> getListeners() {
        return this.listeners;
    }

    public Map<String, ChemicalEntity> getSpecies() {
        return this.species;
    }

    public void setSpecies(Map<String, ChemicalEntity> species) {
        this.species = species;
    }
}