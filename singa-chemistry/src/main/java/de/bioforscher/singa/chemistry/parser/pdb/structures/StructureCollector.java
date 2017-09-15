package de.bioforscher.singa.chemistry.parser.pdb.structures;

import de.bioforscher.singa.chemistry.parser.pdb.ligands.LigandParserService;
import de.bioforscher.singa.chemistry.parser.pdb.structures.StructureParser.Reducer;
import de.bioforscher.singa.chemistry.parser.pdb.structures.tokens.*;
import de.bioforscher.singa.chemistry.physical.atoms.Atom;
import de.bioforscher.singa.chemistry.physical.branches.Chain;
import de.bioforscher.singa.chemistry.physical.branches.StructuralModel;
import de.bioforscher.singa.chemistry.physical.families.AminoAcidFamily;
import de.bioforscher.singa.chemistry.physical.families.LeafFactory;
import de.bioforscher.singa.chemistry.physical.families.LigandFamily;
import de.bioforscher.singa.chemistry.physical.families.NucleotideFamily;
import de.bioforscher.singa.chemistry.physical.leaves.AminoAcid;
import de.bioforscher.singa.chemistry.physical.leaves.AtomContainer;
import de.bioforscher.singa.chemistry.physical.leaves.LeafSubstructure;
import de.bioforscher.singa.chemistry.physical.leaves.Nucleotide;
import de.bioforscher.singa.chemistry.physical.model.LeafIdentifier;
import de.bioforscher.singa.chemistry.physical.model.Structure;
import de.bioforscher.singa.chemistry.physical.model.UniqueAtomIdentifer;
import de.bioforscher.singa.core.identifier.PDBIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * The actual processing of pdb files. This class collects all required information form the a list of lines from a pdb
 * file.
 *
 * @author cl
 */
public class StructureCollector {

    /**
     * The logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(StructureCollector.class);

    /**
     * The currently parsed pdb file.
     */
    private String currentPDB = LeafIdentifier.DEFAULT_PDB_IDENTIFIER;

    /**
     * The current model.
     */
    private int currentModel = LeafIdentifier.DEFAULT_MODEL_IDENTIFIER;

    /**
     * The current chain.
     */
    private String currentChain = LeafIdentifier.DEFAULT_CHAIN_IDENTIFIER;

    /**
     * The string builder building the title.
     */
    private StringBuilder titleBuilder = new StringBuilder();

    /**
     * A cache of all atoms identified by unique atom identifiers.
     */
    private Map<UniqueAtomIdentifer, Atom> atoms;

    /**
     * A cache of all leafs and their three letter codes.
     */
    private Map<LeafIdentifier, String> leafCodes;

    /**
     * Remembers all leafs that have been parsed from HETATM entries.
     */
    private Set<LeafIdentifier> hetAtoms;

    /**
     * Remembers all leafs the were part of the consecutive part of the chain.
     */
    private Set<LeafIdentifier> notInConsecutiveChain;

    /**
     * Chains that have already been terminated by a terminate record.
     */
    private Set<String> closedChains;

    /**
     * The root node of the content tree.
     */
    private ContentTreeNode contentTree;

    /**
     * The reducer containing the information of what should be parsed and how it should be done.
     */
    private Reducer reducer;

    /**
     * The list of relevant pdb lines.
     */
    private List<String> pdbLines;

    /**
     * Creates a new structure collector to extract structural information from pdb lines and reducing information.
     *
     * @param pdbLines The lines of a pdb file.
     * @param reducer The information on what should be parsed and how it should be done.
     */
    private StructureCollector(List<String> pdbLines, Reducer reducer) {
        this.reducer = reducer;
        this.pdbLines = pdbLines;
        this.atoms = new HashMap<>();
        this.leafCodes = new TreeMap<>();
        this.hetAtoms = new HashSet<>();
        this.notInConsecutiveChain = new HashSet<>();
        this.closedChains = new HashSet<>();
    }

