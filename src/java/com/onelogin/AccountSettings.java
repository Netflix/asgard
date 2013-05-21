package com.onelogin;

/**
 * POJO representing a (OneLogin) SAML IdP account. Contains the IdP certificate and the IdP sso login URL.
 */
public class AccountSettings {
	private String certificate;
	private String idpSsoTargetUrl;
	
	public String getCertificate() {
		return certificate;
	}
	public void setCertificate(String certificate) {
		this.certificate = certificate;
	}
	public String getIdpSsoTargetUrl() {
		return idpSsoTargetUrl;
	}
	public void setIdpSsoTargetUrl(String idpSsoTargetUrl) {
		this.idpSsoTargetUrl = idpSsoTargetUrl;
	}
}
