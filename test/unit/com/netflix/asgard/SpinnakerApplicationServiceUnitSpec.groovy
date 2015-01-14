/*
 * Copyright 2014 Netflix, Inc.
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

import com.amazonaws.services.autoscaling.model.AutoScalingGroup
import com.netflix.asgard.applications.SpinnakerApplicationService
import com.netflix.asgard.model.MonitorBucketType
import com.netflix.spinnaker.client.Spinnaker
import com.netflix.spinnaker.client.model.ApplicationMetadata
import com.netflix.spinnaker.client.model.MutableApplication
import com.netflix.spinnaker.client.model.SpinnakerOperations
import com.netflix.spinnaker.client.model.TaskExecutionException
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

@SuppressWarnings("GroovyAssignabilityCheck")
class SpinnakerApplicationServiceUnitSpec extends Specification {
    def spinnaker = Mock(Spinnaker)

    @Subject
    def applicationService = new SpinnakerApplicationService(spinnaker, "test")

    void setup() {
        applicationService.awsAutoScalingService = Mock(AwsAutoScalingService)
        applicationService.awsLoadBalancerService = Mock(AwsLoadBalancerService)
        applicationService.awsEc2Service = Mock(AwsEc2Service)
        applicationService.mergedInstanceGroupingService = Mock(MergedInstanceGroupingService)
        applicationService.fastPropertyService = Mock(FastPropertyService)
        applicationService.taskService = new TaskService() {
            def runTask(UserContext context, String name, Closure work, Link link = null, Task existingTask = null) {
                work(new Task())
            }
        }
        applicationService.caches = new Caches(new MockCachedMapBuilder([
            (EntityType.application): new CachedMapBuilder(null).of(EntityType.application).buildCachedMap(),
        ]))
    }

    @Unroll
    void "should filter out applications not in the current account"() {
        given:
        def metadata = new ApplicationMetadata(
            null, null, null, null, null, null, null, null, null, accounts, null, null
        )

        when:
        def appRegistration = applicationService.convertToAppRegistration(new MutableApplication(null, metadata))

        then:
        appRegistration == expectedAppRegistration

        where:
        accounts                | expectedAppRegistration
        ["prod"] as Set         | null
        ["prod", "test"] as Set | new AppRegistration(monitorBucketType: MonitorBucketType.none)
    }

    void "should return AppRegistration with lower case names"() {
        given:
        def metadata = new ApplicationMetadata(
            name, null, null, null, null, null, null, null, null, [applicationService.accountName] as Set, null, null
        )

        when:
        def appRegistration = applicationService.convertToAppRegistration(new MutableApplication(null, metadata))

        then:
        appRegistration.name == expectedName

        where:
        name        | expectedName
        "360"       | "360"
        "mixedCase" | "mixedcase"
        "lower"     | "lower"
        "UPPER"     | "upper"
        "miXED123"  | "mixed123"
    }

    void "should create application if it does not already exist"() {
        given:
        def mutableApplication = Mock(MutableApplication)
        def spinnakerOperations = Mock(SpinnakerOperations) {
            1 * application() >> mutableApplication
        }

        when:
        def result = applicationService.createRegisteredApplication(
            UserContext.auto(), name, group, type, description, owner, email, monitorBucketType, tags.join(",")
        )

        then:
        1 * spinnaker.operations() >> spinnakerOperations
        1 * mutableApplication.withAccount(applicationService.accountName) >> mutableApplication
        1 * mutableApplication.withName(name) >> mutableApplication
        1 * mutableApplication.withGroup(group) >> mutableApplication
        1 * mutableApplication.withType(type) >> mutableApplication
        1 * mutableApplication.withDescription(description) >> mutableApplication
        1 * mutableApplication.withOwner(owner) >> mutableApplication
        1 * mutableApplication.withEmail(email) >> mutableApplication
        1 * mutableApplication.withMonitorBucketType(monitorBucketType.name()) >> mutableApplication
        1 * mutableApplication.withTags(tags) >> mutableApplication
        1 * mutableApplication.saveAndGet()

        result.succeeded()

        where:
        name = "app"
        group = "group"
        type = "type"
        description = "description"
        owner = "owner"
        email = "email"
        monitorBucketType = MonitorBucketType.none
        tags = ["tag1", "tag2"] as Set
    }

    void "should surface an error message if create application fails"() {
        when:
        def result = applicationService.createRegisteredApplication(
            UserContext.auto(), "app", null, null, null, null, null, null, null
        )

        then:
        1 * spinnaker.operations() >> {
          throw new TaskExecutionException("Failed to save application", taskWithErrors())
        }

        result.message == "[TASK-ID] Failed to save application, reason(s)='Error1, Error2'"
        !result.succeeded()
    }

    void "should update application if it already exists"() {
        given:
        def mutableApplication = Mock(MutableApplication)

        when:
        def result = applicationService.updateRegisteredApplication(
            UserContext.auto(), name, group, type, description, owner, email, monitorBucketType, tags.join(",")
        )

        then:
        2 * spinnaker.application(name.toUpperCase()) >> mutableApplication
        1 * mutableApplication.withAccount(applicationService.accountName) >> mutableApplication
        1 * mutableApplication.withName(name) >> mutableApplication
        1 * mutableApplication.withGroup(group) >> mutableApplication
        1 * mutableApplication.withType(type) >> mutableApplication
        1 * mutableApplication.withDescription(description) >> mutableApplication
        1 * mutableApplication.withOwner(owner) >> mutableApplication
        1 * mutableApplication.withEmail(email) >> mutableApplication
        1 * mutableApplication.withMonitorBucketType(monitorBucketType.name()) >> mutableApplication
        1 * mutableApplication.withTags(tags) >> mutableApplication
        1 * mutableApplication.saveAndGet()

        result.succeeded()

        where:
        name = "app"
        group = "group"
        type = "type"
        description = "description"
        owner = "owner"
        email = "email"
        monitorBucketType = MonitorBucketType.none
        tags = ["tag1", "tag2"] as Set
    }

    void "should surface an error message if update application fails"() {
        when:
        def result = applicationService.updateRegisteredApplication(
            UserContext.auto(), appName, null, null, null, null, null, null, null
        )

        then:
        1 * spinnaker.application(appName.toUpperCase()) >> {
            throw new TaskExecutionException("Failed to save application", taskWithErrors())
        }

        result.message == "[TASK-ID] Failed to save application, reason(s)='Error1, Error2'"
        !result.succeeded()

        where:
        appName = "app"
    }

    void "should delete application if validation passes"() {
        given:
        applicationService.caches.allApplications.put(appName, new AppRegistration())

        and:
        def mutableApplication = Mock(MutableApplication)

        when:
        applicationService.deleteRegisteredApplication(UserContext.auto(Region.US_EAST_1), appName)

        then:
        1 * spinnaker.application(appName) >> mutableApplication
        1 * mutableApplication.withAccount(applicationService.accountName) >> mutableApplication
        1 * mutableApplication.deleteAndGet()

        applicationService.caches.allApplications.get(appName) == null

        where:
        appName = "app"
    }

    void "should not delete application if validation fails"() {
        given:
        applicationService.awsAutoScalingService = Mock(AwsAutoScalingService) {
            1 * getAutoScalingGroupsForApp(_, _) >> [
                new AutoScalingGroup()
            ]
        }

        when:
        applicationService.deleteRegisteredApplication(UserContext.auto(Region.US_EAST_1), "app")

        then:
        0 * applicationService.taskService.runTask(_, _, _, _)
        thrown(ValidationException)
    }

    private taskWithErrors() {
        Mock(com.netflix.spinnaker.client.model.Task) {
            1 * getId() >> "TASK-ID"
            1 * getVariables() >> [
                new com.netflix.spinnaker.client.model.Task.TaskVariable("exception", [
                    details: [
                        errors: [ "Error1", "Error2"]
                    ]
                ])
            ]
        }
    }
}
