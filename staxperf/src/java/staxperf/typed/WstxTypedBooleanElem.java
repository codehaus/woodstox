package staxperf.typed;

import java.io.*;

import javax.xml.stream.*;

import org.codehaus.stax2.*;
import org.codehaus.stax2.typed.*;

/**
 * Test that uses Woodstox via basic vanilla Stax 1.0 interface
 */
public final class WstxTypedBooleanElem
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
            if (sr.getElementAsBoolean()) {
                ++total;
            }
        }
        return total;
    }

    public static void main(String[] args) throws Exception
    {
        new WstxTypedBooleanElem().test(args);
    }
}
