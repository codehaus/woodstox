/* Woodstox XML processor
 *
 * Copyright (c) 2004 Tatu Saloranta, tatu.saloranta@iki.fi
 *
 * Licensed under the License specified in file LICENSE, included with
 * the source code.
 * You may not use this file except in compliance with the License.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ctc.wstx.stax;

import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.*;
import javax.xml.stream.events.*;

import org.codehaus.stax2.evt.XMLEventFactory2;
import org.codehaus.stax2.evt.DTD2;

import com.ctc.wstx.compat.QNameCreator;
import com.ctc.wstx.dtd.DTDSubset;
import com.ctc.wstx.evt.*;

/**
 * Basic implementation of {@link XMLEventFactory} to be used with
 * Woodstox.
 */
public final class WstxEventFactory
    extends XMLEventFactory2
{
    /**
     * "Current" location of this factory; ie. location assigned for all
     * events created by this factory.
     */
    private Location mLocation;

    public WstxEventFactory() {
    }

    /*
    /////////////////////////////////////////////////////////////
    // XMLEventFactory API
    /////////////////////////////////////////////////////////////
     */

    public Attribute createAttribute(QName name, String value) {
        return new WAttribute(mLocation, name, value, true);
    }

    public Attribute createAttribute(String localName, String value) {
        return new WAttribute(mLocation, localName, null, null, value, true);
    }

    public Attribute createAttribute(String prefix, String nsURI,
                                     String localName, String value)
    {
        return new WAttribute(mLocation, localName, nsURI, prefix, value, true);
    }

    public Characters createCData(String content) {
        return new WCharacters(mLocation, content, true);
    }

    public Characters createCharacters(String content) {
        return new WCharacters(mLocation, content, false);
    }

    public Comment createComment(String text) {
        return new WComment(mLocation, text);
    }

    /**
     * Note: constructing DTD events this way means that there will be no
     * internal presentation of actual DTD; no parsing is implied by
     * construction.
     */
    public DTD createDTD(String dtd) {
        return new WDTD(mLocation, dtd);
    }

    public EndDocument createEndDocument() {
        return new WEndDocument(mLocation);
    }

    public EndElement createEndElement(QName name, Iterator namespaces) {
        return new WEndElement(mLocation, name, namespaces);
    }

    public EndElement createEndElement(String prefix, String nsURI,
                                       String localName)
    {
        return createEndElement(new QName(nsURI, localName), null);
    }

    public EndElement createEndElement(String prefix, String nsURI,
                                       String localName, Iterator ns)
    {
        return createEndElement(QNameCreator.create(nsURI, localName, prefix), ns);
    }

    public EntityReference createEntityReference(String name, EntityDeclaration decl)
    {
        return new WEntityReference(mLocation, decl);
    }

    public Characters createIgnorableSpace(String content) {
        return WCharacters.createIgnorableWS(mLocation, content);
    }

    public Namespace createNamespace(String nsURI) {
        return new WNamespace(mLocation, nsURI);
    }
    
    public Namespace createNamespace(String prefix, String nsUri) {
        return new WNamespace(mLocation, prefix, nsUri);
    }

    public ProcessingInstruction createProcessingInstruction(String target, String data) {
        return new WProcInstr(mLocation, target, data);
    }
    
    public Characters createSpace(String content) {
        return WCharacters.createNonIgnorableWS(mLocation, content);
    }

    public StartDocument createStartDocument() {
        return new WStartDocument(mLocation);
    }

    public StartDocument createStartDocument(String encoding) {
        return new WStartDocument(mLocation, encoding);
    }

    public StartDocument createStartDocument(String encoding, String version) {
        return new WStartDocument(mLocation, encoding, version);
    }

    public StartDocument createStartDocument(String encoding, String version, boolean standalone)
    {
        return new WStartDocument(mLocation, encoding, version,
                                  true, standalone);
    }

    public StartElement createStartElement(QName name, Iterator attr, Iterator ns)
    {
        return createStartElement(name, attr, ns, null);
    }

    public StartElement createStartElement(String prefix, String nsURI, String localName)
    {
        return createStartElement(QNameCreator.create(nsURI, localName, prefix),
                                  null, null, null);
    }

    public StartElement createStartElement(String prefix, String nsURI,
                                           String localName, Iterator attr,
                                           Iterator ns)
    {
        return createStartElement(QNameCreator.create(nsURI, localName, prefix),
                                  attr, ns, null);
    }

    public StartElement createStartElement(String prefix, String nsURI,
                                           String localName, Iterator attr,
                                           Iterator ns, NamespaceContext nsCtxt)
    {
        return createStartElement(QNameCreator.create(nsURI, localName, prefix),
                                  attr, ns, nsCtxt);
    }

    public void setLocation(Location loc) {
        mLocation = loc;
    }

    /*
    /////////////////////////////////////////////////////////////
    // XMLEventFactory2 methods
    /////////////////////////////////////////////////////////////
     */

    public DTD2 createDTD(String rootName, String sysId, String pubId,
                          String intSubset)
    {
        return new WDTD(mLocation, rootName, sysId, pubId, intSubset);
    }

    public DTD2 createDTD(String rootName, String sysId, String pubId,
                          String intSubset, Object processedDTD)
    {
        return new WDTD(mLocation, rootName, sysId, pubId, intSubset,
                        (DTDSubset) processedDTD);
    }

    /*
    /////////////////////////////////////////////////////////////
    // Internal methods
    /////////////////////////////////////////////////////////////
     */

    private StartElement createStartElement(QName name, Iterator attr,
					    Iterator ns, NamespaceContext ctxt)
    {
        return SimpleStartElement.construct(mLocation, name, attr, ns, ctxt);
    }
}
