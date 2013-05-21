/*
 * Copyright 2012 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.asgard.auth

import com.netflix.asgard.ConfigService
import com.netflix.asgard.plugin.AuthenticationProvider
import com.onelogin.AccountSettings
import com.onelogin.AppSettings
import com.onelogin.saml.AuthRequest
import com.onelogin.saml.Response
import org.apache.commons.lang.WordUtils
import org.springframework.beans.factory.InitializingBean

import javax.servlet.http.HttpServletRequest
import org.apache.shiro.authc.AuthenticationException
import org.apache.shiro.authc.AuthenticationInfo
import org.apache.shiro.authc.RememberMeAuthenticationToken
import org.apache.shiro.authc.SimpleAuthenticationInfo
import org.springframework.beans.factory.annotation.Autowired

/**
 * Example of building an AuthenticationProvider for SSO using a SAML 2 IdP.
 */
class SamlAuthenticationProvider implements AuthenticationProvider, InitializingBean {

    @Autowired
    ConfigService configService
    AccountSettings accountSettings

    @Override
    void afterPropertiesSet() {
        accountSettings = new AccountSettings(
                certificate: configService.samlCertificate,
                idpSsoTargetUrl: configService.samlLoginUrl)
    }

    @Override
    String loginUrl(HttpServletRequest request) {
        // The SAML issuer URL.
        String samlIssuer = configService.samlIssuer

        // The SAML IdP Response will be posted back to us at this URL.
        URL postBackUrl = new URL(request.scheme, request.serverName, request.serverPort, '/auth/signIn')

        // The app settings are about this SP app.
        AppSettings appSettings = new AppSettings(issuer: samlIssuer, assertionConsumerServiceUrl: postBackUrl as String)

        AuthRequest authReq = new AuthRequest(appSettings, accountSettings)
        String samlRequest = authReq.getRequestEncoded(AuthRequest.base64 | AuthRequest.url)

        "${accountSettings.idpSsoTargetUrl}?SAMLRequest=${samlRequest}"
    }

    @Override
    AsgardToken tokenFromRequest(HttpServletRequest request) {
        new SamlToken(request.getParameter('SAMLResponse'))
    }

    @Override
    AuthenticationInfo authenticate(AsgardToken authToken) {
        SamlToken samlToken = (SamlToken) authToken
        if (samlToken == null) {
            throw new AuthenticationException('SAML token cannot be null')
        }
        if (!samlToken.valid) {
            throw new AuthenticationException('Invalid SAML token')
        }

        new SimpleAuthenticationInfo(samlToken.principal, samlToken.credentials, 'AsgardRealm')
    }

    class SamlToken implements AsgardToken, RememberMeAuthenticationToken {

        String samlResponseString
        Response samlResponse

        SamlToken(String samlResponseString) {
            this.samlResponseString = samlResponseString
            this.samlResponse = new Response(accountSettings)
            try {
                samlResponse.loadXmlFromBase64(samlResponseString)
            } catch (Exception e) {
                throw new AuthenticationException("Unable to parse response from IdP: ${samlResponseString}", e)
            }
        }

        @Override
        boolean isValid() {
            samlResponse.valid
        }

        @Override
        boolean isRememberMe() {
            true
        }

        @Override
        Object getCredentials() {
            samlResponseString
        }

        @Override
        Object getPrincipal() {
            String suffix = configService.samlUsernameSuffix
            suffix ? samlResponse.nameId.replaceAll("${suffix}\$", '') : samlResponse.nameId
        }
    }

    @Override
    String logoutUrl(HttpServletRequest request) {
        configService.samlLogoutUrl
    }
}
