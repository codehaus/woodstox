/* Woodstox XML processor
 *
 * Copyright (c) 2004- Tatu Saloranta, tatu.saloranta@iki.fi
 *
 * Licensed under the License specified in the file LICENSE which is
 * included with the source code.
 * You may not use this file except in compliance with the License.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ctc.wstx.dtd;

import javax.xml.stream.Location;

/**
 * Simple container class used to contain information about the default
 * value for an attribute. Although for most use cases a simple String
 * would suffice, there are cases where additional information is needed
 * (especially status of 'broken' default values, which only need to be
 * reported should the default value be needed).
 */
public final class DefaultAttrValue
{
    private final NameKey mAttrName;

    private String mValue = null;

    /**
     * For now, let's only keep track of the first undeclared entity:
     * can be extended if necessary.
     */
    private UndeclaredEntity mUndeclaredEntity = null;
    
    /*
    ////////////////////////////////////////////////////
    // Life-cycle (creation, configuration)
    ////////////////////////////////////////////////////
     */

    public DefaultAttrValue(NameKey attrName)
    {
        mAttrName = attrName;
    }

    public void setValue(String v) {
        mValue = v;
    }

    public void addUndeclaredPE(String name, Location loc)
    {
        addUndeclaredEntity(name, loc, true);
    }

    public void addUndeclaredGE(String name, Location loc)
    {
        addUndeclaredEntity(name, loc, false);
    }

    /*
    ////////////////////////////////////////////////////
    // Accessors:
    ////////////////////////////////////////////////////
     */

    public boolean hasUndeclaredEntities() {
        return (mUndeclaredEntity != null);
    }

    public String getValue() {
        return mValue;
    }

    /*
    ////////////////////////////////////////////////////
    // Internal methods
    ////////////////////////////////////////////////////
     */

    private void addUndeclaredEntity(String name, Location loc, boolean isPe)
    {
        if (mUndeclaredEntity == null) {
            mUndeclaredEntity = new UndeclaredEntity(name, loc, isPe);
        }
    }

    /*
    ////////////////////////////////////////////////////
    // Helper class(es):
    ////////////////////////////////////////////////////
     */

    final static class UndeclaredEntity
    {
        final String mName;
        final boolean mIsPe;
        final Location mLocation;

        UndeclaredEntity(String name, Location loc, boolean isPe)
        {
            mName = name;
            mIsPe = isPe;
            mLocation = loc;
        }
    }
}
