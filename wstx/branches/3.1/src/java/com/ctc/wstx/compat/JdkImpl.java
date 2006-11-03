/* Woodstox XML processor
 *
 * Copyright (c) 2004 Tatu Saloranta, tatu.saloranta@iki.fi
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

package com.ctc.wstx.compat;

import java.util.*;

/**
 * This is the interface used to access JDK-dependant functionality; generally
 * things that later JDKs have in their APIs, but that can be simulated
 * with earlier JDKs to some degree.
 */
public abstract class JdkImpl
{
    protected JdkImpl() { }

    /*
    /////////////////////////////////////////
    // Public API
    /////////////////////////////////////////
     */

    // // // Properties

    /**
     * This method is used to check whether GC-friendly caching can be
     * implemented using {@link java.lang.ThreadLocal}. Some JDKs (notably,
     * JDK 1.3.x) have possibility for memory leaks, and when running on
     * them, such caching should not be used.
     *
     * @return True if using ThreadLocal is safe, and should not (in itself)
     *   be able to cause memory leaks; false if it is possible
     */
    public abstract boolean leakingThreadLocal();

    // // // Methods for accessing dummy data structures

    public abstract List getEmptyList();
    public abstract Map getEmptyMap();
    public abstract Set getEmptySet();

    // // // Methods for accessing 'advanced' data structs:

    public abstract HashMap getInsertOrderedMap();
    public abstract HashMap getInsertOrderedMap(int initialSize);

    public abstract HashMap getLRULimitMap(int maxSize);

    // // // Methods for injecting root cause to exceptions

    /**
     * Method that sets init cause of the specified Throwable to be
     * another specified Throwable. Note: not all JDKs support such
     * functionality.
     * 
     * @return True if call succeeds, false if not.
     */
    public abstract boolean setInitCause(Throwable newT, Throwable rootT);
}

