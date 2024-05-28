package com.integratedgraphics.util;

import java.io.BufferedReader;
import java.util.Map;
import java.util.TreeMap;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A generic XML reader template -- by itself, does nothing.
 * 
 * Superclass of com.integratedgraphics.crawler.Crawler
 * 
 * XmlReader takes all XML streams, whether from a file reader or from DOM.
 * 
 * This class handles generic XML tag parsing.
 * 
 * XmlHandler extends DefaultHandler
 * 
 * @author Bob Hanson
 * 
 */

abstract public class XmlReader {

	private static class XmlHandler extends DefaultHandler {

		private XmlReader xmlReader;

		void parseXML(XmlReader xmlReader, Object saxReaderObj, BufferedReader reader) throws Exception {
			this.xmlReader = xmlReader;
			XMLReader saxReader = (XMLReader) saxReaderObj;
			saxReader.setFeature("http://xml.org/sax/features/validation", false);
			saxReader.setFeature("http://xml.org/sax/features/namespaces", true);
			saxReader.setEntityResolver(this);
			saxReader.setContentHandler(this);
			saxReader.setErrorHandler(this);
			InputSource is = new InputSource(reader);
			is.setSystemId("foo");
			saxReader.parse(is);
		}

		@Override
		public void startDocument() {
		}

		@Override
		public void endDocument() {
			xmlReader.endDocument();
		}

		@Override
		public void startElement(String namespaceURI, String localName, String nodeName, Attributes attributes) {
			xmlReader.atts.clear();
			for (int i = attributes.getLength(); --i >= 0;)
				xmlReader.atts.put(attributes.getLocalName(i).toLowerCase(), attributes.getValue(i));
			xmlReader.processStartElement(localName.toLowerCase(), nodeName.toLowerCase());
		}

		@Override
		public void endElement(String uri, String localName, String qName) {
			xmlReader.processEndElement(localName.toLowerCase());
		}

		@Override
		public void characters(char[] ch, int start, int length) {
			if (xmlReader.keepChars)
				xmlReader.chars.append(ch, start, length);
		}

		@Override
		public void error(SAXParseException exception) {
			xmlReader.error("SAX ERROR:" + exception.getMessage());
		}

		@Override
		public void fatalError(SAXParseException exception) {
			xmlReader.fatalError("SAX FATAL:" + exception.getMessage());
		}

		@Override
		public void warning(SAXParseException exception) {
			xmlReader.warn("SAX WARNING:" + exception.getMessage());
		}

	}

	protected Map<String, String> atts = new TreeMap<String, String>();
	protected String myError;

	protected StringBuffer log = new StringBuffer();

	/////////////// file reader option //////////////

	protected String parseXML(BufferedReader reader) throws Exception {
		org.xml.sax.XMLReader saxReader = null;
		javax.xml.parsers.SAXParserFactory spf = javax.xml.parsers.SAXParserFactory.newInstance();
		spf.setNamespaceAware(true);
		javax.xml.parsers.SAXParser saxParser = spf.newSAXParser();
		saxReader = saxParser.getXMLReader();

		// otherwise, DTD UTI is treated as a URL, retrieved, and scanned.
		// see
		// https://stackoverflow.com/questions/10257576/how-to-ignore-inline-dtd-when-parsing-xml-file-in-java
		saxReader.setFeature("http://xml.org/sax/features/validation", false);
		saxReader.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
		saxReader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		new XmlHandler().parseXML(this, saxReader, reader);
		return myError;
	}

	/**
	 * 
	 * @param localName
	 * @param nodeName  TODO
	 */
	protected abstract void processStartElement(String localName, String nodeName);

	/*
	 * keepChars is used to signal that characters between end tags should be kept
	 * 
	 */

	protected boolean keepChars = true;
	protected StringBuffer chars = new StringBuffer(2000);

	protected void setKeepChars(boolean TF) {
		keepChars = TF;
		chars.setLength(0);
	}

	protected abstract void processEndElement(String localName);

	protected abstract void endDocument();

	void error(String msg) {
		myError = msg;
		log.append("\nSAX Error:" + msg);
	}

	void fatalError(String msg) {
		myError = msg;
		log.append("\nSAX FatalError:" + msg);
	}

	void warn(String msg) {
		log.append("\nSAX warning:" + msg);
	}

}
