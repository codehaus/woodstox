package org.codehaus.staxbind.std;

import java.io.*;

/**
 * Templatized version of the JDK Serialization based converter.
 */
public class StdJdkConverter<T extends StdItem>
    extends StdConverter<T>
{
    public StdJdkConverter() { }

    @Override
    public T readData(InputStream in)
        throws IOException
    {
        ObjectInputStream oi = new ObjectInputStream(in);
        try {
            Object result = oi.readObject();
            oi.close();
            return (T) result;
        } catch (ClassNotFoundException cnfe) {
            throw new IOException(cnfe);
        }
    }
    
    @Override
    public int writeData(OutputStream out, T item) throws Exception
    {
        ObjectOutputStream oo = new ObjectOutputStream(out);
        oo.writeObject(item);
        oo.close();
        return -1;
    }
}
