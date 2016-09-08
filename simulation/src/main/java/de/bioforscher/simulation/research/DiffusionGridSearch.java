package de.bioforscher.simulation.research;

import de.bioforscher.chemistry.descriptive.ChemicalEntity;
import de.bioforscher.chemistry.descriptive.Species;
import de.bioforscher.chemistry.parser.ChEBIParserService;
import de.bioforscher.core.parameters.*;
import de.bioforscher.mathematics.algorithms.optimization.AbstractGridSearch;
import de.bioforscher.mathematics.combinatorics.Permutations;
import de.bioforscher.mathematics.geometry.faces.Rectangle;
import de.bioforscher.mathematics.graphs.util.GraphFactory;
import de.bioforscher.mathematics.graphs.util.RectangularGridCoordinateConverter;
import de.bioforscher.mathematics.vectors.Vector2D;
import de.bioforscher.simulation.model.AutomatonGraph;
import de.bioforscher.simulation.model.BioEdge;
import de.bioforscher.simulation.model.BioNode;
import de.bioforscher.simulation.modules.diffusion.FreeDiffusion;
import de.bioforscher.simulation.modules.model.Simulation;
import de.bioforscher.simulation.util.BioGraphUtilities;
import de.bioforscher.simulation.util.EnvironmentalVariables;
import de.bioforscher.units.quantities.Diffusivity;
import tec.units.ri.quantity.Quantities;

import javax.measure.Quantity;
import javax.measure.quantity.Time;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.bioforscher.units.UnitDictionary.SQUARECENTIMETER_PER_SECOND;
import static tec.units.ri.unit.MetricPrefix.NANO;
import static tec.units.ri.unit.Units.METRE;
import static tec.units.ri.unit.Units.SECOND;

/**
 * Created by Christoph on 04.08.2016.
 */
public class DiffusionGridSearch extends AbstractGridSearch {

    private static Rectangle defaultBoundingBox = new Rectangle(new Vector2D(0, 400), new Vector2D(400, 0));
    private Map<ChemicalEntity, Quantity<Diffusivity>> lookupSpecies;

    public DiffusionGridSearch(List<UniqueParameterList<?>> inputParameterList) {
        super(inputParameterList);

        this.lookupSpecies = new HashMap<>();

        Species hydrogen = ChEBIParserService.parse("CHEBI:18276");
        Species ammonia = ChEBIParserService.parse("CHEBI:16134");
        Species benzene = ChEBIParserService.parse("CHEBI:16716");
        Species methanol = ChEBIParserService.parse("CHEBI:17790");
        Species succinicAcid = ChEBIParserService.parse("CHEBI:15741");
        Species ethaneDiol = ChEBIParserService.parse("CHEBI:30742");

        this.lookupSpecies.put(hydrogen, Quantities.getQuantity(4.40E-05,
                SQUARECENTIMETER_PER_SECOND));
        this.lookupSpecies.put(ammonia, Quantities.getQuantity(2.28E-05,
                SQUARECENTIMETER_PER_SECOND));
        this.lookupSpecies.put(benzene, Quantities.getQuantity(1.09E-05,
                SQUARECENTIMETER_PER_SECOND));
        this.lookupSpecies.put(methanol, Quantities.getQuantity(1.66E-05,
                SQUARECENTIMETER_PER_SECOND));
        this.lookupSpecies.put(succinicAcid, Quantities.getQuantity(8.60E-06,
                SQUARECENTIMETER_PER_SECOND));
        this.lookupSpecies.put(ethaneDiol, Quantities.getQuantity(6.40E-06,
                SQUARECENTIMETER_PER_SECOND));


    }

    @Override
    public void search() {
        List<MixedParameterList> parameterSets = Permutations.generateAllCombinations(getInputParameterList());
        for (ChemicalEntity species : this.lookupSpecies.keySet()) {
            System.out.println("Calculating half life times for "+species.getName());
            for (MixedParameterList parameters : parameterSets) {
                searchWithParameters(parameters, species);
            }
        }
    }

    private void searchWithParameters(MixedParameterList parameters, ChemicalEntity entity) {
        // get parameters back
        ParameterValue<Integer> numberOfNodesParameter = parameters.getValue(0, Integer.class);
        ParameterValue<Double> timeStepParameter = parameters.getValue(1, Double.class);
        Quantity<Time> timeStep = Quantities.getQuantity(timeStepParameter.getValue(), NANO(SECOND));
        int numberOfNodes = numberOfNodesParameter.getValue();

        // setup rectangular graph with number of nodes
        AutomatonGraph graph = BioGraphUtilities.castUndirectedGraphToBioGraph(GraphFactory.buildGridGraph(
                numberOfNodes, numberOfNodes, defaultBoundingBox, false));

        // initialize species in graph with desired concentration leaving the right "half" empty
        for (BioNode node : graph.getNodes()) {
            if (node.getIdentifier() % numberOfNodes < numberOfNodes / 2) {
                node.addEntity(entity, 1.0);
            } else {
                node.addEntity(entity, 0.0);
            }
        }

        for (BioEdge edge : graph.getEdges()) {
            edge.addPermeability(entity, 1.0);
        }

        // setup time step size as given
        EnvironmentalVariables.getInstance().setTimeStep(timeStep);
        // setup node distance to diameter / (numberOfNodes - 1)
        EnvironmentalVariables.getInstance().setNodeSpacingToDiameter(
                Quantities.getQuantity(2500.0, NANO(METRE)), numberOfNodes);

        // setup simulation
        Simulation simulation = new Simulation();
        // add graph
        simulation.setGraph(graph);

        // setup diffusion
        FreeDiffusion freeDiffusion = new FreeDiffusion();
        freeDiffusion.fixDiffusionCoefficientForEntity(entity, this.lookupSpecies.get(entity));
        simulation.getModules().add(freeDiffusion);
        // add desired species to the simulation for easy access
        simulation.getChemicalEntities().add(entity);

        RectangularGridCoordinateConverter converter = new RectangularGridCoordinateConverter(numberOfNodes,
                numberOfNodes);
        int observedNodeIdentifier = converter.convert(new Vector2D(numberOfNodes-1, (numberOfNodes/2)-1));
        graph.getNode(observedNodeIdentifier).setObserved(true);

        while (graph.getNode(observedNodeIdentifier).getConcentration(entity).getValue().doubleValue() < 0.25) {
            simulation.nextEpoch();
        }

        // check correct diffusion
        System.out.println("  N: "+numberOfNodes+"\tT: "+timeStep.getValue().doubleValue()+"\tH: "+
                simulation.getElapsedTime());
        writeResults(parameters, simulation.getElapsedTime().divide(1000).getValue().doubleValue(), entity);

    }

    private void writeResults(MixedParameterList parameters, double time, ChemicalEntity entity) {
        String eol = System.getProperty("line.separator");
        try (Writer writer = new FileWriter("result_" + entity.getName() + ".csv", true)) {
            writer.append(parameters.toString()).append(", ").append(String.valueOf(time)).append(eol);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    public static void main(String[] args) {

        IntegerParameter numberOfNodes = new IntegerParameter("number of nodes", 10, 70);
        IntegerParameter timeStepSize = new IntegerParameter("time step size", 10, 1501);

        UniqueParameterList<Integer> numberOfNodesSample = ParameterSampler.sample(numberOfNodes, 31);
        UniqueParameterList<Integer> timeStepSizeSample = ParameterSampler.sample(timeStepSize, 22);

        DiffusionGridSearch gs = new DiffusionGridSearch(Arrays.asList(numberOfNodesSample, timeStepSizeSample));
        gs.search();

    }

}
