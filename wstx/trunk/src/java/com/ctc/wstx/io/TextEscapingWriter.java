package com.ctc.wstx.io;

import java.io.*;

/**
 * Basic escaping writer used when outputting normal textual content.
 * Only needs to escape '&lt;' and '&amp;' characters, plus '&gt' when
 * following "]]" string. Note that to detect the last case, the logic
 * is bit simplified, so that any '&gt;' that immediately follows a ']'
 * gets escaped. Further, since the Writer does not know of text segment
 * boundaries, it is possible that there is no immediate sequence in the
 * output. This is not a correctness problem,
 * however (since such escaping is perfectly legal, although not
 * strictly necessary), just a slightly "unoptimal" behaviour.
 *<p>
 */
public class TextEscapingWriter
    extends FilterWriter
{
    private boolean mJustWroteBracket = false;

    public TextEscapingWriter(Writer out) {
        super(out);
    }

    public void write(int c) throws IOException
    {
        if (c == '<') {
            out.write("&lt;");
        } else if (c == '&') {
            out.write("&amp;");
        } else if (c == '>') {
            if (mJustWroteBracket) {
                out.write("&gt;");
            } else {
                out.write(c);
            }
        } else if (c == ']') {
            out.write(c);
            mJustWroteBracket = true;
            return;
        } else {
            out.write(c);
        }
        mJustWroteBracket = false;
    }

    public void write(char cbuf[], int off, int len) throws IOException
    {
        // !!! TBI
    }

    public void write(String str, int off, int len) throws IOException
    {
        // !!! TBI
    }
}
