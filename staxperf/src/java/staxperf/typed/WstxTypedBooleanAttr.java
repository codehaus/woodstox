package staxperf.typed;

import java.io.*;

import javax.xml.stream.*;

import org.codehaus.stax2.*;
import org.codehaus.stax2.typed.*;

/**
 * Test that uses Woodstox via basic vanilla Stax 1.0 interface
 */
public final class WstxTypedBooleanAttr
    extends BaseTypedTest
{
    @Override
    protected XMLInputFactory2 getFactory()
    {
        return new com.ctc.wstx.stax.WstxInputFactory();
    }

    @Override
    protected final int testExec2(XMLStreamReader2 sr) throws XMLStreamException
    {
        // Ok: we point to the root node now

        int total = 0;
        
        while (sr.nextTag() == START_ELEMENT) {
            int i = sr.getAttributeCount();
            while (--i >= 0) {
                if (sr.getAttributeAsBoolean(i)) {
                    ++total;
                }
            }
            // Just need to skip to end tag:
            if (sr.nextTag() != END_ELEMENT) {
                throw new XMLStreamException("Expected end element for attribute value");
            }
        }
        return total;
    }

    public static void main(String[] args) throws Exception
    {
        new WstxTypedBooleanAttr().test(args);
    }
}
