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
import javax.servlet.http.HttpServletRequest
import org.apache.shiro.authc.AuthenticationException
import org.apache.shiro.authc.AuthenticationInfo
import org.apache.shiro.authc.RememberMeAuthenticationToken
import org.apache.shiro.authc.SimpleAuthenticationInfo
import org.springframework.beans.factory.annotation.Autowired

/**
 * Example of building an AuthenticationProvider for SSO using OneLogin.
 */
class OneLoginAuthenticationProvider implements AuthenticationProvider {

    @Autowired
    ConfigService configService

    String loginUrl(HttpServletRequest request) {
        String oneloginUrl = configService.oneLoginUrl
        AppSettings appSettings = new AppSettings(issuer: oneloginUrl)

        // The SAML Response will be posted to this URL.
        URL url = new URL(request.scheme, request.serverName, request.serverPort, '/auth/signIn')
        appSettings.setAssertionConsumerServiceUrl(url as String)

        AccountSettings accSettings = new AccountSettings(idpSsoTargetUrl: oneloginUrl)

        AuthRequest authReq = new AuthRequest(appSettings, accSettings)
        String samlRequest = AuthRequest.getRidOfCRLF(URLEncoder.encode(authReq.getRequest(AuthRequest.base64),
                'UTF-8'))
        "${accSettings.idp_sso_target_url}?SAMLRequest=${samlRequest}"
    }

    AsgardToken tokenFromRequest(HttpServletRequest request) {
        new SamlToken(request.getParameter('SAMLResponse'))
    }

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
            this.samlResponseString = samlResponse
            AccountSettings accountSettings = new AccountSettings(certificate: configService.oneLoginCertificate)

            samlResponse = new Response(accountSettings)
            try {
                samlResponse.loadXmlFromBase64(samlResponseString)
            } catch (Exception e) {
                throw new AuthenticationException("Unable to parse response from OneLogin: ${samlResponseString}", e)
            }
        }

        boolean isValid() {
            samlResponse.valid
        }

        boolean isRememberMe() {
            true
        }

        Object getCredentials() {
            samlResponseString
        }

        Object getPrincipal() {
            String suffix = configService.oneLoginUsernameSuffix
            suffix ? samlResponse.nameId.replaceAll("${suffix}\$", '') : samlResponse.nameId
        }
    }

    @Override
    public String logoutUrl(HttpServletRequest request) {
        configService.oneLoginLogoutUrl
    }
}