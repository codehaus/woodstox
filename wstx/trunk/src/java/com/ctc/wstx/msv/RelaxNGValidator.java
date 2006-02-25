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

import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.*;

import org.codehaus.stax2.*;
import org.codehaus.stax2.validation.*;

import com.sun.msv.util.StartTagInfo;
import com.sun.msv.util.StringRef;
import com.sun.msv.verifier.Acceptor;
import com.sun.msv.verifier.regexp.REDocumentDeclaration;

import com.ctc.wstx.exc.WstxIOException;
/**
 * Actual non-shareable validator instance, that is bound to an XML
 * document, or document subset.
 */
public class RelaxNGValidator
    extends XMLValidator
{
    /*
    ////////////////////////////////////
    // Configuration
    ////////////////////////////////////
    */

    final XMLValidationSchema mParentSchema;

    final ValidationContext mContext;

    final REDocumentDeclaration mVGM;

    /*
    ////////////////////////////////////
    // State
    ////////////////////////////////////
    */

    final ArrayList mAcceptors = new ArrayList();

    Acceptor mCurrAcceptor = null;

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
    final StartTagInfo mStartTag = new StartTagInfo("", "", "", null, null);

    /*
    ////////////////////////////////////
    // Construction, configuration
    ////////////////////////////////////
    */

    public RelaxNGValidator(XMLValidationSchema parent, ValidationContext ctxt,
                            REDocumentDeclaration vgm)
    {
        mParentSchema = parent;
        mContext = ctxt;
        mVGM = vgm;

        mCurrAcceptor = mVGM.createAcceptor();
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
        // Do we need to properly fill it? Or could we just put local name?
        String qname = (prefix == null || prefix.length() == 0) ?
            localName : (prefix + ":" +localName);
        mStartTag.reinit(uri, localName, qname, null, null);
        mCurrAcceptor = mCurrAcceptor.createChildAcceptor(mStartTag, mErrorRef);
        mAcceptors.add(mCurrAcceptor);
        if (mCurrAcceptor == null || mErrorRef.str != null) {
            String msg = mErrorRef.str;
            mErrorRef.str = null;
            if (msg == null) {
                msg = "Unknown reason";
            }
            mContext.reportProblem(new XMLValidationProblem
                                   (mContext.getValidationLocation(), msg, XMLValidationProblem.SEVERITY_ERROR));
        }
    }

    public String validateAttribute(String localName, String uri,
                                    String prefix, String value)
        throws XMLValidationException
    {
        // !!! TBI
        return null;
    }

    public String validateAttribute(String localName, String uri,
                                    String prefix,
                                    char[] valueChars, int valueStart,
                                    int valueEnd)
        throws XMLValidationException
    {
        // !!! TBI
        return null;
    }
    
    public int validateElementAndAttributes()
        throws XMLValidationException
    {
        // !!! TBI
        return CONTENT_ALLOW_ANY_TEXT;
    }

    /**
     * @return Validation state that should be effective for the parent
     *   element state
     */
    public int validateElementEnd(String localName, String uri, String prefix)
        throws XMLValidationException
    {
        Acceptor acc = (Acceptor)mAcceptors.remove(mAcceptors.size()-1);
        if (acc != null) { // may be null during error recovery
            if (!acc.isAcceptState(mErrorRef) || mErrorRef.str != null) {
                String msg = mErrorRef.str;
                mErrorRef.str = null;
                if (msg == null) {
                    String qname = (prefix == null || prefix.length() == 0) ?
                        localName : (prefix + ":" +localName);
                    msg = "Not in accepting state when </"+qname+"> encountered";
                }
                mContext.reportProblem(new XMLValidationProblem
                                       (mContext.getValidationLocation(), msg, XMLValidationProblem.SEVERITY_ERROR));
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
                String msg = mErrorRef.str;
                mErrorRef.str = null;
                if (msg == null) {
                    String qname = (prefix == null || prefix.length() == 0) ?
                        localName : (prefix + ":" +localName);
                    msg = "Parent.stepForward failed on </"+qname+">";
                }
                mContext.reportProblem(new XMLValidationProblem
                                       (mContext.getValidationLocation(), msg, XMLValidationProblem.SEVERITY_ERROR));
            }
        }
        return XMLValidator.CONTENT_ALLOW_ANY_TEXT;
    }

    public void validateText(String text, boolean lastTextSegment)
        throws XMLValidationException
    {
        // !!! TBI
    }

    public void validateText(char[] cbuf, int textStart, int textEnd,
                             boolean lastTextSegment)
        throws XMLValidationException
    {
        // !!! TBI
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

}
