package com.ctc.wstx.evt;

import java.io.IOException;
import java.io.Writer;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Comment;

public class WComment
    extends WEvent
    implements Comment
{
    final String mContent;

    public WComment(Location loc, String content)
    {
        super(loc);
        mContent = content;
    }

    public String getText() {
        return mContent;
    }

    /*
    ///////////////////////////////////////////
    // Implementation of abstract base methods
    ///////////////////////////////////////////
     */

    public int getEventType() {
        return COMMENT;
    }

    public void writeAsEncodedUnicode(Writer w)
        throws XMLStreamException
    {
        try {
            w.write("<!--");
            w.write(mContent);
            w.write("-->");
        } catch (IOException ie) {
            throwFromIOE(ie);
        }
    }
}
