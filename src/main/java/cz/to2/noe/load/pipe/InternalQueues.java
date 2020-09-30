package cz.to2.noe.load.pipe;

import cz.to2.noe.load.jms.data.JmsMessage;
import cz.to2.noe.load.pipe.data.comparator.MessageTimeComparator;
import cz.to2.noe.load.util.Util;
import cz.to2.noe.load.xmlutil.Differences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import cz.to2.noe.load.pipe.data.Message;
import cz.to2.noe.load.pipe.data.Order;

import java.util.*;

/**
 * Created by pavelnovotny on 10.09.20.
 */
public class InternalQueues {
    private static Logger logger = LoggerFactory.getLogger(InternalQueues.class);

    private Map<Order, Queue<Message>> noeOrders;
    private Map<String, Order> orderMap;
    private Queue<JmsMessage> jmsReceived;
    private Queue<Message> scheduledDelays;
    private Queue<Message> activeDelays;
    private Queue<Message> jmsSend;


    public InternalQueues() {
        //input from read load
        noeOrders = new HashMap<Order, Queue<Message>>();
        //to quickly find order via crmOrderId
        orderMap = new HashMap<String, Order>();
        //input from jms receive threads
        jmsReceived =  new LinkedList<JmsMessage>(); //FIFO
        //internal output, based on read noe orders and/or received messages
        scheduledDelays = new PriorityQueue<Message>(11,new MessageTimeComparator());
        activeDelays = new PriorityQueue<Message>(11,new MessageTimeComparator());
        //internal output after delay for jms send
        jmsSend =  new LinkedList<Message>(); //FIFO

    }

    public void sendMessage(Order order) {
        Queue<Message> msgQueue;
        synchronized (noeOrders) {
            msgQueue = noeOrders.get(order);
        }
        if (msgQueue == null) {
            logger.error("Send, no message queue {}", order.crmOrderId);
            return;
        }
        synchronized (msgQueue) {
            Message message = msgQueue.peek();
            if (message != null) {
                if (message.jmsIsToNoe) {
                    message = msgQueue.poll();
                    logger.trace("determine correlationId");
                    message = transformSendMessage(message, order);
                    logger.debug("Send {} {}",message.crmOrderId, message.msgSerialNumber);
                    Message activeDelayMessage;
                    synchronized (activeDelays) {
                        activeDelayMessage = activeDelays.peek();
                    }
                    synchronized (scheduledDelays) {
                        scheduledDelays.add(message);
                        if (activeDelayMessage == null) {
                            scheduledDelays.notify();
                        } else {
                            if (message.timeOffset > activeDelayMessage.timeOffset) {
                                scheduledDelays.notify();
                            }
                        }
                    }
                    message = msgQueue.peek();
                    if (message != null && message.jmsIsToNoe) { //recursively send all subsequent toNoe, until fromNoe or empty queue
                        sendMessage(order);
                    }
                } else {
                    logger.error("Send, message is fromNoe {} {}", message.crmOrderId, message.msgSerialNumber);
                }
            } else {
                logger.error("Send, empty message queue {}", order.crmOrderId);
            }
        }
    }

    public void receiveMessage(String crmOrderId, String jmsXml, String jmsType) {
        Order receiveOrder = orderMap.get(crmOrderId);
        Queue<Message> msgQueue = noeOrders.get(receiveOrder);
        if (msgQueue == null) {
            logger.error("Receive, no message queue {}", crmOrderId);
            return;
        }
        synchronized (msgQueue) {
            Message message;
            if ((message = findMessage(msgQueue, jmsXml, jmsType)) !=null) {
                logger.debug("Receive {} {}",message.crmOrderId, message.msgSerialNumber);
                receiveMessage(receiveOrder, message, jmsXml);
                message = msgQueue.peek();
                if (message != null && message.jmsIsToNoe) {
                    sendMessage(orderMap.get(crmOrderId));
                }
            } else {
                logger.error("Receive, no matching message fromNoe. {} {} {}", msgQueue.size(), crmOrderId, jmsType);
            }
        }
    }

    private Message findMessage(Queue<Message> msgQueue, String jmsXml, String jmsType) {
        Iterator value = msgQueue.iterator();
        while (value.hasNext()) {
            Message message = (Message) value.next();
            if (message.jmsIsToNoe) {
                break;
            }
            if (Differences.checkMessage(message, jmsXml, jmsType)) {
                msgQueue.remove(message);
                return message;
            }
        }
        return null;
    }


    private void receiveMessage(Order order, Message message, String jmsXml) {
        String jmsCorrelationId = Differences.elementValue(jmsXml, "//*[local-name()='correlationId']");
        String storedCorrelationId = Differences.elementValue(message.msg, "//*[local-name()='correlationId']");
        logger.trace("correlationId fromNoe {}",jmsCorrelationId);
        logger.trace("correlationId stored  {}",storedCorrelationId);
        if (!storedCorrelationId.equals(jmsCorrelationId)) {
            order.translateCorrelationId.put(storedCorrelationId, jmsCorrelationId);
        }
    }

    public void put(Order order, Queue<Message> msgQueue) {
        synchronized (noeOrders) {
            noeOrders.put(order, msgQueue);
        }
        synchronized (orderMap) {
            orderMap.put(order.crmOrderId, order);
        }
    }

    public Queue<Message> get(Order order) {
        synchronized (noeOrders) {
            return noeOrders.get(order);
        }
    }

    public int getOrdersSize() {
        synchronized (noeOrders) {
            return noeOrders.size();
        }
    }

    public void ordersStatistics() {
        synchronized (noeOrders) {
            for (Order order : noeOrders.keySet()) {
                Queue<Message> queue = noeOrders.get(order);
                if (queue == null) {
                    logger.info("{} null messages queue", order.crmOrderId);
                    continue;
                }
                if (queue.size() > 0) {
                    logger.info("{} active messages {}", order.crmOrderId, queue.size());
                } else {
                    logger.info("{} finished", order.crmOrderId, queue.size());
                }
            }
        }
    }


    public Queue<Message> getJmsSend() {
        return jmsSend;
    }

    public int getJmsSendSize() {
        synchronized (jmsSend) {
            return jmsSend.size();
        }
    }

    public Queue<Message> getScheduledDelays() {
        return scheduledDelays;
    }

    public int getScheduledDelaysSize() {
        synchronized (scheduledDelays) {
            return scheduledDelays.size();
        }
    }

    public Queue<JmsMessage> getJmsReceived() {
        return jmsReceived;
    }

    public int getJmsReceivedSize() {
        synchronized (jmsReceived) {
            return jmsReceived.size();
        }
    }

    public Queue<Message> getActiveDelays() {
        return activeDelays;
    }

    public int getActiveDelaysSize() {
        synchronized (activeDelays) {
            return activeDelays.size();
        }
    }

    public void addMessage(Queue<Message> messages, Message message) {
        Message transformedMessage = transformReadMessage(message);
        synchronized (messages) {
            messages.add(transformedMessage);
        }
    }

    private Message transformReadMessage(Message message) {
        logger.trace("{} {} transformReadMessage", message.crmOrderId, message.msgSerialNumber);
        message.msg = message.msg.replace("$ITERATION$", String.format("_%03d", Util.loadIteration));
        message.correlationId = Differences.elementValue(message.msg, "//*[local-name()='correlationId']");
        return message;
    }

    private Message transformSendMessage(Message message, Order order) {
        String correlationId = order.translateCorrelationId.get(message.correlationId);
        if (correlationId != null) {
            message.msg = message.msg.replace(message.correlationId, correlationId);
        }
        return message;
    }

}
