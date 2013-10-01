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

import com.google.common.base.Objects
import com.netflix.asgard.Check
import com.netflix.asgard.Time
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import org.apache.shiro.SecurityUtils
import org.apache.shiro.authc.AuthenticationToken
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter

/**
 * Class representing API tokens for use in automatic scripts calling Asgard that require authentication. Tokens are
 * secured by generating a HMAC value with an encryption key of the supplied values to include in the token string.
 *
 * New tokens are generated using the public constructor and tokens are validated by creating an instance of this class
 * with the {@link fromApiTokenString(String)} method.
 */
class ApiToken implements AuthenticationToken {

    private static final String HMAC_SHA1_ALGORITHM = 'HmacSHA1'
    protected static final DateTimeFormatter TOKEN_DATE_FORMAT = DateTimeFormat.forPattern('yyyy-MM-dd')

    /**
     * User generated string to indicate what script(s) this token will be used for
     */
    final String purpose

    /**
     * Email address to send notifications to when this token is near expiration
     */
    final String email

    /**
     * When this token is valid until.
     */
    final DateTime expires

    /**
     * Username of the user who generated this token
     */
    final String username

    /**
     * Truncated HMAC string used for verifying the authenticity of incoming tokens.
     */
    private final String hash

    /**
     * Construct a new API Token. Builds the hash from the parameters using the supplied key.
     *
     * @param purpose What this token will be used for
     * @param email Email address to send warnings when this token is about to expire
     * @param daysValidFor Number of days from today when this token will expire
     * @param encryptionKey Key to use to sign the hash used in this token
     */
    ApiToken(String purpose, String email, int daysValidFor, String encryptionKey) {
        this.purpose = purpose
        this.email = email
        this.expires = new DateTime().plusDays(daysValidFor).withMillisOfDay(0)
        this.username = SecurityUtils.subject.principal
        this.hash = generateHash(encryptionKey)
    }

    private ApiToken(String purpose, DateTime expires, String username, String hash, String email) {
        this.purpose = purpose
        this.email = email
        this.expires = expires
        this.username = username
        this.hash = hash
    }

    /**
     * Parse the API token from a user provided String.
     *
     * @param String representation of an ApiToken
     * @return ApiToken constructed from the input String
     */
    static ApiToken fromApiTokenString(String apiTokenString) {
        Check.notNull(apiTokenString, 'apiTokenString')
        List<String> tokens = apiTokenString?.tokenize(':')
        if (tokens.size() != 4 && tokens.size() != 5) {
            throw new IllegalArgumentException('Invalid token format')
        }
        String purpose = tokens[0]
        DateTime expires = TOKEN_DATE_FORMAT.parseDateTime(tokens[1])
        String username = tokens[2]
        String hash = tokens[3]
        String email = (tokens.size() == 5) ? tokens[4] : username
        new ApiToken(purpose, expires, username, hash, email)
    }

    /**
     * Checks if the expiration time of the token has passed.
     *
     * @return true if before the expiration time, false otherwise.
     */
    boolean isExpired() {
        expires.isBeforeNow()
    }

    /**
     * Determine if the hash on this token object was generated with this encryption key
     *
     * @param encryptionKey key to use on the hash function
     * @return true if the hash on this token was generated for this key, false otherwise
     */
    boolean isValid(String encryptionKey) {
        !isExpired() && hash == generateHash(encryptionKey)
    }

    /**
     * Returns API Token as a String. This will be the input to validate requests.
     *
     * @return String representation of this token. Token can be reconstructed using {@link fromApiTokenString(String)}
     */
    String getTokenString() {
        List<String> components = [purpose, TOKEN_DATE_FORMAT.print(expires), username, URLEncoder.encode(hash)]
        if (username != email) {
            components << email
        }
        components.join(':')
    }

    /**
     * @return This token's expiration date in Asgard standard ISO readable format
     */
    String getExpiresReadable() {
        Time.format(expires)
    }

    /**
     * Compute a HMAC hash to validate the contents of this key using the supplied encryption key.
     *
     * Not to be confused with generating a delicious hash of sweet potatoes, curry powder, and zucchini.
     *
     * @param encryptionKey Key used as the HMAC signing key for the hash
     * @return First 7 characters of base64 encoded HMAC that was computed.
     */
    private String generateHash(String encryptionKey) {
        long expiresUTC = expires.withZoneRetainFields(DateTimeZone.UTC).millis
        String stringToHash = "username:${username},purpose:${purpose},email:${email},expires:${expiresUTC}"

        // Get an hmac_sha1 key from the raw key bytes
        SecretKeySpec signingKey = new SecretKeySpec(encryptionKey.bytes, HMAC_SHA1_ALGORITHM)

        // Get an hmac_sha1 Mac instance and initialize with the signing key
        Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM)
        mac.init(signingKey)

        // Compute the hmac on input data bytes
        byte[] rawHmac = mac.doFinal(stringToHash.bytes)

        // Base64-encode the hmac
        String fullHash = rawHmac.encodeBase64().toString()

        // To keep the token size small, only check the first 7 characters (a la git)
        fullHash[0..7]
    }

    boolean equals(Object obj) {
        if (!(obj instanceof ApiToken)) {
            return false
        }
        ApiToken other = (ApiToken) obj
        Objects.equal(purpose, other.purpose) && Objects.equal(expires, other.expires) &&
                Objects.equal(username, other.username) && Objects.equal(hash, other.hash) &&
                Objects.equal(email, other.email)
    }

    int hashCode() {
        Objects.hashCode(purpose, expires, username, hash, email)
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.shiro.authc.AuthenticationToken#getCredentials()
     */
    Object getCredentials() {
        getTokenString()
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.shiro.authc.AuthenticationToken#getPrincipal()
     */
    Object getPrincipal() {
        username
    }

}
