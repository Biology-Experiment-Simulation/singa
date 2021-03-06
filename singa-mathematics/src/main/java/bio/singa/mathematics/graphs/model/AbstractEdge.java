package bio.singa.mathematics.graphs.model;

import bio.singa.mathematics.vectors.Vector;

/**
 * A simple implementation of th edge interface. References target and source nodes by source an target attributes.
 *
 * @param <NodeType> The type of nodes this edge connects.
 */
public abstract class AbstractEdge<NodeType extends Node<NodeType, ? extends Vector, ?>> implements Edge<NodeType> {

    /**
     * An unique identifier.
     */
    protected int identifier;

    /**
     * The source {@link Node}.
     */
    protected NodeType source;

    /**
     * The target {@link Node}.
     */
    protected NodeType target;

    /**
     * Creates a new empty edge.
     */
    protected AbstractEdge() {
    }

    /**
     * Creates a new edge with the given identifier.
     *
     * @param identifier The identifer.
     */
    public AbstractEdge(int identifier) {
        this.identifier = identifier;
    }

    /**
     * Creates a new Edge connecting two {@link Node}.
     *
     * @param source The source {@link Node}.
     * @param target The target {@link Node}.
     * @param identifier The identifer.
     */
    public AbstractEdge(int identifier, NodeType source, NodeType target) {
        this.identifier = identifier;
        this.source = source;
        this.target = target;
    }

    public AbstractEdge(AbstractEdge<NodeType> abstractEdge) {
        this(abstractEdge.getIdentifier());
    }

    @Override
    public int getIdentifier() {
        return identifier;
    }

    @Override
    public void setIdentifier(int identifier) {
        this.identifier = identifier;
    }

    @Override
    public NodeType getSource() {
        return source;
    }

    @Override
    public void setSource(NodeType source) {
        this.source = source;
    }

    @Override
    public NodeType getTarget() {
        return target;
    }

    @Override
    public void setTarget(NodeType target) {
        this.target = target;
    }

    /**
     * Returns true only if the node is source or target of this edge.
     *
     * @param node The node.
     * @return true only if the node is source or target of this edge.
     */
    public boolean containsNode(NodeType node) {
        return source.equals(node) || target.equals(node);
    }

    @Override
    public String toString() {
        return "Edge connecting " + source + " and " + target;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractEdge<?> that = (AbstractEdge<?>) o;
        return identifier == that.identifier;

    }

    @Override
    public int hashCode() {
        return identifier;
    }

}
