package org.codehaus.staxbind.std;

import java.io.*;
import java.util.*;

import com.caucho.hessian.io.*;

/**
 * Converter that uses Hessian protocol (and Caucho impl) for binding.
 */
public class StdHessianConverter<T extends StdItem>
    extends StdConverter<T>
{
    public StdHessianConverter() { }

    public T readData(InputStream in) throws IOException
    {
        Hessian2StreamingInput hin = new Hessian2StreamingInput(in);
        return (T) hin.readObject();
    }
    
    public int writeData(OutputStream out, T data) throws Exception
    {
        Hessian2StreamingOutput hout = new Hessian2StreamingOutput(out);
        hout.writeObject(data);
        return -1;
    }
}
