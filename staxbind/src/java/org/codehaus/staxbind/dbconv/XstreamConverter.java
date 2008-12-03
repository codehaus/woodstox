package org.codehaus.staxbind.dbconv;

import java.io.*;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

/**
 * Converter that uses XStream on top of regular Stax 1
 * implementation (such as Woodstox).
 */
public class XstreamConverter
    extends DbConverter
{
    /**
     * Root-level 'factory' object should be thread-safe, since it
     * carries no state.
     */
    final XStream _xstream;

    public XstreamConverter()
    {
        _xstream = new XStream(new StaxDriver());
        // No need to resolve refs, won't have cycles
        _xstream.setMode(XStream.NO_REFERENCES);
        // Also, XStream needs to know main-level binding:
        _xstream.alias("table", DbData.class);
        // ... and it looks like row class too... not sure why
        _xstream.alias("row", DbRow.class);
    }

    public DbData readData(InputStream in) throws Exception
    {
        return (DbData) _xstream.fromXML(in);
    }
    
    public int writeData(OutputStream out, DbData data) throws Exception
    {
        _xstream.toXML(data, out);
        return data.size();
    }
}
