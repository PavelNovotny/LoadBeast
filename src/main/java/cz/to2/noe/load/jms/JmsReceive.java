package cz.to2.noe.load.jms;

import cz.to2.noe.load.jms.data.JmsMessage;
import cz.to2.noe.load.pipe.InternalQueues;
import cz.to2.noe.load.interfaces.LoadRunnable;
import cz.to2.noe.load.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import javax.jms.Message;
import javax.jms.Queue;
import javax.naming.*;
import java.util.*;

/**
 * Created by pavelnovotny on 23.09.20.
 */
public class JmsReceive implements LoadRunnable, ExceptionListener {
    private static Logger logger = LoggerFactory.getLogger(JmsReceive.class);

    private Properties properties;
    private volatile boolean run = true;
    private MessageConsumer consumer;
    private InternalQueues internalQueues;
    private Map<String, String> params;
    private java.util.Queue<JmsMessage> jmsReceived;
    private static Object jmsReceiveMonitor;

    static {
        jmsReceiveMonitor = new Object();
    }

    public void receive() throws InterruptedException, NamingException, JMSException {
        //todo vyzkoušet jms na NOE
        logger.info("Initializing JMS");
        String jndiInitialFactory = properties.getProperty("jms.receive.jndi.initial.factory");
        String jndiProviderUrl = params.get("jms.receive.jndi.provider.url");
        String queueName = properties.getProperty("jms.receive.queue.name");
        String connectionFactoryJndiName = properties.getProperty("jms.receive.connection.factory.jndi.name");
        Connection connection;
        TextMessage message;
        Hashtable env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, jndiInitialFactory);
        env.put(Context.PROVIDER_URL, jndiProviderUrl);
        InitialContext context = new InitialContext(env);
        ConnectionFactory factory = (ConnectionFactory)context.lookup(connectionFactoryJndiName);
        connection = factory.createConnection();
        connection.setExceptionListener(this);
        Queue queue = (Queue) context.lookup(queueName);
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        consumer = session.createConsumer(queue);
        connection.start();
        logger.info("JMS initialized");
        Util.initBarrier.await();
        while (run) {
            Message m;
            synchronized (jmsReceiveMonitor) {
                m = consumer.receive(10000); //blocking until message received or consumer closed
                jmsReceiveMonitor.notify();
                logger.trace("wait");
                jmsReceiveMonitor.wait();
                logger.trace("wake");
            }
            synchronized(jmsReceived) {
                if (m != null && m instanceof TextMessage) {
                    Util.logActivity();
                    message = (TextMessage) m;
                    jmsReceived.add(new JmsMessage(m.getStringProperty("conversationId"), message.getJMSType(), message.getText()));
                    jmsReceived.notify();
                    logger.debug("{} {}", m.getStringProperty("conversationId"),  message.getJMSType(), message.getText());
                }
            }
        }
        connection.close();
    }

    @Override
    public void run() {
        logger.info("Start");
        try {
            receive();
        } catch (InterruptedException e) {
            logger.info("{} was interrupted", Thread.currentThread().getName());
        } catch (JMSException e) {
            e.printStackTrace();
        } catch (NamingException e) {
            e.printStackTrace();
        }
        logger.info("End");
    }

    @Override
    public void onException(JMSException e) {
        e.printStackTrace();
        throw new RuntimeException(e);
    }

    @Override
    public void end() {
        this.run = false;
        try {
            this.consumer.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void setInternalQueues(InternalQueues workingQueue) {
        this.internalQueues = workingQueue;
        this.jmsReceived = internalQueues.getJmsReceived();
    }

    @Override
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    @Override
    public void setParams(Map<String, String> params) {
        this.params = params;
    }
}
