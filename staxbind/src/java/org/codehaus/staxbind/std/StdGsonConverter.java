package org.codehaus.staxbind.std;

import java.io.*;

import com.google.gson.*;

/**
 * Converter that uses Google-gson package for JSON data binding,
 * using automatic bindings for serialization and deserialization.
 */
public class StdGsonConverter<T extends StdItem<T>>
    extends StdConverter<T>
{
    final Gson _gson;

    final Class<T> _itemClass;

    public StdGsonConverter(Class<T> itemClass)
    {
        _itemClass = itemClass;
        _gson = new Gson();
    }

    public T readData(InputStream in) throws IOException
    {
        // Alas, Gson can't eat InputStreams...
        return _gson.fromJson(new InputStreamReader(in, "UTF-8"), _itemClass);
    }
    
    public int writeData(OutputStream out, T data) throws Exception
    {
        OutputStreamWriter w = new OutputStreamWriter(out, "UTF-8");
        this._gson.toJson(data, w);
        return -1;
    }
}
