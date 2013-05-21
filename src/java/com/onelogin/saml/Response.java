package com.onelogin.saml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.xml.crypto.MarshalException;

import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.onelogin.AccountSettings;
import org.apache.commons.codec.binary.Base64;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * A SAML Authentication Response.
 */
public class Response {
    private final Certificate certificate;
    private Document xmlDoc;
    private boolean docIsValid;

    /**
     * Constructs a Response instance with a given IdP cert for checking the signature from the IdP.
     */
    public Response(AccountSettings account) throws CertificateException {
        this.certificate = new Certificate(account.getCertificate());
    }

    /**
     * Loads this response from a string containing unencoded xml.
     */
    public void loadXml(String xml) throws ParserConfigurationException, SAXException, IOException, MarshalException, XMLSignatureException {
        DocumentBuilderFactory fty = DocumentBuilderFactory.newInstance();
        fty.setNamespaceAware(true);
        DocumentBuilder builder = fty.newDocumentBuilder();
        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());
        xmlDoc = builder.parse(bais);
        checkDocValid();
    }

    /**
     * Loads this response from a string containing base64 encoded xml.
     */
    public void loadXmlFromBase64(String response) throws ParserConfigurationException, SAXException, IOException, MarshalException, XMLSignatureException {
        Base64 base64 = new Base64();
        byte[] decodedB = base64.decode(response);
        String decodedS = new String(decodedB);
        loadXml(decodedS);
    }

    /**
     * Checks a loaded doc to determine if the signature was valid, and remembers that. That check is expensive, so
     * the result is cached in a boolean field.
     */
    private void checkDocValid() throws SAXException, MarshalException, XMLSignatureException {
        NodeList nodes = xmlDoc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");

        if (nodes == null || nodes.getLength() == 0) {
            throw new SAXException("Can't find signature in document.");
        }

        if (setIdAttributeExists()) {
            tagIdAttributes(xmlDoc);
        }

        X509Certificate cert = certificate.getX509Cert();
        DOMValidateContext ctx = new DOMValidateContext(cert.getPublicKey(), nodes.item(0));
        XMLSignatureFactory sigF = XMLSignatureFactory.getInstance("DOM");
        XMLSignature xmlSignature = sigF.unmarshalXMLSignature(ctx);

        docIsValid = xmlSignature.validate(ctx);
    }

    public boolean isValid() {
        return docIsValid;
    }

    public String getNameId() throws Exception {
        NodeList nodes = xmlDoc.getElementsByTagNameNS("urn:oasis:names:tc:SAML:2.0:assertion", "NameID");

        if (nodes.getLength() == 0) {
            throw new Exception("No name id found in document");
        }

        return nodes.item(0).getTextContent();
    }

    private void tagIdAttributes(Document xmlDoc) {
        NodeList nodeList = xmlDoc.getElementsByTagName("*");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                if (node.getAttributes().getNamedItem("ID") != null) {
                    ((Element) node).setIdAttribute("ID", true);
                }
            }
        }
    }

    private boolean setIdAttributeExists() {
        for (Method method : Element.class.getDeclaredMethods()) {
            if (method.getName().equals("setIdAttribute")) {
                return true;
            }
        }
        return false;
    }
}
