package bio.singa.structure.model.mmcif;

import bio.singa.structure.model.identifiers.LeafIdentifier;
import bio.singa.structure.model.interfaces.*;
import org.rcsb.cif.CifIO;
import org.rcsb.cif.model.Block;
import org.rcsb.cif.model.CifFile;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author cl
 */
public class MmcifStructure implements Structure {

    private Block data;

    /**
     * The models that have already been requested.
     */
    private final Map<Integer, MmcifModel> cachedModels;
    private boolean allModelsCached = false;

    public MmcifStructure(CifFile cifFile) {
        data = cifFile.getFirstBlock();
        cachedModels = new HashMap<>();
    }

    private MmcifStructure(MmcifStructure mmcifStructure) {
        data = mmcifStructure.data;
        cachedModels = new HashMap<>();
    }

    @Override
    public String getPdbIdentifier() {
        return data.getEntry().getId().get(0);
    }

    @Override
    public String getTitle() {
        return data.getStruct().getTitle().get(0);
    }

    @Override
    public List<Model> getAllModels() {
        // get model of each atom
        List<Integer> modelIdentifiers = data.getAtomSite().getPdbxPDBModelNum().values()
                .boxed()
                .collect(Collectors.toList());
        int previousModelIdentifier = 1;
        int initialAtomIndex = 1;
        for (int atomIndex = 0; atomIndex < modelIdentifiers.size(); atomIndex++) {
            int modelIdentifier = modelIdentifiers.get(atomIndex);
            if (modelIdentifier != previousModelIdentifier) {
                MmcifModel finishedModel = new MmcifModel(data, previousModelIdentifier, initialAtomIndex, atomIndex);
                cachedModels.put(previousModelIdentifier, finishedModel);
                initialAtomIndex = atomIndex + 1;
                previousModelIdentifier = modelIdentifier;
            }
        }
        MmcifModel current = new MmcifModel(data, previousModelIdentifier, initialAtomIndex, modelIdentifiers.size());
        cachedModels.put(previousModelIdentifier, current);
        allModelsCached = true;
        return new ArrayList<>(cachedModels.values());
    }

    @Override
    public Set<Integer> getAllModelIdentifiers() {
        if (allModelsCached) {
            return cachedModels.keySet();
        } else {
            getAllModels();
        }
        return cachedModels.keySet();
    }

    @Override
    public Model getFirstModel() {
        return getModel(1).orElseThrow(() -> new IllegalStateException("No model found in structure."));
    }

    @Override
    public Optional<Model> getModel(int modelIdentifier) {
        if (!allModelsCached) {
            getAllModels();
        }
        if (cachedModels.containsKey(modelIdentifier)) {
            return Optional.of(cachedModels.get(modelIdentifier));
        }
        return Optional.empty();
    }

    @Override
    public void removeModel(int modelIdentifier) {
        // TODO implement
    }

    @Override
    public Optional<Chain> getChain(int modelIdentifier, String chainIdentifier) {
        if (!allModelsCached) {
            getAllModels();
        }
        if (cachedModels.containsKey(modelIdentifier)) {
            return cachedModels.get(modelIdentifier).getChain(chainIdentifier);
        }
        return Optional.empty();
    }

    @Override
    public Structure getCopy() {
        return new MmcifStructure(this);
    }

    @Override
    public List<Chain> getAllChains() {
        if (!allModelsCached) {
            getAllModels();
        }
        List<Chain> allChains = new ArrayList<>();
        for (Model model : cachedModels.values()) {
            allChains.addAll(model.getAllChains());
        }
        return allChains;
    }

    @Override
    public Chain getFirstChain() {
        return getFirstModel().getFirstChain();
    }

    @Override
    public List<LeafSubstructure<?>> getAllLeafSubstructures() {
        List<LeafSubstructure<?>> leafSubstructures = new ArrayList<>();
        for (Chain chain : getAllChains()) {
            leafSubstructures.addAll(chain.getAllLeafSubstructures());
        }
        return leafSubstructures;
    }

    @Override
    public Optional<LeafSubstructure<?>> getLeafSubstructure(LeafIdentifier leafIdentifier) {
        Optional<Chain> chainOptional = getChain(leafIdentifier.getModelIdentifier(), leafIdentifier.getChainIdentifier());
        return chainOptional.flatMap(chain -> chain.getLeafSubstructure(leafIdentifier));
    }

    @Override
    public LeafSubstructure<?> getFirstLeafSubstructure() {
        return getFirstChain().getFirstLeafSubstructure();
    }

    @Override
    public boolean removeLeafSubstructure(LeafIdentifier leafIdentifier) {
        // TODO implement
        return false;
    }

    @Override
    public Optional<Atom> getAtom(Integer atomIdentifier) {
        for (LeafSubstructure leafSubstructure : getAllLeafSubstructures()) {
            final Optional<Atom> optionalAtom = leafSubstructure.getAtom(atomIdentifier);
            if (optionalAtom.isPresent()) {
                return optionalAtom;
            }
        }
        return Optional.empty();
    }

    @Override
    public void removeAtom(Integer atomIdentifier) {
        // TODO implement
    }


    public static void main(String[] args) throws IOException {
        String pdbId = "2N5E";
        MmcifStructure structure = new MmcifStructure(CifIO.readFromURL(new URL("https://files.rcsb.org/download/" + pdbId + ".cif")));

        System.out.println(structure.getFirstModel());
        System.out.println(structure.getFirstChain());

    }

}
