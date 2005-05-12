package com.ctc.wstx.evt;

import java.io.IOException;
import java.io.Writer;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.ProcessingInstruction;

public class WProcInstr
    extends WEvent
    implements ProcessingInstruction
{
    final String mTarget;
    final String mData;

    public WProcInstr(Location loc, String target, String data)
    {
        super(loc);
        mTarget = target;
        mData = data;
    }

    public String getData() {
        return mData;
    }

    public String getTarget() {
        return mTarget;
    }

    /*
    ///////////////////////////////////////////
    // Implementation of abstract base methods
    ///////////////////////////////////////////
     */

    public int getEventType() {
        return PROCESSING_INSTRUCTION;
    }

    public boolean isProcessingInstruction() {
        return true;
    }

    public void writeAsEncodedUnicode(Writer w)
        throws XMLStreamException
    {
        try {
            w.write("<?");
            w.write(mTarget);
            if (mData != null && mData.length() > 0) {
                w.write(mData);
            }
            w.write("?>");
        } catch (IOException ie) {
            throwFromIOE(ie);
        }
    }

    public void writeUsing(XMLStreamWriter w) throws XMLStreamException
    {
        if (mData != null && mData.length() > 0) {
            w.writeProcessingInstruction(mTarget, mData);
        } else {
            w.writeProcessingInstruction(mTarget);
        }
    }
}
