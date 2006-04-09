package staxperf.misc;

import javax.xml.stream.*;

/**
 * Micro-benchmark that tests to see how costly is instantiation of 
 * XMLOutputFactory instances via JAXP.
 */
public class TestOutputFactoryCreation
{
    final int ROUNDS = 1000;

    TestOutputFactoryCreation() { }

    void test()
        throws XMLStreamException
    {
        while (true) {
            long now = System.currentTimeMillis();
            // Let's do it first once to know class name:
            XMLOutputFactory ifact = XMLOutputFactory.newInstance();
            Class cls = ifact.getClass();
            for (int i = 1; i < ROUNDS; ++i) {
                ifact = XMLOutputFactory.newInstance();
                //ifact = new com.ctc.wstx.stax.WstxOutputFactory();
            }
            now = System.currentTimeMillis() - now;
            System.out.println("Took "+now+" ms to create "+ROUNDS+" instances of "+cls.getName()+".");
            try { Thread.sleep(100L); } catch (InterruptedException ie) { }
            System.gc();
            try { Thread.sleep(100L); } catch (InterruptedException ie) { }
        }
    }

    public static void main(String[] args)
        throws XMLStreamException
    {
        new TestOutputFactoryCreation().test();
    }
}

