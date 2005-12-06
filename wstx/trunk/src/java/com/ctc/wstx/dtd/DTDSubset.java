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

import java.util.*;

import com.ctc.wstx.exc.WstxException;
import com.ctc.wstx.sr.InputProblemReporter;

/**
 * This is the abstract base class that defines API for Objects that contain
 * specification read from DTDs (internal and external subsets).
 *<p>
 * API is separated from its implementations so that different XML reader
 * subsets can be created; specifically ones with no DTD processing 
 * functionality.
 */
public abstract class DTDSubset
{
    /*
    //////////////////////////////////////////////////////
    // Life-cycle
    //////////////////////////////////////////////////////
     */

    protected DTDSubset() { }

    /**
     * Method that will combine definitions from this internal subset with
     * definitions from passed-in external subset, producing a new combined
     * DTDSubset instance.
     */
    public abstract DTDSubset combineWithExternalSubset(InputProblemReporter rep,
                                                        DTDSubset extSubset)
        throws WstxException;

    /*
    //////////////////////////////////////////////////////
    // Public API
    //////////////////////////////////////////////////////
     */

    public abstract boolean isCachable();
    
    public abstract HashMap getGeneralEntityMap();

    public abstract List getGeneralEntityList();

    public abstract HashMap getParameterEntityMap();

    public abstract HashMap getNotationMap();

    public abstract List getNotationList();

    public abstract HashMap getElementMap();

    /**
     * Method used in determining whether cached external subset instance
     * can be used with specified internal subset. If ext. subset references
     * any parameter entities int subset (re-)defines, it can not; otherwise
     * it can be used.
     *
     * @return True if this (external) subset refers to a parameter entity
     *    defined in passed-in internal subset.
     */
    public abstract boolean isReusableWith(DTDSubset intSubset);
}
