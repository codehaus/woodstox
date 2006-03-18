package com.ctc.wstx.stax;

/**
 * This is a simple static class that contains properties about this
 * particular Woodstox version.
 */
public final class ImplInfo
{
    public static String getImplName() {
        return "woodstox";
    }
    public static String getImplVersion() {
        /* !!! TBI: get from props file or so? Or build as part of Ant
         *    build process?
         */
        return "2.9.2";
    }
}

