package de.bioforscher.chemistry.algorithms.superimposition.fit3d;

import de.bioforscher.chemistry.algorithms.superimposition.SubstructureSuperimposition;
import de.bioforscher.chemistry.parser.pdb.structures.PDBParserService;
import de.bioforscher.chemistry.parser.pdb.structures.PDBWriterService;
import de.bioforscher.chemistry.parser.pdb.structures.StructureCollector;
import de.bioforscher.chemistry.physical.atoms.AtomFilter;
import de.bioforscher.chemistry.physical.atoms.representations.RepresentationSchemeType;
import de.bioforscher.chemistry.physical.branches.StructuralMotif;
import de.bioforscher.chemistry.physical.families.NucleotideFamily;
import de.bioforscher.chemistry.physical.families.ResidueFamily;
import de.bioforscher.chemistry.physical.model.LeafIdentifers;
import de.bioforscher.chemistry.physical.model.LeafIdentifier;
import de.bioforscher.chemistry.physical.model.Structure;
import de.bioforscher.mathematics.combinatorics.StreamCombinations;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * A test for the implementation of the Fit3D algorithm.
 *
 * @author fk
 */
public class Fit3DAlignmentTest {

    private StructuralMotif queryMotif;
    private Structure target;

    @Before
    public void setUp() throws IOException {
        this.target = PDBParserService.parseProteinById("1GL0");
        Structure motifContainingStructure = PDBParserService.parsePDBFile(Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("1GL0_HDS_intra_E-H57_E-D102_E-S195.pdb"));
        this.queryMotif = StructuralMotif.fromLeafs(1, motifContainingStructure,
                LeafIdentifers.of("E-57", "E-102", "E-195"));
        this.queryMotif.addExchangableType(LeafIdentifier.fromString("E-57"), ResidueFamily.GLUTAMIC_ACID);
    }

    @Test
    public void shouldRunFit3DAlignment() {
        Fit3D fit3d = Fit3DBuilder.create()
                .query(this.queryMotif)
                .target(this.target.getAllChains().get(0))
                .run();
        TreeMap<Double, SubstructureSuperimposition> matches = fit3d.getMatches();
        assertEquals(0.0005, matches.firstKey(), 1E-4);
    }

    @Test
    public void shouldRunFit3DAlignmentBatch() throws IOException {
        StructureCollector.parseLigandInformation = false;
        Structure nucleotideTarget = PDBParserService.parseProteinById("2EES", "A");
        StructuralMotif nucleotideMotif = StructuralMotif.fromLeafs(1, nucleotideTarget,
                LeafIdentifers.of("A-22", "A-51", "A-52", "A-74"));
        nucleotideMotif.addExchangableType(LeafIdentifier.fromString("A-74"), NucleotideFamily.URIDINE);
        List<String> targetStructures = Files.list(
                Paths.get("src/test/resources/RF00167"))
                .map(Path::toString)
                .collect(Collectors.toList());
        Fit3D fit3dBatch = Fit3DBuilder.create()
                .query(nucleotideMotif)
                .targets(targetStructures)
                .maximalParallelism()
                .run();
        assertTrue(fit3dBatch.getMatches().size() == 218);
    }

    @Test
    public void shouldRunFindIntraMolecularMatches() throws IOException {
        Structure target = PDBParserService.parseProteinById("4CHA");
        StructuralMotif queryMotif = StructuralMotif.fromLeafs(1, target,
                LeafIdentifers.of("B-57", "B-102", "C-195"));
        Fit3D fit3d = Fit3DBuilder.create()
                .query(queryMotif)
                .target(target.getAllModels().get(0))
                .run();
        TreeMap<Double, SubstructureSuperimposition> matches = fit3d.getMatches();
        assertEquals(0.0000, matches.firstKey(), 1E-6);
    }

    @Test
    public void shouldGenerateCombinations() {
        assertEquals(1L, StreamCombinations.combinations(3, this.queryMotif.getLeafSubstructures()).count());
    }

    @Test
    public void shouldAlignNucleotideMotif() throws IOException {
        Structure nucleotideTarget = PDBParserService.parseProteinById("2EES", "A");
        StructuralMotif nucleotideMotif = StructuralMotif.fromLeafs(1, nucleotideTarget,
                LeafIdentifers.of("A-22", "A-51", "A-52", "A-74"));

        PDBWriterService.writeBranchSubstructures(nucleotideMotif, Paths.get("/tmp/a/query.pdb"));

        nucleotideMotif.addExchangableType(LeafIdentifier.fromString("A-74"), NucleotideFamily.URIDINE);
        Fit3D fit3d = Fit3DBuilder.create()
                .query(nucleotideMotif)
                .target(nucleotideTarget.getAllChains().get(0))
                .run();
        TreeMap<Double, SubstructureSuperimposition> matches = fit3d.getMatches();

        assertEquals(0.0, matches.firstKey(), 1E-6);
    }

//    @Test
//    public void shouldAlignKDEEH() throws IOException {
//
//        Structure target1 = PDBParserService.parseProteinById("1BKH","A");
//
//        // KDEEH template motif
//        Structure motifContainingStructure = PDBParserService.parsePDBFile("motif_KDEEH.pdb");
//        StructuralMotif motif = StructuralMotif.fromLeafs(1, motifContainingStructure,
//                LeafIdentifers.of("A-164", "A-195", "A-221", "A-247", "A-297"));
//        motif.addExchangableType(LeafIdentifier.fromString("A-164"), ResidueFamily.HISTIDINE);
//        motif.addExchangableType(LeafIdentifier.fromString("E-247"), ResidueFamily.ASPARTIC_ACID);
//        motif.addExchangableType(LeafIdentifier.fromString("E-247"), ResidueFamily.ASPARAGINE);
//        motif.addExchangableType(LeafIdentifier.fromString("H-297"), ResidueFamily.LYSINE);
//
//        Fit3DAlignment fit3d = new Fit3DAlignment(motif, target1.getAllBranches().get(0), 3.5, 3.0,
//                RepresentationSchemeFactory.createRepresentationScheme(RepresentationSchemeType.SIDECHAIN_CENTROID));
//    }
}