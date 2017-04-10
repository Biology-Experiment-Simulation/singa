package de.bioforscher.singa.simulation.modules.reactions.model;

import de.bioforscher.singa.chemistry.descriptive.ChemicalEntity;
import de.bioforscher.singa.simulation.model.graphs.AutomatonGraph;
import de.bioforscher.singa.simulation.model.graphs.BioNode;
import de.bioforscher.singa.simulation.modules.model.updates.ImmediateUpdateBehavior;
import de.bioforscher.singa.simulation.modules.model.Module;
import de.bioforscher.singa.simulation.modules.model.updates.PotentialUpdate;
import de.bioforscher.singa.simulation.modules.model.Simulation;
import de.bioforscher.singa.simulation.modules.reactions.implementations.kineticLaws.model.KineticLaw;
import de.bioforscher.singa.units.UnitProvider;
import de.bioforscher.singa.units.quantities.ReactionRate;
import tec.units.ri.quantity.Quantities;

import javax.measure.Quantity;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The Reactions module defines the entirety of chemical conversions that take place in the
 * {@link Simulation Simulation}. All Reactions are calculated according to
 * their specified {@link KineticLaw KineticLaws}s
 * and new concentrations are set using the {@link Reactions#applyTo(AutomatonGraph)} method.
 */
public class Reactions implements Module, ImmediateUpdateBehavior {

    private List<Reaction> reactions;
    private Map<ChemicalEntity<?>, Quantity<ReactionRate>> velocities;

    public Reactions() {
        this.reactions = new ArrayList<>();
        this.velocities = new HashMap<>();
    }

    public List<Reaction> getReactions() {
        return this.reactions;
    }

    public void setReactions(List<Reaction> reactions) {
        this.reactions = reactions;
    }

    @Override
    public void applyTo(AutomatonGraph graph) {
        // update graph calls updateNodes(BioNode)
        updateGraph(graph);
    }

    @Override
    public Set<ChemicalEntity<?>> collectAllReferencesEntities() {
        return this.reactions.stream()
                .map(Reaction::collectAllReferencedEntities)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    @Override
    public void updateNode(BioNode node) {
        // calculate acceleration for each reaction
        Map<Reaction, Quantity<ReactionRate>> reactionRates = new HashMap<>();
        // assign reaction rates
        this.reactions.forEach(reaction -> reactionRates.put(reaction, reaction.calculateAcceleration(node)));
        // apply acceleration to the reactants of each reaction resulting in the velocity of the concentration change
        this.reactions.forEach(reaction ->
                reaction.getStoichiometricReactants().forEach(reactant -> {
                    if (reactant.isSubstrate()) {
                        // substrates are consumed and acceleration is therefore negative
                        updateVelocity(reactant, reactionRates.get(reaction).multiply(-1));
                    } else {
                        // products are created and acceleration is therefore positive
                        updateVelocity(reactant, reactionRates.get(reaction));
                    }
                }));
        // update every concentration using the calculateUpdateMethod

        this.velocities.forEach(((entity, velocity) -> {
                if (entity.getIdentifier().toString().equals("BPG")) {
                    System.out.println("BPG "+velocity);
                }
                updateSpecies(node, entity);
        }));
        this.velocities.clear();
    }

    private void updateVelocity(StoichiometricReactant reactant, Quantity<ReactionRate> acceleration) {
        if (!this.velocities.containsKey(reactant.getEntity())) {
            // if species is not in map put it
            this.velocities.put(reactant.getEntity(), acceleration.multiply(reactant.getStoichiometricNumber()));
        } else {
            // else compute the new value
            this.velocities.compute(reactant.getEntity(), (entity, velocity) ->
                    velocity.add(acceleration.multiply(reactant.getStoichiometricNumber())));
        }
    }

    @Override
    public PotentialUpdate calculateUpdate(BioNode node, ChemicalEntity entity) {
        return new PotentialUpdate(entity, node.getConcentration(entity)
                .add(Quantities.getQuantity(this.velocities.get(entity).getValue(),
                        UnitProvider.MOLE_PER_LITRE)));
    }

}