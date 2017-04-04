package de.bioforscher.mathematics.graphs.model;

import de.bioforscher.mathematics.vectors.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple implementation of the node interface. References to neighboring nodes are stored in a list. Every node is
 * positioned using a vector.
 *
 * @param <NodeType>
 * @param <VectorType>
 */
public abstract class AbstractNode<NodeType extends Node<NodeType, VectorType>, VectorType extends Vector>
        implements Node<NodeType, VectorType> {

    /**
     * The identifier.
     */
    private int identifier;

    /**
     * The neighbours.
     */
    private List<NodeType> neighbours;

    /**
     * A positional representation.
     */
    private VectorType position;

    /**
     * Creates a new node with the given identifier. The position in not initialized.
     * @param identifier The identifier.
     */
    public AbstractNode(int identifier) {
        this.identifier = identifier;
        this.neighbours = new ArrayList<>();
    }

    /**
     * Creates a new node with the given position.
     * @param identifier
     * @param position
     */
    public AbstractNode(int identifier, VectorType position) {
        this(identifier);
        this.position = position;
    }

    @Override
    public int getIdentifier() {
        return this.identifier;
    }

    public void setIdentifier(int identifier) {
        this.identifier = identifier;
    }

    @Override
    public List<NodeType> getNeighbours() {
        return this.neighbours;
    }

    public void setNeighbours(List<NodeType> neighbours) {
        this.neighbours = neighbours;
    }

    @Override
    public void addNeighbour(NodeType node) {
        this.neighbours.add(node);
    }

    public void removeNeighbour(RegularNode node) {
        this.neighbours.remove(node);
    }

    public boolean hasNeighbour(RegularNode node) {
        return this.neighbours.contains(node);
    }

    @Override
    public VectorType getPosition() {
        return this.position;
    }

    public void setPosition(VectorType position) {
        this.position = position;
    }

    public int getDegree() {
        return this.neighbours.size();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.identifier;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        // TODO maybe (surely) this should not happen here
        NodeType other = (NodeType) obj;
        return this.identifier == other.getIdentifier();
    }

    @Override
    public String toString() {
        return "Node " + this.identifier;
    }

}
