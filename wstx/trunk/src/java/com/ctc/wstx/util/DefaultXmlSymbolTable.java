package com.ctc.wstx.util;

import javax.xml.XMLConstants;

import com.ctc.wstx.util.SymbolTable;

/**
 * Factory class used for instantiating pre-populated XML symbol
 * tables. Such tables already have basic String constants that
 * XML standard defines.
 */
public final class DefaultXmlSymbolTable
{
    /**
     * Root symbol table from which child instances are derived.
     */
    final static SymbolTable sInstance;

    final static String mNsPrefixXml;
    final static String mNsPrefixXmlns;

    final static String mRefAmp;
    final static String mRefGt;
    final static String mRefLt;
    final static String mRefApos;
    final static String mRefQuot;

    static {
        /* 128 means it's ok without resize up to ~96 symbols; true that
         * default symbols added will be interned.
         */
        sInstance = new SymbolTable(true, 128);
        // Let's add default namespace identifiers
        mNsPrefixXml = sInstance.findSymbol(XMLConstants.XML_NS_PREFIX); // "xml"
        mNsPrefixXmlns = sInstance.findSymbol(XMLConstants.XMLNS_ATTRIBUTE); // "xmlns"

        mRefAmp = sInstance.findSymbol("amp");
        mRefGt = sInstance.findSymbol("gt");
        mRefLt = sInstance.findSymbol("lt");
        mRefApos = sInstance.findSymbol("apos");
        mRefQuot = sInstance.findSymbol("quot");

        /* No need to add keywords, as they are checked directly by
         * Reader, without constructing Strings.
         */
    }

    /*
    ///////////////////////////////////////////////////
    // Public API, factory method(s):
    ///////////////////////////////////////////////////
     */

    /**
     * Method that will return an instance of SymbolTable that has basic
     * XML 1.0 constants pre-populated.
     */
    public static SymbolTable getInstance() {
        return sInstance.makeChild();
    }

    /*
    ///////////////////////////////////////////////////
    // Public API, efficient access to (shared)
    // constants values:
    ///////////////////////////////////////////////////
     */

    public static String getXmlSymbol() {
        return mNsPrefixXml;
    }

    public static String getXmlnsSymbol() {
        return mNsPrefixXmlns;
    }

    public static char getDefaultCharEntity(String ent) {
        if (ent == mRefAmp) {
            return '&';
        }
        if (ent == mRefLt) {
            return '<';
        }
        if (ent == mRefQuot) {
            return '"';
        }
        if (ent == mRefApos) {
            return '\'';
        }
        if (ent == mRefGt) {
            return '>';
        }
        return '\0';
    }
}
