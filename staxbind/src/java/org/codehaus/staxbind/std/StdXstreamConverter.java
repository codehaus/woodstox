package org.codehaus.staxbind.std;

import java.io.*;

import com.sun.japex.Params;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.CGLIBEnhancedConverter;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import com.thoughtworks.xstream.mapper.CGLIBMapper;
import com.thoughtworks.xstream.mapper.MapperWrapper;

/**
 * Converter that uses XStream on top of regular Stax 1
 * implementation (such as Woodstox).
 */
public class StdXstreamConverter<T extends StdItem<T>>
    extends StdConverter<T>
{
    /**
     * Root-level 'factory' object should be thread-safe, since it
     * carries no state.
     */
    protected XStream _xstream;

    public StdXstreamConverter() {
        // nothing to set as we need access to params...
    }

    public void prepare(Params driverParams)
    {
        /* Hmmh. Looks like prepare() does get called multiple
         * times on a single driver; once per each test case.
         */

        /* 09-Dec-2008, tatus: Let's allow enabling/disabling
         *   of CGLIB proxies; supposedly adds measurable
         *   overhead
         */
        String prop = driverParams.getParam("xstream.enableCglib");
        if (prop != null && "true".equalsIgnoreCase(prop)) {
            System.out.print("[Xstream, WITH Cglib support]");
            _xstream = new CglibXStream();
        } else {
            System.out.print("[Xstream, with NO Cglib support]");
            _xstream = new XStream(new StaxDriver());
        }

        // No need to resolve refs, won't have cycles
        _xstream.setMode(XStream.NO_REFERENCES);
    }

    @SuppressWarnings("unchecked")
    public T readData(InputStream in) throws Exception
    {
        return (T) _xstream.fromXML(in);
    }
    
    public int writeData(OutputStream out, T data) throws Exception
    {
        _xstream.toXML(data, out);
        return -1;
    }

    /*
    ///////////////////////////////////////////////
    // Helper class for testing CGLIB support
    ///////////////////////////////////////////////
     */

    final static class CglibXStream
        extends XStream
    {
        public CglibXStream()
        {
            super(new StaxDriver());
            registerConverter(new CGLIBEnhancedConverter(getMapper(), getReflectionProvider()));
        }

        protected MapperWrapper wrapMapper(MapperWrapper next) {
            return new CGLIBMapper(next);
        }
    }
}

