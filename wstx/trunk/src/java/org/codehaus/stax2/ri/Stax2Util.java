/* Stax2 API extension for Streaming Api for Xml processing (StAX).
 *
 * Copyright (c) 2006- Tatu Saloranta, tatu.saloranta@iki.fi
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

package org.codehaus.stax2.ri;

import javax.xml.stream.XMLStreamConstants;

public final class Stax2Util
    implements XMLStreamConstants
{
    private Stax2Util() { } // no instantiation

    /**
     * Method that converts given standard Stax event type into
     * textual representation.
     */
    public static String eventTypeDesc(int type)
    {
        switch (type) {
        case START_ELEMENT:
            return "START_ELEMENT";
        case END_ELEMENT:
            return "END_ELEMENT";
        case START_DOCUMENT:
            return "START_DOCUMENT";
        case END_DOCUMENT:
            return "END_DOCUMENT";

        case CHARACTERS:
            return "CHARACTERS";
        case CDATA:
            return "CDATA";
        case SPACE:
            return "SPACE";

        case COMMENT:
            return "COMMENT";
        case PROCESSING_INSTRUCTION:
            return "PROCESSING_INSTRUCTION";
        case DTD:
            return "DTD";
        case ENTITY_REFERENCE:
            return "ENTITY_REFERENCE";
        }
        return "["+type+"]";
    }

    /**
     * Helper class used to simplify text gathering while keeping
     * at as efficient as possible.
     */
    public final static class TextBuffer
    {
        private String mText = null;

        /* !!! JDK 1.5: when we can upgrade to Java 5, can convert
         *  to using <code>StringBuilder</code> instead.
         */
        private StringBuffer mBuilder = null;

        public TextBuffer() { }

        public void reset()
        {
            mText = null;
            mBuilder = null;
        }

        public void append(String text)
        {
            int len = text.length();
            if (len > 0) {
                // Any prior text?
                if (mText != null) {
                    mBuilder = new StringBuffer(mText.length() + len);
                    mBuilder.append(mText);
                    mText = null;
                }
                if (mBuilder != null) {
                    mBuilder.append(text);
                } else {
                    mText = text;
                }
            }
        }

        public String get()
        {
            if (mText != null) {
                return mText;
            }
            if (mBuilder != null) {
                return mBuilder.toString();
            }
            return "";
        }

        public boolean isEmpty() { return (mText == null) && (mBuilder == null); }
    }
}
