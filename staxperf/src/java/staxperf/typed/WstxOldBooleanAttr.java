package staxperf.typed;

import java.io.*;

import javax.xml.stream.*;

import org.codehaus.stax2.*;

/**
 * Test that uses Woodstox via basic vanilla Stax 1.0 interface
 */
public final class WstxOldBooleanAttr
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
                String str = sr.getAttributeValue(i).trim();
                if (str.equals("true")) {
                    ++total;
                } else if (str.equals("false") || str.equals("0")) {
                    ;
                } else if (str.equals("1")) {
                    ++total;
                } else {
                    throw new XMLStreamException("Illegal value '"+str+"', not boolean");
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
        new WstxOldBooleanAttr().test(args);
    }
}
