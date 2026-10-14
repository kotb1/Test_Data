package org.example.Creations;

import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class XML
{
    protected Document xml_document;

    public XML(String xml2) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException, TransformerException
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        String xml = xml2.toString().replace("\uFEFF", "").trim();
        Document doc = builder.parse(new InputSource(new StringReader(xml.trim())));
        this.xml_document= doc;
    }
    public String get_value_by_attribute(String attribute) throws XPathExpressionException {
        try
        {
            Node attribute_Node = this.xml_document.getElementsByTagName(attribute).item(0);
            String value = attribute_Node.getTextContent();
            return value;
        }catch(Exception e1)
        {
            XPath xPath = XPathFactory.newInstance().newXPath();
            String expression = String.format("//addList[attCode='%s']/attValue", attribute);
            Node valueNode = (Node) xPath.evaluate(expression, this.xml_document, XPathConstants.NODE);
            return valueNode.getTextContent();
        }
    }
    public String randomize_OM_Order_ID() throws TransformerException {
        Node omOrderIDNode = this.xml_document.getElementsByTagName("OMOrderID").item(0);
        Random random = new Random();
        long randomNumber = 1_000_000_000L + (long)(random.nextDouble() * 9_000_000_000L);
        String randomNumberString = String.valueOf(randomNumber);
        omOrderIDNode.setTextContent(randomNumberString);
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(this.xml_document), new StreamResult(writer));
        String updatedXML = writer.getBuffer().toString();
        return updatedXML;
    }
    public String update_complain_number() throws TransformerException {
        Node omOrderIDNode = this.xml_document.getElementsByTagName("complainNo").item(0);
        Random random = new Random();
        long randomNumber = 1_000_000_000L + (long)(random.nextDouble() * 9_000_000_000L);
        String randomNumberString = String.valueOf(randomNumber);
        omOrderIDNode.setTextContent(randomNumberString);
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(this.xml_document), new StreamResult(writer));
        String updatedXML = writer.getBuffer().toString();
        return updatedXML;
    }
    public String send_creation_request_installation(String request_body, String URL) throws IOException {
        URL url = new URL(URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/xml");

        try (DataOutputStream dos = new DataOutputStream(conn.getOutputStream())) {
            dos.write(request_body.getBytes(StandardCharsets.UTF_8));
        }

        int statusCode = conn.getResponseCode();

        InputStream responseStream = statusCode >= 400
                ? conn.getErrorStream()
                : conn.getInputStream();

        if (responseStream == null) {
            throw new IOException("Empty response. HTTP status: " + statusCode);
        }

        StringBuilder responseBuilder = new StringBuilder();

        try (BufferedReader bf = new BufferedReader(
                new InputStreamReader(responseStream, StandardCharsets.UTF_8))) {
            String responseLine;
            while ((responseLine = bf.readLine()) != null) {
                responseBuilder.append(responseLine);
            }
        }

        String response = responseBuilder.toString();
        //System.out.println("HTTP Status: " + statusCode);
        //System.out.println("Response: " + response);

        if (statusCode >= 400) {
            throw new IOException("Request failed: " + response);
        }

        JSONObject json = new JSONObject(response);

        if (!"Ok".equals(json.optString("state"))) {
            throw new IOException("Order creation failed: " + response);
        }

        String workOrderNo = json.getString("workOrderNo");
        System.out.println("Work Order Number: " + workOrderNo);

        return workOrderNo;
    }
    public String send_creation_request_Maintenance(String request_body, String URL) throws Exception {
        // SOAP endpoint URL
        URL url = new URL(URL);
        //System.out.println(request_body);
        //System.out.println(URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);

        // SOAP requires XML
        conn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");

        // Send SOAP Request Body
        try (DataOutputStream dos = new DataOutputStream(conn.getOutputStream())) {
            dos.writeBytes(request_body);
        }

        // Read response (XML)
        BufferedReader bf = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder responseBuilder = new StringBuilder();
        String line;
        while ((line = bf.readLine()) != null) {
            responseBuilder.append(line);
        }
        bf.close();

        String responseXml = responseBuilder.toString();

        // مثال: لو الـ response فيه <workOrderNo>12345</workOrderNo>
        String workOrderNo= parseSoapResponse(responseXml);
        System.out.println(workOrderNo);
        return workOrderNo;
    }
    public String parseSoapResponse(String responseXml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true); // مهم عشان فيه namespaces
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(responseXml));
        Document doc = builder.parse(is);

        // هنجيب الـ "Result" من أي namespace
        NodeList resultNodes = doc.getElementsByTagNameNS("*", "Result");
        if (resultNodes.getLength() > 0) {
            return resultNodes.item(0).getTextContent();
        } else {
            return null; // لو مفيش Result
        }
    }
}
