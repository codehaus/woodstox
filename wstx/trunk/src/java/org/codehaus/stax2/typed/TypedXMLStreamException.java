package org.codehaus.stax2.typed;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

/**
 * <p>This class represents an exception throw by an {@link
 * TypedXMLStreamReader} or an {@link TypedXMLStreamWriter}. It is
 * typically used to indicate a problem reading or writing
 * typed data.</p>
 *
 * @author Santiago.PericasGeertsen@sun.com
 * @author Tatu Saloranta
 */
public class TypedXMLStreamException extends XMLStreamException {
    
    /**
     * Lexical representation of the content that could not be 
     * converted to the requested type. May be <code>null</code> 
     * if a processor is unable to provide it.
     */
    protected String characterData;
    
    /**
     * Default constructor.
     */
    public TypedXMLStreamException(String characterData){
        super();
        this.characterData = characterData;
    }
    
    /**
     * Construct an exception with the assocated message.
     *
     * @param msg  The message to report.
     */
    public TypedXMLStreamException(String msg, String lexical,
            String characterData) {
        super(msg);
        this.characterData = characterData;        
    }
    
    /**
     * Construct an exception with the assocated exception
     *
     * @param th  A nested exception.
     */
    public TypedXMLStreamException(Throwable th, String characterData) {
        super(th);
        this.characterData = characterData;
    }
    
    /**
     * Construct an exception with the assocated message and exception
     *
     * @param th  A nested exception.
     * @param msg  The message to report.
     */
    public TypedXMLStreamException(String msg, Throwable th, 
            String characterData) {
        super(msg, th);
        this.characterData = characterData;        
    }
    
    /**
     * Construct an exception with the assocated message, exception and 
     * location.
     *
     * @param th  A nested exception.
     * @param msg  The message to report.
     * @param location  The location of the error.
     */
    public TypedXMLStreamException(String msg, Location location, 
            Throwable th, String characterData) {
        super(msg, location, th);
        this.characterData = characterData;
    }
    
    /**
     * Construct an exception with the assocated message, exception and 
     * location.
     *
     * @param msg  The message to report.
     * @param location  The location of the error.
     */
    public TypedXMLStreamException(String msg, Location location,
            String characterData) {
        super(msg, location);
        this.characterData = characterData;
    }
    
    /** 
     * Return the lexical representation of the attribute or element
     * content that could not be converted as requested. 
     *
     * @return  Lexical representation of unconverted content or
     *          <code>null</code> if unavailable.
     */
    public String getCharacterData() {
        return characterData;
    }
}
