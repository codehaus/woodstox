package org.codehaus.staxmate;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.codehaus.staxmate.sw.SMOutputContext;
import org.codehaus.staxmate.sw.SMOutputDocument;
import org.codehaus.staxmate.sw.SMRootFragment;

/**
 * Factory class used to create various outputter (like
 * {@link SMOutputDocument} and {@link SMRootFragment}) instances.
 */
public final class SMOutputFactory
{
    private SMOutputFactory() { }

    /*
    ////////////////////////////////////////////////////
    // Document output construction
    //
    // note: no buffered alternatives -- they are easy
    // to create, just add a buffered fragment inside
    // the document fragment
    ////////////////////////////////////////////////////
     */

    public static SMOutputDocument createOutputDocument(XMLStreamWriter sw)
        throws XMLStreamException
    {
        SMOutputContext ctxt = SMOutputContext.createInstance(sw);
        return ctxt.createDocument();
    }

    public static SMOutputDocument createOutputDocument(XMLStreamWriter sw,
							String version,
							String encoding,
							boolean standAlone)
        throws XMLStreamException
    {
        SMOutputContext ctxt = SMOutputContext.createInstance(sw);
        return ctxt.createDocument(version, encoding, standAlone);
    }

    /*
    ///////////////////////////////////////////////////////
    // Fragment output construction
    // 
    // May be useful when only sub-tree(s) of a document
    // is done using StaxMate
    ///////////////////////////////////////////////////////
     */

    public static SMRootFragment createOutputFragment(XMLStreamWriter sw)
        throws XMLStreamException
    {
        SMOutputContext ctxt = SMOutputContext.createInstance(sw);
        return ctxt.createRootFragment();
    }
}
