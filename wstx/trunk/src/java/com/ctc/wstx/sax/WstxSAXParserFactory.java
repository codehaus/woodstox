/*
 * Copyright (c) 2004- Tatu Saloranta, tatu.saloranta@iki.fi
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

package com.ctc.wstx.sax;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.XMLReader;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

import com.ctc.wstx.stax.WstxInputFactory;

/**
 * This is implementation of the main JAXP SAX factory, and as such
 * acts as the entry point from JAXP.
 *<p>
 * Note: most of the SAX features are not configurable as of yet.
 * However, effort is made to recognize all existing standard features
 * and properties, to allow using code to figure out existing
 * capabilities automatically.
 */
public class WstxSAXParserFactory
    extends SAXParserFactory
{
    final WstxInputFactory mStaxFactory;

    public WstxSAXParserFactory()
    {
        mStaxFactory = new WstxInputFactory();

        /* defaults should be fine... except that for some weird
         * reason, by default namespace support is defined to be off
         */
        setNamespaceAware(true);
    }

    public boolean getFeature(String name)
        throws SAXNotRecognizedException, SAXNotSupportedException
    {
        SAXFeature stdFeat = SAXFeature.findByUri(name);

        if (stdFeat == SAXFeature.EXTERNAL_GENERAL_ENTITIES) {
        } else if (stdFeat == SAXFeature.EXTERNAL_PARAMETER_ENTITIES) {
        } else if (stdFeat == SAXFeature.IS_STANDALONE) {
        } else if (stdFeat == SAXFeature.LEXICAL_HANDLER_PARAMETER_ENTITIES) {
        } else if (stdFeat == SAXFeature.NAMESPACES) {
            return mStaxFactory.getConfig().willSupportNamespaces();
        } else if (stdFeat == SAXFeature.NAMESPACE_PREFIXES) {
        } else if (stdFeat == SAXFeature.RESOLVE_DTD_URIS) {
        } else if (stdFeat == SAXFeature.STRING_INTERNING) {
        } else if (stdFeat == SAXFeature.UNICODE_NORMALIZATION_CHECKING) {
        } else if (stdFeat == SAXFeature.USE_ATTRIBUTES2) {
        } else if (stdFeat == SAXFeature.USE_LOCATOR2) {
        } else if (stdFeat == SAXFeature.USE_ENTITY_RESOLVER2) {
        } else if (stdFeat == SAXFeature.VALIDATION) {
        } else if (stdFeat == SAXFeature.XMLNS_URIS) {
        } else if (stdFeat == SAXFeature.XML_1_1) {
        } else {
            throw new SAXNotRecognizedException("Feature '"+name+"' not recognized");
        }
        // nope, not recognized:
        return false; // never gets here
    }

    public SAXParser newSAXParser()
    {
        return new WstxSAXParser(mStaxFactory, isNamespaceAware(), isValidating());
    }

    public void setFeature(String name, boolean enabled)
        throws SAXNotRecognizedException, SAXNotSupportedException
    {
        boolean ok = true;
        SAXFeature stdFeat = SAXFeature.findByUri(name);

        if (stdFeat == SAXFeature.EXTERNAL_GENERAL_ENTITIES) {
        } else if (stdFeat == SAXFeature.EXTERNAL_PARAMETER_ENTITIES) {
        } else if (stdFeat == SAXFeature.IS_STANDALONE) {
        } else if (stdFeat == SAXFeature.LEXICAL_HANDLER_PARAMETER_ENTITIES) {
        } else if (stdFeat == SAXFeature.NAMESPACES) {
        } else if (stdFeat == SAXFeature.NAMESPACE_PREFIXES) {
        } else if (stdFeat == SAXFeature.RESOLVE_DTD_URIS) {
        } else if (stdFeat == SAXFeature.STRING_INTERNING) {
        } else if (stdFeat == SAXFeature.UNICODE_NORMALIZATION_CHECKING) {
        } else if (stdFeat == SAXFeature.USE_ATTRIBUTES2) {
        } else if (stdFeat == SAXFeature.USE_LOCATOR2) {
        } else if (stdFeat == SAXFeature.USE_ENTITY_RESOLVER2) {
        } else if (stdFeat == SAXFeature.VALIDATION) {
        } else if (stdFeat == SAXFeature.XMLNS_URIS) {
        } else if (stdFeat == SAXFeature.XML_1_1) {
        } else {
            throw new SAXNotRecognizedException("Feature '"+name+"' not recognized");
        }

        if (!ok) {
            throw new SAXNotSupportedException("Setting std feature "+stdFeat+" to "+enabled+" not supported");
        }
        return;
    }

    /*
    /////////////////////////////////////////////////
    // Helper methods
    /////////////////////////////////////////////////
     */

    public static void main(String [] args)
    {
        WstxSAXParserFactory f = 
            new WstxSAXParserFactory();
        System.out.println("Ns -> "+f.isNamespaceAware());
    }

}



