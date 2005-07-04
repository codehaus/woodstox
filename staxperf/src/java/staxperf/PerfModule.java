package staxperf;

import java.io.InputStream;
import java.util.Arrays;

public abstract class PerfModule
{
    final String mImplName;

    /**
     * Array in which results are (temporarily) stored
     */
    int[] mMedCounts;

    int[] mTopCounts;

    protected PerfModule(String implName) {
        mImplName = implName;
    }

    public void init(int roundCount) {
        mMedCounts = new int[roundCount];
        mTopCounts = new int[roundCount];
    }

    public abstract int runFor(int seconds, String systemId, byte[] data) throws Exception;

    public String getImplName() { return mImplName; }

    public void addResult(int index, int med, int top) {
        mMedCounts[index] = med;
        mTopCounts[index] = top;
    }

    public int[] getMedianResults() { return mMedCounts; }
    public int[] getTopResults() { return mTopCounts; }

    public void finalizeResults() {
        Arrays.sort(mMedCounts);
        Arrays.sort(mTopCounts);
    }

    public int getMedianResult() { return mMedCounts[mMedCounts.length / 2]; }
    public int getTopResult() { return mTopCounts[mTopCounts.length-1]; }
}
