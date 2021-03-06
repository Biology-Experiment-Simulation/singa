package bio.singa.simulation.model.modules.concentration.imlementations;

import bio.singa.chemistry.entities.ChemicalEntity;
import bio.singa.chemistry.entities.ComplexEntity;
import bio.singa.chemistry.entities.Protein;
import bio.singa.chemistry.entities.SmallMolecule;
import bio.singa.chemistry.features.reactions.RateConstant;
import bio.singa.features.identifiers.ChEBIIdentifier;
import bio.singa.features.identifiers.UniProtIdentifier;
import bio.singa.features.parameters.Environment;
import bio.singa.features.units.UnitRegistry;
import bio.singa.mathematics.geometry.faces.Rectangle;
import bio.singa.mathematics.vectors.Vector2D;
import bio.singa.simulation.model.agents.pointlike.Vesicle;
import bio.singa.simulation.model.agents.pointlike.VesicleLayer;
import bio.singa.simulation.model.graphs.AutomatonGraph;
import bio.singa.simulation.model.graphs.AutomatonGraphs;
import bio.singa.simulation.model.graphs.AutomatonNode;
import bio.singa.simulation.model.modules.concentration.imlementations.reactions.ReactionBuilder;
import bio.singa.simulation.model.sections.CellRegion;
import bio.singa.simulation.model.sections.CellSubsection;
import bio.singa.simulation.model.sections.ConcentrationContainer;
import bio.singa.simulation.model.simulation.Simulation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tec.units.indriya.ComparableQuantity;
import tec.units.indriya.quantity.Quantities;

import javax.measure.Quantity;
import javax.measure.quantity.Length;
import javax.measure.quantity.Time;

import static bio.singa.features.units.UnitProvider.MOLE_PER_LITRE;
import static bio.singa.simulation.model.sections.CellSubsection.SECTION_A;
import static bio.singa.simulation.model.sections.CellTopology.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tec.units.indriya.unit.MetricPrefix.*;
import static tec.units.indriya.unit.Units.*;

/**
 * @author cl
 */
class ComplexBuildingReactionTest {

    @BeforeAll
    static void initialize() {
        UnitRegistry.reinitialize();
    }

    @AfterEach
    void cleanUp() {
        UnitRegistry.reinitialize();
    }

    @Test
    @DisplayName("complex building reaction - minimal setup")
    void minimalSetUpTest() {
        // create simulation
        Simulation simulation = new Simulation();

        // setup graph
        AutomatonGraph automatonGraph = AutomatonGraphs.singularGraph();
        simulation.setGraph(automatonGraph);

        // rate constants
        RateConstant forwardRate = RateConstant.create(1)
                .forward().secondOrder()
                .concentrationUnit(MOLE_PER_LITRE)
                .timeUnit(SECOND)
                .build();

        RateConstant backwardRate = RateConstant.create(1)
                .backward().firstOrder()
                .timeUnit(SECOND)
                .build();

        // reactants
        ChemicalEntity bindee = SmallMolecule.create("bindee").build();
        Protein binder = Protein.create("binder").build();
        ComplexEntity complex = ComplexEntity.from(binder, bindee);

        // create and add module
        ReactionBuilder.staticReactants(simulation)
                .addSubstrate(binder, MEMBRANE)
                .addSubstrate(bindee, OUTER)
                .addProduct(complex, MEMBRANE)
                .complexBuilding()
                .associationRate(forwardRate)
                .dissociationRate(backwardRate)
                .build();

        // set concentrations
        AutomatonNode membraneNode = automatonGraph.getNode(0, 0);
        membraneNode.setCellRegion(CellRegion.MEMBRANE);
        membraneNode.getConcentrationContainer().initialize(OUTER, bindee, Quantities.getQuantity(1.0, MOLE_PER_LITRE));
        membraneNode.getConcentrationContainer().initialize(MEMBRANE, binder, Quantities.getQuantity(1.0, MOLE_PER_LITRE));
        membraneNode.getConcentrationContainer().initialize(MEMBRANE, complex, Quantities.getQuantity(1.0, MOLE_PER_LITRE));

        // forward and backward reactions should cancel each other out
        for (int i = 0; i < 10; i++) {
            ConcentrationContainer container = membraneNode.getConcentrationContainer();

            assertEquals(0.0, container.get(INNER, bindee));
            assertEquals(0.0, container.get(INNER, binder));
            assertEquals(0.0, container.get(INNER, complex));

            assertEquals(0.0, container.get(MEMBRANE, bindee));
            assertEquals(1.0, UnitRegistry.concentration(container.get(MEMBRANE, binder)).to(MOLE_PER_LITRE).getValue().doubleValue());
            assertEquals(1.0, UnitRegistry.concentration(container.get(MEMBRANE, complex)).to(MOLE_PER_LITRE).getValue().doubleValue());

            assertEquals(1.0, UnitRegistry.concentration(container.get(OUTER, bindee)).to(MOLE_PER_LITRE).getValue().doubleValue());
            assertEquals(0, container.get(OUTER, binder));
            assertEquals(0, container.get(OUTER, complex));

            simulation.nextEpoch();
        }
    }

