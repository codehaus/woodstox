package org.codehaus.staxbind.std;

import java.io.*;

import org.codehaus.jackson.map.ObjectMapper;

/**
 * Converter that uses Jackson JSON processor (v1.x) for data binding,
 * using automatic bindings for serialization and deserialization.
 */
public class StdJacksonConverter<T extends StdItem<T>>
    extends StdConverter<T>
{
    protected final ObjectMapper _mapper;

    protected final Class<T> _itemClass;

    public StdJacksonConverter(Class<T> itemClass)
    {
        _mapper = new ObjectMapper();
        _itemClass = itemClass;
    }

    public T readData(InputStream in) throws IOException
    {
        return _mapper.readValue(in, _itemClass);
    }
    
    public int writeData(OutputStream out, T data) throws Exception
    {
        _mapper.writeValue(out, data);
        return -1;
    }
}
