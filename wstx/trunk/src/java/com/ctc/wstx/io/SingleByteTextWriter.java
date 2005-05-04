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
public class SingleByteTextWriter
    extends WriterBase
{
    private boolean mJustWroteBracket = false;

    public SingleByteTextWriter(Writer out, String enc) {
        super(out);
    }


    public void write(int c) throws IOException
    {
	if (c <= HIGHEST_ENCODABLE_TEXT_CHAR) {
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
            } else {
		out.write(c);
	    } 
	    mJustWroteBracket = false;
	} else {
            out.write(c);
	    mJustWroteBracket = (c == ']');
        }
    }

    public void write(char cbuf[], int offset, int len) throws IOException
    {
        // Let's simplify code a bit and offload the trivial case...
        if (len < 2) {
            if (len == 1) {
                write(cbuf[offset]);
            }
            return;
        }

	char c = CHAR_NULL;
        len += offset; // to get the index past last char to output
        // Need special handing for leftover ']' to cause quoting of '>'
	if (mJustWroteBracket) {
	    c = cbuf[offset];
	    if (c == '>') {
                out.write("&gt;");
                ++offset;
            }
	}

        do {
            int start = offset;
	    String ent = null;

            for (; offset < len; ++offset) {
                c = cbuf[offset]; 
		if (c > HIGHEST_ENCODABLE_TEXT_CHAR) {
		    continue;
		}
                if (c == '<') {
		    ent = "&lt;";
		} else if (c == '&') {
		    ent = "&amp;";
                } else if (c == '>' && (offset > start)
			   && cbuf[offset-1] == ']') {
		    ent = "&gt;";
                } else {
		    continue;
		}
		break;
	    }
            int outLen = offset - start;

            if (outLen > 0) {
                out.write(cbuf, start, outLen);
            }
            if (ent != null) {
		out.write(ent);
		ent = null;
            }
        } while (++offset < len);

        // Ok, did we end up with a bracket?
        mJustWroteBracket = (c == ']');
    }

    public void write(String str, int offset, int len) throws IOException
    {
        if (len < 2) { // let's do a simple check here
            if (len == 1) {
                write(str.charAt(offset));
            }
            return;
        }

	char c = CHAR_NULL;
        len += offset; // to get the index past last char to output
        // Ok, leftover ']' to cause quoting of '>'?
        if (mJustWroteBracket) {
            c = str.charAt(offset);
            if (c == '>') {
                out.write("&gt;");
                ++offset;
            }
        }

        do {
            int start = offset;
	    String ent = null;

            for (; offset < len; ++offset) {
                c = str.charAt(offset); 
		if (c > HIGHEST_ENCODABLE_TEXT_CHAR) {
		    continue;
		}
               if (c == '<') {
		    ent = "&lt;";
		} else if (c == '&') {
		    ent = "&amp;";
                } else if (c == '>' && (offset > start)
			   && str.charAt(offset-1) == ']') {
		    ent = "&gt;";
                } else {
		    continue;
		}
		break;
            }
            int outLen = offset - start;
            if (outLen > 0) {
                out.write(str, start, outLen);
            } 
	    if (ent != null) {
		out.write(ent);
		ent = null;
	    }
        } while (++offset < len);

        // Ok, did we end up with a bracket?
        mJustWroteBracket = (c == ']');
    }
}