    @Test
    @DisplayName("complex building reaction - monovalent receptor binding")
    void testPrazosinExample() {
        UnitRegistry.setSpace(Quantities.getQuantity(1.0, MILLI(METRE)));

        // see Receptors (Lauffenburger) p. 30
        // prazosin, CHEBI:8364
        ChemicalEntity ligand = SmallMolecule.create("ligand")
                .additionalIdentifier(new ChEBIIdentifier("CHEBI:8364"))
                .build();

        // alpha-1 adrenergic receptor, P35348
        Protein receptor = Protein.create("receptor")
                .additionalIdentifier(new UniProtIdentifier("P35348"))
                .build();

        ComplexEntity complex = ComplexEntity.from(receptor, ligand);

        // create simulation
        Simulation simulation = new Simulation();

        // setup graph
        final AutomatonGraph automatonGraph = AutomatonGraphs.singularGraph();
        simulation.setGraph(automatonGraph);
        // concentrations
        AutomatonNode membraneNode = automatonGraph.getNode(0, 0);
        membraneNode.setCellRegion(CellRegion.MEMBRANE);
        membraneNode.getConcentrationContainer().initialize(SECTION_A, ligand, UnitRegistry.concentration(0.1, MOLE_PER_LITRE));
        membraneNode.getConcentrationContainer().initialize(CellSubsection.MEMBRANE, receptor, UnitRegistry.concentration(0.1, MOLE_PER_LITRE));

        // the corresponding rate constants
        RateConstant forwardsRate = RateConstant.create(2.4e8)
                .forward().secondOrder()
                .concentrationUnit(MOLE_PER_LITRE)
                .timeUnit(MINUTE)
                .build();

        RateConstant backwardsRate = RateConstant.create(0.018)
                .backward().firstOrder()
                .timeUnit(MINUTE)
                .build();

        // create and add module
        ReactionBuilder.staticReactants(simulation)
                .addSubstrate(receptor, MEMBRANE)
                .addSubstrate(ligand)
                .addProduct(complex, MEMBRANE)
                .complexBuilding()
                .associationRate(forwardsRate)
                .dissociationRate(backwardsRate)
                .build();

        // checkpoints
        Quantity<Time> currentTime;
        Quantity<Time> firstCheckpoint = Quantities.getQuantity(0.05, MILLI(SECOND));
        boolean firstCheckpointPassed = false;
        Quantity<Time> secondCheckpoint = Quantities.getQuantity(2.0, MILLI(SECOND));
        // run simulation
        while ((currentTime = simulation.getElapsedTime().to(MILLI(SECOND))).getValue().doubleValue() < secondCheckpoint.getValue().doubleValue()) {
            simulation.nextEpoch();
            if (!firstCheckpointPassed && currentTime.getValue().doubleValue() > firstCheckpoint.getValue().doubleValue()) {
                assertEquals(0.00476, UnitRegistry.concentration(membraneNode.getConcentrationContainer().get(CellSubsection.MEMBRANE, receptor)).to(MOLE_PER_LITRE).getValue().doubleValue(), 1e-3);
                assertEquals(0.00476, UnitRegistry.concentration(membraneNode.getConcentrationContainer().get(INNER, ligand)).to(MOLE_PER_LITRE).getValue().doubleValue(), 1e-3);
                assertEquals(0.09523, UnitRegistry.concentration(membraneNode.getConcentrationContainer().get(CellSubsection.MEMBRANE, complex)).to(MOLE_PER_LITRE).getValue().doubleValue(), 1e-3);
                firstCheckpointPassed = true;
            }
        }

        // check final values
        assertEquals(0.0001, UnitRegistry.concentration(membraneNode.getConcentrationContainer().get(CellSubsection.MEMBRANE, receptor)).to(MOLE_PER_LITRE).getValue().doubleValue(), 1e-3);
        assertEquals(0.0001, UnitRegistry.concentration(membraneNode.getConcentrationContainer().get(INNER, ligand)).to(MOLE_PER_LITRE).getValue().doubleValue(), 1e-3);
        assertEquals(0.0998, UnitRegistry.concentration(membraneNode.getConcentrationContainer().get(CellSubsection.MEMBRANE, complex)).to(MOLE_PER_LITRE).getValue().doubleValue(), 1e-3);
    }

