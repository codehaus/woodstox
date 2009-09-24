package org.codehaus.staxbind.jsoncount;

import java.io.*;
import java.util.*;

import org.json.simple.parser.*;

/**
 * Test driver that uses the "Json-simple" parser, from
 * [http://code.google.com/p/json-simple/] using the
 * tree model.
 */
public final class JsonSimpleDriver
    extends JsonCountDriver
{
    final JSONParser _parser;

    public JsonSimpleDriver() {
        _parser = new JSONParser();
    }

    protected void read(byte[] docData, CountResult results)
        throws Exception
    {
        /* Hmmh. Should really take an InputStream, but at least
         * we can give a Reader...
         */
        Object ob = _parser.parse(new InputStreamReader(new ByteArrayInputStream(docData), "UTF-8"));
        _processNode(ob, results);
    }

    private void _processNode(Object node, CountResult results)
        throws Exception
    {
        if (node instanceof Map) {
            _processObject((Map<?,?>) node, results);
        } else if (node instanceof List) {
            _processArray((List<?>) node, results);
        }
    }

    private void _processArray(List<?> array, CountResult results)
        throws Exception
    {
        for (Object ob : array) {
            _processNode(ob, results);
        }
    }

    private void _processObject(Map<?,?> object, CountResult results)
        throws Exception
    {
        for (Map.Entry<?,?> en : object.entrySet()) {
            String fn = (String) en.getKey();
            results.addReference(fn);
            _processNode(en.getValue(), results);
        }
    }
}
