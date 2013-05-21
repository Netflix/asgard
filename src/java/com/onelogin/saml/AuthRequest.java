package com.onelogin.saml;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.codec.binary.Base64;

import com.onelogin.AccountSettings;
import com.onelogin.AppSettings;
import org.apache.commons.lang.StringUtils;

/**
 * A SAML authentication request from an SP to an IdP.
 */
public class AuthRequest {
	
	private final String id;
	private final String issueInstant;
	private final AppSettings appSettings;

	public static final int base64 = 1;
    public static final int url = 2;

    /**
     * Constructs an authentication request for a given SP with its application settings. Currently
     * does not need to know about the account settings to form this request.
     */
	public AuthRequest(AppSettings appSettings, AccountSettings accountSettings) {
		this.appSettings = appSettings;
        this.id = "_" + UUID.randomUUID().toString();
		SimpleDateFormat simpleDf = new SimpleDateFormat("yyyy-MM-dd'T'H:mm:ss");
        this.issueInstant = simpleDf.format(new Date());
	}

    /**
     * Returns a ByteArrayOutputStream containing the raw unencoded request XML.
     */
	public ByteArrayOutputStream getRawRequestStream() throws XMLStreamException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();		
		XMLOutputFactory factory = XMLOutputFactory.newInstance();
		XMLStreamWriter writer = factory.createXMLStreamWriter(baos);
					
		writer.writeStartElement("samlp", "AuthnRequest", "urn:oasis:names:tc:SAML:2.0:protocol");
		writer.writeNamespace("samlp","urn:oasis:names:tc:SAML:2.0:protocol");
        writer.writeAttribute("Version", "2.0");
        writer.writeAttribute("ProtocolBinding", "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect");
		writer.writeAttribute("ID", id);
		writer.writeAttribute("IssueInstant", this.issueInstant);
		writer.writeAttribute("AssertionConsumerServiceURL", this.appSettings.getAssertionConsumerServiceUrl());
		
		writer.writeStartElement("saml","Issuer","urn:oasis:names:tc:SAML:2.0:assertion");
		writer.writeNamespace("saml","urn:oasis:names:tc:SAML:2.0:assertion");
		writer.writeCharacters(this.appSettings.getIssuer());
		writer.writeEndElement();

        // nameid-format:unspecified tells IdP to use default. Might want :transient or :persistent, plus :emailAddress
		writer.writeStartElement("samlp", "NameIDPolicy", "urn:oasis:names:tc:SAML:2.0:protocol");
        writer.writeAttribute("Format", "urn:oasis:names:tc:SAML:2.0:nameid-format:unspecified");
		writer.writeAttribute("AllowCreate", "true");
		writer.writeEndElement();

		writer.writeStartElement("samlp", "RequestedAuthnContext", "urn:oasis:names:tc:SAML:2.0:protocol");
		writer.writeAttribute("Comparison", "exact");
		writer.writeEndElement();
		
		writer.writeStartElement("saml","AuthnContextClassRef","urn:oasis:names:tc:SAML:2.0:assertion");
		writer.writeNamespace("saml", "urn:oasis:names:tc:SAML:2.0:assertion");
		writer.writeCharacters("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport");
		writer.writeEndElement();

        // TODO(CQ): where/how do we sign this if that is needed?

		writer.writeEndElement();
		writer.flush();		
		
		return baos;
	}

    /**
     * Returns a String containing the request encoded in base64 encoding and/or then in URL encoding.
     */
    public String getRequestEncoded(int format) throws XMLStreamException {
        byte[] reqB = getRawRequestStream().toByteArray();
        System.out.println("SAMLRequestRaw: " + new String(reqB, Charset.forName("UTF-8")));
        if ((format & base64) != 0) {
            reqB = Base64.encodeBase64Chunked(reqB);
        }
        String reqS = new String(reqB, Charset.forName("UTF-8"));
        if ((format & url) != 0) {
            try {
                reqS = StringUtils.remove(reqS, "\r\n");
                reqS = URLEncoder.encode(reqS, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                // ignore
            }
        }
        return reqS;
    }

}
