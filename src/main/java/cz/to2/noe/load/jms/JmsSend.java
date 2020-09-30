package cz.to2.noe.load.jms;

import javax.jms.*;
import javax.jms.Queue;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.*;

import cz.to2.noe.load.pipe.InternalQueues;
import cz.to2.noe.load.interfaces.LoadRunnable;
import cz.to2.noe.load.pipe.data.Message;
import cz.to2.noe.load.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by pavelnovotny on 23.09.20.
 */
public class JmsSend implements ExceptionListener, LoadRunnable {

    private static Logger logger = LoggerFactory.getLogger(JmsSend.class);

    private Properties properties;
    private volatile boolean run = true;
    private InternalQueues internalQueues;
    private java.util.Queue<Message> jmsSendMessages;
    private Map<String, String> params;


    public void send() throws JMSException, NamingException, InterruptedException {
        logger.info("Initializing JMS");
        String jndiInitialFactory = properties.getProperty("jms.send.jndi.initial.factory");
        String jndiProviderUrl = params.get("jms.send.jndi.provider.url");
        String queueName = properties.getProperty("jms.send.queue.name");
        String connectionFactoryJndiName = properties.getProperty("jms.send.connection.factory.jndi.name");
        Connection connection;
        Hashtable env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, jndiInitialFactory);
        env.put(Context.PROVIDER_URL, jndiProviderUrl);
        InitialContext context = new InitialContext(env);
        ConnectionFactory factory = (ConnectionFactory)context.lookup(connectionFactoryJndiName);
        connection = factory.createConnection();
        connection.setExceptionListener(this);
        Queue queue = (Queue) context.lookup(queueName);
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        MessageProducer producer = session.createProducer(queue);
        connection.start();
        logger.info("JMS initialized");
        Util.initBarrier.await();
        while (run) {
            synchronized (jmsSendMessages) {
                if (jmsSendMessages.size() > 0) {
                    jmsSendMessages.notify();
                }
                logger.trace("wait");
                jmsSendMessages.wait();
                logger.trace("wake");
            }
            Message message;
            synchronized (jmsSendMessages) {
                message = jmsSendMessages.poll();
            }
            if (message != null) {
                Util.logActivity();
                javax.jms.Message jmsMessage = session.createTextMessage(message.msg);
                jmsMessage.setJMSType(message.jmsType);
                producer.send(jmsMessage);
                logger.debug("{} {}", message.crmOrderId, message.msgSerialNumber);
            }
        }
        producer.close();
        session.close();
        connection.close();
    }


    @Override
    public void onException(JMSException e) {
        e.printStackTrace();
        throw new RuntimeException(e);
    }

    @Override
    public void run() {
        logger.info("Start");
        try {
            send();
        } catch (JMSException e) {
            e.printStackTrace();
        } catch (NamingException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            logger.info("{} was interrupted", Thread.currentThread().getName());
        }
        logger.info("End");
    }

    @Override
    public void setInternalQueues(InternalQueues workingQueue) {
        this.internalQueues = workingQueue;
        this.jmsSendMessages = internalQueues.getJmsSend();
    }

    @Override
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    @Override
    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    @Override
    public void end() {
        this.run = false;
        synchronized (jmsSendMessages) {
            jmsSendMessages.notify();
        }
    }

}
