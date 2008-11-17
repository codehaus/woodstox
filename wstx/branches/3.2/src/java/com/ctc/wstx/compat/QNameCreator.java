package com.ctc.wstx.compat;

import javax.xml.namespace.QName;

/**
 * Helper class used to solve [WSTX-174]: some older AppServers were
 * shipped with incompatible version of QName class, which is missing
 * the 3 argument constructor. To address this, we'll use bit of
 * ClassLoader hacker to gracefully (?) downgrade to using 2 arg
 * alternatives if necessary.
 *
 * @author Tatu Saloranta
 * 
 * @since 3.2.8
 */
public final class QNameCreator
{
    /**
     * Creator object that creates QNames using proper 3-arg constructor.
     * If dynamic class loading fails
     */
    private final static Helper _helper;
    static {
        Helper h = null;
        try {
            // Not sure where it'll fail, constructor or create...
            Helper h0 = new Helper();
            /*QName n =*/ h0.create("elem", "http://dummy", "ns");
            h = h0;
        } catch (Throwable t) {
            System.err.println("WARN: Could not construct QNameCreator.Helper; assume 3-arg QName constructor not available and use 2-arg method instead. Problem: "+t.getMessage());
        }
        _helper = h;
    }

    public static QName create(String uri, String localName, String prefix)
    {
        if (_helper == null) { // can't use 3-arg constructor; but 2-arg will be there
            return new QName(uri, localName);
        }
        return _helper.create(uri, localName, prefix);
    }

    /**
     * Helper class used to encapsulate calls to the missing method.
     */
    private final static class Helper
    {
        public Helper() { }
        
        public QName create(String localName, String nsURI, String prefix)
        {
            return new QName(localName, nsURI, prefix);
        }
    }
}

