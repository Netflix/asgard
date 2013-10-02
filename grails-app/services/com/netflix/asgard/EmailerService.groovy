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
package com.netflix.asgard

import com.amazonaws.AmazonServiceException
import org.apache.commons.lang.StringUtils
import org.codehaus.groovy.runtime.StackTraceUtils
import org.springframework.beans.factory.InitializingBean
import org.springframework.mail.MailSender
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSenderImpl

/**
 * Simple service for sending emails.
 * Work is planned in the Grails roadmap to implement first-class email
 * support, so there's no point in making this code any more sophisticated
 */
class EmailerService implements InitializingBean {
    static transactional = false
    boolean systemEmailsEnabled = true
    boolean userEmailsEnabled = true
    MailSender mailSender
    SimpleMailMessage mailMessage // a "prototype" email instance
    def configService

    void afterPropertiesSet() {
        // Hide Spring and Tomcat stack trace elements in sanitized exceptions.
        // See org.codehaus.groovy.runtime.StackTraceUtils
        System.setProperty("groovy.sanitized.stacktraces", "groovy.,org.codehaus.groovy.,java.,javax.,sun.," +
                "gjdk.groovy.,org.apache.catalina.,org.apache.coyote.,org.apache.tomcat.,org.springframework.web.,")

        // Only send error emails for non-development instances
        systemEmailsEnabled = configService.systemEmailEnabled
        userEmailsEnabled = configService.userEmailEnabled
        mailSender = new JavaMailSenderImpl()
        mailSender.host = configService.smtpHost
        mailMessage = new SimpleMailMessage()
    }

    def sendUserEmail(String to, String subject, String text) {
        if (userEmailsEnabled) {
            String from = configService.fromAddressForEmail
            sendEmail(to, from, from, subject, text)
        }
    }

    def sendSystemEmail(String subject, String text) {
        if (systemEmailsEnabled) {
            String systemEmailAddress = configService.systemEmailAddress
            sendEmail(systemEmailAddress, systemEmailAddress, systemEmailAddress, subject, text)
        }
    }

    private sendEmail(String to, String from, String replyTo, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage(mailMessage)
        message.to = [to]
        message.from = from
        message.replyTo = replyTo
        message.subject = subject
        message.text = text
        log.info "Sending email to ${message.to[0]} with subject $subject"
        mailSender.send(message)
    }

    def sendExceptionEmail(String debugData, Exception exception) {
        StringWriter sw = new StringWriter()
        sw.write(debugData + "\n")
        PrintWriter printWriter = new PrintWriter(sw)
        String emailSubject = configService.errorEmailSubjectStart
        if (exception) {

            // Find the root cause, but don't risk infinite loops of causes. Don't use ExceptionUtils.getRootCause
            // because it returns null for some NullPointerException cases where the cause is the same as the exception.
            Throwable rootProblem = exception
            for (int i = 0; rootProblem.cause && i < 10; i++) {
                rootProblem = rootProblem.cause
            }

            String message ="${rootProblem.class.simpleName} '${rootProblem.message}'"
            if (rootProblem instanceof AmazonServiceException) {
                String serviceName = rootProblem.serviceName
                String errorCode = rootProblem.errorCode
                message = "${serviceName} ${errorCode} ${message}"
            }
            emailSubject += ": ${StringUtils.abbreviate(message, 160)}"

            Throwable cleanThrowable = StackTraceUtils.sanitize(rootProblem)
            cleanThrowable.printStackTrace(printWriter)
        }
        String emailBody = sw.toString()
        log.info "Sending email: ${emailBody}"
        sendSystemEmail(emailSubject, emailBody)
        emailBody
    }

    void enable() {
        systemEmailsEnabled = true
    }

    void disable() {
        systemEmailsEnabled = false
    }
}
