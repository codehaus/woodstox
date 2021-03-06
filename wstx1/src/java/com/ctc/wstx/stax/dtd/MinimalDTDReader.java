/* Woodstox XML processor.
 *<p>
 * Copyright (c) 2004 Tatu Saloranta, tatu.saloranta@iki.fi
 *<p>
 * You can redistribute this work and/or modify it under the terms of
 * LGPL (Lesser Gnu Public License), as published by
 * Free Software Foundation (http://www.fsf.org). No warranty is
 * implied. See LICENSE for details about licensing.
 */

package com.ctc.wstx.stax.dtd;

import java.io.IOException;

import com.ctc.wstx.stax.cfg.ReaderConfig;
import com.ctc.wstx.stax.exc.WstxException;
import com.ctc.wstx.stax.io.WstxInputSource;
import com.ctc.wstx.stax.stream.StreamScanner;

/**
 * Minimal DTD reader implementation that only knows how to skip
 * internal DTD subsets.
 */
public class MinimalDTDReader
    extends StreamScanner
{
    /*
    //////////////////////////////////////////////////
    // Configuration
    //////////////////////////////////////////////////
     */

    /**
     * True, when reading external subset, false when reading internal
     * subset.
     */
    final boolean mIsExternal;

    /*
    //////////////////////////////////////////////////
    // Life-cycle
    //////////////////////////////////////////////////
     */

    /**
     * Constructor used for reading/skipping internal subset.
     */
    private MinimalDTDReader(StreamScanner master, WstxInputSource input,
                             ReaderConfig cfg)
    {
        this(input, cfg, master, false);
    }

    /**
     * Common initialization part of int/ext subset constructors.
     */
    protected MinimalDTDReader(WstxInputSource input, ReaderConfig cfg,
                               StreamScanner master, boolean isExt)
    {
        super(input, cfg, cfg.getDtdResolver());
        mIsExternal = isExt;
        // Let's reuse the name buffer:
        mNameBuffer = (master == null) ? null : master.mNameBuffer;
    }

    /**
     * Method that just skims
     * through structure of internal subset, but without doing any sort
     * of validation, or parsing of contents. Method may still throw an
     * exception, if skipping causes EOF or there's an I/O problem.
     */
    public static void skipInternalSubset(StreamScanner master, WstxInputSource input,
                                          ReaderConfig cfg)
        throws IOException, WstxException
    {
        MinimalDTDReader r = new MinimalDTDReader(master, input, cfg);
        // Parser should reuse master's input buffers:
        r.copyBufferStateFrom(master);
        try {
            r.skipInternalSubset();
        } finally {
            /* And then need to restore changes back to master (line nrs etc);
             * effectively means that we'll stop reading external DTD subset,
             * if so.
             */
            master.copyBufferStateFrom(r);
        }
    }

    /*
    //////////////////////////////////////////////////
    // Main-level skipping method(s)
    //////////////////////////////////////////////////
     */

    /**
     * Method that will skip through internal DTD subset, without doing
     * any parsing, except for trying to match end of subset properly.
     */
    protected void skipInternalSubset()
        throws IOException, WstxException
    {
        while (true) {
            int i = getNextAfterWS();
            if (i < 0) {
                // Error for internal subset
                throwUnexpectedEOF(SUFFIX_IN_DTD_INTERNAL);
            }
            if (i == '%') { // parameter entity
                skipPE();
                continue;
            }
            if (i == '<') {
                /* Let's determine type here, and call appropriate skip
                 * methods.
                 */
                char c = getNextSkippingPEs();
                if (c == '?') { // xml decl?
                    /* Not sure if PIs are really allowed in DTDs, but let's
                     * just allow them until proven otherwise. XML declaration
                     * is legal in the beginning, anyhow
                     */
                    skipPI();
                } else if (c == '!') { // ignore/include, comment, declaration?
                    c = getNextSkippingPEs();
                    if (c == '[') {
                        /* Shouldn't really get these, as they are not allowed
                         * in the internal subset? So let's just leave it
                         * as is, and see what happens. :-)
                         */
                        ;
                    } else if (c == '-') { // plain comment
                        skipComment();
                    } else if (c >= 'A' && c <= 'Z') {
                        skipDeclaration(c);
                    } else {
                        /* Hmmh, let's not care too much; but we need to try
                         * to match the closing gt-char nonetheless?
                         */
                        skipDeclaration(c);
                    }
                } else {
                    /* Shouldn't fail (since we are to completely ignore
                     * subset); let's just push it back and continue.
                     */
                    --mInputPtr;
                }
                continue;
            }

            if (i == ']') {
                // Int. subset has no conditional sections, has to be the end...
                /* 18-Jul-2004, TSa: Let's just make sure it happened
                 *   in the main input source, not at external entity...
                 */
                if (mInput != mRootInput) {
                        throwParseError("Encountered int. subset end marker ']]>' in an expanded entity; has to be at main level.");
                }
                // End of internal subset
                break;
            }
            throwUnexpectedChar(i, SUFFIX_IN_DTD_INTERNAL+"; expected a '<' to start a directive, or \"]>\" to end internal subset.");
        }
    }

    /*
    //////////////////////////////////////////////////
    // Internal methods, input access:
    //////////////////////////////////////////////////
     */

    protected char getNextSkippingPEs()
        throws IOException, WstxException
    {
        while (true) {
            char c = (mInputPtr < mInputLen) ?
                mInputBuffer[mInputPtr++] : getNextChar(getErrorMsg());
            if (c != '%') {
                return c;
            }
            skipPE();
        }
    }

    /*
    //////////////////////////////////////////////////
    // Internal methods, skipping:
    //////////////////////////////////////////////////
     */

    private void skipPE()
        throws IOException, WstxException
    {
        skipDTDName();
        /* Should now get semicolon... let's try to find and skip it; but
         * if none found, let's not throw an exception -- we are just skipping
         * internal subset here.
         */
        char c = (mInputPtr < mInputLen) ?
            mInputBuffer[mInputPtr++] : getNextChar(getErrorMsg());
        if (c != ';') {
            --mInputPtr;
        }
    }

    protected void skipComment()
        throws IOException, WstxException
    {
        skipCommentContent();
        // Now, we may be getting end mark; first need second marker char:.
        char c = (mInputPtr < mInputLen)
            ? mInputBuffer[mInputPtr++] : getNextChar(getErrorMsg());
        if (c != '>') {
            throwParseError("String '--' not allowed in comment (missing '>'?)");
        }
    }

    protected void skipCommentContent()
        throws IOException, WstxException
    {
        while (true) {
            char c = (mInputPtr < mInputLen) ?
                mInputBuffer[mInputPtr++] : getNextChar(getErrorMsg());
            if (c == '-') {
                c = (mInputPtr < mInputLen) ?
                    mInputBuffer[mInputPtr++] : getNextChar(getErrorMsg());
                if (c == '-') {
                    return;
                }
            } else if (c == '\n' || c == '\r') {
                skipCRLF(c);
            }
        }
    }

    protected void skipPI()
        throws IOException, WstxException
    {
        while (true) {
            char c = (mInputPtr < mInputLen)
                ? mInputBuffer[mInputPtr++] : getNextChar(getErrorMsg());
            if (c == '?') {
                do {
                    c = (mInputPtr < mInputLen)
                        ? mInputBuffer[mInputPtr++] : getNextChar(getErrorMsg());
                } while (c == '?');
                if (c == '>') {
                    break;
                }
            }
            if (c == '\n' || c == '\r') {
                skipCRLF(c);
            }
        }
    }

    private void skipDeclaration(char c)
        throws IOException, WstxException
    {
        while (c != '>') {
            c = (mInputPtr < mInputLen)
                ? mInputBuffer[mInputPtr++] : getNextChar(getErrorMsg());
            if (c == '\n' || c == '\r') {
                skipCRLF(c);
            /* No need for specific handling for PE refs; they just have
             * identifier that'll get properly skipped.
             */
            /* 17-Jul-2004, TSa: But we do need to properly handle literals;
             *   it is possible to add '>' char in entity expansion values.
             */
            } else if (c == '\'' || c == '"') {
                skipLiteral(c);
            }
        }
    }

    private void skipLiteral(char quoteChar)
        throws IOException, WstxException
    {
        while (true) {
            char c = (mInputPtr < mInputLen)
                ? mInputBuffer[mInputPtr++] : getNextChar(getErrorMsg());
            if (c == '\n' || c == '\r') {
                skipCRLF(c);
            } else if (c == quoteChar) {
                break;
            }
            /* No need for specific handling for PE refs, should be ignored
             * just ok (plus they need to properly nested in any case)
             */
        }
    }

    private void skipDTDName()
        throws IOException, WstxException
    {
        int len = skipFullName(getNextChar(getErrorMsg()));
        /* Should we give an error about missing name? For now,
         * let's just exit.
         */
    }

    /*
    //////////////////////////////////////////////////
    // Internal methods, error handling:
    //////////////////////////////////////////////////
     */

    protected String getErrorMsg() {
        return mIsExternal ? SUFFIX_IN_DTD_EXTERNAL : SUFFIX_IN_DTD_INTERNAL;
    }

}

