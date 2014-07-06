/**
 * 
 */
package io.github.vladast.avrcommunicator.util;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Date;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;

import io.github.vladast.avrcommunicator.db.dao.EventDAO;
import io.github.vladast.avrcommunicator.db.dao.EventRecorderDAO;
import io.github.vladast.avrcommunicator.db.dao.SessionDAO;
import io.github.vladast.avrcommunicator.db.dao.TouchableDAO;


/**
 * Class used for serialization/deserialization of Event Recorder objects.
 * Used to parse objects to XML/JSON/CSV and vice versa, enabling import/export capabilities.
 * @author vladimir.stankovic
 *
 */
public class EventRecorderStream implements ContentHandler, XMLReader {

	private ContentHandler mContentHandler;
	private EventRecorderDAO mEventRecorderDAO;
	
	public String daoToXML(EventRecorderDAO eventRecorderDAO) {
		
		mEventRecorderDAO = eventRecorderDAO;
		
		String resultXml = "";
		
		try {
            XMLReader generator = this;
            SAXSource source = new SAXSource(generator, new InputSource());

            StringWriter sw = new StringWriter();
            BufferedWriter bw = new BufferedWriter(sw);
            StreamResult result = new StreamResult(bw);

            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();      
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(source, result);
            
            System.out.println(sw.toString());
        }
        catch (TransformerConfigurationException e) {
            System.out.println("Transformer Configuration Exception: " + e.getMessage());
        }
        catch (TransformerException e) {
            System.out.println("Transformer Exception: " + e.getMessage());
        }
		
		
		
		Class<?> clazz = eventRecorderDAO.getClass();
		
		if(clazz == SessionDAO.class) {
			
		} else if (clazz == EventDAO.class) {
			
		} else if (clazz == TouchableDAO.class) {
			
		}
		
		return resultXml;
	}
	

	/** SAX.ContentHandler implementation */

	@Override // SAX.ContentHandler
	public void setDocumentLocator(Locator locator) {
		// TODO Auto-generated method stub
		
	}

	@Override // SAX.ContentHandler
	public void startDocument() throws SAXException {
		// TODO Auto-generated method stub
		
	}

	@Override // SAX.ContentHandler
	public void endDocument() throws SAXException {
		// TODO Auto-generated method stub
		
	}

	@Override // SAX.ContentHandler
	public void startPrefixMapping(String prefix, String uri)
			throws SAXException {
		// TODO Auto-generated method stub
		
	}

	@Override // SAX.ContentHandler
	public void endPrefixMapping(String prefix) throws SAXException {
		// TODO Auto-generated method stub
		
	}

	@Override // SAX.ContentHandler
	public void startElement(String uri, String localName, String qName,
			Attributes atts) throws SAXException {
		// TODO Auto-generated method stub
		
	}

	@Override // SAX.ContentHandler
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		// TODO Auto-generated method stub
		
	}

	@Override // SAX.ContentHandler
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		// TODO Auto-generated method stub
		
	}

	@Override // SAX.ContentHandler
	public void ignorableWhitespace(char[] ch, int start, int length)
			throws SAXException {
		// TODO Auto-generated method stub
		
	}

	@Override // SAX.ContentHandler
	public void processingInstruction(String target, String data)
			throws SAXException {
		// TODO Auto-generated method stub
		
	}

	@Override // SAX.ContentHandler
	public void skippedEntity(String name) throws SAXException {
		// TODO Auto-generated method stub
		
	}
	
	/** SAX.XMLReader implementation */

	@Override // SAX.XMLReader
	public boolean getFeature(String name) throws SAXNotRecognizedException,
			SAXNotSupportedException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override // SAX.XMLReader
	public void setFeature(String name, boolean value)
			throws SAXNotRecognizedException, SAXNotSupportedException {
		// TODO Auto-generated method stub
		
	}

	@Override // SAX.XMLReader
	public Object getProperty(String name) throws SAXNotRecognizedException,
			SAXNotSupportedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override // SAX.XMLReader
	public void setProperty(String name, Object value)
			throws SAXNotRecognizedException, SAXNotSupportedException {
		// TODO Auto-generated method stub
		
	}

	@Override // SAX.XMLReader
	public void setEntityResolver(EntityResolver resolver) {
		// TODO Auto-generated method stub
		
	}

	@Override // SAX.XMLReader
	public EntityResolver getEntityResolver() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override // SAX.XMLReader
	public void setDTDHandler(DTDHandler handler) {
		// TODO Auto-generated method stub
		
	}

	@Override // SAX.XMLReader
	public DTDHandler getDTDHandler() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override // SAX.XMLReader
	public void setContentHandler(ContentHandler handler) {
		mContentHandler = handler;
	}

	@Override // SAX.XMLReader
	public ContentHandler getContentHandler() {
		return mContentHandler;
	}

	@Override // SAX.XMLReader
	public void setErrorHandler(ErrorHandler handler) {
		// TODO Auto-generated method stub
		
	}

	@Override // SAX.XMLReader
	public ErrorHandler getErrorHandler() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override // SAX.XMLReader
	public void parse(InputSource input) throws IOException, SAXException {
		
		mContentHandler.startDocument();
		
		if(mEventRecorderDAO.getClass() == SessionDAO.class) {
			mContentHandler.startElement("", SessionDAO.XML_ROOT, SessionDAO.XML_ROOT, new AttributesImpl());
			/* Session ID */
			insertElement(SessionDAO.XML_FIELD_ID, ((SessionDAO)mEventRecorderDAO).getId());
	        /* Device ID */
			insertElement(SessionDAO.XML_FIELD_ID_DEVICE, ((SessionDAO)mEventRecorderDAO).getIdDevice());
			/* Name */
	        insertElement(SessionDAO.XML_FIELD_NAME, ((SessionDAO)mEventRecorderDAO).getName());
			/* Description */
	        insertElement(SessionDAO.XML_FIELD_DESCRIPTION, ((SessionDAO)mEventRecorderDAO).getDescription());
			/* Number of events */
	        insertElement(SessionDAO.XML_FIELD_NUMBER_OF_EVENTS, ((SessionDAO)mEventRecorderDAO).getNumberOfEvents());
			/* Number of event types */
	        insertElement(SessionDAO.XML_FIELD_NUMBER_OF_EVENT_TYPES, ((SessionDAO)mEventRecorderDAO).getNumberOfEventTypes());
			/* Session index */
	        insertElement(SessionDAO.XML_FIELD_INDEX_DEV_SESSION, ((SessionDAO)mEventRecorderDAO).getIndexDeviceSession());
			/* Timestamp recorded */
	        insertElement(SessionDAO.XML_FIELD_TIMESTAMP_RECORDED, ((SessionDAO)mEventRecorderDAO).getTimestampRecorded());
	        /* Timestamp uploaded */
	        insertElement(SessionDAO.XML_FIELD_TIMESTAMP_UPLOADED, ((SessionDAO)mEventRecorderDAO).getTimestampUploaded());
	        
	        mContentHandler.endElement("", "session", "session");	
		}
		
		mContentHandler.endDocument();
	}

	@Override // SAX.XMLReader
	public void parse(String systemId) throws IOException, SAXException {
		// TODO Auto-generated method stub
		
	}
	
	private void insertElement(String name, Object value) throws SAXException {
        mContentHandler.startElement("", name, name, new AttributesImpl());
        mContentHandler.characters(String.valueOf(value).toCharArray(), 0, String.valueOf(value).length());
        mContentHandler.endElement("", name, name);
	}
}
