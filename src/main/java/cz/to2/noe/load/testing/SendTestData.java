package cz.to2.noe.load.testing;

import cz.to2.noe.load.pipe.data.Message;
import cz.to2.noe.load.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.*;
import java.util.Hashtable;
import java.util.Properties;

/**
 * Created by pavelnovotny on 23.09.20.
 */
public class SendTestData implements ExceptionListener {

    private static Logger logger = LoggerFactory.getLogger(SendTestData.class);

    private Properties properties;

    public static void main(String[] args) throws InterruptedException, JMSException, NamingException, IOException {
        SendTestData jmsSend = new SendTestData();
        jmsSend.properties = loadProperties("beast-noe.properties");
        Properties iterProperties = loadProperties("beast-iteration.properties");
        Util.loadIteration = Integer.parseInt(iterProperties.getProperty("current.load.iteration"));
        jmsSend.prepareJmsTestData();
    }

    public void prepareJmsTestData() throws NamingException, JMSException, IOException {
        logger.info("Initializing JMS send");
        String jndiInitialFactory = properties.getProperty("jms.send.jndi.initial.factory");
        String jndiProviderUrl = properties.getProperty("jms.send.jndi.provider.url").split(",")[0];
        String connectionFactoryJndiName = properties.getProperty("jms.send.connection.factory.jndi.name");
        String queueName = "cipesb.bscs.queue.error";
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
        FileInputStream fis = new FileInputStream("/Users/pavelnovotny/projects/NoeLoad/data/load/noeResponse.txt");
        BufferedReader br = new BufferedReader(new InputStreamReader(fis));
        connection.start();
        String line;
        int i=0;
        logger.info("JMS send initialized");
        while ((line = br.readLine()) != null) {
            Message message = createMessage(line, br);
            message = transform(message);
            javax.jms.Message jmsMessage = session.createTextMessage(message.msg);
            if (++i % 2 == 0) {
                jmsMessage.setStringProperty("conversationId", message.crmOrderId );
            } else {
                jmsMessage.setStringProperty("conversationId", message.crmOrderId + "-" +"XXXXXXX" );
            }
            //logger.debug("conversationId: {} crmOrderId:'{}' xml:'{}'",jmsMessage.getStringProperty("conversationId"), message.crmOrderId, message.msg);
            logger.debug("{} {} {}", message.crmOrderId, message.msgSerialNumber, message.timeOffset);
            jmsMessage.setJMSType(message.jmsType);
            producer.send(jmsMessage);
        }
        br.close();
        producer.close();
        session.close();
        connection.close();
    }


    private Message createMessage(String line, BufferedReader br) throws IOException {
        Message message = new Message();
        message.parseJmsType(line);
        String line1;
        if ((line1 = br.readLine()) != null ) { //xml
            message.parseMsg(line1);
        }
        return message;
    }

    @Override
    public void onException(JMSException e) {
        e.printStackTrace();
        throw new RuntimeException(e);
    }

    private Message transform(Message message) {
        logger.trace("{} {} transform", message.crmOrderId, message.msgSerialNumber);
        message.msg = message.msg.replaceAll("\\$ITERATION\\$", String.format("_%03d", Util.loadIteration));
        return message;
    }

    private static Properties loadProperties(String propertyFileName){
        File propsFile=new File(propertyFileName);
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(propsFile));
        } catch (Throwable t) {
            throw new RuntimeException("Nelze nahrat properties :"+propsFile.getAbsolutePath(),t);
        }
        return properties;
    }

}