    /**
     * parses a structure from pdb lines and reducing information.
     *
     * @param pdbLines The lines of a pdb file.
     * @param reducer The information on what should be parsed and how it should be done.
     * @return The resulting structure.
     * @throws StructureParserException if any problem occur during parsing.
     */
    static Structure parse(List<String> pdbLines, Reducer reducer) throws StructureParserException {
        StructureCollector collector = new StructureCollector(pdbLines, reducer);
        collector.reduceLines();
        return collector.collectStructure();
    }

    /**
     * Reduces the lines as described in the {@link Reducer}.
     *
     * @throws StructureParserException if any problem occur during reducing.
     */
    private void reduceLines() throws StructureParserException {
        String firstLine = this.pdbLines.get(0);
        // parse meta information
        if (this.reducer.options.isInferringIdentifierFromFileName()) {
            String currentSource = this.reducer.sourceSelector.contentIterator.getCurrentSource();
            String identifier = PDBIdentifier.extractFirst(currentSource);
            if (identifier != null) {
                this.currentPDB = identifier;
            }
        } else {
            if (HeaderToken.RECORD_PATTERN.matcher(firstLine).matches()) {
                this.currentPDB = HeaderToken.ID_CODE.extract(firstLine);
            }
        }
        getTitle();
        if (this.reducer.parseMapping) {
            this.reducer.updatePdbIdentifer();
            this.reducer.updateChainIdentifier();
            reduceToChain(this.reducer.chainIdentifier);
            logger.info("Parsing structure {} chainIdentifier {}", this.reducer.pdbIdentifier, this.reducer.chainIdentifier);
        } else {
            if (!this.reducer.allModels) {
                // parse only specific model
                // reduce lines to specific model
                reduceToModel(this.reducer.modelIdentifier);
            }
            if (!this.reducer.allChains) {
                // parse only specific chainIdentifier
                // reduce lines to specific chainIdentifier
                reduceToChain(this.reducer.chainIdentifier);
            }
        }
    }

    /**
     * Extracts the title from tha pdb header.
     */
    private void getTitle() {
        if (this.reducer.options.isInferringTitleFromFileName()) {
            this.titleBuilder.append(this.reducer.sourceSelector.contentIterator.getCurrentSource());
        } else {
            boolean titleFound = false;
            for (String currentLine : this.pdbLines) {
                // check if title line
                if (TitleToken.RECORD_PATTERN.matcher(currentLine).matches()) {
                    // if this is the first time such a line occurs, the title was found
                    if (!titleFound) {
                        titleFound = true;
                    }
                    // append title
                    this.titleBuilder.append(trimEnd(TitleToken.TEXT.extract(currentLine)));
                } else {
                    // if title has been found and a line with another content is found
                    if (titleFound) {
                        // quit parsing title
                        return;
                    }
                }
            }
        }
    }

    /**
     * Removes trailing whitespaces.
     * @param source The original string.
     * @return The original string without trailing white spaces.
     */
    public static String trimEnd(String source) {
        int pos = source.length() - 1;
        while ((pos >= 0) && Character.isWhitespace(source.charAt(pos))) {
            pos--;
        }
        pos++;
        return (pos < source.length()) ? source.substring(0, pos) : source;
    }


    /**
     * Keeps only lines, that belong to a certain model.
     *
     * @param modelIdentifier The identifier of the model.
     */
    private void reduceToModel(int modelIdentifier) {
        List<String> reducedList = new ArrayList<>();
        boolean collectLines = false;
        // for each line
        for (String currentLine : this.pdbLines) {
            // check if the correct model has begun
            if (ModelToken.RECORD_PATTERN.matcher(currentLine).matches()) {
                int currentModel = Integer.valueOf(ModelToken.MODEL_SERIAL.extract(currentLine));
                // turn on collection of lines
                if (currentModel == modelIdentifier) {
                    this.currentModel = currentModel;
                    collectLines = true;
                    continue;
                }
            }
            // check if the terminator is hit prevent further parsing
            if (collectLines && TerminatorTokens.MODEL_TERMINATOR.matcher(currentLine).matches()) {
                break;
            }
            // collect lines if we are in the correct model
            if (collectLines) {
                reducedList.add(currentLine);
            }
        }
        this.pdbLines = reducedList;
    }

