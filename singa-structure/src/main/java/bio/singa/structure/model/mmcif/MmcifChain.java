package bio.singa.structure.model.mmcif;

import bio.singa.structure.model.identifiers.LeafIdentifier;
import bio.singa.structure.model.interfaces.Atom;
import bio.singa.structure.model.interfaces.Chain;
import bio.singa.structure.model.interfaces.LeafSubstructure;
import org.rcsb.cif.model.Block;

import java.util.List;
import java.util.Optional;

/**
 * @author cl
 */
public class MmcifChain implements Chain {

    private Block data;

    private int modelIdentifier;
    private String chainIdentifier;

    private int initialIndex;
    private int endIndex;

    public MmcifChain(Block data, int modelIdentifier, String chainIdentifier, int initialIndex, int endIndex) {
        this.data = data;
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
//        String identifier = data.getEntry().getId().get(0);
//
//        data.getAtomSite().getLabelSeqId().values()
//
//        List<String> threeLetterCodes = data.getAtomSite().getLabelCompId().values()
//                .collect(Collectors.toList());


        return null;
    }

    @Override
    public Optional<LeafSubstructure<?>> getLeafSubstructure(LeafIdentifier leafIdentifier) {
        return Optional.empty();
    }

    @Override
    public LeafSubstructure<?> getFirstLeafSubstructure() {
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
