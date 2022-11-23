package org.iupac.fairdata.contrib.fairspec;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

public class XMLUtil {

	public static class XLSXSheetReader {

		public Map<String, String> getCellData(InputStream is, String sheetName) throws IOException {
			ZipInputStream zis = null;
			Map<String, String> cellData = null;
			try {
				zis = new ZipInputStream(is);
				String sheetXML = null;
				String sharedXML = null;
				ZipEntry entry;
				while ((entry = zis.getNextEntry()) != null && sheetXML == null && sharedXML == null) {
					String name = entry.getName();
					if (name.endsWith(sheetName)) {
						sheetXML = new String(FAIRSpecUtilities.getLimitedStreamBytes(zis, entry.getSize(), null, false, true), "UTF-8");
					} else if (name.endsWith("sharedStrings.xml")) {
						sharedXML = new String(FAIRSpecUtilities.getLimitedStreamBytes(zis, entry.getSize(), null, false, true), "UTF-8");
					}
				}
				if (sheetXML == null)
					throw new IOException("XMLSheetReader - no sheet named " + sheetName + " found");
				cellData = processData(sheetXML, sharedXML);
		
			} finally {
				if (zis != null)
					zis.close();
			}
			return cellData;
		}

		private Map<String, String> processData(String sheetXML, String sharedXML) {
			Map<String, String> cellData = new HashMap<String, String>();
			String[] sharedStrings = null;
			if (sharedXML != null) {
				String[] tokens = sharedXML.split("\\<si\\>\\<t\\>");
				sharedStrings = new String[tokens.length - 1];
				for (int i = 1; i < tokens.length; i++) {
					sharedStrings[i - 1] = tokens[i].substring(0, tokens[i].indexOf("</t>"));
				}
			}

			// ArrayList<ArrayList<String>> cells = new ArrayList<ArrayList<String>>();
			String[] tokens = sheetXML.split("\\<c r");
			for (int i = 1; i < tokens.length; i++) {
				String cell = tokens[i].substring(0, tokens[i].indexOf("</c>"));
				boolean isShared = (cell.indexOf("t=\"s\"") >= 0);
				String cr = cell.substring(2, cell.indexOf('"', 3));
				String val = cell.substring(cell.indexOf("<v>") + 3);
				val = val.substring(0, val.indexOf("</v>"));
				if (isShared) {
					val = sharedStrings[Integer.parseInt(val)];
				}
				// nonbreaking spaces can be here
				val = FAIRSpecUtilities.rep(val, "\uC2A0", " ").trim();
				cellData.put(cr, val);
			}
			return cellData;
		}
	}
	
	public static class XmlReader {

		protected String debugContext = "";
		protected String warn;
		protected String err;


		final public Map<String, String> atts = new Hashtable<String, String>();

		/////////////// file reader option //////////////

		protected XmlReader(BufferedReader reader) {
			err = parseXML(reader);
		}

		private String parseXML(BufferedReader reader) {
			XMLReader saxReader = null;

			try {
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

				processXml(saxReader, reader);

			} catch (Exception e) {
				System.err.println("Could not instantiate JAXP/SAX XML reader: " + e.getMessage());
				return e.getMessage();
			}
			return null;
		}

		/**
		 * 
		 * @param parent
		 * @param saxReader
		 * @throws Exception
		 */
		protected void processXml(org.xml.sax.XMLReader saxReader, BufferedReader reader) throws Exception {
			new XmlHandler().parseXML(this, saxReader, reader);
		}

		/**
		 * keepChars is used to signal that characters between end tags should be kept
		 * 
		 */

		protected boolean keepChars;
		protected StringBuffer chars = new StringBuffer(2000);
		
		protected void setKeepChars(boolean TF) {
			keepChars = TF;
			chars.setLength(0);
		}

		/**
		 * 
		 * @param localName
		 * @param nodeName  TODO
		 */
		protected void processStartElement(String localName, String nodeName) {
			// override this
			System.out.println("XmlReader.processStartElement " + localName + " " + nodeName);
		}

		/**
		 * 
		 * @param localName
		 */
		void processEndElement(String localName) {
			System.out.println("XmlReader.processEndElement " + localName);
		}

		public void endDocument() {
			System.out.println("XmlReader.endDocument");
			// override this
		}

		public class XmlHandler extends DefaultHandler {

			private XmlReader xmlReader;

			void parseXML(XmlReader xmlReader, XMLReader saxReader, BufferedReader reader) throws Exception {
				this.xmlReader = xmlReader;
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
				System.out.println("XmlReader.characters " + new String(ch, start, length));
				if (xmlReader.keepChars)
					xmlReader.chars.append(ch, start, length);
			}

			@Override
			public void warning(SAXParseException exception) {
				warn = ("SAX WARNING:" + exception.getMessage());
				System.err.println("XmlReader.warning " + warn);
			}

			@Override
			public void error(SAXParseException exception) {
				err = ("SAX ERROR:" + exception.getMessage());
				System.err.println("XmlReader.error " + err);
			}

			@Override
			public void fatalError(SAXParseException exception) {
				err = ("SAX FATAL:" + exception.getMessage());
				System.err.println("XmlReader.fatalError " + err);
			}

		}
	}

}