    /**
     * Keeps only lines, that belong to a certain model.
     *
     * @param chainIdentifier The identifier of the model.
     */
    private void reduceToChain(String chainIdentifier) {
        List<String> reducedList = new ArrayList<>();
        // for each line
        for (String currentLine : this.pdbLines) {
            // check if this is a atom line
            if (AtomToken.RECORD_PATTERN.matcher(currentLine).matches()) {
                String currentChain = AtomToken.CHAIN_IDENTIFIER.extract(currentLine);
                // collect line if it has the correct chainIdentifier
                if (currentChain.equals(chainIdentifier)) {
                    reducedList.add(currentLine);
                }
            } else if (ModelToken.RECORD_PATTERN.matcher(currentLine).matches()) {
                // keel lines that indicate models
                reducedList.add(currentLine);
            } else if (ChainTerminatorToken.RECORD_PATTERN.matcher(currentLine).matches()) {
                // keel lines that indicate models
                reducedList.add(currentLine);
            }
        }
        this.pdbLines = reducedList;
    }

    /**
     * Collects and creates the actual structure from all remaining lines.
     *
     * @return The parsde structure.
     */
    private Structure collectStructure() {
        collectAtomInformation();
        createContentTree();

        logger.debug("Creating structure for {}", this.contentTree.getIdentifier());

        Structure structure = new Structure();
        structure.setPdbIdentifier(this.contentTree.getIdentifier());
        structure.setTitle(this.titleBuilder.toString());

        int chainGraphId = 0;
        for (ContentTreeNode modelNode : this.contentTree.getNodesFromLevel(ContentTreeNode.StructureLevel.MODEL)) {
            logger.debug("Collecting chains for model {}", modelNode.getIdentifier());
            StructuralModel model = new StructuralModel(Integer.valueOf(modelNode.getIdentifier()));
            for (ContentTreeNode chainNode : modelNode.getNodesFromLevel(ContentTreeNode.StructureLevel.CHAIN)) {
                logger.trace("Collecting leafs for chain {}", chainNode.getIdentifier());
                Chain chain = new Chain(chainNode.getIdentifier());
                for (ContentTreeNode leafNode : chainNode.getNodesFromLevel(ContentTreeNode.StructureLevel.LEAF)) {
                    LeafSubstructure<?, ?> leafSubstructure = assignLeaf(leafNode, Integer.valueOf(modelNode.getIdentifier()), chainNode.getIdentifier());
                    if (this.hetAtoms.contains(leafSubstructure.getIdentifier())) {
                        leafSubstructure.setAnnotatedAsHetAtom(true);
                    }
                    if (this.notInConsecutiveChain.contains(leafSubstructure.getIdentifier())) {
                        chain.addSubstructure(leafSubstructure);
                    } else {
                        chain.addToConsecutivePart(leafSubstructure);
                    }
                }
                model.addSubstructure(chain);
            }
            structure.addBranchSubstructure(model);
        }
        if (this.reducer.options.isCreatingEdges()) {
            structure.getAllChains().forEach(Chain::connectChainBackbone);
        }
        UniqueAtomIdentifer lastAtom = Collections.max(atoms.keySet());
        structure.setLastAddedAtomIdentifier(lastAtom.getAtomSerial());
        return structure;
    }

