package de.bioforscher.chemistry.parser;

import de.bioforscher.chemistry.descriptive.Enzyme;
import de.bioforscher.chemistry.descriptive.annotations.Annotation;
import de.bioforscher.chemistry.descriptive.annotations.AnnotationType;
import de.bioforscher.core.biology.Organism;
import de.bioforscher.core.biology.Taxon;
import de.bioforscher.core.identifier.NCBITaxonomyIdentifier;
import de.bioforscher.core.identifier.UniProtIdentifier;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by Christoph on 10.09.2016.
 */
public class UniProtContentHandler implements ContentHandler {

    private static List<String> TEXT_COMMENTS_TO_PARSE = new ArrayList<>();

    static {
        Collections.addAll(TEXT_COMMENTS_TO_PARSE,
                "function",
                "catalytic activity");
    }

    // enzyme attributes
    private UniProtIdentifier identifier;
    private String recommendedName;
    private double molarMass;
    private List<String> additionalNames;
    private String aminoAcidSequence;
    private Organism sourceOrganism;
    private List<Annotation<String>> textComments;

    // parser attributes
    private String currentTag = "";
    private Annotation<String> temoraryCommentAnnotation;

    // reading name
    private boolean inRecommendedName = false;
    private boolean inAlternativeName = false;
    private boolean inOrganism = false;
    private boolean inRelevantComment = false;
    private boolean isScientificName = false;
    private boolean isCommonName = false;

    public UniProtContentHandler() {
        this.additionalNames = new ArrayList<>();
        this.textComments = new ArrayList<>();
    }

    Enzyme getChemicalSpecies() {
        // create base enzyme
        Enzyme enzyme = new Enzyme.Builder(this.identifier)
                .name(this.recommendedName)
                .molarMass(this.molarMass)
                .build();
        // add organism
        enzyme.addOrganism(this.sourceOrganism);
        // add sequence without white spaces
        enzyme.addAminoAcidSequence(this.aminoAcidSequence.replaceAll("\\s", ""));
        // add additional names
        this.additionalNames.forEach(enzyme::addAdditionalName);
        // add textComments
        this.textComments.forEach(enzyme::addAnnotation);

        return enzyme;
    }

    @Override
    public void setDocumentLocator(Locator locator) {

    }

    @Override
    public void startDocument() throws SAXException {

    }

    @Override
    public void endDocument() throws SAXException {

    }

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {

    }

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {

    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {

        switch (qName) {
            case "accession":
            case "fullName":
            case "text":
            case "taxon": {
                this.currentTag = qName;
                break;
            }
            case "recommendedName": {
                this.currentTag = qName;
                this.inRecommendedName = true;
                break;
            }
            case "alternativeName": {
                this.currentTag = qName;
                this.inAlternativeName = true;
                break;
            }
            case "organism": {
                this.currentTag = qName;
                this.inOrganism = true;
                break;
            }
            case "comment": {
                if (TEXT_COMMENTS_TO_PARSE.contains(atts.getValue("type"))) {
                    this.currentTag = qName;
                    this.inRelevantComment = true;
                    this.temoraryCommentAnnotation = new Annotation<>(AnnotationType.NOTE);
                    this.temoraryCommentAnnotation.setDescription(atts.getValue("type"));
                }
                break;
            }
            case "name": {
                this.currentTag = qName;
                if (this.inOrganism) {
                    if (atts.getValue("type").equals("scientific")) {
                        this.isScientificName = true;
                    } else if (atts.getValue("type").equals("common")) {
                        this.isCommonName = true;
                    }
                }
                break;
            }
            case "dbReference": {
                if (this.inOrganism && atts.getValue("type").equals("NCBI Taxonomy")) {
                    // set tax id for organism
                    this.sourceOrganism.setIdentifier(new NCBITaxonomyIdentifier(atts.getValue("id")));
                }
                break;
            }
            case "sequence": {
                this.currentTag = qName;
                // set weight
                if (atts.getValue("mass") != null) {
                    this.molarMass = Double.valueOf(atts.getValue("mass"));
                    break;
                }
            }
        }

    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {

        if (qName.equals(this.currentTag)) {
            this.currentTag = "";
        }

        switch (qName) {
            case "recommendedName": {
                this.inRecommendedName = false;
                break;
            }
            case "alternativeName": {
                this.inAlternativeName = false;
                break;
            }
            case "organism": {
                this.inOrganism = false;
                break;
            }
            case "name": {
                this.isScientificName = false;
                this.isCommonName = false;
                break;
            }
            case "comment": {
                if (this.inRelevantComment) {
                    if (this.temoraryCommentAnnotation.getContent() != null &&
                            !this.temoraryCommentAnnotation.getContent().trim().isEmpty()) {
                        this.textComments.add(this.temoraryCommentAnnotation);
                    }
                    this.inRelevantComment = false;
                }
            }
        }

    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {

        switch (this.currentTag) {
            case "accession": {
                // set identifier
                this.identifier = new UniProtIdentifier(new String(ch, start, length));

            }
            case "fullName": {
                if (this.inRecommendedName) {
                    // set recommended name
                    this.recommendedName = new String(ch, start, length);
                } else if (this.inAlternativeName) {
                    // add alternative name
                    this.additionalNames.add(new String(ch, start, length));
                }
                break;
            }
            case "name": {
                if (this.inOrganism) {
                    if (this.isScientificName) {
                        // create Organism with name
                        this.sourceOrganism = new Organism(new String(ch, start, length));
                    } else if (this.isCommonName) {
                        // set common name
                        this.sourceOrganism.setCommonName(new String(ch, start, length));
                    }
                }
                break;
            }
            case "taxon": {
                if (this.inOrganism) {
                    // add linage to organism
                    this.sourceOrganism.getLineage().add(new Taxon(new String(ch, start, length)));
                }
                break;
            }
            case "sequence": {
                // set sequence
                if (this.aminoAcidSequence == null) {
                    this.aminoAcidSequence = new String(ch, start, length);
                } else {
                    this.aminoAcidSequence += new String(ch, start, length);
                }
                break;
            }
            case "text": {
                if (this.inRelevantComment) {
                    if (this.temoraryCommentAnnotation.getContent() == null) {
                        this.temoraryCommentAnnotation.setContent(new String(ch, start, length));
                    } else {
                        this.temoraryCommentAnnotation.setContent(this.temoraryCommentAnnotation.getContent()
                                + new String(ch, start, length));
                    }
                }
            }

        }

    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {

    }

    @Override
    public void processingInstruction(String target, String data) throws SAXException {

    }

    @Override
    public void skippedEntity(String name) throws SAXException {

    }

}
