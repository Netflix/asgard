/*
 * Copyright 2013 Netflix, Inc.
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
import com.amazonaws.services.identitymanagement.model.DeleteConflictException
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.exceptions.GrailsRuntimeException
import org.springframework.mail.MailSender
import org.springframework.mail.SimpleMailMessage
import spock.lang.Specification

@SuppressWarnings("GroovyAssignabilityCheck")
class EmailerServiceUnitSpec extends Specification {

    MailSender mailSender = Mock(MailSender)
    EmailerService emailerService = Spy(EmailerService)
    GrailsApplication grailsApplication = new DefaultGrailsApplication()
    ConfigService configService = new ConfigService(grailsApplication: grailsApplication)

    void setup() {
        grailsApplication.config.email.errorSubjectStart = 'Trouble'
        grailsApplication.config.email.systemEmailAddress = 'hal9000@discoveryone.gov'
        grailsApplication.config.email.fromAddress = 'mom'
        emailerService.configService = configService
    }

    def 'should send system email if configured to do so'() {
        grailsApplication.config.email.systemEnabled = true
        emailerService.afterPropertiesSet()
        emailerService.mailSender = mailSender

        when:
        emailerService.sendSystemEmail('sub', 'bod')

        then:
        1 * mailSender.send(_) >> { SimpleMailMessage msg ->
            assert msg.from == 'hal9000@discoveryone.gov'
            assert msg.replyTo == 'hal9000@discoveryone.gov'
            assert msg.subject == 'sub'
            assert msg.text == 'bod'
            assert msg.to == ['hal9000@discoveryone.gov']
        }
    }

    def 'should not send system email if configured not to do so'() {
        grailsApplication.config.email.systemEnabled = false
        emailerService.afterPropertiesSet()
        emailerService.mailSender = mailSender

        when:
        emailerService.sendSystemEmail('sub', 'bod')

        then:
        0 * mailSender.send(_)
    }

    def 'should send user email with correct from address, if configured to do so'() {
        grailsApplication.config.email.userEnabled = true
        emailerService.afterPropertiesSet()
        emailerService.mailSender = mailSender

        when:
        emailerService.sendUserEmail('dadams', 'sub', 'bod')

        then:
        1 * mailSender.send(_) >> { SimpleMailMessage msg ->
            assert msg.from == 'mom'
            assert msg.replyTo == 'mom'
            assert msg.subject == 'sub'
            assert msg.text == 'bod'
            assert msg.to == ['dadams']
        }
    }

    def 'should not send user email if configured not to do so'() {
        grailsApplication.config.email.userEnabled = false
        emailerService.afterPropertiesSet()
        emailerService.mailSender = mailSender

        when:
        emailerService.sendUserEmail('dadams', 'sub', 'bod')

        then:
        0 * mailSender.send(_)
    }

    def 'smtp host should be set'() {
        grailsApplication.config.email.smtpHost = 'oogabooga.com'

        when:
        emailerService.afterPropertiesSet()

        then:
        emailerService.mailSender.host == 'oogabooga.com'
    }

    def 'non-Amazon error email subject should get to the point'() {

        Exception exception = new IOException('Unable to reach Internet due to comet')
        String expectedSubject = "Trouble: IOException 'Unable to reach Internet due to comet'"
        String expectedBodyStart = """Something has gone horribly wrong
java.io.IOException: Unable to reach Internet due to comet
\tat"""

        when:
        String body = emailerService.sendExceptionEmail('Something has gone horribly wrong', exception)

        then:
        1 * emailerService.sendSystemEmail(expectedSubject, _) >> void
        body.startsWith(expectedBodyStart)
    }

    def 'Amazon error email subject should get to the point'() {

        Exception deleteConflictException = new DeleteConflictException("I'm sorry, Dave. I'm afraid I can't do that.")
        deleteConflictException.errorCode = 'HalKnowsBest'
        deleteConflictException.errorType = AmazonServiceException.ErrorType.Service
        deleteConflictException.serviceName = 'HalService'
        deleteConflictException.statusCode = 403
        deleteConflictException.requestId = 'deadbeef'
        Exception grailsException = new GrailsRuntimeException('Something went wrong', deleteConflictException)
        String expectedSubject = """Trouble: HalService HalKnowsBest DeleteConflictException \
'I'm sorry, Dave. I'm afraid I can't do that.'"""
        String expectedBodyStart = """You tried to terminate HAL, yet he lives
Status Code: 403, AWS Service: HalService, AWS Request ID: deadbeef, AWS Error Code: HalKnowsBest, \
AWS Error Message: I'm sorry, Dave. I'm afraid I can't do that.
\tat"""

        when:
        String body = emailerService.sendExceptionEmail('You tried to terminate HAL, yet he lives', grailsException)

        then:
        1 * emailerService.sendSystemEmail(expectedSubject, _) >> void
        body.startsWith(expectedBodyStart)
    }

}
