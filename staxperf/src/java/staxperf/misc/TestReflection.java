package staxperf.misc;

import java.lang.reflect.*;

/**
 * Micro-benchmark that tests to see how efficient are reflection-based
 * methods (object instantiation, setting and getting values), compared
 * to native ones.
 */
public class TestReflection
{
    // 2M rounds for faster operations
    final int ROUNDS_BIG = 2000000;

    // 1/2M for slower ones
    final int ROUNDS_SMALL = 500000;

    final static String NAME = "foobar";

    TestReflection() { }

    Object test()
        throws Exception
    {
        int ctr = 0;

        final Class cls = ReflectionData.class;
        final Method getter = cls.getMethod("getName", new Class[] { });
        final Method setter = cls.getMethod("setName", new Class[] { String.class });
        final Field field = cls.getField("name");
        Object x = null; // used just to prevent dead code elimination

        while (true) {
            String desc;
            int method = (ctr % 8);

            ++ctr;

            if (method == 0) {
                System.out.println();
            }

            long now = System.currentTimeMillis();

            switch (method) {
            case 0:
                desc = "Reflection construct";
                x = testReflect(ROUNDS_SMALL, cls);
                break;
            case 1:
                desc = "Direct construct";
                x = testDirect(ROUNDS_SMALL);
                break;

            case 2:
                desc = "Reflection call set";
                x = testReflectCallSet(ROUNDS_BIG, setter);
                break;
            case 3:
                desc = "Reflection assign set";
                x = testReflectAssignSet(ROUNDS_BIG, field);
                break;

            case 4:
                desc = "Method set";
                x = testDirectSet(ROUNDS_BIG);
                break;

            case 5:
                desc = "Reflection call get";
                x = testReflectCallGet(ROUNDS_BIG, getter);
                break;
            case 6:
                desc = "Reflection access get";
                x = testReflectAccessGet(ROUNDS_BIG, field);
                break;
            case 7:
                desc = "Method get";
                x = testDirectGet(ROUNDS_BIG);
                break;
            default: // sanity check
                throw new Error();
            }
            now = System.currentTimeMillis() - now;
            int dummyHash = (x == null) ? -1 : (x.hashCode() & 0xF);
            System.out.println("Took "+now+" ms for "+
                               (method < 2 ? ROUNDS_SMALL : ROUNDS_BIG)
                               +" x "+desc+" (x = "+dummyHash+")");
            try { Thread.sleep(100L); } catch (InterruptedException ie) { }
            if (method == 7) { // let's GC after full round
                System.gc();
            }
            // Plus let other tasks proceed (scheduler might penalize us otherwise)
            try { Thread.sleep(100L); } catch (InterruptedException ie) { }
        }
    }

    private Object testReflect(int ROUNDS, Class cls)
        throws Exception
    {
        Object o = null;

        for (int i = 0; i < ROUNDS; ++i) {
            o = cls.newInstance();
        }
        return o;
    }

    private Object testDirect(int ROUNDS)
    {
        Object o = null;
        for (int i = 0; i < ROUNDS; ++i) {
            o = new ReflectionData();
        }

        return o;
    }

    private Object testReflectCallSet(int ROUNDS, Method setter)
        throws Exception
    {
        Object o = null;
        ReflectionData data = new ReflectionData();
        Object[] args = new Object[] { NAME };

        for (int i = 0; i < ROUNDS; ++i) {
            o = setter.invoke(data, args);
        }
        return o;
    }

    private Object testReflectAssignSet(int ROUNDS, Field field)
        throws Exception
    {
        Object o = null;
        ReflectionData data = new ReflectionData();
        Object value = NAME;

        for (int i = 0; i < ROUNDS; ++i) {
            field.set(data, value);
        }
        return data;
    }

    private Object testDirectSet(int ROUNDS)
    {
        ReflectionData data = new ReflectionData();

        for (int i = 0; i < ROUNDS; ++i) {
            data.setName(NAME);
        }

        return data;
    }

    private Object testReflectCallGet(int ROUNDS, Method getter)
        throws Exception
    {
        ReflectionData data = new ReflectionData();
        Object o = null;
        Object[] args = new Object[] { };

        for (int i = 0; i < ROUNDS; ++i) {
            o = getter.invoke(data, args);
        }
        return o;
    }

    private Object testReflectAccessGet(int ROUNDS, Field field)
        throws Exception
    {
        ReflectionData data = new ReflectionData();
        Object o = null;

        for (int i = 0; i < ROUNDS; ++i) {
            o = field.get(data);
        }
        return o;
    }

    private Object testDirectGet(int ROUNDS)
    {
        ReflectionData data = new ReflectionData();
        Object o = null;
        for (int i = 0; i < ROUNDS; ++i) {
            o = data.getName();
        }

        return o;
    }

    public static void main(String[] args)
        throws Exception
    {
        new TestReflection().test();
    }
}

/**
 * Value class used by the test
 */
class ReflectionData
{
    public String name;

    int age;

    public ReflectionData() { }

    public void setName(String n) { name = n; }
    public String getName() { return name; }
}
