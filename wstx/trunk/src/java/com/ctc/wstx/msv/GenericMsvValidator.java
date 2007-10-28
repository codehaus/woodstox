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

package com.ctc.wstx.msv;

import java.io.*;
import java.net.URL;
import java.util.*;

import org.codehaus.stax2.*;
import org.codehaus.stax2.validation.*;

import com.sun.msv.grammar.IDContextProvider2;
import com.sun.msv.util.DatatypeRef;
import com.sun.msv.util.StartTagInfo;
import com.sun.msv.util.StringRef;
import com.sun.msv.verifier.Acceptor;
import com.sun.msv.verifier.DocumentDeclaration;

import com.ctc.wstx.exc.WstxIOException;
import com.ctc.wstx.util.TextAccumulator;

/**
 * Generic validator instance to be used for all Multi-Schema Validator
 * backed implementations. A common class can be used since functionality
 * is almost identical between variants (RNG, W3C SChema); minor
 * differences that exist can be configured by settings provided.
 */
public final class GenericMsvValidator
    extends XMLValidator
{
    /*
    ////////////////////////////////////
    // Configuration
    ////////////////////////////////////
    */

    final XMLValidationSchema mParentSchema;

    final ValidationContext mContext;

    final DocumentDeclaration mVGM;

    /*
    ////////////////////////////////////
    // State
    ////////////////////////////////////
    */

    final ArrayList mAcceptors = new ArrayList();

    Acceptor mCurrAcceptor = null;

    final TextAccumulator mTextAccumulator = new TextAccumulator();

    /*
    ////////////////////////////////////
    // Helper objects
    ////////////////////////////////////
    */

    final StringRef mErrorRef = new StringRef();

    /**
     * StartTagInfo instance need not be thread-safe, and it is not immutable
     * so let's reuse one instance during a single validation.
     */
    final StartTagInfo mStartTag = new StartTagInfo("", "", "", null, (IDContextProvider2) null);

    /**
     * Since RelaxNG never has to look into attributes during element
     * processing (start/end) -- unlike W3C schema, which may need to
     * access xsi:type and xsi:nillable -- we can just use an empty
     * shared instance.
     */
    final AttributeProxy mAttributeProxy;

    final IDContextProvider2 mMsvContext;

    /*
    ////////////////////////////////////
    // Construction, configuration
    ////////////////////////////////////
    */

    public GenericMsvValidator(XMLValidationSchema parent, ValidationContext ctxt,
                               DocumentDeclaration vgm)
    {
        mParentSchema = parent;
        mContext = ctxt;
        mVGM = vgm;

        mCurrAcceptor = mVGM.createAcceptor();
        mMsvContext = new MSVContextProvider(ctxt);
        mAttributeProxy = new AttributeProxy(ctxt);
    }

    /*
    ///////////////////////////////////////
    // XMLValidator implementation
    ///////////////////////////////////////
    */

    public XMLValidationSchema getSchema() {
        return mParentSchema;
    }

    /**
     * Method called to update information about the newly encountered (start)
     * element. At this point namespace information has been resolved, but
     * no DTD validation has been done. Validator is to do these validations,
     * including checking for attribute value (and existence) compatibility.
     */
    public void validateElementStart(String localName, String uri, String prefix)
        throws XMLValidationException
    {
        // Very first thing: do we have text collected?
        if (mTextAccumulator.hasText()) {
            doValidateText(mTextAccumulator);
        }

        /* 31-Mar-2006, TSa: MSV seems to require empty String for empty/no
         *   namespace, not null.
         */
        if (uri == null) {
            uri = "";
        }

        /* Do we need to properly fill it? Or could we just put local name?
         * Looking at code, I do believe it's only used for error reporting
         * purposes...
         */
        //String qname = (prefix == null || prefix.length() == 0) ? localName : (prefix + ":" +localName);
        String qname = localName;
        mStartTag.reinit(uri, localName, qname, mAttributeProxy, mMsvContext);

        mCurrAcceptor = mCurrAcceptor.createChildAcceptor(mStartTag, mErrorRef);
        /* As per documentation, the side-effect of getting the error message
         * is that we also get a recoverable non-null acceptor... thus, should
         * never (?) see null acceptor being returned
         */
        if (mErrorRef.str != null) {
            reportError(mErrorRef);
        }
        mAcceptors.add(mCurrAcceptor);
    }

    public String validateAttribute(String localName, String uri,
                                    String prefix, String value)
        throws XMLValidationException
    {
        if (mCurrAcceptor != null) {
            String qname = localName; // for now, let's assume we don't need prefixed version
            DatatypeRef typeRef = null; // for now, let's not care

            /* 31-Mar-2006, TSa: MSV seems to require empty String for empty/no
             *   namespace, not null.
             */
            if (uri == null) {
                uri = "";
            }

            if (!mCurrAcceptor.onAttribute2(uri, localName, qname, value, mMsvContext, mErrorRef, typeRef)
                || mErrorRef.str != null) {
                reportError(mErrorRef);
            }
        }
        /* No normalization done by RelaxNG, is there? (at least nothing
         * visible to callers that is)
         */
        return null;
    }

    public String validateAttribute(String localName, String uri,
                                    String prefix,
                                    char[] valueChars, int valueStart,
                                    int valueEnd)
        throws XMLValidationException
    {
        int len = valueEnd - valueStart;
        /* This is very sub-optimal... but MSV doesn't deal with char
         * arrays.
         */
        return validateAttribute(localName, uri, prefix,
                                 new String(valueChars, valueStart, len));
    }
    
    public int validateElementAndAttributes()
        throws XMLValidationException
    {
        if (mCurrAcceptor != null) {
            /* start tag info is still intact here (only attributes sent
             * since child acceptor was created)
             */
            if (!mCurrAcceptor.onEndAttributes(mStartTag, mErrorRef)
                || mErrorRef.str != null) {
                reportError(mErrorRef);
            }

            int stringChecks = mCurrAcceptor.getStringCareLevel();
            switch (stringChecks) {
            case Acceptor.STRING_PROHIBITED: // only WS
                return XMLValidator.CONTENT_ALLOW_WS;
            case Acceptor.STRING_IGNORE: // anything (mixed content models)
                return XMLValidator.CONTENT_ALLOW_ANY_TEXT;
            case Acceptor.STRING_STRICT: // validatable (data-oriented)
                return XMLValidator.CONTENT_ALLOW_VALIDATABLE_TEXT;
            default:
                throw new IllegalArgumentException("Internal error: unexpected string care level value return by MSV: "+stringChecks);
            }
        }

        // If no acceptor, we are recovering, no need or use to validate text
        return CONTENT_ALLOW_ANY_TEXT;
    }

    /**
     * @return Validation state that should be effective for the parent
     *   element state
     */
    public int validateElementEnd(String localName, String uri, String prefix)
        throws XMLValidationException
    {
        // Very first thing: do we have text collected?
        if (mTextAccumulator.hasText()) {
            doValidateText(mTextAccumulator);
        }

        Acceptor acc = (Acceptor)mAcceptors.remove(mAcceptors.size()-1);
        if (acc != null) { // may be null during error recovery? or not?
            if (!acc.isAcceptState(mErrorRef) || mErrorRef.str != null) {
                reportError(mErrorRef);
            }
        }
        int len = mAcceptors.size();
        if (len == 0) { // root closed
            mCurrAcceptor = null;
        } else {
            mCurrAcceptor = (Acceptor) mAcceptors.get(len-1);
        }
        if (mCurrAcceptor != null && acc != null) {
            if (!mCurrAcceptor.stepForward(acc, mErrorRef)
                || mErrorRef.str != null) {
                reportError(mErrorRef);
            }
            int stringChecks = mCurrAcceptor.getStringCareLevel();
            switch (stringChecks) {
            case Acceptor.STRING_PROHIBITED: // only WS
                return XMLValidator.CONTENT_ALLOW_WS;
            case Acceptor.STRING_IGNORE: // anything (mixed content models)
                return XMLValidator.CONTENT_ALLOW_ANY_TEXT;
            case Acceptor.STRING_STRICT: // validatable (data-oriented)
                return XMLValidator.CONTENT_ALLOW_VALIDATABLE_TEXT;
            default:
                throw new IllegalArgumentException("Internal error: unexpected string care level value return by MSV: "+stringChecks);
            }
        }
        return XMLValidator.CONTENT_ALLOW_ANY_TEXT;
    }

    public void validateText(String text, boolean lastTextSegment)
        throws XMLValidationException
    {
        /* If we got here, then it's likely we do need to call onText2().
         * (not guaranteed, though; in case of multiple parallel validators,
         * only one of them may actually be interested)
         */
        mTextAccumulator.addText(text);
        if (lastTextSegment) {
            doValidateText(mTextAccumulator);
        }
    }

    public void validateText(char[] cbuf, int textStart, int textEnd,
                             boolean lastTextSegment)
        throws XMLValidationException
    {
        /* If we got here, then it's likely we do need to call onText().
         * (not guaranteed, though; in case of multiple parallel validators,
         * only one of them may actually be interested)
         */
        mTextAccumulator.addText(cbuf, textStart, textEnd);
        if (lastTextSegment) {
            doValidateText(mTextAccumulator);
        }
    }

    public void validationCompleted(boolean eod)
        throws XMLValidationException
    {
        // Is there something we should do here...?
    }

    /*
    ///////////////////////////////////////
    // Attribute info access
    ///////////////////////////////////////
    */

    // // // Access to type info

    public String getAttributeType(int index)
    {
        // !!! TBI
        return null;
    }    

    public int getIdAttrIndex()
    {
        // !!! TBI
        return -1;
    }

    public int getNotationAttrIndex()
    {
        // !!! TBI
        return -1;
    }

    /*
    ///////////////////////////////////////
    // Internal methods
    ///////////////////////////////////////
    */

    private void doValidateText(TextAccumulator textAcc)
        throws XMLValidationException
    {
        if (mCurrAcceptor != null) {
            String str = textAcc.getAndClear();
            DatatypeRef typeRef = null;
            if (!mCurrAcceptor.onText2(str, mMsvContext, mErrorRef, typeRef)
                || mErrorRef.str != null) {
                reportError(mErrorRef);
            }
        }
    }

    private void reportError(StringRef errorRef)
        throws XMLValidationException
    {
        String msg = errorRef.str;
        errorRef.str = null;
        if (msg == null) {
            msg = "Unknown reason";
        }
        mContext.reportProblem(new XMLValidationProblem
                               (mContext.getValidationLocation(), msg, XMLValidationProblem.SEVERITY_ERROR));
    }
}
