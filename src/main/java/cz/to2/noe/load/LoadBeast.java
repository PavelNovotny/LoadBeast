package cz.to2.noe.load;

import cz.to2.noe.load.interfaces.LoadRunnable;
import cz.to2.noe.load.pipe.InternalQueues;
import cz.to2.noe.load.thread.Barrier;
import cz.to2.noe.load.util.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * Created by pavelnovotny on 07.09.20.
 */
public class LoadBeast {
    private static Logger logger = LoggerFactory.getLogger(LoadBeast.class);
    private final int delayThreadCount;
    private final int workThreadCount;

    private final Properties properties;
    private InternalQueues internalQueues;
    private Set<LoadRunnable> runnables = new HashSet<LoadRunnable>();
    private Set<Thread> threads = new HashSet<Thread>();
    private long inactivityThreshold;


    public LoadBeast(String propertyFileName, String propertyIterationFileName) {
        this.internalQueues = new InternalQueues();
        this.properties = loadProperties(propertyFileName);
        Util.setProperties(properties);
        this.inactivityThreshold = Long.parseLong(properties.getProperty("work.monitor.inactivity.threshold"));
        this.delayThreadCount = Integer.parseInt(properties.getProperty("threads.pipe.delay.count"));
        this.workThreadCount = Integer.parseInt(properties.getProperty("threads.pipe.receive.count"));

        Properties iterProperties = loadProperties(propertyIterationFileName);
        Util.loadIteration = 1 + Integer.parseInt(iterProperties.getProperty("current.load.iteration"));
        iterProperties.setProperty("current.load.iteration", String.format("%s",Util.loadIteration));
        saveProperties(iterProperties, propertyIterationFileName);
    }

    public static void main(String[] args) {
        //todo závislosti maven nebo gradle.
        String propertyFile = "beast-noe.properties";
        String propertyIteration = "beast-iteration.properties";
        if (args.length == 1) {
            propertyFile = args[0];
        }
        if (args.length == 2) {
            propertyIteration = args[1];
        }
        LoadBeast load = new LoadBeast(propertyFile, propertyIteration);
        try {
            load.loadBeast();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void loadBeast() throws IllegalAccessException, ClassNotFoundException, InstantiationException, InterruptedException {
        Util.logActivity(); //init activity check
        prepareJmsThreads("cz.to2.noe.load.jms.JmsSend", "jms.send.jndi.provider.url");
        prepareJmsThreads("cz.to2.noe.load.jms.JmsReceive", "jms.receive.jndi.provider.url");
        prepareThreads(1, "cz.to2.noe.load.file.ReadLoad", null);
        prepareThreads(workThreadCount, "cz.to2.noe.load.pipe.Receive", null);
        prepareThreads(delayThreadCount, "cz.to2.noe.load.pipe.DelaySend", null);
        Util.initBarrier = new Barrier(this.threads.size() + 1);  //+1 main thread
        startThreads(this.threads);
        Util.initBarrier.await();
        Util.init(); //after initialization of all threads, to be more precise when computing delays
        monitor(); //monitor end of program and/or various statistics
    }

    private void prepareJmsThreads(String className, String propertyKey) throws IllegalAccessException, ClassNotFoundException, InstantiationException {
        String urls = properties.getProperty(propertyKey);
        String[] split = urls.split(",");
        for (int i=0; i<split.length; i++) {
            Map<String, String> params = new HashMap<String, String>();
            params.put(propertyKey, split[i]);
            prepareThread(i, className, params);
        }
    }

    private void monitor() throws InterruptedException {
        Thread.currentThread().setName("Monitoring_THREAD_0");
        logger.info("Monitoring started");
        int monitoringInterval = 2000;
        while (true) {
            logger.trace("sleep {}", monitoringInterval);
            Thread.sleep(monitoringInterval);
            logger.info("Statistics: orders {}, jmsReceived {}, scheduledDelays {}, activeDelays {}, jmsSend {}",
                    internalQueues.getOrdersSize()
                    ,internalQueues.getJmsReceivedSize()
                    ,internalQueues.getScheduledDelaysSize()
                    ,internalQueues.getActiveDelaysSize()
                    ,internalQueues.getJmsSendSize()
            );
            logger.debug("Checking inactivity");
            if (Util.inactive() > this.inactivityThreshold) {
                logger.info("Inactivity detected");
                break;
            }
        }
        logger.info("Statistics: orders {}, jmsReceived {}, scheduledDelays {}, activeDelays {}, jmsSend {}",
                internalQueues.getOrdersSize()
                ,internalQueues.getJmsReceivedSize()
                ,internalQueues.getScheduledDelaysSize()
                ,internalQueues.getActiveDelaysSize()
                ,internalQueues.getJmsSendSize()
        );
        logger.info("Order statistics:");
        internalQueues.ordersStatistics();
        logger.info("Signaling threads to end graciously");
        for (LoadRunnable runnable: this.runnables) {
            runnable.end();
        }
        logger.trace("sleep {}", monitoringInterval);
        Thread.sleep(monitoringInterval); //wait some time to finish
        for (Thread thread: this.threads) {//interrupt not finished threads (stuck in sleep, etc..)
            if (thread.isAlive()) {
                logger.info("{} must be interrupted.", thread.getName());
                thread.interrupt();
            }
        }
        logger.info("Monitoring finished");
    }

    private Thread prepareThread(int num, String className, Map<String, String> params) throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        LoadRunnable runnable = (LoadRunnable) Class.forName(className).newInstance();
        runnable.setInternalQueues(this.internalQueues);
        runnable.setProperties(this.properties);
        runnable.setParams(params);
        runnables.add(runnable);
        Thread thread = new Thread(runnable);
        String[] split = className.split("\\.");
        String threadNamePrefix = split[split.length-1];
        thread.setName(String.format("%s_THREAD_%s", threadNamePrefix, num));
        this.threads.add(thread);
        return thread;
    }

    private Set<Thread> prepareThreads(int count, String className, Map<String, String> params) throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        Set <Thread> threads = new HashSet<Thread>();
        for (int i=0;i<count;i++) {
            Thread thread = prepareThread(i, className, params);
            threads.add(thread);
        }
        return threads;
    }

    private Properties loadProperties(String propertyFileName){
        File propsFile=new File(propertyFileName);
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(propsFile));
        } catch (Throwable t) {
            throw new RuntimeException("Nelze nahrat properties :"+propsFile.getAbsolutePath(),t);
        }
        return properties;
    }

    private void saveProperties(Properties properties, String fileName) {
        try(OutputStream outputStream = new FileOutputStream(fileName)){
            properties.store(outputStream, null);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void startThreads(Set<Thread> threads) {
        for (Thread thread: threads) {
            thread.start();
        }
    }



}
