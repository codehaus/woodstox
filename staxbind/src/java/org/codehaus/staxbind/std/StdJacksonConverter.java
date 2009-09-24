package org.codehaus.staxbind.std;

import java.io.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * Converter that uses Jackson JSON processor for data binding,
 * using automatic bindings for serialization and deserialization.
 */
public class StdJacksonConverter<T extends StdItem<T>>
    extends StdConverter<T>
{
    final JsonFactory _jsonFactory;

    final ObjectMapper _mapper;

    final Class<T> _itemClass;

    public StdJacksonConverter(Class<T> itemClass)
    {
        _jsonFactory = new JsonFactory();
        _mapper = new ObjectMapper();
        _itemClass = itemClass;
    }

    public T readData(InputStream in) throws IOException
    {
        return _mapper.readValue(in, _itemClass);
    }
    
    public int writeData(OutputStream out, T data) throws Exception
    {
        JsonGenerator jg = _jsonFactory.createJsonGenerator(out, JsonEncoding.UTF8);
        _mapper.writeValue(jg, data);
        jg.close();
        return -1;
    }
}
