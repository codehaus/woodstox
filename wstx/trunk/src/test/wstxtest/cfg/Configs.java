package wstxtest.cfg;

import com.ctc.wstx.stax.WstxInputFactory;

public class Configs
{
    private Configs() { }

    // // // Configs for standard Woodstox properties

    public static void addAll(InputConfigIterator it) {
        // First standard configs:
        it.addConfig(getNamespaceConfig())
            .addConfig(getCoalescingConfig())
            .addConfig(getEntityExpansionConfig());

        // Then woodstox-specific ones
        it.addConfig(getLazyParsingConfig())
            .addConfig(getNormalizeLFsConfig())
            .addConfig(getInputBufferSizeConfig())
            .addConfig(getMinTextSegmentConfig());
    }

    // // // Configs for standard Woodstox properties

    public static InputTestConfig getNamespaceConfig() {
        return new NamespaceConfig();
    }

    public static InputTestConfig getCoalescingConfig() {
        return new CoalescingConfig();
    }

    public static InputTestConfig getEntityExpansionConfig() {
        return new EntityExpansionConfig();
    }

    // // // Configs for Woodstox properties

    public static InputTestConfig getLazyParsingConfig() {
        return new LazyParsingConfig();
    }

    public static InputTestConfig getNormalizeLFsConfig() {
        return new NormalizeLFsConfig();
    }

    public static InputTestConfig getInputBufferSizeConfig() {
        return new InputBufferSizeConfig();
    }

    public static InputTestConfig getMinTextSegmentConfig() {
        return new TextSegmentConfig();
    }

    /*
    /////////////////////////////////////////////////////
    // Config base class
    /////////////////////////////////////////////////////
     */

    abstract static class BaseConfig
        implements InputTestConfig
    {
        final int mTotalCount;

        int mPos;

        BaseConfig(int count) {
            mTotalCount = count;
            mPos = -1;
        }

        public boolean nextConfig(WstxInputFactory f) {
            if (++mPos >= mTotalCount) {
                return false;
            }
            config(f, mPos);
            return true;
        }

        public void firstConfig(WstxInputFactory f) {
            mPos = 0;
            config(f, 0);
        }

        public String getDesc() {
            return getDesc(mPos);
        }

        public String toString() { return getDesc(); }

        public static boolean booleanFromInt(int i) {
            return (i != 0);
        }

        // // // Abstract methods for sub-classes

        public abstract String getDesc(int index);

        public abstract void config(WstxInputFactory f, int index);
    }

    /*
    /////////////////////////////////////////////////////
    // Actual config classes, std StAX properties
    /////////////////////////////////////////////////////
     */

    public static class NamespaceConfig
        extends BaseConfig
    {
        NamespaceConfig() {
            super(2);
        }

        public String getDesc(int index) {
            return "namespaces: "+booleanFromInt(index);
        }

        public void config(WstxInputFactory f, int index) {
            f.doSupportNamespaces(booleanFromInt(index));
        }
    }

    public static class CoalescingConfig
        extends BaseConfig
    {
        CoalescingConfig() {
            super(2);
        }

        public String getDesc(int index) {
            return "coalescing: "+booleanFromInt(index);
        }

        public void config(WstxInputFactory f, int index) {
            f.doCoalesceText(booleanFromInt(index));
        }
    }

    public static class EntityExpansionConfig
        extends BaseConfig
    {
        EntityExpansionConfig() {
            super(2);
        }

        public String getDesc(int index) {
            return "expand-entities: "+booleanFromInt(index);
        }

        public void config(WstxInputFactory f, int index) {
            f.doReplaceEntityRefs(booleanFromInt(index));
        }
    }

    /*
    /////////////////////////////////////////////////////
    // Actual config classes, Woodstox custom properties
    /////////////////////////////////////////////////////
     */

    public static class LazyParsingConfig
        extends BaseConfig
    {
        LazyParsingConfig() {
            super(2);
        }

        public String getDesc(int index) {
            return "lazy-parsing: "+booleanFromInt(index);
        }

        public void config(WstxInputFactory f, int index) {
            f.doParseLazily(booleanFromInt(index));
        }
    }

    public static class NormalizeLFsConfig
        extends BaseConfig
    {
        NormalizeLFsConfig() {
            super(2);
        }

        public String getDesc(int index) {
            return "normalize-lfs: "+booleanFromInt(index);
        }

        public void config(WstxInputFactory f, int index) {
            f.doNormalizeLFs(booleanFromInt(index));
        }
    }

    public static class InputBufferSizeConfig
        extends BaseConfig
    {
        final static int[] mSizes = new int[] {
            8, 17, 200, 4000
        };

        InputBufferSizeConfig() {
            super(4);
        }

        public String getDesc(int index) {
            return "input-buffer: "+mSizes[index];
        }

        public void config(WstxInputFactory f, int index) {
            f.setInputBufferLength(mSizes[index]);
        }
    }

    public static class TextSegmentConfig
        extends BaseConfig
    {
        final static int[] mSizes = new int[] {
            6, 23, 100, 4000
        };

        TextSegmentConfig() {
            super(4);
        }

        public String getDesc(int index) {
            return "input-buffer: "+mSizes[index];
        }

        public void config(WstxInputFactory f, int index) {
            f.setInputBufferLength(mSizes[index]);
        }
    }
}