    /**
     * Collects information from atom and hetatm lines.
     */
    private void collectAtomInformation() {
        logger.debug("Collecting information from {} PDB lines", this.pdbLines.size());
        for (String currentLine : this.pdbLines) {
            String currentRecordType = AtomToken.RECORD_TYPE.extract(currentLine);
            if (AtomToken.RECORD_PATTERN.matcher(currentRecordType).matches()) {
                UniqueAtomIdentifer identifier = createUniqueAtomIdentifier(currentLine);
                this.atoms.put(identifier, AtomToken.assembleAtom(currentLine));
                LeafIdentifier leafIdentifier = new LeafIdentifier(identifier.getPdbIdentifier(),
                        identifier.getModelIdentifier(), identifier.getChainIdentifier(),
                        identifier.getLeafSerial(), identifier.getLeafInsertionCode());
                this.currentChain = leafIdentifier.getChainIdentifier();
                if (currentRecordType.equals("HETATM")) {
                    this.hetAtoms.add(leafIdentifier);
                }
                // add everything before termination record to consecutive chain
                if (this.closedChains.contains(this.currentModel + "-" + this.currentChain)) {
                    this.notInConsecutiveChain.add(leafIdentifier);
                }
                this.leafCodes.put(leafIdentifier, AtomToken.RESIDUE_NAME.extract(currentLine));
            } else if (currentRecordType.equals("MODEL")) {
                this.currentModel = Integer.valueOf(ModelToken.MODEL_SERIAL.extract(currentLine));
            } else if (currentRecordType.equals("TER")) {
                this.closedChains.add(this.currentModel + "-" + this.currentChain);
            }
        }
    }

    /**
     * Places each atom in the content tree.
     */
    private void createContentTree() {
        logger.debug("Creating content tree.");
        this.contentTree = new ContentTreeNode(this.currentPDB, ContentTreeNode.StructureLevel.STRUCTURE);
        this.atoms.forEach((identifer, atom) -> this.contentTree.appendAtom(atom, identifer));
        if (this.atoms.isEmpty()) {
            throw new StructureParserException("Unable to apply the reduction, supplied with the reducer: " + this.reducer);
        }
    }

    /**
     * Creates a unique atom identifier for the given atom line.
     *
     * @param atomLine The atom line.
     * @return An unique atom identifier.
     */
    private UniqueAtomIdentifer createUniqueAtomIdentifier(String atomLine) {
        int atomSerial = Integer.valueOf(AtomToken.ATOM_SERIAL.extract(atomLine));
        String chain = AtomToken.CHAIN_IDENTIFIER.extract(atomLine);
        int leaf = Integer.valueOf(AtomToken.RESIDUE_SERIAL.extract(atomLine));
        String insertion = AtomToken.RESIDUE_INSERTION.extract(atomLine);
        char insertionCode = insertion.isEmpty() ? ' ' : insertion.charAt(0);
        return new UniqueAtomIdentifer(this.currentPDB, this.currentModel, chain, leaf, insertionCode, atomSerial);
    }

    /**
     * Chooses which kind of leaf to create and returns the assembled {@link LeafSubstructure}.
     *
     * @param leafNode The {@link ContentTreeNode} of a leaf.
     * @param modelIdentifier The model of the leaf.
     * @param chainIdentifer The chain of the leaf.
     * @return The assembled leaf.
     */
    private LeafSubstructure<?, ?> assignLeaf(ContentTreeNode leafNode, int modelIdentifier, String chainIdentifer) {
        // generate leaf pdbIdentifier
        LeafIdentifier leafIdentifier = new LeafIdentifier(this.currentPDB, modelIdentifier, chainIdentifer, Integer.valueOf(leafNode.getIdentifier()), leafNode.getInsertionCode());
        // get leaf name for leaf identifer
        String leafName = this.leafCodes.get(leafIdentifier);
        // get atoms of this leaf
        Map<String, Atom> atoms = leafNode.getAtomMap();
        // log it
        logger.trace("Creating leaf {}-{} in chain {}", leafNode.getIdentifier(), leafName, chainIdentifer);
        // find most suitable implementation
        if (isPlainAminoAcid(leafName)) {
            AminoAcidFamily family = AminoAcidFamily.getAminoAcidTypeByThreeLetterCode(leafName).get();
            return createAminoAcid(leafIdentifier, family, atoms);
        }
        if (isPlainNucleotide(leafName)) {
            NucleotideFamily family = NucleotideFamily.getNucleotideByThreeLetterCode(leafName).get();
            return createNucleotide(leafIdentifier, family, atoms);
        }
        if (this.reducer.options.isRetrievingLigandInformation()) {
            return createLeafWithAdditionalInformation(leafIdentifier, leafName, atoms);
        } else {
            return createLeafWithoutAdditionalInformation(leafIdentifier, leafName, atoms);
        }
    }