    @Test
    @DisplayName("complex building reaction - simple section changing binding")
    void testMembraneAbsorption() {
        // create simulation
        Simulation simulation = new Simulation();

        // setup graph
        final AutomatonGraph automatonGraph = AutomatonGraphs.singularGraph();
        simulation.setGraph(automatonGraph);

        // rate constants
        RateConstant forwardRate = RateConstant.create(1.0e6)
                .forward().secondOrder()
                .concentrationUnit(MOLE_PER_LITRE)
                .timeUnit(MINUTE)
                .build();

        RateConstant backwardRate = RateConstant.create(0.01)
                .backward().firstOrder()
                .timeUnit(MINUTE)
                .build();

        // reactants
        ChemicalEntity bindee = SmallMolecule.create("bindee").build();
        Protein binder = Protein.create("binder").build();
        ComplexEntity complex = ComplexEntity.from(binder, bindee);

        // create and add module
        ReactionBuilder.staticReactants(simulation)
                .addSubstrate(binder, MEMBRANE)
                .addSubstrate(bindee, OUTER)
                .addProduct(complex, MEMBRANE)
                .complexBuilding()
                .associationRate(forwardRate)
                .dissociationRate(backwardRate)
                .build();

        // concentrations
        AutomatonNode membraneNode = automatonGraph.getNode(0, 0);
        membraneNode.setCellRegion(CellRegion.MEMBRANE);
        membraneNode.getConcentrationContainer().set(OUTER, bindee, 1.0);
        membraneNode.getConcentrationContainer().set(MEMBRANE, binder, 0.1);
        membraneNode.getConcentrationContainer().set(MEMBRANE, complex, 0.0);

        double previousConcentration = 0.0;
        for (int i = 0; i < 10; i++) {
            simulation.nextEpoch();
            double currentConcentration = membraneNode.getConcentrationContainer().get(CellSubsection.MEMBRANE, complex);
            assertTrue(currentConcentration > previousConcentration);
            previousConcentration = currentConcentration;
        }
    }

    @Test
    @DisplayName("complex building reaction - section changing binding with concurrent inside and outside reactions")
    void shouldReactInsideAndOutside() {
        // create simulation
        Simulation simulation = new Simulation();

        // setup graph
        final AutomatonGraph automatonGraph = AutomatonGraphs.singularGraph();
        simulation.setGraph(automatonGraph);

        // rate constants
        RateConstant innerForwardsRateConstant = RateConstant.create(1.0e6)
                .forward().secondOrder()
                .concentrationUnit(MOLE_PER_LITRE)
                .timeUnit(MINUTE)
                .build();

        RateConstant innerBackwardsRateConstant = RateConstant.create(0.01)
                .backward().firstOrder()
                .timeUnit(MINUTE)
                .build();

        RateConstant outerForwardsRateConstant = RateConstant.create(1.0e6)
                .forward().secondOrder()
                .concentrationUnit(MOLE_PER_LITRE)
                .timeUnit(MINUTE)
                .build();

        RateConstant outerBackwardsRateConstant = RateConstant.create(0.01)
                .backward().firstOrder()
                .timeUnit(MINUTE)
                .build();

        // reactants
        ChemicalEntity innerBindee = SmallMolecule.create("inner bindee").build();
        ChemicalEntity outerBindee = SmallMolecule.create("outer bindee").build();
        Protein binder = Protein.create("binder").build();
        ComplexEntity innerComplex = ComplexEntity.from(binder, innerBindee);
        ComplexEntity outerComplex = ComplexEntity.from(binder, outerBindee);


        // create and add modules
        ReactionBuilder.staticReactants(simulation)
                .addSubstrate(binder, MEMBRANE)
                .addSubstrate(innerBindee, INNER)
                .addProduct(innerComplex, MEMBRANE)
                .complexBuilding()
                .associationRate(innerForwardsRateConstant)
                .dissociationRate(innerBackwardsRateConstant)
                .build();

        ReactionBuilder.staticReactants(simulation)
                .addSubstrate(binder, MEMBRANE)
                .addSubstrate(outerBindee, OUTER)
                .addProduct(outerComplex, MEMBRANE)
                .complexBuilding()
                .associationRate(outerForwardsRateConstant)
                .dissociationRate(outerBackwardsRateConstant)
                .build();

        // concentrations
        AutomatonNode membraneNode = automatonGraph.getNode(0, 0);
        membraneNode.setCellRegion(CellRegion.MEMBRANE);

        membraneNode.getConcentrationContainer().initialize(INNER, innerBindee, Quantities.getQuantity(0.1, MOLE_PER_LITRE));
        membraneNode.getConcentrationContainer().initialize(OUTER, outerBindee, Quantities.getQuantity(0.1, MOLE_PER_LITRE));
        membraneNode.getConcentrationContainer().initialize(MEMBRANE, binder, Quantities.getQuantity(0.1, MOLE_PER_LITRE));

        double previousInnerConcentration = 0.0;
        double previousOuterConcentration = 0.0;
        for (int i = 0; i < 10; i++) {
            simulation.nextEpoch();
            // inner assertions
            double currentInnerConcentration = membraneNode.getConcentrationContainer().get(MEMBRANE, innerComplex);
            assertTrue(currentInnerConcentration > previousInnerConcentration);
            previousInnerConcentration = currentInnerConcentration;
            // outer assertions
            double currentOuterConcentration = membraneNode.getConcentrationContainer().get(MEMBRANE, outerComplex);
            assertTrue(currentOuterConcentration > previousOuterConcentration);
            previousOuterConcentration = currentOuterConcentration;
        }
    }

