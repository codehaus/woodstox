package com.ctc.wstx.util;

import java.io.*;

/**
 * Simple decorator Writer, that can optionally suppress empty lines (lines
 * with only white space content); configuration is done by specifying
 * what is the maximum number of consequtive empty lines allowed. Setting
 * this value to 0 will filter out all lines; setting it to, say,
 * {@link java.lang.Integer#MAX_VALUE} essentially does nothing.
 *<p>
 * Note: only ascii white space (space char and below) are considered
 * white space.
 */
public class LineSuppressWriter
    extends Writer
{
    final static char CHAR_LAST_WS = 0x0020;

    /*
    ///////////////////////////////////////////
    // Configuration
    ///////////////////////////////////////////
     */

    protected final Writer mWriter;

    protected int mMaxEmptyLines = 0;

    protected boolean mTrimLeading = false;

    protected boolean mTrimTrailing = false;

    /*
    ///////////////////////////////////////////
    // Output state:
    ///////////////////////////////////////////
     */

    protected int mCurrEmptyLines = 0;

    protected char[] mCurrLine = new char[160];

    protected int mCurrPtr = 0;

    protected boolean mCurrIsEmpty = true;

    protected char[] mSingleCharBuf = null;

    /*
    ///////////////////////////////////////////
    // Life-cycle, configuring
    ///////////////////////////////////////////
     */

    public LineSuppressWriter(Writer sink)
    {
        mWriter = sink;
        mCurrIsEmpty = true;
    }

    public void setMaxConsequtiveEmptyLines(int max) {
        mMaxEmptyLines = max;
    }

    public void setTrim(boolean lead, boolean trail) {
        mTrimLeading = lead;
        mTrimTrailing = trail;
    }

    public void setTrimLeading(boolean state) {
        mTrimLeading = state;
    }

    public void setTrimTrailing(boolean state) {
        mTrimTrailing = state;
    }

    /*
    ///////////////////////////////////////////
    // Writer API, trivial methods:
    ///////////////////////////////////////////
     */

    public void close()
        throws IOException
    {
        // Need to output line, if we have any
        if (mCurrPtr > 0) {
            flushLine();
        }

        mWriter.close();
    }

    public void flush()
        throws IOException
    {
        mWriter.flush();
    }

    public void write(char[] cbuf) throws IOException {
        write(cbuf, 0, cbuf.length);
    }

    public void write(int c)
        throws IOException
    {
        if (mSingleCharBuf == null) {
            mSingleCharBuf = new char[1];
        }
        mSingleCharBuf[0] = (char) c;
        write(mSingleCharBuf, 0, 1);
    }

    public void write(String str)
        throws IOException
    {
        write(str, 0, str.length());
    }

    /*
    ///////////////////////////////////////////
    // Writer API, main methods:
    ///////////////////////////////////////////
     */

    public void write(String str, int off, int len)
        throws IOException
    {
        int i = off;

        // Split line break from last output?
        if (mCurrPtr > 0 && mCurrLine[mCurrPtr-1] == '\r') {
            if (str.charAt(i) == '\n') {
                appendChar('\n');
                ++i;
            }
            flushLine();
        }
        int last = off+len;

        /* Ok, and then we'll find complete lines to process and
         * output:
         */
        main_loop:
        while (i < last) {
            // empty line to check?
            if (mCurrIsEmpty) {
                ws_loop:
                while (i < last) {
                    char c = str.charAt(i++);
                    if (mCurrPtr >= mCurrLine.length) {
                        resize();
                    }
                    mCurrLine[mCurrPtr++] = c;
                    if (c > CHAR_LAST_WS) {
                        mCurrIsEmpty = false;
                        break ws_loop;
                    }
                    if (c == '\n') {
                        ;
                    } else if (c == '\r') {
                        if (i >= last) { // split line-break, can't yet output
                            return;
                        }
                        if (str.charAt(i) == '\n') {
                            ++i;
                            appendChar('\n');
                        }
                    } else {
                        continue;
                    }
                    flushLine();
                    continue main_loop;
                }
            }

            // Nah, non-empty
            nonws_loop:
            while (i < last) {
                char c = str.charAt(i++);
                if (mCurrPtr >= mCurrLine.length) {
                    resize();
                }
                mCurrLine[mCurrPtr++] = c;
                if (c == '\n') {
                    ;
                } else if (c == '\r') {
                    if (i >= last) { // split line-break, can't yet output
                        return;
                    }
                    if (str.charAt(i) == '\n') {
                        ++i;
                        appendChar('\n');
                    }
                } else {
                    continue;
                }
                flushLine();
                continue main_loop;
            }
        }
    }

    public void write(char[] cbuf, int off, int len)
        throws IOException
    {
        int i = off;

        // Split line break from last output?
        if (mCurrPtr > 0 && mCurrLine[mCurrPtr-1] == '\r') {
            if (cbuf[i] == '\n') {
                appendChar('\n');
                ++i;
            }
            flushLine();
        }
        int last = off+len;

        /* Ok, and then we'll find complete lines to process and
         * output:
         */
        main_loop:
        while (i < last) {
            // empty line to check?
            if (mCurrIsEmpty) {
                ws_loop:
                while (i < last) {
                    char c = cbuf[i++];
                    if (mCurrPtr >= mCurrLine.length) {
                        resize();
                    }
                    mCurrLine[mCurrPtr++] = c;
                    if (c > CHAR_LAST_WS) {
                        mCurrIsEmpty = false;
                        break ws_loop;
                    }
                    if (c == '\n') {
                        ;
                    } else if (c == '\r') {
                        if (i >= last) { // split line-break, can't yet output
                            return;
                        }
                        if (cbuf[i] == '\n') {
                            ++i;
                            appendChar('\n');
                        }
                    } else {
                        continue;
                    }
                    flushLine();
                    continue main_loop;
                }
            }

            // Nah, non-empty
            nonws_loop:
            while (i < last) {
                char c = cbuf[i++];
                if (mCurrPtr >= mCurrLine.length) {
                    resize();
                }
                mCurrLine[mCurrPtr++] = c;
                if (c == '\n') {
                    ;
                } else if (c == '\r') {
                    if (i >= last) { // split line-break, can't yet output
                        return;
                    }
                    if (cbuf[i] == '\n') {
                        ++i;
                        appendChar('\n');
                    }
                } else {
                    continue;
                }
                flushLine();
                continue main_loop;
            }
        }
    }

    /*
    ///////////////////////////////////////////
    // Private methods:
    ///////////////////////////////////////////
     */

    private void resize() {
        mCurrLine = resize(mCurrLine);
    }

    private char[] resize(char[] old)
    {
        int oldLen = old.length;
        char[] newBuf = new char[oldLen + oldLen];
        System.arraycopy(old, 0, newBuf, 0, oldLen);
        return newBuf;
    }

    /**
     *<p>
     * Note: this method was renamed from 'append()' due to collision
     * with JDK 1.5 added method of same name (as reported by
     * Olivier Potonniee)
     */
    private void appendChar(char c) {
        if (mCurrPtr >= mCurrLine.length) {
            resize();
        }
        mCurrLine[mCurrPtr] = c;
        ++mCurrPtr;
    }

    private void flushLine()
        throws IOException
    {
        do { // dummy block to allow break
            // Empty or non-empty?
            if (mCurrIsEmpty) {
                if (++mCurrEmptyLines > mMaxEmptyLines) {
                    break;
                }
            } else { // non-empty:
                mCurrEmptyLines = 0;
            }


            // Ok; let's output it then. But do we want to trim it?
            if (!mTrimLeading && !mTrimTrailing) { // nope
                mWriter.write(mCurrLine, 0, mCurrPtr);
                break;
            }

            int lfStart = mCurrPtr;
            
            // First, need to by-pass linefeed, if have one:
            if (lfStart > 0) {
                char c = mCurrLine[lfStart-1];
                if (c == '\r') {
                    --lfStart;
                } else if (c == '\n') {
                    if (--lfStart > 0) {
                        if (mCurrLine[lfStart-1] == '\r') {
                            --lfStart;
                        }
                    }
                } 
            }
            
            // From there on, we can do trailing trimming:
            int end = lfStart;
            if (mTrimTrailing) {
                while (end > 0 && mCurrLine[end-1] <= CHAR_LAST_WS) {
                    --end;
                }
            }
            int start = 0;
            if (mTrimLeading) {
                while (start < end && mCurrLine[start] <= CHAR_LAST_WS) {
                    ++start;
                }
            }

            if (end == lfStart) { // no trailing trimming done
                mWriter.write(mCurrLine, start, mCurrPtr - start);
            } else {
                // May need to do 2 outputs; first main line, then LF
                mWriter.write(mCurrLine, start, end - start);
                mWriter.write(mCurrLine, lfStart, mCurrPtr - lfStart);
            }
 
        } while (false);

        // Ok, let's reinitialize the line info then:
        mCurrPtr = 0;
        mCurrIsEmpty = true;
    }
}
