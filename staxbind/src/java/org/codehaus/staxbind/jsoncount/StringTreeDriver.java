package org.codehaus.staxbind.jsoncount;

import java.util.*;

import org.stringtree.json.JSONReader;

/**
 * Test driver that uses the "StringTree" json parser, from
 * [http://stringtree.org/stringtree-json.html]
 */
public final class StringTreeDriver
    extends JsonCountDriver
{
    public StringTreeDriver() {
    }

    protected void read(byte[] docData, CountResult results)
        throws Exception
    {
        // Ugh. Yet another toy parser that requires a String...
        String doc = new String(docData, "UTF-8");        
        Object ob = new JSONReader().read(doc);
        // but at least we get Maps, Lists etc
        _processNode(ob, results);
    }

    private void _processNode(Object node, CountResult results)
        throws Exception
    {
        if (node instanceof Map<?,?>) {
            _processObject((Map<?,?>) node, results);
        } else if (node instanceof List<?>) {
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
