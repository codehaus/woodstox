package org.codehaus.staxmate;

import java.io.IOException;
import java.io.Writer;

import javax.xml.stream.*;

import org.codehaus.staxmate.sr.SMElementInfo;
import org.codehaus.staxmate.sr.SMFilter;
import org.codehaus.staxmate.sr.SMFilterFactory;
import org.codehaus.staxmate.sr.SMFlatIterator;
import org.codehaus.staxmate.sr.SMIterator;
import org.codehaus.staxmate.sr.SMNestedIterator;


/**
 * Factory class used to create {@link SMIterator} instances.
 *
 * @author Tatu Saloranta
 */
public final class SMIteratorFactory
{
    private SMIteratorFactory() { }

    /*
    /////////////////////////////////////////////////
    // Iterator construction
    /////////////////////////////////////////////////
     */

    public static SMNestedIterator nestedIterator(XMLStreamReader sr, SMFilter f) {
        return new SMNestedIterator(null, sr, f);
    }

    public static SMFlatIterator flatIterator(XMLStreamReader sr, SMFilter f) {
        return new SMFlatIterator(null, sr, f);
    }

    /**
     * Return a nested iterator that will only ever iterate to one node, that
     * is, the root element of the document reader is reading.
     */
    public static SMNestedIterator rootElementIterator(XMLStreamReader sr)
    {
        return nestedIterator(sr, SMFilterFactory.getElementOnlyFilter());
    }

    /*
    /////////////////////////////////////////////////
    // Simple test driver functionality
    /////////////////////////////////////////////////
     */

    public static void main(String[] args)
        throws Exception
    {
        if (args.length != 1) {
            System.err.println("Usage: java "+SMIteratorFactory.class+" [input file]");
            System.exit(1);
        }
        XMLInputFactory f = XMLInputFactory.newInstance();
        java.io.File file = new java.io.File(args[0]);
        XMLStreamReader r = f.createXMLStreamReader(file.toURL().toExternalForm(),
                                                    new java.io.FileInputStream(file));

        ///*
        SMIterator it = nestedIterator(r, null);
        it.setElementTracking(SMIterator.TRACK_ELEM_VISIBLE_SIBLINGS);
        traverseNested(it);
        //*/

        /*
        SMIterator it = flatIterator(r, null);
        it.setElementTracking(SMIterator.TRACK_ELEM_VISIBLE_SIBLINGS);
        traverseFlat(it);
        */

        r.close();
    }

    static void traverseNested(SMIterator it)
        throws Exception
    {
        int type;

        while ((type = it.getNext()) != SMIterator.SM_EVENT_NONE) {
            System.out.print("["+it.getDepth()+"] -> "+type);
            if (type == XMLStreamConstants.START_ELEMENT) {
                //XMLStreamReader sr = it.getStreamReader();
                System.out.print(" <"+it.getElemPrefix()+":"+it.getElemLocalName()+">");
                System.out.println(" Path: "+getPath(it));
                System.out.println(" Prev: "+getSiblings(it));

                traverseNested(it.childIterator(null));
            } else if (type == XMLStreamConstants.END_ELEMENT) {
                System.out.println(" </"+it.getElemPrefix()+":"+it.getElemLocalName()+">");
            } else if (it.isCurrentText()) {
                System.out.println(" Text (trim): '"+it.getText().trim()+"'");
            } else {
                System.out.println();
            }
        }

        System.out.println("["+it.getDepth()+"] END");
    }

    static void traverseFlat(SMIterator it)
        throws Exception
    {
        int type;

        while ((type = it.getNext()) != SMIterator.SM_EVENT_NONE) {
            System.out.print("["+it.getDepth()+"] -> "+type);
            if (type == XMLStreamConstants.START_ELEMENT) {
                XMLStreamReader sr = it.getStreamReader();
                System.out.print(" <"+sr.getPrefix()+":"+sr.getLocalName()+">");
                System.out.println(" Path: "+getPath(it));
                System.out.println(" Prev: "+getSiblings(it));

            } else if (type == XMLStreamConstants.END_ELEMENT) {
                XMLStreamReader sr = it.getStreamReader();
                System.out.println(" </"+sr.getPrefix()+":"+sr.getLocalName()+">");
                System.out.println(" Path: "+getPath(it));
                System.out.println(" Prev: "+getSiblings(it));
            } else if (it.isCurrentText()) {
                System.out.println(" Text (trim): '"+it.getText().trim()+"'");
            } else {
                System.out.println();
            }
        }

        System.out.println("["+it.getDepth()+"] END");
    }

    static String getPath(SMIterator it)
    {
        SMElementInfo curr = it.getTrackedElement();
        int nodeIndex = curr.getNodeIndex();
        int elemIndex = curr.getElementIndex();

        StringBuffer sb = new StringBuffer();
        for (; curr != null; curr = curr.getParent()) {
            sb.insert(0, '/');
            sb.insert(0, curr.getLocalName());
        }
        sb.insert(0, "["+nodeIndex+" / "+elemIndex+"] ");
        return sb.toString();
    }

    static String getSiblings(SMIterator it)
    {
        SMElementInfo curr = it.getTrackedElement();
        StringBuffer sb = new StringBuffer();
        for (; curr != null; curr = curr.getPreviousSibling()) {
            sb.insert(0, "->");
            sb.insert(0, curr.getLocalName());
        }
        return sb.toString();
    }
}