    @Test
    @DisplayName("complex building reaction - section changing binding with fully contained vesicle")
    void testComplexBuildingWithVesicle() {
        double simulationExtend = 150;
        int nodesHorizontal = 3;
        int nodesVertical = 3;

        Rectangle rectangle = new Rectangle(simulationExtend, simulationExtend);
        Simulation simulation = new Simulation();
        simulation.setSimulationRegion(rectangle);

        // setup scaling
        ComparableQuantity<Length> systemExtend = Quantities.getQuantity(1, MICRO(METRE));
        Environment.setSystemExtend(systemExtend);
        Environment.setSimulationExtend(simulationExtend);
        Environment.setNodeSpacingToDiameter(systemExtend, nodesHorizontal);

        // setup graph and assign regions
        AutomatonGraph graph = AutomatonGraphs.createRectangularAutomatonGraph(nodesHorizontal, nodesVertical);
        simulation.setGraph(graph);

        // rate constants
        RateConstant forwardRate = RateConstant.create(1.0e6)
                .forward().secondOrder()
                .concentrationUnit(MOLE_PER_LITRE)
                .timeUnit(MINUTE)
                .build();

        RateConstant backwardRate = RateConstant.create(0.01)
                .backward().firstOrder()
                .timeUnit(MINUTE)
                .build();

        // reactants
        ChemicalEntity bindee = SmallMolecule.create("bindee").build();
        Protein binder = new Protein.Builder("binder").build();
        ComplexEntity complex = ComplexEntity.from(binder, bindee);

        // create and add module
        ReactionBuilder.staticReactants(simulation)
                .addSubstrate(binder, MEMBRANE)
                .addSubstrate(bindee, INNER)
                .addProduct(complex, MEMBRANE)
                .complexBuilding()
                .associationRate(forwardRate)
                .dissociationRate(backwardRate)
                .build();

        // initialize vesicle layer
        VesicleLayer vesicleLayer = new VesicleLayer(simulation);
        simulation.setVesicleLayer(vesicleLayer);

        // vesicle contained
        Vesicle vesicle = new Vesicle(new Vector2D(25.0, 25.0), Quantities.getQuantity(20, NANO(METRE)));
        vesicle.getConcentrationContainer().initialize(MEMBRANE, binder, Quantities.getQuantity(0.1, MOLE_PER_LITRE));
        vesicle.getConcentrationContainer().initialize(MEMBRANE, complex, Quantities.getQuantity(0.0, MOLE_PER_LITRE));
        vesicleLayer.addVesicle(vesicle);

        // concentrations
        AutomatonNode node = graph.getNode(0, 0);
        node.getConcentrationContainer().initialize(INNER, bindee, Quantities.getQuantity(1.0, MOLE_PER_LITRE));

        double previousNodeConcentration = 1.0;
        double previousVesicleConcentration = 0.0;
        for (int i = 0; i < 10; i++) {
            simulation.nextEpoch();
            // node assertion
            double currentNodeConcentration = node.getConcentrationContainer().get(INNER, bindee);
            assertTrue(currentNodeConcentration < previousNodeConcentration);
            previousNodeConcentration = currentNodeConcentration;
            // vesicle assertion
            double currentVesicleConcentration = vesicle.getConcentrationContainer().get(MEMBRANE, complex);
            assertTrue(currentVesicleConcentration > previousVesicleConcentration);
            previousVesicleConcentration = currentVesicleConcentration;
        }
    }


