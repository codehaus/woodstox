package staxperf.speed;

import java.io.File;
import java.io.Reader;

import javax.xml.namespace.QName;
import javax.xml.stream.*;
import javax.xml.stream.events.*;

import com.ctc.wstx.stax.*;

public class TestWstxEventSpeed
    extends BaseSpeedTest
{
    final XMLInputFactory mFactory;

    XMLEventReader msc;

    public TestWstxEventSpeed()
    {
        super();
        System.setProperty("javax.xml.stream.XMLInputFactory",
                           "com.ctc.wstx.stax.WstxInputFactory");
        mFactory = XMLInputFactory.newInstance();
        //mFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE);
        System.out.println("Factory instance: "+mFactory.getClass());
        System.out.println("  coalescing: "+mFactory.getProperty(XMLInputFactory.IS_COALESCING));
    }

    public int testExec(File f, Reader r)
        throws Exception
    {
        msc = mFactory.createXMLEventReader(f.toURL().toString(), r);
        int total = 0;

        while (msc.hasNext()) {
            XMLEvent evt = msc.nextEvent();
            int type = evt.getEventType();
            total += type; // so it won't be optimized out...

            /*
            System.out.println("Type: "+type);
            if (type == DTD) {
                String pubId = msc.getPublicId();
                String sysId = msc.getSystemId();
                System.out.println("Public id = "+((pubId == null) ? "null"
                                   : ("'"+pubId+"'")));
                System.out.println("System id = "+((sysId == null) ? "null"
                                   : ("'"+sysId+"'")));
                
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
                System.out.println(" ["+ac+"]");
                if (ac > 0) {
                    for (int i = 0; i < ac; ++i) {
                        QName qn = msc.getAttributeName(i);
                        String val = msc.getAttributeValue(i);
                        System.out.println("  "+qn+" -> '"+val+"'.");
                        String a = qn.getNamespaceURI();
                        String b = qn.getLocalPart();
                        String val2 = msc.getAttributeValue(a, b);
                        if (!val.equals(val2)) {
                            throw new Error("Attribute access problem, access by FQN returned '"+val2+"'.");
                        }
                        val2 = msc.getAttributeValue(""+a, b+"");
                        if (!val.equals(val2)) {
                            throw new Error("Attribute access problem(2), access by FQN returned '"+val2+"'.");
                        }
                    }
                }
            }
            */

            if (evt.isCharacters()) {
                Characters ch = evt.asCharacters();
                int textLen = ch.getData().length();
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

    public void testFinish()
        throws Exception
    {
        /*
        com.ctc.wstx.util.SymbolTable symt = ((WstxStreamReader) msc).getSymbolTable();
        double seek = symt.calcAvgSeek();
        seek = ((int) (100.0  * seek)) / 100.0;
        System.out.println("Symbol count: "+symt.size()+", avg len: "+seek+".");
        */
    }

    public static void main(String[] args) throws Exception
    {
        new TestWstxEventSpeed().test(args);
    }
}
