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

import java.net.URL;
import java.net.MalformedURLException;
import javax.xml.bind.ValidationEventLocator;
import org.w3c.dom.Node;
import org.xml.sax.Locator;
import org.xml.sax.SAXParseException;

/**
 * Default implementation of the ValidationEventLocator interface.
 * 
 * <p>
 * JAXB providers are allowed to use whatever class that implements
 * the ValidationEventLocator interface. This class is just provided for a
 * convenience.
 *
 * @author <ul><li>Kohsuke Kawaguchi, Sun Microsystems, Inc.</li></ul> 
 * @see javax.xml.bind.Validator
 * @see javax.xml.bind.ValidationEventHandler
 * @see javax.xml.bind.ValidationEvent
 * @see javax.xml.bind.ValidationEventLocator
 * @since 1.6, JAXB 1.0
 */
public class ValidationEventLocatorImpl implements ValidationEventLocator
{
    /**
     * Creates an object with all fields unavailable.
     */
    public ValidationEventLocatorImpl() {
    }

    /** 
     * Constructs an object from an org.xml.sax.Locator. 
     * 
     * The object's ColumnNumber, LineNumber, and URL become available from the 
     * values returned by the locator's getColumnNumber(), getLineNumber(), and
     * getSystemId() methods respectively. Node, Object, and Offset are not 
     * available. 
     * 
     * @param loc the SAX Locator object that will be used to populate this
     * event locator.
     * @throws IllegalArgumentException if the Locator is null
     */
    public ValidationEventLocatorImpl( Locator loc ) {
        if( loc == null ) {
            throw new IllegalArgumentException(
                Messages.format( Messages.MUST_NOT_BE_NULL, "loc" ) );
        }

        this.url = toURL(loc.getSystemId());
        this.columnNumber = loc.getColumnNumber();
        this.lineNumber = loc.getLineNumber();
    }

    /** 
     * Constructs an object from the location information of a SAXParseException. 
     * 
     * The object's ColumnNumber, LineNumber, and URL become available from the 
     * values returned by the locator's getColumnNumber(), getLineNumber(), and
     * getSystemId() methods respectively. Node, Object, and Offset are not 
     * available. 
     * 
     * @param e the SAXParseException object that will be used to populate this
     * event locator.
     * @throws IllegalArgumentException if the SAXParseException is null
     */
    public ValidationEventLocatorImpl( SAXParseException e ) {
        if( e == null ) {
            throw new IllegalArgumentException(
                Messages.format( Messages.MUST_NOT_BE_NULL, "e" ) );
        }

        this.url = toURL(e.getSystemId());
        this.columnNumber = e.getColumnNumber();
        this.lineNumber = e.getLineNumber();
    }

    /** 
     * Constructs an object that points to a DOM Node. 
     * 
     * The object's Node becomes available.  ColumnNumber, LineNumber, Object, 
     * Offset, and URL are not available.
     * 
     * @param _node the DOM Node object that will be used to populate this
     * event locator.
     * @throws IllegalArgumentException if the Node is null
     */
    public ValidationEventLocatorImpl(Node _node) {
        if( _node == null ) {
            throw new IllegalArgumentException(
                Messages.format( Messages.MUST_NOT_BE_NULL, "_node" ) );
        }

        this.node = _node;
    }

    /** 
     * Constructs an object that points to a JAXB content object. 
     * 
     * The object's Object becomes available. ColumnNumber, LineNumber, Node, 
     * Offset, and URL are not available.
     * 
     * @param _object the Object that will be used to populate this
     * event locator.
     * @throws IllegalArgumentException if the Object is null
     */
    public ValidationEventLocatorImpl(Object _object) {
        if( _object == null ) {
            throw new IllegalArgumentException(
                Messages.format( Messages.MUST_NOT_BE_NULL, "_object" ) );
        }

        this.object = _object;
    }
    
    /** Converts a system ID to an URL object. */
    private static URL toURL( String systemId ) {
        try {
            return new URL(systemId);
        } catch( MalformedURLException e ) {
            // TODO: how should we handle system id here?
            return null;    // for now
        }
    }
    
    private URL url = null;
    private int offset = -1;
    private int lineNumber = -1;
    private int columnNumber = -1;
    private Object object = null;
    private Node node = null;
    
    
    /**
     * @see javax.xml.bind.ValidationEventLocator#getURL()
     */
    public URL getURL() {
        return url;
    }    
    
    /**
     * Set the URL field on this event locator.  Null values are allowed.
     * 
     * @param _url the url
     */
    public void setURL( URL _url ) {
        this.url = _url;
    }    
    
    /**
     * @see javax.xml.bind.ValidationEventLocator#getOffset()
     */
    public int getOffset() {
        return offset;
    }
    
    /**
     * Set the offset field on this event locator.  
     * 
     * @param _offset the offset
     */
    public void setOffset( int _offset ) {
        this.offset = _offset;
    }
    
    /**
     * @see javax.xml.bind.ValidationEventLocator#getLineNumber()
     */
    public int getLineNumber() {
        return lineNumber;
    }
    
    /**
     * Set the lineNumber field on this event locator.
     * 
     * @param _lineNumber the line number
     */
    public void setLineNumber( int _lineNumber ) {
        this.lineNumber = _lineNumber;
    }
    
    /**
     * @see javax.xml.bind.ValidationEventLocator#getColumnNumber()
     */
    public int getColumnNumber() {
        return columnNumber;
    }
    
    /**
     * Set the columnNumber field on this event locator.
     * 
     * @param _columnNumber the column number
     */
    public void setColumnNumber( int _columnNumber ) {
        this.columnNumber = _columnNumber;
    }
    
    /**
     * @see javax.xml.bind.ValidationEventLocator#getObject()
     */
    public Object getObject() {
        return object;
    }
    
    /**
     * Set the Object field on this event locator.  Null values are allowed.
     * 
     * @param _object the java content object
     */
    public void setObject( Object _object ) {
        this.object = _object;
    }
    
    /**
     * @see javax.xml.bind.ValidationEventLocator#getNode()
     */
    public Node getNode() {
        return node;
    }
    
    /**
     * Set the Node field on this event locator.  Null values are allowed.
     * 
     * @param _node the Node
     */
    public void setNode( Node _node ) {
        this.node = _node;
    }
    
//    /**
//     * Returns a string representation of this object in a format
//     * helpful to debugging.
//     * 
//     * @see Object#equals(Object)
//     */
//    public String toString() {
//    	// SwingJS no Node;
//        return MessageFormat.format("[node={0},object={1},url={2},line={3},col={4},offset={5}]",
//            getNode(),
//            getObject(),
//            getURL(),
//            String.valueOf(getLineNumber()),
//            String.valueOf(getColumnNumber()),
//            String.valueOf(getOffset()));
//    }
}
