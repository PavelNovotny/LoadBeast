package cz.to2.noe.load.pipe;

import cz.to2.noe.load.interfaces.LoadRunnable;
import cz.to2.noe.load.jms.data.JmsMessage;
import cz.to2.noe.load.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Properties;
import java.util.Queue;

/**
 * Created by pavelnovotny on 27.09.20.
 */
public class Receive implements LoadRunnable {
    private static Logger logger = LoggerFactory.getLogger(Receive.class);

    private InternalQueues internalQueues;
    private volatile boolean run = true;
    private Queue<JmsMessage> jmsReceived;

    @Override
    public void setInternalQueues(InternalQueues workingQueue) {
        this.internalQueues = workingQueue;
        this.jmsReceived = internalQueues.getJmsReceived();
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
    }

    private void processMessage() throws InterruptedException {
        Util.initBarrier.await();
        while (run) {
            JmsMessage jmsMessage;
            synchronized (jmsReceived) {
                if (jmsReceived.size() > 0) {
                    jmsReceived.notify();
                }
                logger.trace("wait");
                jmsReceived.wait();
                logger.trace("wake");
            }
            synchronized (jmsReceived) {
                jmsMessage = jmsReceived.poll();
            }
            if(jmsMessage != null) {
                jmsReceiveMessage(jmsMessage.getConversationId(), jmsMessage.getXml(), jmsMessage.getJmsType());
            }
        }
    }

    private void jmsReceiveMessage(String conversationId, String xml, String jmsType) {
        String crmOrderId = null;
        if (conversationId != null) {
            String[] split = conversationId.split("-");
            crmOrderId = split[0];
        }
        internalQueues.receiveMessage(crmOrderId, xml, jmsType);
    }


    @Override
    public void run() {
        logger.info("Start");
        try {
            processMessage();
        } catch (InterruptedException e) {
            logger.info("{} was interrupted", Thread.currentThread().getName());
        }
        logger.info("End");
    }
}
