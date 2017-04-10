package de.bioforscher.singa.simulation.parser.graphs;

import de.bioforscher.singa.core.parser.xml.AbstractXMLParser;
import de.bioforscher.singa.simulation.model.graphs.AutomatonGraph;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GraphMLParserService extends AbstractXMLParser {

    public GraphMLParserService(String filePath) {
        getXmlReader().setContentHandler(new GraphMLContentHandler());
        setResource(filePath);
    }

    @Override
    public void fetchResource() {
        try {
            this.getXmlReader().parse(getResource());
        } catch (IOException | SAXException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Object> parseObjects() {
        List<Object> list = new ArrayList<>();
        AutomatonGraph graph = ((GraphMLContentHandler) this.getXmlReader().getContentHandler()).getGraph();
        list.add(graph);
        return list;
    }

    public AutomatonGraph fetchGraph() {
        fetchResource();
        List<Object> list = parseObjects();
        return (AutomatonGraph) list.get(0);
    }

}