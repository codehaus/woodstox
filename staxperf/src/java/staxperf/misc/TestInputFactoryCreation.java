package staxperf.misc;

import javax.xml.stream.*;

/**
 * Micro-benchmark that tests to see how costly is instantiation of 
 * XMLInputFactory instances via JAXP.
 */
public class TestInputFactoryCreation
{
    final int ROUNDS = 1000;

    TestInputFactoryCreation() { }

    void test()
        throws XMLStreamException
    {
        while (true) {
            long now = System.currentTimeMillis();
            // Let's do it first once to know class name:
            XMLInputFactory ifact = XMLInputFactory.newInstance();
            Class cls = ifact.getClass();
            for (int i = 1; i < ROUNDS; ++i) {
                ifact = XMLInputFactory.newInstance();
                //                ifact = new com.ctc.wstx.stax.WstxInputFactory();
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
        new TestInputFactoryCreation().test();
    }
}

