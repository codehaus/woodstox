package com.ctc.stax.staxmate;

import java.io.IOException;
import java.io.Writer;

import javax.xml.stream.*;

/**
 * Main utility class of the package, that is usually used to create the
 * main-level (usually root) iterator.
 *
 * @author Tatu Saloranta
 */
public final class StaxMate
{
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
    // Text collection functionality
    /////////////////////////////////////////////////
     */

    /**
     * Method that will traverse through all the descendant nodes of the
     * start element
     * pointed to by the specified iterator, collects all text from text
     * nodes, and returns the text as String. After traversing specified
     * iterator needs to be advanced to point to the node that comes
     * after end element.
     *
     * @param it Iterator that points to a start element of which descendant
     *    text nodes to process.
     */
    public static String collectDescendantText(SMIterator it, boolean includeIgnorable)
        throws XMLStreamException
    {
        SMFilter f = includeIgnorable
            ? SMFilterFactory.getTextOnlyFilter()
            : SMFilterFactory.getNonIgnorableTextFilter();
        SMIterator childIt = it.descendantIterator(f);

        // Any text in there?
        if (childIt.getNext() == SMIterator.SM_NODE_NONE) {
            return "";
        }

        String text = childIt.getCurrentText();
        if ((childIt.getNext()) == SMIterator.SM_NODE_NONE) {
            return text;
        }

        int size = text.length();
        StringBuffer sb = new StringBuffer((size < 500) ? 500 : size);
        sb.append(text);
        XMLStreamReader sr = childIt.getStreamReader();
        do {
            // Let's assume char array access is more efficient...
            sb.append(sr.getTextCharacters(), sr.getTextStart(),
                      sr.getTextLength());
        } while (childIt.getNext() != SMIterator.SM_NODE_NONE);

        return sb.toString();
    }

    /**
     * Method similar to {@link #collectDescendantText}, but will write
     * the text to specified Writer instead of collecting it into a
     * String.
     *
     * @param it Iterator that points to a start element of which descendant
     *    text nodes to process.
     */
    public static void processDescendantText(SMIterator it, Writer w, boolean includeIgnorable)
        throws IOException, XMLStreamException
    {
        SMFilter f = includeIgnorable
            ? SMFilterFactory.getTextOnlyFilter()
            : SMFilterFactory.getNonIgnorableTextFilter();
        SMIterator childIt = it.descendantIterator(f);

        // Any text in there?
        XMLStreamReader sr = childIt.getStreamReader();
        while (childIt.getNext() != SMIterator.SM_NODE_NONE) {
            // Let's assume char array access is more efficient...
            w.write(sr.getTextCharacters(), sr.getTextStart(),
                    sr.getTextLength());
        }
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
            System.err.println("Usage: java "+StaxMate.class+" [input file]");
            System.exit(1);
        }
        XMLInputFactory f = XMLInputFactory.newInstance();
        XMLStreamReader r = f.createXMLStreamReader(new java.io.FileInputStream(args[0]));

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

        while ((type = it.getNext()) != SMIterator.SM_NODE_NONE) {
            System.out.print("["+it.getDepth()+"] -> "+type);
            if (type == XMLStreamConstants.START_ELEMENT) {
                XMLStreamReader sr = it.getStreamReader();
                System.out.print(" <"+sr.getPrefix()+":"+sr.getLocalName()+">");
                System.out.println(" Path: "+getPath(it));
                System.out.println(" Prev: "+getSiblings(it));

                traverseNested(it.childIterator(null));
            } else if (type == XMLStreamConstants.END_ELEMENT) {
                XMLStreamReader sr = it.getStreamReader();
                System.out.println(" </"+sr.getPrefix()+":"+sr.getLocalName()+">");
            } else if (it.isCurrentText()) {
                System.out.println(" Text (trim): '"+it.getCurrentText().trim()+"'");
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

        while ((type = it.getNext()) != SMIterator.SM_NODE_NONE) {
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
                System.out.println(" Text (trim): '"+it.getCurrentText().trim()+"'");
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
