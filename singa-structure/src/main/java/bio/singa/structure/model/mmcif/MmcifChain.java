package bio.singa.structure.model.mmcif;

import bio.singa.structure.model.identifiers.LeafIdentifier;
import bio.singa.structure.model.interfaces.Atom;
import bio.singa.structure.model.interfaces.Chain;
import bio.singa.structure.model.interfaces.LeafSubstructure;
import bio.singa.structure.model.mmtf.LeafFactory;
import org.rcsb.cif.model.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * @author cl
 */
public class MmcifChain implements Chain {

    private Block data;

    private int modelIdentifier;
    private String chainIdentifier;

    private TreeMap<LeafIdentifier, MmcifLeafSubstructure<?>> cachedLeaves;
    private boolean chachedAllLeafes;

    private int initialIndex;
    private int endIndex;

    public MmcifChain(Block data, int modelIdentifier, String chainIdentifier, int initialIndex, int endIndex) {
        this.data = data;
        cachedLeaves = new TreeMap<>();
        this.modelIdentifier = modelIdentifier;
        this.chainIdentifier = chainIdentifier;
        this.initialIndex = initialIndex;
        this.endIndex = endIndex;
    }

    public MmcifChain(MmcifChain mmcifChain) {
        data = mmcifChain.data;
        chainIdentifier = mmcifChain.chainIdentifier;
        initialIndex = mmcifChain.initialIndex;
        endIndex = mmcifChain.endIndex;
    }

    @Override
    public String getChainIdentifier() {
        return chainIdentifier;
    }

    @Override
    public Chain getCopy() {
        return new MmcifChain(this);
    }

    @Override
    public List<LeafSubstructure<?>> getAllLeafSubstructures() {
        String pdbId = data.getEntry().getId().get(0);

        List<Integer> leafSerials = data.getAtomSite().getAuthSeqId().values().boxed()
                .collect(Collectors.toList());

        List<String> threeLetterCodes = data.getAtomSite().getAuthCompId().values()
                .collect(Collectors.toList());

        List<String> insertionCodes = data.getAtomSite().getPdbxPDBInsCode().values()
                .collect(Collectors.toList());

        int previousLeafSerial = leafSerials.get(initialIndex-1);
        int initialAtomIndex = initialIndex;
        for (int atomIndex = initialIndex-1; atomIndex < endIndex; atomIndex++) {
            int leafSerial = leafSerials.get(atomIndex);
            if (leafSerial != previousLeafSerial) {
                String insertionString = insertionCodes.get(atomIndex);
                char insertionCode;
                if (insertionString.isEmpty()) {
                    insertionCode = LeafIdentifier.DEFAULT_INSERTION_CODE;
                } else {
                    insertionCode = insertionString.charAt(0);
                }
                LeafIdentifier leafIdentifier = new LeafIdentifier(pdbId, modelIdentifier, chainIdentifier, previousLeafSerial, insertionCode);
                MmcifLeafSubstructure<?> finishedLeaf = LeafFactory.createLeaf(data, leafIdentifier, threeLetterCodes.get(atomIndex), initialAtomIndex, atomIndex-1);
                cachedLeaves.put(leafIdentifier, finishedLeaf);
                initialAtomIndex = atomIndex + 1;
                previousLeafSerial = leafSerial;
            }
        }
        chachedAllLeafes = true;
        return new ArrayList<>(cachedLeaves.values());
    }

    @Override
    public Optional<LeafSubstructure<?>> getLeafSubstructure(LeafIdentifier leafIdentifier) {
        if (!chachedAllLeafes) {
            getAllLeafSubstructures();
        }
        if (cachedLeaves.containsKey(leafIdentifier)) {
            return Optional.of(cachedLeaves.get(leafIdentifier));
        }
        return Optional.empty();
    }

    @Override
    public LeafSubstructure<?> getFirstLeafSubstructure() {
        // TODO continue here
        return null;
    }

    @Override
    public boolean removeLeafSubstructure(LeafIdentifier leafIdentifier) {
        return false;
    }

    @Override
    public Optional<Atom> getAtom(Integer atomIdentifier) {
        return Optional.empty();
    }

    @Override
    public void removeAtom(Integer atomIdentifier) {

    }

    @Override
    public String toString() {
        return "MmcifChain{" +
                "chainIdentifier='" + chainIdentifier + '\'' +
                ", initialIndex=" + initialIndex +
                ", endIndex=" + endIndex +
                '}';
    }
}
