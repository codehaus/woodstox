package staxperf.misc;

import java.io.*;
import java.util.*;

/**
 * Simple micro benchmark to test how overhead related to
 * de-serializing instances of simple classes
 */
public class TestSerialization
{
    final static int REPS = 200;

    final static String[] KEYS = new String[] {
        "red", "tricycle", "with", "radio", "flyer",
        "recliner", "sofa", "loveseat", "Free",
        "shipping"
    };

    private TestSerialization() { }

    public void test(String[] args)
        throws IOException, ClassNotFoundException
    {
        if (args.length > 0) {
            System.err.println("Usage: java ... "+getClass().getName()+"");
            System.exit(1);
        }
        Weights[] input = buildData();
        test2(input);
    }

    void test2(Weights[] input)
        throws IOException, ClassNotFoundException
    {
        byte[][] jdkData = buildJdkData(input);
        byte[][] customData = buildCustomData(input);

        verifyData(jdkData, "JDK");
        verifyData(customData, "custom");

        int sum = 0;

        for (int i = 0; true; ++i) {
            try {  Thread.sleep(100L); } catch (InterruptedException ie) { }
            //System.gc();
            try {  Thread.sleep(100L); } catch (InterruptedException ie) { }

            long curr = System.currentTimeMillis();
            String msg;

            switch (i % 2) {
            case 0:
                msg = "Deserialize, custom";
                for (int j = 0; j < REPS; ++j) {
                    sum += testCustom(customData);
                }
                break;
            case 1:
                System.out.println();
                msg = "Deserialize, JDK";
                for (int j = 0; j < REPS; ++j) {
                    sum += testJDK(jdkData);
                }
                break;
            default:
                throw new Error();
            }

            curr = System.currentTimeMillis() - curr;
            System.out.println("Test '"+msg+"' -> "+curr+" msecs ("
                               +(sum & 0xFF)+").");
        }
    }

    private int testJDK(byte[][] data)
        throws IOException, ClassNotFoundException
    {
        int total = 0;
        Weights w = null;

        for (int i = 0, len = data.length; i < len; ++i) {
            ByteArrayInputStream bis = new ByteArrayInputStream(data[i]);
            ObjectInputStream ois = new ObjectInputStream(bis);
            w = (Weights) ois.readObject();
            ois.close();
            bis.close();
        }
        return w.size();
    }

    private int testCustom(byte[][] data)
        throws IOException
    {
        int total = 0;
        Weights w = null;

        for (int i = 0, len = data.length; i < len; ++i) {
            ByteArrayInputStream bis = new ByteArrayInputStream(data[i]);
            w = Weights.customDeserialize(bis);
            bis.close();
        }
        return w.size();
    }

    private Weights[] buildData()
    {
        Weights[] result = new Weights[KEYS.length];
        for (int i = 0, len = KEYS.length; i < len; ++i) {
            String key = KEYS[i];
            int count = (key.length() & 3) + 2;
            Weight[] weights = new Weight[count];
            for (int j = 0; j < count; ++j) {
                weights[j] = new Weight(key+""+j, 1.0 / ((double) j + 0.32));
            }
            result[i] = new Weights(key, weights);
        }
        return result;
    }

    private byte[][] buildJdkData(Weights[] input)
        throws IOException
    {
        byte[][] result = new byte[input.length][];
        for (int i = 0; i < input.length; ++i) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(input[i]);
            oos.flush();
            oos.close();
            result[i] = bos.toByteArray();
        }
        return result;
    }

    private byte[][] buildCustomData(Weights[] input)
        throws IOException
    {
        byte[][] result = new byte[input.length][];
        for (int i = 0; i < input.length; ++i) {
            Weights w = input[i];
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            w.customSerialize(bos);
            byte[] data = bos.toByteArray();
            result[i] = data;

            // Let's verify it right away:
            ByteArrayInputStream bis = new ByteArrayInputStream(data);
            Weights w2 = Weights.customDeserialize(bis);
            if (!w2.equals(w)) {
                throw new IllegalStateException("Failed to correctly serialize/deserialize test entry #"+i);
            }
//System.out.println("Custom entry #"+i+" ok, size "+data.length);
        }
        return result;
    }

    private void verifyData(byte[][] data, String type)
    {
        int total = 0;

        for (int i = 0; i < data.length; ++i) {
            total += data[i].length;
        }
        System.out.println("Total size (method '"+type+"'): "+total);
    }

    public static void main(String[] args)
        throws Exception
    {
        new TestSerialization().test(args);
    }

    final static class Weights
        implements Serializable
    {
        String mKey;

        Weight[] mWeights;

        public Weights(String key, Weight[] weights)
        {
            mKey = key;
            mWeights = weights;
        }

        int size() { return mWeights.length; }

        public void customSerialize(ByteArrayOutputStream bos)
            throws IOException
        {
            DataOutputStream dos = new DataOutputStream(bos);
            dos.writeUTF(mKey);
            int len = mWeights.length;
            dos.writeInt(len);
            for (int i = 0; i < len; ++i) {
                mWeights[i].customSerialize(dos);
            }
            dos.flush();
        }

        public static Weights customDeserialize(InputStream in)
            throws IOException
        {
            DataInputStream din = new DataInputStream(in);
            String key = din.readUTF();
            int count = din.readInt();
            Weight[] weights = new Weight[count];
            for (int i = 0; i < count; ++i) {
                weights[i] = Weight.customDeserialize(din);
            }
            return new Weights(key, weights);
        }

        public boolean equals(Object o)
        {
            if (o == this) {
                return true;
            }
            if (o instanceof Weights) {
                Weights other = (Weights) o;
                if (mKey.equals(other.mKey)) {
                    Weight[] otherW = other.mWeights;
                    if (otherW.length == mWeights.length) {
                        for (int i = 0; i < mWeights.length; ++i) {
                            if (!mWeights[i].equals(other.mWeights[i])) {
                                return false;
                            }
                        }
                        return true;
                    }
                    
                }
            }
            return false;
        }
    }

    final static class Weight
        implements Serializable
    {
        String mKey;
        double mWeight;

        public Weight(String key, double weight)
        {
            mKey = key;
            mWeight = weight;
        }

        public void customSerialize(DataOutputStream dos)
            throws IOException
        {
            dos.writeUTF(mKey);
            dos.writeDouble(mWeight);
        }

        public static Weight customDeserialize(DataInputStream din)
            throws IOException
        {
            return new Weight(din.readUTF(), din.readDouble());
        }

        public boolean equals(Object o)
        {
            if (o == this) {
                return true;
            }
            if (o instanceof Weight) {
                Weight other = (Weight) o;
                return mKey.equals(other.mKey) && mWeight == other.mWeight;
            }
            return false;
        }
    }
}
