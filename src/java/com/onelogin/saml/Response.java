package com.onelogin.saml;

import com.onelogin.AccountSettings;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.codec.binary.Base64;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Response {

        private Document xmlDoc;
        private AccountSettings accountSettings;
        private Certificate certificate;

        public Response(AccountSettings accountSettings) throws CertificateException {
                this.accountSettings = accountSettings;
                certificate = new Certificate();
                certificate.loadCertificate(this.accountSettings.getCertificate());
        }

        public void loadXml(String xml) throws ParserConfigurationException, SAXException, IOException {
                DocumentBuilderFactory fty = DocumentBuilderFactory.newInstance();
                fty.setNamespaceAware(true);
                DocumentBuilder builder = fty.newDocumentBuilder();
                ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());
                xmlDoc = builder.parse(bais);
        }


        public void loadXmlFromBase64(String response) throws ParserConfigurationException, SAXException, IOException {
                Base64 base64 = new Base64();
                byte [] decodedB = base64.decode(response);
                String decodedS = new String(decodedB);
                loadXml(decodedS);
        }

        public boolean isValid() throws Exception {
                NodeList nodes = xmlDoc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");

                if(nodes==null || nodes.getLength()==0){
                        throw new Exception("Can't find signature in document.");
                }

                X509Certificate cert = certificate.getX509Cert();
                DOMValidateContext ctx = new DOMValidateContext(cert.getPublicKey() , nodes.item(0));
                XMLSignatureFactory sigF = XMLSignatureFactory.getInstance("DOM");
                XMLSignature xmlSignature = sigF.unmarshalXMLSignature(ctx);

                return xmlSignature.validate(ctx);
        }

        public String getNameId() throws Exception {
                NodeList nodes = xmlDoc.getElementsByTagNameNS("urn:oasis:names:tc:SAML:2.0:assertion", "NameID");

                if(nodes.getLength()==0){
                        throw new Exception("No name id found in document");
                }

                return nodes.item(0).getTextContent();
        }
}