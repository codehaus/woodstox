package org.codehaus.staxbind.std;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.smile.SmileFactory;

/**
 * Converter that uses Jackson JSON processor with its binary JSON-compatible
 * format, "Smile", for data binding.
 * Data binding is automatic using ObjectMapper.
 */
public class StdSmileConverter<T extends StdItem<T>>
   extends StdConverter<T>
{
   protected final ObjectMapper _mapper;

   protected final Class<T> _itemClass;

   public StdSmileConverter(Class<T> itemClass)
   {
       SmileFactory f = new SmileFactory();
       _mapper = new ObjectMapper(f);
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
