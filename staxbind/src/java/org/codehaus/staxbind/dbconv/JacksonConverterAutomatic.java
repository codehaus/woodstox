package org.codehaus.staxbind.dbconv;

import java.io.*;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * Converter that uses Jackson JSON processor for data binding,
 * using automatic bindings for serialization and deserialization.
 */
public class JacksonConverterAutomatic
    extends DbConverter
{
    final JsonFactory _jsonFactory;

    final ObjectMapper _mapper;

    public JacksonConverterAutomatic()
    {
        _jsonFactory = new JsonFactory();
        _mapper = new ObjectMapper();
    }

    public DbData readData(InputStream in)
        throws IOException
    {
        return _mapper.readValue(in, DbData.class);
    }
    
    public int writeData(OutputStream out, DbData data) throws Exception
    {
        JsonGenerator jg = _jsonFactory.createJsonGenerator(out, JsonEncoding.UTF8);
        _mapper.writeValue(jg, data);
        jg.close();
        return -1;
    }
}
