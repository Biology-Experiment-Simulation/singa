package de.bioforscher.singa.structure.parser.pdb.structures;


import de.bioforscher.singa.structure.model.interfaces.*;
import de.bioforscher.singa.structure.model.oak.OakStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;


/**
 * A class to write Structures objects to PDB format.
 *
 * @author fk
 */
public class StructureWriter {

    private static final Logger logger = LoggerFactory.getLogger(StructureWriter.class);

    /**
     * Prevent instantiation.
     */
    private StructureWriter() {

    }

    /**
     * Writes a given {@link LeafSubstructureContainer} in PDB format.
     *
     * @param leafSubstructureContainer The {@link LeafSubstructureContainer} to be written.
     * @param outputPath The output {@link Path}.
     * @throws IOException If the path cannot be written.
     */
    public static void writeLeafSubstructureContainer(LeafSubstructureContainer leafSubstructureContainer, Path outputPath) throws IOException {
        logger.info("Writing branch substructure {} to {}.", leafSubstructureContainer, outputPath);
        writeLeafSubstructures(leafSubstructureContainer.getAllLeafSubstructures(), outputPath);
    }

    /**
     * Writes a given list of {@link LeafSubstructure}s in PDB format.
     *
     * @param leafSubstructures The list of {@link LeafSubstructure}s to be written.
     * @param outputPath The output {@link Path}.
     * @throws IOException If the path cannot be written.
     */
    public static void writeLeafSubstructures(List<LeafSubstructure<?>> leafSubstructures, Path outputPath) throws IOException {
        logger.info("Writing {} leaf substructures to {}.", leafSubstructures.size(), outputPath);
        Files.createDirectories(outputPath.getParent());
        Files.write(outputPath, StructureRepresentation.composePdbRepresentation(leafSubstructures).getBytes());
    }

    /**
     * Writes a {@link Structure} in PDB format.
     *
     * @param structure The structure to be written.
     * @param outputPath The output {@link Path}.
     * @throws IOException If the path cannot be written.
     */
    public static void writeStructure(OakStructure structure, Path outputPath) throws IOException {
        logger.info("Writing structure {} to {}.", structure, outputPath);
        Files.createDirectories(outputPath.getParent());
        Files.write(outputPath, StructureRepresentation.composePdbRepresentation(structure).getBytes());
    }

    public static void writeToXYZ(AtomContainer atomContainer, Path outputPath) throws IOException {
        StringBuilder builder = new StringBuilder();
        DecimalFormat coordinateFormat = new DecimalFormat("0.00000", new DecimalFormatSymbols(Locale.US));
        builder.append(atomContainer.getAllAtoms().size())
                .append(System.lineSeparator())
                .append(System.lineSeparator());

        for (Atom atom : atomContainer.getAllAtoms()) {
            builder.append(atom.getElement().getSymbol()).append("\t")
                    .append(coordinateFormat.format(atom.getPosition().getX())).append("\t")
                    .append(coordinateFormat.format(atom.getPosition().getY())).append("\t")
                    .append(coordinateFormat.format(atom.getPosition().getZ())).append(System.lineSeparator());
        }

        Files.createDirectories(outputPath.getParent());
        Files.write(outputPath, builder.toString().getBytes());

    }

}