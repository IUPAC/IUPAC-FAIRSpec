/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2003-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package javax.xml.bind.helpers;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.Reader;
import java.net.URL;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.PropertyException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;

import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Partial default {@code Unmarshaller} implementation.
 * 
 * <p>
 * This class provides a partial default implementation for the
 * {@link javax.xml.bind.Unmarshaller}interface.
 * 
 * <p>
 * A JAXB Provider has to implement five methods (getUnmarshallerHandler,
 * unmarshal(Node), unmarshal(XMLReader,InputSource),
 * unmarshal(XMLStreamReader), and unmarshal(XMLEventReader).
 * 
 * @author <ul>
 *         <li>Kohsuke Kawaguchi, Sun Microsystems, Inc.</li>
 *         </ul>
 * @see javax.xml.bind.Unmarshaller
 * @since 1.6, JAXB 1.0
 */
public abstract class AbstractUnmarshallerImpl implements Unmarshaller
{    
    /** handler that will be used to process errors and warnings during unmarshal */
    private ValidationEventHandler eventHandler = 
        new DefaultValidationEventHandler();
    
    /** whether or not the unmarshaller will validate */
    protected boolean validating = false;

    /**
     * XMLReader that will be used to parse a document.
     */
    private XMLReader reader = null;

    /**
     * Obtains a configured XMLReader.
     * 
     * This method is used when the client-specified
     * {@link SAXSource} object doesn't have XMLReader.
     * 
     * {@link Unmarshaller} is not re-entrant, so we will
     * only use one instance of XMLReader.
     */
    protected XMLReader getXMLReader() throws JAXBException {
        if(reader==null) {
            try {
                SAXParserFactory parserFactory;
                parserFactory = SAXParserFactory.newInstance();
                parserFactory.setNamespaceAware(true);
                // there is no point in asking a validation because 
                // there is no guarantee that the document will come with
                // a proper schemaLocation.
                parserFactory.setValidating(false);
                reader = parserFactory.newSAXParser().getXMLReader();
            } catch( ParserConfigurationException e ) {
                throw new JAXBException(e);
            } catch( SAXException e ) {
                throw new JAXBException(e);
            }
        }
        return reader;
    }
    
    public Object unmarshal( Source source ) throws JAXBException {
        if( source == null ) {
            throw new IllegalArgumentException(
                Messages.format( Messages.MUST_NOT_BE_NULL, "source" ) );
        }
        
        if(source instanceof SAXSource)
            return unmarshal( (SAXSource)source );
        if(source instanceof StreamSource)
            return unmarshal( streamSourceToInputSource((StreamSource)source));
        if(source instanceof DOMSource)
            return unmarshal( ((DOMSource)source).getNode() );
        
        // we don't handle other types of Source
        throw new IllegalArgumentException();
    }

    // use the client specified XMLReader contained in the SAXSource.
    private Object unmarshal( SAXSource source ) throws JAXBException {
        
        XMLReader r = source.getXMLReader();
        if( r == null )
            r = getXMLReader();
        
        return unmarshal( r, source.getInputSource() );
    }

    /**
     * Unmarshals an object by using the specified XMLReader and the InputSource.
     * 
     * The callee should call the setErrorHandler method of the XMLReader
     * so that errors are passed to the client-specified ValidationEventHandler.
     */
    protected abstract Object unmarshal( XMLReader reader, InputSource source ) throws JAXBException;
    
    public final Object unmarshal( InputSource source ) throws JAXBException {
        if( source == null ) {
            throw new IllegalArgumentException(
                Messages.format( Messages.MUST_NOT_BE_NULL, "source" ) );
        }

        return unmarshal( getXMLReader(), source );
    }
        

    private Object unmarshal( String url ) throws JAXBException {
        return unmarshal( new InputSource(url) );
    }
    
    public final Object unmarshal( URL url ) throws JAXBException {
        if( url == null ) {
            throw new IllegalArgumentException(
                Messages.format( Messages.MUST_NOT_BE_NULL, "url" ) );
        }

        return unmarshal( url.toExternalForm() );
    }
    
    public final Object unmarshal( File f ) throws JAXBException {
        if( f == null ) {
            throw new IllegalArgumentException(
                Messages.format( Messages.MUST_NOT_BE_NULL, "file" ) );
        }

        try {
            return unmarshal(new BufferedInputStream(new FileInputStream(f)));
        } catch( FileNotFoundException e ) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }
    
    public final Object unmarshal( java.io.InputStream is ) 
        throws JAXBException {
            
        if( is == null ) {
            throw new IllegalArgumentException(
                Messages.format( Messages.MUST_NOT_BE_NULL, "is" ) );
        }

        InputSource isrc = new InputSource( is );
        return unmarshal( isrc );
    }

    public final Object unmarshal( Reader reader ) throws JAXBException {
        if( reader == null ) {
            throw new IllegalArgumentException(
                Messages.format( Messages.MUST_NOT_BE_NULL, "reader" ) );
        }

        InputSource isrc = new InputSource( reader );
        return unmarshal( isrc );
    }


    private static InputSource streamSourceToInputSource( StreamSource ss ) {
        InputSource is = new InputSource();
        is.setSystemId( ss.getSystemId() );
        is.setByteStream( ss.getInputStream() );
        is.setCharacterStream( ss.getReader() );
        
        return is;
    }
    
    
    /**
     * Indicates whether or not the Unmarshaller is configured to validate
     * during unmarshal operations.
     * <p>
     * <i><b>Note:</b> I named this method isValidating() to stay in-line
     * with JAXP, as opposed to naming it getValidating(). </i>
     *
     * @return true if the Unmarshaller is configured to validate during
     *        unmarshal operations, false otherwise
     * @throws JAXBException if an error occurs while retrieving the validating
     *        flag
     */
    public boolean isValidating() throws JAXBException {
        return validating;
    }
    
    /**
     * Allow an application to register a validation event handler.
     * <p>
     * The validation event handler will be called by the JAXB Provider if any
     * validation errors are encountered during calls to any of the
     * {@code unmarshal} methods.  If the client application does not register
     * a validation event handler before invoking the unmarshal methods, then
     * all validation events will be silently ignored and may result in
     * unexpected behaviour.
     *
     * @param handler the validation event handler
     * @throws JAXBException if an error was encountered while setting the
     *        event handler
     */
    public void setEventHandler(ValidationEventHandler handler) 
        throws JAXBException {
        
        if( handler == null ) {
            eventHandler = new DefaultValidationEventHandler();
        } else {
            eventHandler = handler;
        }
    }
    
    /**
     * Specifies whether or not the Unmarshaller should validate during
     * unmarshal operations. By default, the {@code Unmarshaller} does
     * not validate.
     * <p>
     * This method may only be invoked before or after calling one of the
     * unmarshal methods.
     *
     * @param validating true if the Unmarshaller should validate during
     *       unmarshal, false otherwise
     * @throws JAXBException if an error occurred while enabling or disabling
     * validation at unmarshal time
     */
    public void setValidating(boolean validating) throws JAXBException {
        this.validating = validating;
    }
    
    /**
     * Return the current event handler or the default event handler if one
     * hasn't been set.
     *
     * @return the current ValidationEventHandler or the default event handler
     *        if it hasn't been set
     * @throws JAXBException if an error was encountered while getting the
     *        current event handler
     */
    public ValidationEventHandler getEventHandler() throws JAXBException {
        return eventHandler;
    }
    
    
    /**
     * Creates an UnmarshalException from a SAXException.
     * 
     * This is an utility method provided for the derived classes.
     * 
     * <p>
     * When a provider-implemented ContentHandler wants to throw a
     * JAXBException, it needs to wrap the exception by a SAXException.
     * If the unmarshaller implementation blindly wrap SAXException
     * by JAXBException, such an exception will be a JAXBException
     * wrapped by a SAXException wrapped by another JAXBException.
     * This is silly.
     * 
     * <p>
     * This method checks the nested exception of SAXException
     * and reduce those excessive wrapping.
     * 
     * @return the resulting UnmarshalException
     */
    protected UnmarshalException createUnmarshalException( SAXException e ) {
        // check the nested exception to see if it's an UnmarshalException
        Exception nested = e.getException();
        if(nested instanceof UnmarshalException)
            return (UnmarshalException)nested;
        
        if(nested instanceof RuntimeException)
            // typically this is an unexpected exception,
            // just throw it rather than wrap it, so that the full stack
            // trace can be displayed.
            throw (RuntimeException)nested;
                
        
        // otherwise simply wrap it
        if(nested!=null)
            return new UnmarshalException(nested);
        else
            return new UnmarshalException(e);
    }
    
    /**
     * Default implementation of the setProperty method always 
     * throws PropertyException since there are no required
     * properties. If a provider needs to handle additional 
     * properties, it should override this method in a derived class.
     */
    public void setProperty( String name, Object value )
        throws PropertyException {

        if( name == null ) {
            throw new IllegalArgumentException(
                Messages.format( Messages.MUST_NOT_BE_NULL, "name" ) );
        }

        throw new PropertyException(name, value);
    }
    
    /**
     * Default implementation of the getProperty method always 
     * throws PropertyException since there are no required
     * properties. If a provider needs to handle additional 
     * properties, it should override this method in a derived class.
     */
    public Object getProperty( String name )
        throws PropertyException {
            
        if( name == null ) {
            throw new IllegalArgumentException(
                Messages.format( Messages.MUST_NOT_BE_NULL, "name" ) );
        }

        throw new PropertyException(name);
    }
    
    public Object unmarshal(XMLEventReader reader) throws JAXBException {
        
        throw new UnsupportedOperationException();
    }

    public Object unmarshal(XMLStreamReader reader) throws JAXBException {
        
        throw new UnsupportedOperationException();
    }

    public <T> JAXBElement<T> unmarshal(Node node, Class<T> expectedType) throws JAXBException {
        throw new UnsupportedOperationException();
    }

    public <T> JAXBElement<T> unmarshal(Source source, Class<T> expectedType) throws JAXBException {
        throw new UnsupportedOperationException();
    }

    public <T> JAXBElement<T> unmarshal(XMLStreamReader reader, Class<T> expectedType) throws JAXBException {
        throw new UnsupportedOperationException();
    }

    public <T> JAXBElement<T> unmarshal(XMLEventReader reader, Class<T> expectedType) throws JAXBException {
        throw new UnsupportedOperationException();
    }

    public void setSchema(Schema schema) {
        throw new UnsupportedOperationException();
    }

    public Schema getSchema() {
        throw new UnsupportedOperationException();
    }

    public void setAdapter(XmlAdapter adapter) {
        if(adapter==null)
            throw new IllegalArgumentException();
        setAdapter((Class)adapter.getClass(),adapter);
    }

    public <A extends XmlAdapter> void setAdapter(Class<A> type, A adapter) {
        throw new UnsupportedOperationException();
    }

    public <A extends XmlAdapter> A getAdapter(Class<A> type) {
        throw new UnsupportedOperationException();
    }

//    public void setAttachmentUnmarshaller(AttachmentUnmarshaller au) {
//        throw new UnsupportedOperationException();
//    }
//
//    public AttachmentUnmarshaller getAttachmentUnmarshaller() {
//        throw new UnsupportedOperationException();
//    }
//
    public void setListener(Listener listener) {
        throw new UnsupportedOperationException();
    }

    public Listener getListener() {
        throw new UnsupportedOperationException();
    }
}
