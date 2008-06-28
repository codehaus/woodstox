/* Woodstox XML processor
 *
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

package org.codehaus.stax2.ri.typed;

/**
 * This base class defines interface used for efficient decoding
 * of typed values, by stream reader. The abstraction is necessary
 * to reduce amount of duplicated code while avoiding significant
 * additional overhead. The idea is that generic decoder can be
 * passed to low-level parsing methods, which will feed decoder
 * via generic callback; but that calling higher-level methods
 * can then access results in the usual type-safe (and decoder
 * specific methods)
 */
public abstract class AsciiValueDecoder
{
    protected AsciiValueDecoder() { }

    public abstract void decode(String contents)
        throws IllegalArgumentException;

    public abstract void decode(char[] buffer, int start, int end)
        throws IllegalArgumentException;
}


