package com.ctc.xpp;

import java.io.*;
import java.util.*;

import javax.xml.stream.*;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

/**
 * Simple factory implementation to use with
 * {@link XppParserOnStaxReader} and {@link XppSerializerOnStaxWriter}.
 *
 * @author Tatu Saloranta
 */
public class XppOnStaxFactory
    extends XmlPullParserFactory
{
    final XMLInputFactory mStaxInputFactory = XMLInputFactory.newInstance();

    final XMLOutputFactory mStaxOutputFactory = XMLOutputFactory.newInstance();

    // // // Default settings for parsers

    boolean mNsAware = false;

    boolean mValidating = false;

    boolean mSupportsDTDs = false;

    public XppOnStaxFactory() {
        try {
            /* XmlPull seems to expect that automatic namespace repairing is
             * done automatically....
             */
            mStaxOutputFactory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, Boolean.TRUE);
        } catch (Throwable t) {
            System.err.println("Warning, XMLOutputFactory configuration problems: "+t);
        }
    }

    public boolean getFeature(String name)
    {
        if (name.equals(XmlPullParser.FEATURE_PROCESS_DOCDECL)) {
            return mSupportsDTDs;
        } else if (name.equals(XmlPullParser.FEATURE_PROCESS_NAMESPACES)) {
            return isNamespaceAware();
        } else if (name.equals(XmlPullParser.FEATURE_REPORT_NAMESPACE_ATTRIBUTES)) {
            /* Not supported by StAX; but let's still recognize it ok.
             */
            return false;
        } else if (name.equals(XmlPullParser.FEATURE_VALIDATION)) {
            return isValidating();
        }
        return false;
    }
    
    public boolean isNamespaceAware()
    {
        return mNsAware;
    }

    public boolean isValidating()
    {
        return mValidating;
    }

    public XmlPullParser newPullParser()
    {
        return new XppParserOnStaxReader(mNsAware, mSupportsDTDs, mValidating,
                                         mStaxInputFactory);
    }

    public XmlSerializer newSerializer()
    {
        return new XppSerializerOnStaxWriter(mStaxOutputFactory);
    }

    public void setFeature(String name, boolean state)
        throws XmlPullParserException
    {
        if (name.equals(XmlPullParser.FEATURE_PROCESS_DOCDECL)) {
            mSupportsDTDs = state;
        } else if (name.equals(XmlPullParser.FEATURE_PROCESS_NAMESPACES)) {
            setNamespaceAware(state);
        } else if (name.equals(XmlPullParser.FEATURE_REPORT_NAMESPACE_ATTRIBUTES)) {
            /* Not supported by StAX; but let's still recognize it ok.
             */
        } else if (name.equals(XmlPullParser.FEATURE_VALIDATION)) {
            setValidating(state);
        } else {
            throw new XmlPullParserException("Unrecognized feature '"+name+"'.");
        }
    }

    public void setNamespaceAware(boolean state)
    {
        mNsAware = state;
    }

    public void setValidating(boolean state)
    {
        mValidating = state;
    }
    /*
    /////////////////////////////////////////////////////
    // Simple test driver, to check loading of the
    // class and instance creation work; and then just
    // iterate over all the nodes.
    /////////////////////////////////////////////////////
     */

    public static void main(String[] args)
        throws Exception
    {
        if (args.length != 1) {
            System.err.println("Usage: "+(com.ctc.xpp.XppOnStaxFactory.class)+" [input file]");
            System.exit(1);
        }
        
        // Could also use base classes factory method...
        XmlPullParserFactory f = new XppOnStaxFactory();
        f.setNamespaceAware(true);
        XmlPullParser xpp = f.newPullParser();
        System.out.println("Created pull parser: "+xpp.getClass());
        xpp.setInput(new java.io.FileReader(args[0]));

        int eventType;

        do {
            eventType = xpp.getEventType();
            if(eventType == xpp.START_DOCUMENT) {
                System.out.println("[StartDoc]");
            } else if(eventType == xpp.END_DOCUMENT) {
                System.out.println("[EndDoc]");
            } else if(eventType == xpp.START_TAG
                      || eventType == xpp.END_TAG) {
                boolean start = (eventType == xpp.START_TAG);
                System.out.print("<");
                if (!start) {
                    System.out.print("/");
                }
                String uri = xpp.getNamespace();
                if (uri != null && uri.length() > 0) { // has namespaces
                    System.out.print(xpp.getPrefix());
                    System.out.print(':');
                }
                System.out.print(xpp.getName());
                System.out.print(">");
                if (uri != null && uri.length() > 0) {
                    System.out.print('{');
                    System.out.print(xpp.getNamespace());
                    System.out.print('}');
                }
                System.out.println();
            } else if(eventType == xpp.TEXT) {
                System.out.println("[Text] '"+xpp.getText()+"'");
            }
            eventType = xpp.next();
        } while (eventType != xpp.END_DOCUMENT);
    }
}