    @Test
    @DisplayName("complex building reaction - section changing binding with partially contained vesicle")
    void testComplexBuildingWithPartialVesicle() {
        double simulationExtend = 150;
        int nodesHorizontal = 3;
        int nodesVertical = 3;

        Rectangle rectangle = new Rectangle(simulationExtend, simulationExtend);
        Simulation simulation = new Simulation();
        simulation.setSimulationRegion(rectangle);

        // setup scaling
        ComparableQuantity<Length> systemExtend = Quantities.getQuantity(1, MICRO(METRE));
        Environment.setSystemExtend(systemExtend);
        Environment.setSimulationExtend(simulationExtend);
        Environment.setNodeSpacingToDiameter(systemExtend, nodesHorizontal);

        // setup graph and assign regions
        AutomatonGraph graph = AutomatonGraphs.createRectangularAutomatonGraph(nodesHorizontal, nodesVertical);
        simulation.setGraph(graph);

        // rate constants
        RateConstant forwardRate = RateConstant.create(1.0e6)
                .forward().secondOrder()
                .concentrationUnit(MOLE_PER_LITRE)
                .timeUnit(MINUTE)
                .build();

        RateConstant backwardRate = RateConstant.create(0.01)
                .backward().firstOrder()
                .timeUnit(MINUTE)
                .build();

        // reactants
        ChemicalEntity bindee = SmallMolecule.create("bindee").build();
        Protein binder = Protein.create("binder").build();
        ComplexEntity complex = ComplexEntity.from(binder, bindee);

        // create and add module
        ReactionBuilder.staticReactants(simulation)
                .addSubstrate(binder, MEMBRANE)
                .addSubstrate(bindee, INNER)
                .addProduct(complex, MEMBRANE)
                .complexBuilding()
                .associationRate(forwardRate)
                .dissociationRate(backwardRate)
                .build();

        // initialize vesicle layer
        VesicleLayer vesicleLayer = new VesicleLayer(simulation);
        simulation.setVesicleLayer(vesicleLayer);

        // vesicle contained
        Vesicle vesicle = new Vesicle(new Vector2D(25.0, 50.0), Quantities.getQuantity(20, NANO(METRE)));
        vesicle.getConcentrationContainer().initialize(MEMBRANE, binder, Quantities.getQuantity(0.1, MOLE_PER_LITRE));
        vesicle.getConcentrationContainer().initialize(MEMBRANE, complex, Quantities.getQuantity(0.0, MOLE_PER_LITRE));
        vesicleLayer.addVesicle(vesicle);

        // concentrations
        AutomatonNode first = graph.getNode(0, 0);
        first.getConcentrationContainer().initialize(INNER, bindee, Quantities.getQuantity(1.0, MOLE_PER_LITRE));
        AutomatonNode second = graph.getNode(0, 1);
        second.getConcentrationContainer().initialize(INNER, bindee, Quantities.getQuantity(0.5, MOLE_PER_LITRE));

        // checkpoints
        Quantity<Time> firstCheckpoint = Quantities.getQuantity(50, MICRO(SECOND));
        boolean firstCheckpointPassed = false;
        Quantity<Time> secondCheckpoint = Quantities.getQuantity(500, MICRO(SECOND));
        // run simulation
        while (simulation.getElapsedTime().isLessThanOrEqualTo(secondCheckpoint)) {
            simulation.nextEpoch();
            if (!firstCheckpointPassed && simulation.getElapsedTime().isGreaterThanOrEqualTo(firstCheckpoint)) {
                assertEquals(9.695E-7, first.getConcentrationContainer().get(INNER, bindee), 1e-10);
                assertEquals(4.847E-7, second.getConcentrationContainer().get(INNER, bindee), 1e-10);
                assertEquals(4.561E-8, vesicle.getConcentrationContainer().get(MEMBRANE, complex), 1e-10);
                firstCheckpointPassed = true;
            }
        }

        // check final values
        assertEquals(9.335E-7, first.getConcentrationContainer().get(INNER, bindee), 1e-10);
        assertEquals(4.667E-7, second.getConcentrationContainer().get(INNER, bindee), 1e-10);
        assertEquals(9.972E-8, vesicle.getConcentrationContainer().get(MEMBRANE, complex), 1e-10);
    }

}
