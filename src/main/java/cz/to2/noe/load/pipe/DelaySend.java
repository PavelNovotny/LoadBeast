package cz.to2.noe.load.pipe;

import cz.to2.noe.load.interfaces.LoadRunnable;
import cz.to2.noe.load.pipe.data.Message;
import cz.to2.noe.load.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Properties;
import java.util.Queue;

/**
 * Created by pavelnovotny on 27.09.20.
 */
public class DelaySend implements LoadRunnable {
    private static Logger logger = LoggerFactory.getLogger(DelaySend.class);
    private InternalQueues internalQueues;
    private volatile boolean run = true;
    private Queue<Message> delayMessages;
    private Queue<Message> activeDelayMessages;
    private Queue<Message> jmsSendMessages;

    @Override
    public void setInternalQueues(InternalQueues internalQueues) {
        this.internalQueues = internalQueues;
        this.delayMessages = this.internalQueues.getScheduledDelays();
        this.jmsSendMessages = this.internalQueues.getJmsSend();
        this.activeDelayMessages = this.internalQueues.getActiveDelays();
    }

    @Override
    public void setProperties(Properties properties) {
    }

    @Override
    public void setParams(Map<String, String> params) {
    }

    @Override
    public void end() {
        this.run = false;
        synchronized (delayMessages) {
            delayMessages.notify();
        }
    }

    private void sleep(Message message) throws InterruptedException {
        long waitTime = Util.getWaitTime(message.timeOffset);
        logger.debug("{} {} sleep {}",message.crmOrderId, message.msgSerialNumber, waitTime);
        Thread.sleep(waitTime);
    }

    private void delayMessage() throws InterruptedException {
        while (run) {
            synchronized (delayMessages) {
                logger.trace("wait");
                delayMessages.wait();
                logger.trace("wake");
            }
            Message message;
            synchronized (delayMessages) {
                message = delayMessages.poll();
            }
            if(message == null) {
                continue;
            }
            synchronized (activeDelayMessages) {
                activeDelayMessages.add(message);
            }
            sleep(message);
            synchronized (activeDelayMessages) {
                activeDelayMessages.remove(message);
                synchronized (delayMessages) {
                    if (activeDelayMessages.size() == 0 && delayMessages.size()>0) {
                        delayMessages.notify();
                    }
                }
            }
            synchronized (jmsSendMessages) {
                jmsSendMessages.add(message);
                jmsSendMessages.notify();
            }
        }
    }


    @Override
    public void run() {
        logger.info("Start");
        Util.initBarrier.await();
        try {
            delayMessage();
        } catch (InterruptedException e) {
            logger.info("{} was interrupted", Thread.currentThread().getName());
        }
        logger.info("End");
    }
}
