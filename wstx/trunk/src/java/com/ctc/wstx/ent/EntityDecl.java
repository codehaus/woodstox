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

package com.ctc.wstx.ent;

import java.io.IOException;
import java.io.Writer;
import java.net.URL;

import javax.xml.stream.Location;
import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamException;

import com.ctc.wstx.exc.WstxException;
import com.ctc.wstx.io.WstxInputLocation;
import com.ctc.wstx.io.WstxInputSource;

/**
 * Abstract base class for various entity declarations DTD reader
 * has parsed from DTD subsets.
 */
public abstract class EntityDecl
{
    final Location mLocation;

    /**
     * Name/id of the entity used to reference it.
     */
    final String mName;

    /**
     * Context that can be used to resolve references encountered from
     * expanded contents of this entity.
     */
    final URL mContext;

    public EntityDecl(Location loc, String name, URL ctxt)
    {
        mLocation = loc;
        mName = name;
        mContext = ctxt;
    }

    public final String getBaseURI() {
        return mContext.toString();
    }

    public final String getName() {
        return mName;
    }

    public final Location getLocation() {
        return mLocation;
    }

    public abstract String getNotationName();

    public abstract String getPublicId();

    public abstract String getReplacementText();

    public abstract int getReplacementText(Writer w)
        throws IOException;

    public abstract String getSystemId();

    /*
    ///////////////////////////////////////////
    // Implementation of abstract base methods
    ///////////////////////////////////////////
     */

    public abstract void writeEnc(Writer w) throws IOException;

    /*
    ///////////////////////////////////////////
    // Extended API for Wstx core
    ///////////////////////////////////////////
     */

    // // // Extended location info

    public final URL getSource() {
        return mContext;
    }

    // // // Access to data

    public abstract char[] getReplacementChars();

    public final int getReplacementTextLength() {
        String str = getReplacementText();
        return (str == null) ? 0 : str.length();
    }

    // // // Type information

    public abstract boolean isExternal();

    public abstract boolean isParsed();

    // // // Factory methods

    /**
     * Method called to create the new input source through which expansion
     * value of the entity can be read.
     */
    public abstract WstxInputSource createInputSource(WstxInputSource parent, 
                                                      XMLResolver res)
        throws IOException, XMLStreamException;
}
