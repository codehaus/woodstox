package staxperf.speed;

import java.io.File;
import java.io.Reader;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

public class TestRISpeed
    extends BaseSpeedTest
{
    XMLInputFactory ifact;

    public TestRISpeed() {
        System.setProperty("javax.xml.stream.XMLInputFactory",
                           "com.bea.xml.stream.MXParserFactory");
        ifact = XMLInputFactory.newInstance();
        ifact.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE);
        //ifact.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
        ifact.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);
        ifact.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
    }

    public int testExec(File f, Reader r)
        throws Exception
    {
        XMLStreamReader msc = ifact.createXMLStreamReader(r);

        System.out.print("Doc, version = '"+msc.getVersion()+"', enc = '"+msc.getCharacterEncodingScheme()+"' ");
        System.out.println("Standalone: "+msc.isStandalone()+" / "+msc.standaloneSet());

        int total = 0;

        while (msc.hasNext()) {
            int type = msc.next();
            total += type; // so it won't be optimized out...
            /*
            System.out.println("Type: "+type);
            if (type == DTD) {
                ; // STaX has nothing...
            } else if (type == PROCESSING_INSTRUCTION) {
                String target = msc.getPITarget();
                String data = msc.getPIData();
                System.out.println("Target = "+((target == null) ? "null"
                                   : ("'"+target+"'")));
                System.out.println("Data = "+((data == null) ? "null"
                                   : ("'"+data+"'")));
            }

            if (type == START_ELEMENT || type == END_ELEMENT) {
                int nsCount = msc.getNamespaceCount();
                System.out.println("QName = <"+msc.getName()+">");
                if (type == START_ELEMENT) {
                    System.out.println(" attributes: "+msc.getAttributeCount()+", local namespaces: "+nsCount);
                } else {
                    System.out.println(" local namespaces: "+nsCount);
                }
                if (nsCount > 0) {
                    for (int i = 0; i < nsCount; ++i) {
                        String prefix = msc.getNamespacePrefix(i);
                        String URI = msc.getNamespaceURI(i);
                        System.out.println(" #"+i+": '"+prefix
                                           +"' -> '"+URI+"'.");
                    }
                }
            }

            if (type == START_ELEMENT) {
                int ac = msc.getAttributeCount();
                System.out.println(" ["+ac+" attributes]");
                if (ac > 0) {
                    for (int i = 0; i < ac; ++i) {
                        QName qn = msc.getAttributeName(i);
                        String val = msc.getAttributeValue(i);
                        System.out.println("  #"+i+" "+qn+" -> '"+val+"'.");
                    }
                }
            }
            */

            if (msc.hasText()) {
                int textLen = msc.getTextLength();
                total += textLen;
                /*
                String text = msc.getText();
                if (type == CHARACTERS || type == CDATA) {
                    System.out.println("Text (ws = "+msc.isWhiteSpace()+") = '"+text+"'.");
                } else {
                    System.out.println("Text = '"+text+"'.");
                }
                */
            }
        }
        return total;
    }

    public void testFinish() {
    }

    public static void main(String[] args) throws Exception
    {
        new TestRISpeed().test(args);
    }
}
