package cz.to2.noe.load.interfaces;

import cz.to2.noe.load.pipe.InternalQueues;

import java.util.Map;
import java.util.Properties;

/**
 * Created by pavelnovotny on 25.09.20.
 */
public interface LoadRunnable extends Runnable {

    public void setInternalQueues(InternalQueues internalQueues);
    public void setProperties(Properties properties);
    public void setParams(Map<String, String> params);
    public void end();

}
