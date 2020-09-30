package cz.to2.noe.load.xmlutil;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.naming.directory.Attribute;

/**
 * Created by pavelnovotny on 12.04.19.
 */
public class SaxHandler extends DefaultHandler{
    private boolean startConsumerId = false;
    private String consumerId = null;
    private String rootElement = "";
    private String namespace = null;

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (localName.equalsIgnoreCase("consumerId")) {
            startConsumerId=true;
        } else {
            startConsumerId=false;
        }
        if ("".equals(rootElement)
                && !localName.equalsIgnoreCase("Envelope")
                && !localName.equalsIgnoreCase("Header")
                && !localName.startsWith("Trace")
                && !localName.startsWith("TRC_")
                && !localName.equalsIgnoreCase("messageId")
                && !localName.equalsIgnoreCase("sign")
                && !localName.equalsIgnoreCase("applicationName")
                && !localName.equalsIgnoreCase("Action")
                && !localName.equalsIgnoreCase("ReplyTo")
                && !localName.equalsIgnoreCase("Address")
                && !localName.equalsIgnoreCase("To")
                && !localName.equalsIgnoreCase("Security")
                && !localName.equalsIgnoreCase("Timestamp")
                && !localName.equalsIgnoreCase("Created")
                && !localName.equalsIgnoreCase("Expires")
                && !localName.equalsIgnoreCase("Body")) {
            rootElement = localName;
            namespace = uri;
            //System.out.println(String.format("%s", rootElement));
        }
        //System.out.println(String.format("%s %s %s",uri,localName, qName));
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (startConsumerId) {
            consumerId = new String(ch, start, length);
            startConsumerId=false;
        }
    }

    public String getConsumerId() {
        return consumerId;
    }

    public String getRootElement() {
        return rootElement;
    }

    public String getNamespace() {
        return namespace;
    }

    public void init() {
        startConsumerId = false;
        consumerId = null;
        rootElement = "";
        namespace = null;
    }


}
