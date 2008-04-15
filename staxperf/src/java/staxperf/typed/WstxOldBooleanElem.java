package staxperf.typed;

import java.io.*;

import javax.xml.stream.*;

import org.codehaus.stax2.*;

/**
 * Test that uses Woodstox via basic vanilla Stax 1.0 interface
 */
public final class WstxOldBooleanElem
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
            String str = sr.getElementText().trim();
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
        return total;
    }

    public static void main(String[] args) throws Exception
    {
        new WstxOldBooleanElem().test(args);
    }
}
