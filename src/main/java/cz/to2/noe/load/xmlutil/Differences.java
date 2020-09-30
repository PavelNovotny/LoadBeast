package cz.to2.noe.load.xmlutil;

import cz.to2.noe.load.pipe.data.Message;
import org.custommonkey.xmlunit.*;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.Diff;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.custommonkey.xmlunit.Difference;
import org.w3c.dom.Node;
import org.xmlunit.diff.ElementSelectors;
import org.xmlunit.xpath.JAXPXPathEngine;

import javax.xml.transform.Source;

/**
 * Created by pavelnovotny on 28.09.20.
 */
public class Differences {
    private static Logger logger = LoggerFactory.getLogger(Differences.class);

    private static List<String> xmlCompareExceptions;
    private static JAXPXPathEngine jaxpxPathEngine;

    static {
        Differences.xmlCompareExceptions = new ArrayList<String>();
        Differences.xmlCompareExceptions.add("requestHeader[1]/correlationId[1]/text()[1]");
        Differences.xmlCompareExceptions.add("requestHeader[1]/messageId[1]/text()[1]");
        jaxpxPathEngine = new JAXPXPathEngine();
    }

    public static void main (String[] args) throws Exception {
        File file1 = new File("/Users/pavelnovotny/projects/NOE-load/data/load/1.txt");
        File file2 = new File("/Users/pavelnovotny/projects/NOE-load/data/load/2.txt");
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file1), "UTF-8"));
        String xml1;
        xml1 = br.readLine();
        br.close();
        br = new BufferedReader(new InputStreamReader(new FileInputStream(file2), "UTF-8"));
        String xml2;
        xml2 = br.readLine();
        br.close();
        String exception = "/ManageProductRequest[1]/requestBody[1]/products[1]/product[1]/productCode[1]/text()[1]";
        String exception1 = "requestHeader[1]/correlationId[1]/text()[1]";
        List<String> exceptions = new ArrayList<String>();
        exceptions.add(exception);
        exceptions.add(exception1);
        List<Difference> differenceList = checkXMLEquals(xml1, xml2, exceptions);
        for (Difference difference: differenceList) {
            logger.debug("{}\n {}\n {}\n {}\n {}", difference.getDescription()
                    , difference.getControlNodeDetail().getXpathLocation()
                    , difference.getControlNodeDetail().getValue()
                    , difference.getTestNodeDetail().getXpathLocation()
                    , difference.getTestNodeDetail().getValue()
            );
        }
        logger.debug("===============================================================");
        for (Difference difference: differenceList) {
            logger.debug("{}", difference);
        }
        logger.debug("diff 1 ===============================================================");
        Diff diff = checkXMLEqualsVersion1(xml1, xml2, exceptions);
        if (diff.hasDifferences()) {
            Iterator d = diff.getDifferences().iterator();
            while(d.hasNext()) {
                logger.debug("{}", d.next());
            }
        }
    }

    public static String elementValue(String xml, String xpath) {
        Source source = Input.fromString(xml).build();
        Iterable<Node> iterNode;
        synchronized (jaxpxPathEngine) { //pro jistotu
            iterNode = jaxpxPathEngine.selectNodes(xpath, source);
        }
        Iterator<Node> it = iterNode.iterator();
        if (it.hasNext()) {
            Node node = it.next();
            return node.getFirstChild().getNodeValue();
        }
        return null;
    }


    public static Diff checkXMLEqualsVersion1(String expectedXML, String actualXML, List<String> doNotCheckList) throws Exception {
        Diff documentDiff = DiffBuilder.compare(expectedXML).withTest(actualXML)
                .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byName))
                .checkForSimilar().build();
        return documentDiff;

    }

    public static List<Difference> checkXMLEquals(String expectedXML, String actualXML, List<String> doNotCheckList) throws Exception {
        expectedXML = SortingUtil.sortNoeXml(expectedXML);
        actualXML = SortingUtil.sortNoeXml(actualXML);
        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreAttributeOrder(true);
        DetailedDiff diff = new DetailedDiff(XMLUnit.compareXML(expectedXML, actualXML));
        diff.overrideDifferenceListener(new DifferenceListener() {
            @Override
            public int differenceFound(Difference difference) {
                return difference.getDescription().equals("namespace prefix") ? DifferenceListener.RETURN_IGNORE_DIFFERENCE_NODES_IDENTICAL : DifferenceListener.RETURN_ACCEPT_DIFFERENCE;
            }

            @Override
            public void skippedComparison(Node node, Node node1) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

        });
        List<Difference> allDifferences = diff.getAllDifferences();
        List<Difference> selectedDifferences = new ArrayList<Difference>();
        if (allDifferences != null) {
            for (Difference difference : allDifferences) {
                boolean shoulBeIgnored = false;
                NodeDetail controlNodeDetail = difference.getControlNodeDetail();
                NodeDetail testNodeDetail = difference.getTestNodeDetail();
                for (String exceptionXPath : doNotCheckList) {
                    if (controlNodeDetail != null && exceptionXPath != null && controlNodeDetail.getXpathLocation() != null) {
                        if (controlNodeDetail.getXpathLocation().contains(exceptionXPath) || controlNodeDetail.getXpathLocation().replaceAll("(\\[\\d\\])", "").contains(exceptionXPath)) {
                            shoulBeIgnored = true;
                            break;
                        }
                    }
                    if (testNodeDetail != null && exceptionXPath != null && testNodeDetail.getXpathLocation() != null) {
                        if (testNodeDetail.getXpathLocation().contains(exceptionXPath) || testNodeDetail.getXpathLocation().replaceAll("(\\[\\d\\])", "").contains(exceptionXPath)) {
                            shoulBeIgnored = true;
                            break;
                        }
                    }
                }
                if (!shoulBeIgnored) {
                    selectedDifferences.add(difference);
                }
            }
        }
        return selectedDifferences;
    }

    public static boolean checkMessage(Message message, String jmsXml, String jmsType) {
        //todo některé zprávy se budou předbíhat a tato kontrola nestačí - např. ManageProductRequest určitě vybublá v testech
        //je zapotřebí vydefinovat doplňující podmínku, až se bude testovat
        if (!jmsType.equals(message.jmsType)) {
            return false;
        }
        //pokud zpráva vyhovuje, tak zkontrolujeme rozdíly
        try {
            List<Difference> differenceList = Differences.checkXMLEquals(message.msg, jmsXml, xmlCompareExceptions);
            if (differenceList.size() >0) {
                logger.info("{} {} jmsXml: {}", message.crmOrderId, message.msgSerialNumber, jmsXml);
                logger.info("{} {} expectedXml: {}", message.crmOrderId, message.msgSerialNumber, message.msg);
            }
            for (Difference difference: differenceList) {
                logger.info("{}", difference);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }



}
