package org.codehaus.staxbind.dbconv;

import com.sun.japex.Params;

import org.codehaus.staxbind.std.StdXstreamConverter;

/**
 * Converter that uses XStream on top of regular Stax 1
 * implementation (such as Woodstox).
 */
public class XstreamConverter
    extends StdXstreamConverter<DbData>
{
    public XstreamConverter() { }

    @Override
    public void prepare(Params driverParams)
    {
        super.prepare(driverParams);

        /* Let's slightly optimize output by giving aliases; can not
         * be done generically in base class:
         */

        // Also, XStream needs to know main-level binding:
        _xstream.alias("table", DbData.class);
        // ... and it looks like row class too... not sure why
        _xstream.alias("row", DbRow.class);
    }
}

