package de.bioforscher.singa.chemistry.parser.uniprot;

import de.bioforscher.singa.chemistry.descriptive.Protein;
import de.bioforscher.singa.chemistry.descriptive.annotations.Annotation;
import de.bioforscher.singa.chemistry.descriptive.annotations.AnnotationType;
import de.bioforscher.singa.core.biology.Organism;
import de.bioforscher.singa.core.biology.Taxon;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;

/**
 * @author cl
 */
public class UniProtParserTest {

    //@Test
    public void testWholeSwissProt() {
        String accession_number_list = Thread.currentThread().getContextClassLoader().getResource("uniprot_all_accession_numbers.list").toString();
        Path path = Paths.get(accession_number_list.substring(6));

        try {
            int totalNumber = (int)Files.lines(path).count();
            final int[] i = {0};
            Files.lines(path).forEach( accession -> {
                if (i[0] % (10) == 0) {
                    System.out.println(i[0] + " of " + totalNumber + " parsed");
                }
                Protein entity = UniProtParserService.parse(accession);
                assertTrue(!entity.getName().equals(""));
                i[0]++;
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testExemplaryUniProtAccession() {
        Protein entity = UniProtParserService.parse("P12345");
        System.out.println("Primary name:");
        System.out.println(entity.getName());
        System.out.println();

        System.out.println("Additional Names:");
        List<String> names = entity.getAdditionalNames();
        names.forEach(System.out::println);
        System.out.println();

        System.out.println("Sequence");
        for (String sequence : entity.getAllAminoAcidSequences()) {
            System.out.println(sequence);
            System.out.println();
        }

        System.out.println("Organism");
        for (Organism organism : entity.getAllOrganisms()) {
            System.out.println(organism.getName());
            System.out.println(organism.getCommonName());
            System.out.println("TaxID: " + organism.getIdentifier());
            System.out.println(organism.getLineage()
                    .stream()
                    .map(Taxon::getName)
                    .collect(Collectors.joining(" - " + "")));
            System.out.println();
        }

        System.out.println("Notes");
        for (Annotation note: entity.getAnnotationsOfType(AnnotationType.NOTE)) {
            System.out.println(note.getDescription()+": "+note.getContent());
        }
    }

}