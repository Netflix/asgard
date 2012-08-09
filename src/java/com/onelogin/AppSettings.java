package com.onelogin;

public class AppSettings {
	private String assertionConsumerServiceUrl;
	private String issuer;
	
	public String getAssertionConsumerServiceUrl() {
		return assertionConsumerServiceUrl;
	}
	public void setAssertionConsumerServiceUrl(String assertionConsumerServiceUrl) {
		this.assertionConsumerServiceUrl = assertionConsumerServiceUrl;
	}
	public String getIssuer() {
		return issuer;
	}
	public void setIssuer(String issuer) {
		this.issuer = issuer;
	}

}
