package org.codehaus.staxbind.jsoncount;

import org.codehaus.jackson.*;

public final class JacksonDriverStreaming
    extends JsonCountDriver
{
    final JsonFactory _factory;

    public JacksonDriverStreaming() {
        _factory = new JsonFactory();
    }

    protected void read(byte[] docData, CountResult results)
        throws Exception
    {
        JsonParser jp = _factory.createJsonParser(docData);
        JsonToken t;

        while (jp.nextValue() != null) {
            String fieldName = jp.getCurrentName();
            if (fieldName != null) {
                results.addReference(fieldName);
            }
            /* don't care about actual value nodes, for now;
             * in future could complicate
             * testing by keeping track of Strings, or adding
             * numeric values.
             */
        }
        jp.close();
    }
}
