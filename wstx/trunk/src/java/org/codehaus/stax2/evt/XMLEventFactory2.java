package org.codehaus.stax2.evt;

import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.events.*;

/**
 * Interface that adds missing (but required) methods to
 * {@link XMLEventFactory}; especially ones for creating actual
 * well-behaving DOCTYPE events.
 */
public abstract class XMLEventFactory2
    extends XMLEventFactory
{
    protected XMLEventFactory2() {
        super();
    }

    public abstract DTD2 createDTD(String rootName, String sysId, String pubId,
                                   String intSubset);

    public abstract DTD2 createDTD(String rootName, String sysId, String pubId,
                                  String intSubset, Object processedDTD);

    public abstract StartElement createStartElement(QName name,
                                                    Iterator attrs, Iterator ns,
                                                    NamespaceContext nsCtxt,
                                                    boolean wasEmpty);
}

