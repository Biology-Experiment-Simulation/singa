package de.bioforscher.singa.chemistry.parser.pdb.structures.tokens;

import de.bioforscher.singa.core.utility.Range;

import java.util.regex.Pattern;

/**
 * @author cl
 */
public enum TitleToken implements PDBToken {

    CONTINUATION(Range.of(9, 10)),
    TEXT(Range.of(11, 80));

    public static final Pattern RECORD_PATTERN = Pattern.compile("^TITLE.*");
    private final Range<Integer> columns;

    TitleToken(Range<Integer> columns) {
        this.columns = columns;
    }

    @Override
    public Pattern getRecordNamePattern() {
        return RECORD_PATTERN;
    }

    @Override
    public Range<Integer> getColumns() {
        return this.columns;
    }


}
