package cz.to2.noe.load.file;

import cz.to2.noe.load.interfaces.LoadRunnable;
import cz.to2.noe.load.pipe.InternalQueues;
import cz.to2.noe.load.jms.data.JmsTypeMap;
import cz.to2.noe.load.pipe.data.Message;
import cz.to2.noe.load.pipe.data.comparator.MessageSerialComparator;
import cz.to2.noe.load.pipe.data.Order;
import cz.to2.noe.load.util.Util;
import cz.to2.noe.load.xmlutil.ReplaceIdUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/**
 * Created by pavelnovotny on 07.09.20.
 */
public class ReadLoad implements LoadRunnable {

    private Properties properties;
    private JmsTypeMap jmsTypeMap;
    private long firstUnixEpoch = 0l;
    private int orderCount = 0;
    private InternalQueues internalQueues;
    private static Logger logger = LoggerFactory.getLogger(ReadLoad.class);
    private volatile boolean run = true;

    public ReadLoad() {
        this.jmsTypeMap = new JmsTypeMap();
    }

    public void create() throws IOException, InterruptedException {
        File loadFile = new File(properties.getProperty("load.file"));
        FileOutputStream fos = new FileOutputStream(loadFile);
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos, StandardCharsets.UTF_8));
        String dateFrom = (String) properties.get("date.from");
        String dateTo = (String) properties.get("date.to");
        String connectString = (String) properties.get("db.connect.string");
        String user = (String) properties.get("db.user");
        String passwd = (String) properties.get("db.passwd");
        try{
            Class.forName("oracle.jdbc.driver.OracleDriver");
            Connection con=DriverManager.getConnection(connectString, user, passwd);
            Statement stmt=con.createStatement();
            String query = String.format("select distinct id,log_key " +
                    "from noe_repo.log_table a " +
                    "where a.order_id is not null " +
                    " and log_date > to_date('%s', 'DD.MM.YYYY')" +
                    " and log_date < to_date('%s', 'DD.MM.YYYY')+1" +
                    //" and log_key = '200909180nkgqay6kc5'" +
                    " order by a.id", dateFrom, dateTo);
            logger.debug(query);
            ResultSet rs=stmt.executeQuery(query);
            Statement stmt1=con.createStatement();
            Map<String, Boolean> processLogKey = new LinkedHashMap<String, Boolean>();
            while(rs.next()) {
                String logKey = rs.getString(2);
                if (processLogKey.get(logKey) == null)  {
                    processLogKey.put(logKey, Boolean.TRUE);
                } else {
                    processLogKey.put(logKey, Boolean.FALSE);
                }
            }
            for (String logKey : processLogKey.keySet()) {
                boolean canBeProcessed = processLogKey.get(logKey);
                logger.debug("{} {}", logKey, canBeProcessed);
                if (canBeProcessed) {
                    processMessages(stmt1, logKey, bw);
                }
            }
            con.close();
            bw.close();
        } catch(Exception e){
            e.printStackTrace();
            logger.error(e.toString());
        }
    }

    public void processMessages(Statement stmt1, String logKey, BufferedWriter bw ) throws Exception {
        ResultSet rs1=stmt1.executeQuery(String.format("select id, log_message, order_id" +
                ",to_number((cast(log_date as date) - date '1970-01-01')*86400000) + to_number(to_char(log_date, 'FF3')) unix"+
                " from noe_repo.log_table a" +
                " where a.log_key = '%s'" +
                " order by a.id", logKey));
        int count=0;
        orderCount++;
        String orderId=null;
        while(rs1.next()) {
            if (firstUnixEpoch == 0l) {
                firstUnixEpoch = rs1.getLong(4);
            }
            String xml = rs1.getString(2);
            count++;
            JmsTypeMap.JmsInfo jmsInfo = jmsTypeMap.getJmsInfo(xml);
            if (jmsInfo !=null) { //else internal message
                String direction = jmsInfo.toNoe ? "toNoe" : "fromNoe";
                long unixEpoch = rs1.getLong(4) - firstUnixEpoch;
                String convertedXml;
                if (count==1) {
                    convertedXml = ReplaceIdUtil.replaceIds(xml);
                    orderId = ReplaceIdUtil.valuesOfInterest.get("orderId");
                } else {
                    convertedXml = ReplaceIdUtil.replaceMappedIds(xml);
                }
                bw.write(String.format("%s.%s.%s.%d.%03d.%d.jmsType:%s\n", logKey, orderId, direction, orderCount, count, unixEpoch, jmsInfo.JMSType));
                bw.write(String.format("%s.%s.%s.%d.%03d.%d.message:%s", logKey, orderId, direction, orderCount, count, unixEpoch, convertedXml));
            }
        }

    }

    public void read() throws Exception {
        File loadFile = new File(properties.getProperty("load.file"));
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(loadFile), "UTF-8"));
        readOrders(br);
        br.close();
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

    private Order initOrder(Message message) {
        Order order = new Order(message);
        Queue<Message> msgQueue = new PriorityQueue<Message>(11,new MessageSerialComparator());
        internalQueues.addMessage(msgQueue, message);
        internalQueues.put(order, msgQueue);
        return order;
    }

    private void readOrders(BufferedReader br) throws IOException {
        Util.initBarrier.await();
        String line, logKey=null;
        Order order = null;
        if ((line = br.readLine()) != null ) {
            Message message = createMessage(line, br);
            order = initOrder(message);
            logKey = message.noeKeyLog; //first
        }
        while ((line = br.readLine()) != null && run ) { //line with jmsType
            Message message = createMessage(line, br);
            if(logKey.equals(message.noeKeyLog)) {
                internalQueues.addMessage(internalQueues.get(order), message);
            } else { //first message in order
                try {
                    sleep(message);
                } catch (InterruptedException e) {
                    br.close();
                    logger.info("{} was interrupted", Thread.currentThread().getName());
                    return;
                }
                internalQueues.sendMessage(order);
                order = initOrder(message);
                logKey = message.noeKeyLog;
            }
        }
        internalQueues.sendMessage(order); //last without sleep, minor issue
    }

    private void sleep(Message message) throws InterruptedException {
        long waitTime = Util.getWaitTime(message.timeOffset);
        logger.debug("{} {} sleep {}",message.crmOrderId, message.msgSerialNumber, waitTime);
        Thread.sleep(waitTime);
    }

    @Override
    public void setInternalQueues(InternalQueues workingQueue) {
        this.internalQueues = workingQueue;
    }

    @Override
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    @Override
    public void setParams(Map<String, String> params) {
    }

    @Override
    public void end() {
        this.run = false;
    }

    @Override
    public void run() {
        logger.info("Start");
        try {
            read();
        } catch (Exception e) {
            e.printStackTrace();
        }
        logger.info("End");
    }
}
