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

package com.ctc.wstx.sw;

import javax.xml.stream.XMLStreamException;

/**
 * This base class defines interface used for efficient encoding
 * of typed values, by stream writers. The abstraction is necessary
 * to reduce amount of duplicated code while avoiding significant
 * additional overhead. The idea is that the low-level stream
 * writer backend supplies encoder with the result buffer, while
 * encoder itself knows the data and state. Together these allow
 * for efficient serialization with light coupling.
 *<p>
 * Main restrictions for use are that value serializations must
 * produce only 7-bit ascii characters, and that the value can
 * be produced incrementally using limited size buffers. This
 * is true for all current value types of the Typed Access API.
 *
 * @since 4.0
 */
public abstract class AsciiValueEncoder
{
    protected AsciiValueEncoder() { }

    /**
     * Method called by writer to check if it should flush its
     * output buffer (which has specified amount of free space)
     * before encoder can encode more data. Flushing is only
     * needed if (a) encoder has more data to output, and
     * (b) free space is not enough to contain smallest
     * segment of encoded value (individual array element
     * or encoded primitive value).
     *
     * @param freeChars Amount of free space (in characters) in
     *   the output buffer
     *
     * @return True if encoder still has data to output and
     *   specified amount of free space is insufficient for
     *   encoding any more data
     */
    public abstract boolean bufferNeedsFlush(int freeChars);

    /**
     * @param vld Validator to use for validation encoded
     *   content, iff validator exists and content validation
     *   is needed for current element (i.e. not mixed content).
     *   Thus, if non-null, its validation method(s) need to
     *   be called
     *
     * @return Value of pointer after all remaining data (which
     *   may be "none") that can be encoded (as constrained by
     *   buffer length) has been encoded. Has to exceed 'ptr'
     *   value sent in; will be equal to it if nothing was
     *   encoded (which should only occur when everything has
     *   been encoded, as long as {@link #bufferNeedsFlush}
     *   is appropriately called once before calling this
     *   method)
     */
    public abstract int encodeMore(char[] buffer, int ptr, int end);
}
