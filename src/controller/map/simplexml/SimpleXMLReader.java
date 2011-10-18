package controller.map.simplexml;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Deque;
import java.util.LinkedList;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import model.viewer.SPMap;
import util.IteratorWrapper;
import controller.map.simplexml.node.TileNode;

/**
 * An XML-map reader that just converts the XML into XMLNodes, which then
 * convert themselves into the map.
 * 
 * @author Jonathan Lovelace
 * 
 */
public class SimpleXMLReader {
	/**
	 * @param file the name of a file
	 * @return the map contained in that file
	 * @throws IOException on I/O error
	 * @throws SPFormatException if the data is invalid
	 * @throws XMLStreamException if the XML isn't well-formed
	 */
	public SPMap readMap(final String file) throws IOException, XMLStreamException, SPFormatException {
		final FileInputStream istream = new FileInputStream(file);
		try {
			return readMap(istream);
		} finally {
		istream.close();
		}
	}
	/**
	 * @param istream a stream
	 * @return the map contained in that stream
	 * @throws XMLStreamException if XML isn't well-formed.
	 * @throws SPFormatException if the data is invalid.
	 */
	public SPMap readMap(final InputStream istream) throws XMLStreamException, SPFormatException {
		final RootNode root = new RootNode();
		final Deque<AbstractXMLNode> stack = new LinkedList<AbstractXMLNode>();
		stack.push(root);
		@SuppressWarnings("unchecked")
		final IteratorWrapper<XMLEvent> eventReader = new IteratorWrapper<XMLEvent>(
				XMLInputFactory.newInstance().createXMLEventReader(istream));
		for (XMLEvent event : eventReader) {
			if (event.isStartElement()) {
				final AbstractXMLNode node = parseTag(event.asStartElement());
				stack.peek().addChild(node);
				stack.push(node);
			} else if (event.isCharacters()) {
				if (stack.peek() instanceof TileNode) {
					((TileNode) stack.peek()).addText(event.asCharacters().getData());
				}
			} else if (event.isEndElement()) {
				stack.pop();
			}
		}
		root.canonicalize();
		root.checkNode();
		return root.getMapNode().produce(null);
	}
	/**
	 * Turn a tag and its contents (properties) into a Node.
	 * @param element the tag
	 * @return the equivalent node.
	 * @throws SPFormatException on unexpecte or illegal XML.
	 */
	private static AbstractXMLNode parseTag(final StartElement element) throws SPFormatException {
		final AbstractChildNode<?> node = NodeFactory.create(element.getName()
				.getLocalPart(), element.getLocation().getLineNumber());
		@SuppressWarnings("unchecked")
		final IteratorWrapper<Attribute> attributes = new IteratorWrapper<Attribute>(element.getAttributes());
		for (Attribute att : attributes) {
			node.addProperty(att.getName().getLocalPart(), att.getValue());
		}
		return node;
	}
}
