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

import com.netflix.asgard.pages.AutoScalingGroupDetailsPage
import com.netflix.asgard.pages.CreatePage
import com.netflix.asgard.pages.DetailsPage
import com.netflix.asgard.pages.ListPage
import com.netflix.asgard.pages.ManageClusterPage
import geb.spock.GebReportingSpec
import spock.lang.IgnoreIf
import spock.lang.Shared
import spock.lang.Stepwise

@Stepwise
@IgnoreIf({ System.getProperty('regressionSuite') != 'true' })
class RegressionSpec extends GebReportingSpec {

    @Shared
    String APPNAME = "asgard${System.getProperty('user.name')}test"

    @Shared
    String CLUSTER = "${APPNAME}-${APPNAME}_stack-c0latam-d0phones"

    void setupSpec() {
        go '/'
        waitFor(120) {
            title != 'Asgard is Loading'
        }
    }

    void 'can create a new application'() {
        when:
        to CreatePage, 'application'

        and:
        details.name = APPNAME
        details.group = 'group'
        details.type().select "Web Service"
        details.description = 'this is a test application to make sure asgard applications can be created'
        details.owner = 'John Doe'
        details.email = "jdoe@example.com"
        details.monitorBucketType().select "cluster"

        and:
        create()

        then:
        at DetailsPage
        message == "Application '$APPNAME' has been created."
    }

    void 'can create a security group'() {
        when:
        to CreatePage, 'security'

        and:
        details.appName().select(APPNAME)
        details.detail = 'sg-group'
        details.description = "security group description for $APPNAME"

        and:
        create()

        then:
        at DetailsPage
        message == "Security Group '$APPNAME-sg-group' has been created."
    }

    void 'can create a load balancer'() {
        when:
        to CreatePage, 'loadBalancer'

        and:
        details.appName().select(APPNAME)
        details.selectedZones().select(["us-east-1a", "us-east-1d"])

        and:
        create()

        then:
        at DetailsPage
        message == "Load Balancer '$APPNAME--frontend' has been created. Contact your cloud admin to enable security group ingress permissions from elastic load balancers."
    }

    void 'can create a cluster'() {
        when:
        to CreatePage, 'autoScaling'

        and:
        details.appName().select(APPNAME)
        details.newStack = "${APPNAME}_stack"
        details.countries = 'latam'
        details.devPhase = 'phones'
        details.min = '1'
        details.max = '3'
        details.desiredCapacity = '2'
        details.imageId().select('ami-2a249843')
        details.selectedSecurityGroups().select("${APPNAME}-sg-group" as String)
        details.selectedLoadBalancersForVpcId().select("${APPNAME}--frontend" as String)
        details.keyName().select('nflxoss-asgard')
        create()

        then:
        at DetailsPage
        message.startsWith "Launch Config '$CLUSTER"
        message.endsWith "has been created. Auto Scaling Group '$CLUSTER' has been created."
    }

    void 'can scale clusters'() {
        when:
        to ManageClusterPage, CLUSTER
        cluster(0).resizeTo(min, max)

        then:
        waitForTaskToFinish()

        when:
        to AutoScalingGroupDetailsPage, CLUSTER

        then:
        instanceCount >= min
        instanceCount <= max

        where:
        min | max
        3   | 4
        2   | 3
    }

    void 'can manage traffic to a cluster'() {
        when:
        to ManageClusterPage, CLUSTER
        cluster(0)."$action"()

        then:
        waitForTaskToFinish()

        when:
        to AutoScalingGroupDetailsPage, CLUSTER

        then:
        launch.isStatusEnabled() == isEnabled
        terminate.isStatusEnabled() == isEnabled
        addToLoadBalancer.isStatusEnabled() == isEnabled

        where:
        action           | isEnabled
        'disableTraffic' | false
        'enableTraffic'  | true

    }

    void 'can create next sequential auto scaling group'() {
        when:
        to ManageClusterPage, CLUSTER

        then:
        newCluster.name == CLUSTER + '-v000'

        when:
        newCluster.toggleAdvancedOptions()
        newCluster.details.min = 1
        newCluster.details.max = 2
        newCluster.create()

        then:
        message == "Creating auto scaling group '$CLUSTER-v000', min 1, max 2, traffic allowed has been started."
        waitForTaskToFinish()

        when:
        to ManageClusterPage, CLUSTER

        then:
        cluster(1).name == CLUSTER + '-v000'
        newCluster.name == CLUSTER + '-v001'
    }

    void 'can delete sequential auto scaling group'() {
        when:
        to ManageClusterPage, CLUSTER
        cluster(0).delete()

        then:
        waitForTaskToFinish()

        when:
        to ManageClusterPage, CLUSTER

        then:
        cluster(0).name == CLUSTER + '-v000'

        cleanup:
        cleanupAutoScalingGroups()
    }

    void "can delete #type"() {
        when:
        to DetailsPage, type, name
        delete()

        then:
        message == deletionMessage

        where:
        type           | name                 | deletionMessage                                        | taskName
        'loadBalancer' | "$APPNAME--frontend" | "Load Balancer '$APPNAME--frontend' has been deleted." | "Remove Load Balancer $APPNAME--frontend"
        'security'     | "$APPNAME-sg-group"  | "Security Group '$APPNAME-sg-group' has been deleted." | "Remove Security Group $APPNAME-sg-group"
        'application'  | APPNAME              | "Application '$APPNAME' has been deleted."             | "Delete registered app $APPNAME"
    }

    void cleanupSpec() {
        cleanupAutoScalingGroups()
        ['launchConfiguration', 'loadBalancer', 'security', 'application'].each { type ->
            to ListPage, type, APPNAME
            if (!isEmpty()) {
                (0..rowCount() - 1).collect { row(it).name }.each { name ->
                    to DetailsPage, type, name
                    delete()
                }
            }
        }
    }

    private void cleanupAutoScalingGroups() {
        to ListPage, 'autoScaling', APPNAME
        if (!isEmpty()) {
            (0..rowCount() - 1).each {
                to ManageClusterPage, CLUSTER
                cluster(it).delete()
                waitForTaskToFinish(600)
            }
        }
        via ManageClusterPage, CLUSTER
    }

}