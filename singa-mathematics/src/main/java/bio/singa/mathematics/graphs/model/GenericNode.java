package bio.singa.mathematics.graphs.model;

import bio.singa.mathematics.geometry.faces.Rectangle;
import bio.singa.mathematics.vectors.Vector2D;
import bio.singa.mathematics.vectors.Vectors;

/**
 * The generic node class is a container for the content, that is specified by the content type.
 *
 * @param <ContentType> The content of the node.
 * @author cl
 */
public class GenericNode<ContentType> extends AbstractNode<GenericNode<ContentType>, Vector2D, Integer> {

    /**
     * The content.
     */
    private ContentType content;

    /**
     * Creates a new generic node with the given identifier.
     *
     * @param identifier The identifier.
     */
    public GenericNode(int identifier) {
        super(identifier);
    }

    /**
     * Creates a new generic node with the given identifier and the specified content.
     *
     * @param identifier The identifier.
     * @param content The content.
     */
    public GenericNode(int identifier, ContentType content) {
        super(identifier, Vectors.generateRandom2DVector(new Rectangle(500, 500)));
        this.content = content;
    }

    private GenericNode(GenericNode<ContentType> node) {
        super(node);
        content = node.getContent();
    }

    /**
     * Returns the content of the node.
     *
     * @return The content of the node.
     */
    public ContentType getContent() {
        return content;
    }

    /**
     * Sets the content of the node.
     *
     * @param content The content of the node.
     */
    public void setContent(ContentType content) {
        this.content = content;
    }

    @Override
    public GenericNode<ContentType> getCopy() {
        return new GenericNode<>(this);
    }

    @Override
    public String toString() {
        return "Node (" + getContent() + ")";
    }
}
