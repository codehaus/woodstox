package org.codehaus.staxbind.dbconv;

import java.io.*;
import java.util.*;

import com.caucho.hessian.io.*;

/**
 * Converter that uses Hessian protocol (and Caucho impl) for binding.
 */
public class HessianConverter
    extends DbConverter
{
    public HessianConverter()
    {
    }

    public DbData readData(InputStream in)
        throws IOException
    {
        Hessian2StreamingInput hin = new Hessian2StreamingInput(in);
        return (DbData) hin.readObject();
    }
    
    public int writeData(OutputStream out, DbData data) throws Exception
    {
        Hessian2StreamingOutput hout = new Hessian2StreamingOutput(out);
        hout.writeObject(data);
        return -1;
    }
}
