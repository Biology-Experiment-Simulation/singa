package de.bioforscher.mathematics.matrices;

import de.bioforscher.core.utility.Pair;

import java.util.List;

/**
 * The {@link LabeledMatrix} provides possibilities to define labels that point to a value inside of the matrix.
 * Labels can be set for rows and columns and a value from the matrix can be retrieved by giving a combination
 * of row and column label.
 */
public interface LabeledMatrix<LabelType> {

    String STRING_REPRESENTATION_DECIMAL_FORMAT = "0.000000";

    /**
     * Assigns a label to a row.
     *
     * @param label    The label.
     * @param rowIndex The row index.
     */
    void setRowLabel(LabelType label, int rowIndex);

    /**
     * Returns the row label currently assigned to the given row index.
     *
     * @param rowIndex The row index.
     * @return The label of the row.
     */
    LabelType getRowLabel(int rowIndex);

    /**
     * Sets all row labels at once, using the index of the label in the list.
     *
     * @param labels The labels.
     */
    default void setRowLabels(List<LabelType> labels) {
        for (int i = 0; i < labels.size(); i++) {
            setRowLabel(labels.get(i), i);
        }
    }

    /**
     * Assigns a label to a column.
     *
     * @param label       The label.
     * @param columnIndex the column index.
     */
    void setColumnLabel(LabelType label, int columnIndex);

    /**
     * Returns the column label currently assigned to the given row index.
     *
     * @param columnIndex The column index.
     * @return The label of the column.
     */
    LabelType getColumnLabel(int columnIndex);

    /**
     * Sets all column labels at once, using the index of the label in the list.
     *
     * @param labels The labels.
     */
    default void setColumnLabels(List<LabelType> labels) {
        for (int i = 0; i < labels.size(); i++) {
            setColumnLabel(labels.get(i), i);
        }
    }

    /**
     * Returns a value using a row and column label.
     *
     * @param rowLabel    The row label.
     * @param columnLabel The column label.
     * @return The actual value from the matrix
     */
    default double getValueForLabel(LabelType rowLabel, LabelType columnLabel) {
        return getValueFromPosition(getPositionFromLabels(rowLabel, columnLabel));
    }

    /**
     * Returns a {@link Pair} of {@link Integer}s that represent the position of a value that is assigned to the
     * given labels. As a contract the {@link Pair#getFirst()} method will retrieve the row index and the
     * {@link Pair#getSecond()} method the column index.
     *
     * @param rowLabel    The row label.
     * @param columnLabel The column label.
     * @return The position of the value as defined by the labels.
     */
    Pair<Integer> getPositionFromLabels(LabelType rowLabel, LabelType columnLabel);

    /**
     * Returns a value using the position given by a {@link Pair} of {@link Integer}s. As a contract the
     * {@link Pair#getFirst()} method will be interpreted as the row index and the {@link Pair#getSecond()} method
     * as the column index.
     *
     * @param position The position of a value.
     * @return The actual value from the matrix.
     */
    double getValueFromPosition(Pair<Integer> position);

    /**
     * Returns a string representation of the labelled matrix such that a CSV compatible format is achieved. Numbers
     * are rounded to 6 decimal places to keep visual layout.
     * <p>
     * The following symmetric matrix with the labels L1, L2, L3 result in the following string representation:
     * <pre>
     * ,L1,L2,L3
     * L1,1.000000,2.000000,3.000000
     * L2,2.000000,4.000000,5.000000
     * L3,3.000000,5.000000,8.000000
     * </pre>
     *
     * @return a CSV-like string representation
     */
    String getStringRepresentation();
}
