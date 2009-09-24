package org.codehaus.staxbind.jsoncount;

import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.node.ObjectNode;

public final class JacksonDriverTree
    extends JsonCountDriver
{
    final ObjectMapper _mapper;

    public JacksonDriverTree() {
        _mapper = new ObjectMapper();
    }

    protected void read(byte[] docData, CountResult results)
        throws Exception
    {
        JsonNode root = _mapper.readValue(docData, 0, docData.length, JsonNode.class);
        _processNode(root, results);
    }

    private void _processNode(JsonNode node, CountResult results)
    {
        if (node.isObject()) {
            _countObjectFields((ObjectNode) node, results);
        } else if (node.isArray()) {
            _iterateArrayElements(node, results);
        }
    }

    private void  _countObjectFields(ObjectNode objNode, CountResult results)
    {
        Iterator<Map.Entry<String,JsonNode>> it = objNode.getFields();
        while (it.hasNext()) {
            Map.Entry<String,JsonNode> en = it.next();
            results.addReference(en.getKey());
            _processNode(en.getValue(), results);
        }
    }

    private void _iterateArrayElements(JsonNode objNode, CountResult results)
    {
        for (JsonNode n : objNode) {
            _processNode(n, results);
        }
    }
}
