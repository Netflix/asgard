package com.onelogin.saml;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import org.apache.commons.codec.binary.Base64;

/**
 * A SAML certificate. Simply wrapping a standard X.509 cert for now.
 */
public class Certificate {

	private final X509Certificate x509Cert;

	/**
	 * Constructs a certificate from a base64 encoded string
	 */
 	public Certificate(String certificate) throws CertificateException {
        this(certificate.getBytes());
	}

	/**
	 * Constructs a certificate from a encoded base64 byte array.
	 * @param certificate an encoded base64 byte array.
	 * @throws CertificateException In case it can't load the certificate.
	 */
	public Certificate(byte[] certificate) throws CertificateException {
		CertificateFactory fty = CertificateFactory.getInstance("X.509");
		ByteArrayInputStream bais = new ByteArrayInputStream(Base64.decodeBase64(certificate));
		x509Cert = (X509Certificate) fty.generateCertificate(bais);
	}

	public X509Certificate getX509Cert() {
		return x509Cert;
	}
}
