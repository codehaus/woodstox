package org.codehaus.staxbind.std;

import java.io.*;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.*;

/**
 * Converter that uses Jackson JSON processor (v2.x) for data binding,
 * using automatic bindings for serialization and deserialization.
 */
public class StdJackson2Converter<T extends StdItem<T>>
    extends StdConverter<T>
{
    protected final ObjectReader _reader;
    protected final ObjectWriter _writer;
    
    protected final Class<T> _itemClass;
    
    public StdJackson2Converter(Class<T> itemClass) {
        this(itemClass, new ObjectMapper());
    }

    protected StdJackson2Converter(Class<T> itemClass, JsonFactory f)
    {
        this(itemClass, new ObjectMapper(f));
    }
    
    protected StdJackson2Converter(Class<T> itemClass, ObjectMapper mapper)
    {
        _itemClass = itemClass;
        _reader = mapper.reader(itemClass);
        _writer = mapper.writerWithType(itemClass);
    }
    
    public T readData(InputStream in) throws IOException
    {
        return _reader.readValue(in);
    }
    
    public int writeData(OutputStream out, T data) throws Exception
    {
        _writer.writeValue(out, data);
        return -1;
    }
}