    /**
     * Decides if a {@link LeafSubstructure} is a amino acid using this three letter code.
     *
     * @param leafName The three letter code of a leaf.
     * @return True, if the given tree letter code used for an amino acid.
     */
    private boolean isPlainAminoAcid(String leafName) {
        return AminoAcidFamily.getAminoAcidTypeByThreeLetterCode(leafName).isPresent();
    }

    /**
     * Decides if a {@link LeafSubstructure} is a nucleotide using this three letter code.
     *
     * @param leafName The three letter code of a leaf.
     * @return True, if the given tree letter code used for an nucleotide.
     */
    private boolean isPlainNucleotide(String leafName) {
        return NucleotideFamily.getNucleotideByThreeLetterCode(leafName).isPresent();
    }

    /**
     * Creates a new amino acid.
     *
     * @param identifier The identifier of the amino acid.
     * @param family Its concrete family.
     * @param atoms Its atoms.
     * @return The amino acid.
     */
    private AminoAcid createAminoAcid(LeafIdentifier identifier, AminoAcidFamily family, Map<String, Atom> atoms) {
        return LeafFactory.createAminoAcidFromAtoms(identifier, family, atoms, this.reducer.options);
    }

    /**
     * Creates a new nucleotide.
     *
     * @param identifier The identifier of the nucleotide.
     * @param family Its concrete family.
     * @param atoms Its atoms.
     * @return The amino acid.
     */

    private Nucleotide createNucleotide(LeafIdentifier identifier, NucleotideFamily family, Map<String, Atom> atoms) {
        return LeafFactory.createNucleotideFromAtoms(identifier, family, atoms, this.reducer.options);
    }

    /**
     * Creating a leaf from a {@link LeafSkeleton} in the cache.
     *
     * @param identifier The identifier of the leaf.
     * @param leafName Its three letter code.
     * @param atoms Its atoms.
     * @return The Leaf.
     */
    private LeafSubstructure<?, ?> createLeafWithoutAdditionalInformation(LeafIdentifier identifier, String leafName, Map<String, Atom> atoms) {
        LeafSubstructure<?, ?> substructure = new AtomContainer<>(identifier, new LigandFamily("?", leafName));
        atoms.values().forEach(substructure::addNode);
        return substructure;
    }

    /**
     * Creating a leaf using additional information from parsing the corresponding cif file or using already parsed
     * {@link LeafSkeleton}s from the cache.
     *
     * @param identifier The identifier of the leaf.
     * @param leafName Its three letter code.
     * @param atoms Its atoms.
     * @return The Leaf.
     */
    private LeafSubstructure<?, ?> createLeafWithAdditionalInformation(LeafIdentifier identifier, String leafName, Map<String, Atom> atoms) {
        LeafSkeleton leafSkeleton;
        if (!this.reducer.skeletons.containsKey(leafName)) {
            leafSkeleton = LigandParserService.parseLeafSkeleton(leafName);
            this.reducer.skeletons.put(leafName, leafSkeleton);
        } else {
            leafSkeleton = this.reducer.skeletons.get(leafName);
        }
        return leafSkeleton.toRealLeafSubstructure(identifier, atoms);
    }

}
