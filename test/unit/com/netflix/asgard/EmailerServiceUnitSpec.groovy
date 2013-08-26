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
import spock.lang.Specification

class EmailerServiceUnitSpec extends Specification {

    EmailerService emailerService = Spy(EmailerService)
    GrailsApplication grailsApplication = new DefaultGrailsApplication()

    void setup() {
        grailsApplication.config.email.errorSubjectStart = 'Trouble'
        emailerService.grailsApplication = grailsApplication
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
