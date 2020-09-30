package cz.to2.noe.load.util;

import cz.to2.noe.load.thread.Barrier;

import java.util.Calendar;
import java.util.Properties;

/**
 * Created by pavelnovotny on 07.09.20.
 */
public class Util {

    private static long programStartTime;
    private volatile static long lastActivityTime;
    private static float loadSpeedFactor;
    public static Barrier initBarrier;
    public static int loadIteration;

    public static long getWaitTime(int timeOffset) {
        long realOffset = System.currentTimeMillis() - Util.programStartTime;
        long waitTime = (long) ((timeOffset/Util.loadSpeedFactor) - realOffset);
        waitTime = waitTime<0?0:waitTime;
        return waitTime;
    }

    public static void init() {
        Util.programStartTime = System.currentTimeMillis();
    }

    public static void setProperties(Properties properties) {
        Util.loadSpeedFactor  = Float.parseFloat((String) properties.get("load.speed.factor"));
    }

    public static synchronized void logActivity() {
        Util.lastActivityTime = System.currentTimeMillis();
    }

    public static long inactive() {
        long factorOneInactivity = System.currentTimeMillis() - Util.lastActivityTime;
        return (long) (factorOneInactivity*loadSpeedFactor);
    }

}
