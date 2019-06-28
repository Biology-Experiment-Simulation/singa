package bio.singa.structure.model.mmcif;

import bio.singa.structure.model.identifiers.LeafIdentifier;
import bio.singa.structure.model.interfaces.Atom;
import bio.singa.structure.model.interfaces.Chain;
import bio.singa.structure.model.interfaces.LeafSubstructure;
import bio.singa.structure.model.interfaces.Model;
import org.rcsb.cif.model.Block;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author cl
 */
public class MmcifModel implements Model {

    private Block data;

    private int modelIdentifier;

    private final Map<String, MmcifChain> cachedChains;
    private boolean allChainsCached = false;

    private int initialIndex;
    private int endIndex;

    public MmcifModel(Block data, int modelIdentifier, int initialIndex, int endIndex) {
        this.data = data;
        this.modelIdentifier = modelIdentifier;
        this.initialIndex = initialIndex;
        this.endIndex = endIndex;
        cachedChains = new TreeMap<>();
    }

    private MmcifModel(MmcifModel mmcifModel) {
        data = mmcifModel.data;
        modelIdentifier = mmcifModel.modelIdentifier;
        initialIndex = mmcifModel.initialIndex;
        endIndex = mmcifModel.endIndex;
        cachedChains = new TreeMap<>();
    }

    @Override
    public Integer getModelIdentifier() {
        return modelIdentifier;
    }

    @Override
    public Set<String> getAllChainIdentifiers() {
        if (allChainsCached) {
            return cachedChains.keySet();
        } else {
            getAllChains();
        }
        return cachedChains.keySet();
    }

    @Override
    public Optional<Chain> getChain(String chainIdentifier) {
        if (!allChainsCached) {
            getAllChains();
        }
        if (cachedChains.containsKey(chainIdentifier)) {
            return Optional.of(cachedChains.get(chainIdentifier));
        }
        return Optional.empty();
    }

    @Override
    public void removeChain(String chainIdentifier) {
        // TODO implement
    }

    @Override
    public Model getCopy() {
        return new MmcifModel(this);
    }

    @Override
    public List<Chain> getAllChains() {
        List<String> chainIdentifiers = data.getAtomSite().getLabelAsymId().values()
                .collect(Collectors.toList());

        int initialAtomIndex = initialIndex;
        String previousChainIdentifier = chainIdentifiers.iterator().next();
        for (int atomIndex = initialIndex - 1; atomIndex < endIndex; atomIndex++) {
            String chainIdentifier = chainIdentifiers.get(atomIndex);
            if (!previousChainIdentifier.equals(chainIdentifier)) {
                MmcifChain finishedChain = new MmcifChain(data, modelIdentifier, previousChainIdentifier, initialAtomIndex, atomIndex);
                cachedChains.put(previousChainIdentifier, finishedChain);
                initialAtomIndex = atomIndex + 1;
                previousChainIdentifier = chainIdentifier;
            }
        }
        MmcifChain current = new MmcifChain(data, modelIdentifier, previousChainIdentifier, initialAtomIndex, endIndex);
        cachedChains.put(previousChainIdentifier, current);
        allChainsCached = true;
        return new ArrayList<>(cachedChains.values());
    }

    @Override
    public Chain getFirstChain() {
        if (!allChainsCached) {
            getAllChains();
        }
        if (!cachedChains.isEmpty()) {
            return cachedChains.values().iterator().next();
        }
        throw new IllegalStateException("No model found in structure.");
    }

    @Override
    public List<LeafSubstructure<?>> getAllLeafSubstructures() {
        List<LeafSubstructure<?>> leafSubstructures = new ArrayList<>();
        List<Chain> allChains = getAllChains();
        for (Chain chain : allChains) {
            leafSubstructures.addAll(chain.getAllLeafSubstructures());
        }
        return leafSubstructures;
    }

    @Override
    public Optional<LeafSubstructure<?>> getLeafSubstructure(LeafIdentifier leafIdentifier) {
        Optional<Chain> chainOptional = getChain(leafIdentifier.getChainIdentifier());
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

    @Override
    public String toString() {
        return "MmcifModel{" +
                "modelIdentifier=" + modelIdentifier +
                ", initialIndex=" + initialIndex +
                ", endIndex=" + endIndex +
                '}';
    }
}
