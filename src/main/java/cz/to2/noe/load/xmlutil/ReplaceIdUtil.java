package cz.to2.noe.load.xmlutil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.util.*;


/**
 * Created by pavelnovotny on 09.09.20.
 */
public class ReplaceIdUtil {

    static  XPath xPath = XPathFactory.newInstance().newXPath();
    static Map<String, String> replaceMap = new HashMap<String, String>();
    public static Map<String, String> valuesOfInterest = new HashMap<String, String>();

    private static NodeList getNodeList(String path, Object node) throws XPathExpressionException {
        return (NodeList) xPath.compile(path).evaluate(node, XPathConstants.NODESET);
    }

    private static String getNodeTextContent(String path, Object node) throws XPathExpressionException {
        NodeList productIdList = getNodeList(path, node);
        return productIdList.item(0).getTextContent();
    }

    private static void buildOrderStructure(String requestContent, Map<String, String> replaceMap) throws Exception {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setValidating(false);
        dbFactory.setNamespaceAware(false);
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document orderReq = dBuilder.parse(new InputSource(new StringReader(requestContent)));
        Element root = orderReq.getDocumentElement();
        String[] splitRootElement = root.getNodeName().split(":",2);
        String rootElement = splitRootElement[splitRootElement.length-1];
        if ("OrderRequest".equals(rootElement)) {
            fillOrderMap(root,replaceMap);
        } else if ("ManageCustomerRequest".equals(rootElement)) {
            fillManageCustomerMap(root, replaceMap);
        } else if ("ManageCustomerAccountRequest".equals(rootElement)) {
            fillManageCustomerAccountMap(root, replaceMap);
        } else {
            throw new Exception ("unknown xml message");
        }

    }

    private static void fillManageCustomerMap(Element root, Map<String, String> replaceMap) throws XPathExpressionException {
        String correlationId = getNodeTextContent("/ManageCustomerRequest/requestHeader/correlationId", root);
        replaceMap.put(correlationId, correlationId+"\\$ITERATION\\$");
        //System.out.println(correlationId);
        valuesOfInterest.put("orderId", correlationId.split("\\.")[1]);

        String conversationId = getNodeTextContent("/ManageCustomerRequest/requestHeader/trackingInfo/conversationId/value", root);
        replaceMap.put(conversationId, conversationId+"\\$ITERATION\\$");
    }

    private static void fillManageCustomerAccountMap(Element root, Map<String, String> replaceMap) throws XPathExpressionException {
        String correlationId = getNodeTextContent("/ManageCustomerAccountRequest/requestHeader/correlationId", root);
        replaceMap.put(correlationId, correlationId + "\\$ITERATION\\$");
        //System.out.println(correlationId);
        valuesOfInterest.put("orderId", correlationId.split("\\.")[1]);

        String conversationId = getNodeTextContent("/ManageCustomerAccountRequest/requestHeader/trackingInfo/conversationId/value", root);
        replaceMap.put(conversationId, conversationId+"\\$ITERATION\\$");
    }

    private static void fillOrderMap(Element root, Map<String, String> replaceMap) throws XPathExpressionException {

        String requestId = getNodeTextContent("/OrderRequest/requestBody/id", root);
        replaceMap.put(requestId, requestId+"\\$ITERATION\\$");
        valuesOfInterest.put("orderId", requestId);

        String correlationId = getNodeTextContent("/OrderRequest/requestHeader/correlationId", root);
        replaceMap.put(correlationId, correlationId+"\\$ITERATION\\$");

        String conversationId = getNodeTextContent("/OrderRequest/requestHeader/trackingInfo/conversationId/value", root);
        replaceMap.put(conversationId, conversationId+"\\$ITERATION\\$");

        NodeList techOrders = getNodeList("/OrderRequest/requestBody/technicalOrders/technicalOrder", root);
        //System.out.println("techOrders list : " + techOrders.getLength());
        for (int i = 0; i < techOrders.getLength(); i++) {
            Node n = techOrders.item(i);
            String newTOId = String.format("%1$03d", i);
            //System.out.println(i + "_" + n.getNodeName());

            String oldTOId = getNodeTextContent("/OrderRequest/requestBody/technicalOrders/technicalOrder[" + (i + 1) + "]/id", n);
            //System.out.println(i + "_" + " : " + oldTOId);

            String orderType = getNodeTextContent("/OrderRequest/requestBody/technicalOrders/technicalOrder[" + (i + 1) + "]/orderType", n);
            //System.out.println(i + "_" + " : " + orderType);
            replaceMap.put(oldTOId, newTOId + "_" + orderType + "_" + requestId + "\\$ITERATION\\$");

            NodeList itemList = getNodeList("/OrderRequest/requestBody/technicalOrders/technicalOrder[" + (i + 1) + "]/items/item", n);
            for (int k = 0; k < itemList.getLength(); k++) {
                String newItemId = String.format("%1$03d", k);
                //System.out.println(i + "_" + k + "_" + itemList.item(k).getNodeName());

                String orderableCategory = getNodeTextContent("/OrderRequest/requestBody/technicalOrders/technicalOrder[" + (i + 1) + "]/items/item[" + (k + 1) + "]/product/orderableCategory", n);
                //System.out.println(i + "_" + k + "_" + " : " + orderableCategory);

                String itemId = getNodeTextContent("/OrderRequest/requestBody/technicalOrders/technicalOrder[" + (i + 1) + "]/items/item[" + (k + 1) + "]/id", n);
                //System.out.println(i + "_" + k + "_" + " : " + itemId);
                replaceMap.put(itemId, newTOId + "_" + newItemId + "_" + orderableCategory + "_" + requestId+ "\\$ITERATION\\$");

                String productId = getNodeTextContent("/OrderRequest/requestBody/technicalOrders/technicalOrder[" + (i + 1) + "]/items/item[" + (k + 1) + "]/product/id", n);
                //System.out.println(i + "_" + k + "P_" + " : " + productId);
                replaceMap.put(productId, newTOId + "_" + newItemId + "P_" + orderableCategory + "_" + requestId+ "\\$ITERATION\\$");
            }
        }
    }

    public static String replaceIds(String msg) throws Exception{
        replaceMap = new HashMap<String, String>();
        valuesOfInterest = new HashMap<String, String>();
        buildOrderStructure(msg, replaceMap);
        //replace identifiers
        for (Map.Entry<String, String> e : replaceMap.entrySet()) {
            msg = msg.replaceAll(">" + e.getKey() + "<", ">" + e.getValue() + "<");
        }
        return msg;
    }

    public static String replaceMappedIds(String msg) throws Exception{
        //replace identifiers
        for (Map.Entry<String, String> e : replaceMap.entrySet()) {
            msg = msg.replaceAll(">" + e.getKey() + "<", ">" + e.getValue() + "<");
        }
        return msg;
    }

}
