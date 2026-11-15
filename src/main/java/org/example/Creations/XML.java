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

/**
 * Wraps one request template (an XML / SOAP body) for a single work order type.
 * <p>
 * Each call to {@link #randomize_OM_Order_ID()} or {@link #update_complain_number()}
 * puts a new random ID into the template so every request sent is unique.
 * The send_* methods post the body to the server and return the created work order number.
 */
public class XML
{
    private static final Random RANDOM = new Random();

    /** The parsed request template. It is modified in place every time an ID is randomized. */
    protected Document xml_document;
    public String Request_type;


    public String get_request_type(){ return this.Request_type;}
    public XML(String xml2, String request_type) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException, TransformerException
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        // Remove a BOM (byte order mark) if the template was copied from a file that had one
        String xml = xml2.replace("﻿", "").trim();
        this.xml_document = builder.parse(new InputSource(new StringReader(xml)));
        this.Request_type = request_type;
    }

    /**
     * Returns the text of the first tag with the given name.
     * If there is no such tag, looks it up as an attCode inside an addList entry instead.
     */
    public String get_value_by_attribute(String attribute) throws XPathExpressionException {
        Node attribute_Node = this.xml_document.getElementsByTagName(attribute).item(0);
        if (attribute_Node != null)
        {
            return attribute_Node.getTextContent();
        }
        XPath xPath = XPathFactory.newInstance().newXPath();
        String expression = String.format("//addList[attCode='%s']/attValue", attribute);
        Node valueNode = (Node) xPath.evaluate(expression, this.xml_document, XPathConstants.NODE);
        return valueNode == null ? null : valueNode.getTextContent();
    }

    /** Sets a new random OMOrderID (used by installation orders) and returns the updated XML. */
    public String randomize_OM_Order_ID() throws TransformerException {
        return randomize_tag("OMOrderID");
    }

    /** Sets a new random complainNo (used by maintenance orders) and returns the updated XML. */
    public String update_complain_number() throws TransformerException {
        return randomize_tag("complainNo");
    }

    /** Puts a random 10-digit number into the first tag with the given name and returns the updated XML. */
    private String randomize_tag(String tag) throws TransformerException {
        Node node = this.xml_document.getElementsByTagName(tag).item(0);
        if (node == null)
        {
            throw new IllegalStateException("Template has no <" + tag + "> tag");
        }
        node.setTextContent(String.valueOf(random_10_digit_number()));
        return to_xml_string();
    }

    /** Returns a random number between 1,000,000,000 and 9,999,999,999. */
    static long random_10_digit_number() {
        return 1_000_000_000L + (long) (RANDOM.nextDouble() * 9_000_000_000L);
    }

    /** Converts the current document back to an XML string. */
    public String to_xml_string() throws TransformerException {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(this.xml_document), new StreamResult(writer));
        return writer.toString();
    }

    /**
     * Sends an installation order (REST, XML body) and returns the work order number.
     *
     * @throws IOException if the request fails or the server does not answer with state "Ok"
     */
    public String send_creation_request_installation(String request_body, String URL) throws IOException {
        String response = post(request_body, URL, "application/xml");
        String workOrderNo = parse_installation_response(response);
        System.out.println("Work Order Number: " + workOrderNo);
        return workOrderNo;
    }

    /**
     * Sends a maintenance order (SOAP) and returns the work order number,
     * or null if the response has no Result tag.
     */
    public String send_creation_request_Maintenance(String request_body, String URL) throws Exception {
        String responseXml = post(request_body, URL, "text/xml; charset=utf-8");
        String workOrderNo = parseSoapResponse(responseXml);
        System.out.println("Work Order Number: " + workOrderNo);
        return workOrderNo;
    }

    /**
     * Posts the body to the URL and returns the response text.
     *
     * @throws IOException if the server answers with an HTTP status of 400 or higher
     */
    static String post(String request_body, String URL, String content_type) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(URL).openConnection();
        conn.setConnectTimeout(30_000);   // give up if the server cannot be reached in 30s
        conn.setReadTimeout(120_000);     // give up if the server does not answer in 2 minutes
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", content_type);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(request_body.getBytes(StandardCharsets.UTF_8));
        }

        int statusCode = conn.getResponseCode();
        // Error responses are read from a different stream than successful ones
        InputStream responseStream = statusCode >= 400 ? conn.getErrorStream() : conn.getInputStream();
        if (responseStream == null) {
            throw new IOException("Empty response. HTTP status: " + statusCode);
        }

        StringBuilder responseBuilder = new StringBuilder();
        try (BufferedReader bf = new BufferedReader(new InputStreamReader(responseStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = bf.readLine()) != null) {
                responseBuilder.append(line);
            }
        }
        String response = responseBuilder.toString();

        if (statusCode >= 400) {
            throw new IOException("Request failed (HTTP " + statusCode + "): " + response);
        }
        return response;
    }

    /**
     * Reads the work order number from an installation JSON response,
     * for example {"state":"Ok","workOrderNo":"123"}.
     *
     * @throws IOException if state is not "Ok"
     */
    public static String parse_installation_response(String response) throws IOException {
        JSONObject json = new JSONObject(response);
        if (!"Ok".equals(json.optString("state"))) {
            throw new IOException("Order creation failed: " + response);
        }
        return json.getString("workOrderNo");
    }

    /**
     * Reads the work order number from a SOAP response.
     * It is the text of the first Result tag, whatever its namespace.
     * Returns null if there is no Result tag.
     */
    public String parseSoapResponse(String responseXml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true); // needed because SOAP responses use namespaces
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(responseXml)));

        NodeList resultNodes = doc.getElementsByTagNameNS("*", "Result");
        if (resultNodes.getLength() > 0) {
            return resultNodes.item(0).getTextContent();
        }
        return null;
    }
}
