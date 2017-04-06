package de.bioforscher.singa.mathematics.graphs.util;

import de.bioforscher.singa.mathematics.vectors.Vector2D;

/**
 * @author cl
 */
public class RectangularGridCoordinateConverter {

    private int columns;
    private int rows;

    public RectangularGridCoordinateConverter(int columns, int rows) {
        this.columns = columns;
        this.rows = rows;
    }

    public Vector2D convert(int nodeIdentifier) {
        if (nodeIdentifier > this.columns * this.rows) {
            throw new IndexOutOfBoundsException(
                    "The node identifier " + nodeIdentifier + " is out of the possible range (" + (this.columns * this.rows) +
                            ") of a rectangular grid graph with " + this.rows + " rows and " + this.columns +
                            " columns.");
        }
        // TODO eventually reversed
        int x = (int) (nodeIdentifier / (double) this.columns);
        double y = nodeIdentifier % this.columns;
        return new Vector2D(x, y);
    }

    public int convert(Vector2D coordinate) {
        if (coordinate.getX() > this.columns) {
            throw new IndexOutOfBoundsException(
                    "The x coordinate " + coordinate.getX() + " is out of the possible range " + this.columns + ".");
        } else if (coordinate.getY() > this.rows) {
            throw new IndexOutOfBoundsException(
                    "The x coordinate " + coordinate.getY() + " is out of the possible range " + this.rows + ".");
        }
        return (int) (coordinate.getY() * this.columns + coordinate.getX());
    }
}
