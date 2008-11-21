/* Stax2 extension for basic Stax API (JSR-173).
 *
 * Copyright (c) 2005- Tatu Saloranta, tatu.saloranta@iki.fi
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
package org.codehaus.stax2.typed;

/**
 * This class is used as a container for commonly used Base64 variants.
 * 
 * @author Tatu Saloranta
 *
 * @since 3.0.0
 */
public final class Base64Variants
{
    final static String STD_BASE64_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    /**
     * This variant is what most people would think of "the standard"
     * Base64 encoding.
     *<p>
     * See <a href="">wikipedia Base64 entry</a> for details.
     */
    public final static Base64Variant MIME;
    static {
        MIME = new Base64Variant("MIME", STD_BASE64_ALPHABET, true, '=', 76);
    }

    public final static Base64Variant PEM = new Base64Variant(MIME, "PEM", true, '=', 64);

    public final static Base64Variant MODIFIED_FOR_URL;
    static {
        StringBuffer sb = new StringBuffer(STD_BASE64_ALPHABET);
        // Replace plus with hyphen, slash with underscore (and no padding)
        sb.setCharAt(sb.indexOf("+"), '-');
        sb.setCharAt(sb.indexOf("/"), '_');
        /* And finally, let's not split lines either, wouldn't work too
         * well with URLs
         */
        MODIFIED_FOR_URL = new Base64Variant("MODIFIED-FOR-URL", sb.toString(), false, Base64Variant.PADDING_CHAR_NONE, Integer.MAX_VALUE);
    }

    /**
     * Method used to get the default variant ("MIME") for cases
     * where caller does not explicitly specify the variant
     */
    public static Base64Variant getDefaultVariant() {
        return MIME;
    }
}
