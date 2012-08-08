package com.onelogin.saml;

import java.io.ByteArrayOutputStream;
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

public class AuthRequest {
	
	private String id;
	private String issueInstant;
	private AppSettings appSettings;
	public static final int base64 = 1;
	
	public AuthRequest(AppSettings appSettings, AccountSettings accountSettings){		
		this.appSettings = appSettings;
		id="_"+UUID.randomUUID().toString();		
		SimpleDateFormat simpleDf = new SimpleDateFormat("yyyy-MM-dd'T'H:mm:ss");
		issueInstant = simpleDf.format(new Date());		
	}
	
	public String getRequest(int format) throws XMLStreamException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();		
		XMLOutputFactory factory = XMLOutputFactory.newInstance();
		XMLStreamWriter writer = factory.createXMLStreamWriter(baos);
					
		writer.writeStartElement("samlp", "AuthnRequest", "urn:oasis:names:tc:SAML:2.0:protocol");
		writer.writeNamespace("samlp","urn:oasis:names:tc:SAML:2.0:protocol");
		
		writer.writeAttribute("ID", id);
		writer.writeAttribute("Version", "2.0");
		writer.writeAttribute("IssueInstant", this.issueInstant);
		writer.writeAttribute("ProtocolBinding", "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST");
		writer.writeAttribute("AssertionConsumerServiceURL", this.appSettings.getAssertionConsumerServiceUrl());
		
		writer.writeStartElement("saml","Issuer","urn:oasis:names:tc:SAML:2.0:assertion");
		writer.writeNamespace("saml","urn:oasis:names:tc:SAML:2.0:assertion");
		writer.writeCharacters(this.appSettings.getIssuer());
		writer.writeEndElement();
		
		writer.writeStartElement("samlp", "NameIDPolicy", "urn:oasis:names:tc:SAML:2.0:protocol");
		
		writer.writeAttribute("Format", "urn:oasis:names:tc:SAML:2.0:nameid-format:unspecified");
		writer.writeAttribute("AllowCreate", "true");
		writer.writeEndElement();
		
		writer.writeStartElement("samlp","RequestedAuthnContext","urn:oasis:names:tc:SAML:2.0:protocol");
		
		writer.writeAttribute("Comparison", "exact");
		writer.writeEndElement();
		
		writer.writeStartElement("saml","AuthnContextClassRef","urn:oasis:names:tc:SAML:2.0:assertion");
		writer.writeNamespace("saml", "urn:oasis:names:tc:SAML:2.0:assertion");
		writer.writeCharacters("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport");
		writer.writeEndElement();
		
		writer.writeEndElement();
		writer.flush();		
		
		if (format == base64) {
			byte [] encoded = Base64.encodeBase64Chunked(baos.toByteArray());
			String result = new String(encoded,Charset.forName("UTF-8"));
						
			return result;
		}
						
		return null;
	}
	
 	public static String getRidOfCRLF(String what) {
		String lf = "%0D";
		String cr = "%0A";
		String now = lf;

		int index = what.indexOf(now);
		StringBuffer r = new StringBuffer();

		while (index!=-1) {
			r.append(what.substring(0,index));
			what = what.substring(index+3,what.length());
			
			if (now.equals(lf)) {
				now = cr;
			} else {
				now = lf;
			}
			
			index = what.indexOf(now);
		}
		return r.toString();
	}		

}
