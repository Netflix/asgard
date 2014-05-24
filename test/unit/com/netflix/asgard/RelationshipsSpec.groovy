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

import com.netflix.frigga.Names
import com.netflix.frigga.ami.AppVersion
import org.joda.time.DateTime
import spock.lang.Specification

@SuppressWarnings("GroovyAccessibility")
class RelationshipsSpec extends Specification {

    void setup() {
        new MonkeyPatcherService().createDynamicMethods()
    }

    void 'should build next auto scaling group name'() {
        expect:
        Relationships.buildNextAutoScalingGroupName(oldGroup) == newGroup

        where:
        oldGroup             | newGroup
        "discovery-dev"      | "discovery-dev-v000"
        "discovery-dev-v999" | "discovery-dev-v000"
        "discovery-dev-v998" | "discovery-dev-v999"
        "discovery-dev-v997" | "discovery-dev-v998"
        "discovery-dev-v000" | "discovery-dev-v001"
        "discovery-dev-v001" | "discovery-dev-v002"
        "discovery-dev-v002" | "discovery-dev-v003"
        "discovery-dev-v521" | "discovery-dev-v522"
    }

    void 'should parse a compound name that contains a dot'() {

        when:
        Names names = Relationships.dissectCompoundName("chukwa.collector_1-v889")

        then:
        names.group == "chukwa.collector_1-v889"
        names.cluster == "chukwa.collector_1"
        names.app == "chukwa.collector_1"
        names.stack == null
        names.detail == null
        names.push == "v889"
        names.sequence == 889
    }

    void 'should fail to parse an invalid compound name'() {

        when:
        Names names = Relationships.dissectCompoundName('nccp-moviecontrol%27')

        then:
        names.group == null
        names.cluster == null
        names.app == null
        names.stack == null
        names.detail == null
        names.push == null
        names.sequence == null
    }

    void 'should parse names of auto scaling groups'() {

        when:
        Names names = Relationships.dissectCompoundName(name)

        then:
        names.group == group
        names.cluster == cluster
        names.app == app
        names.stack == stack
        names.detail == detail
        names.push == push
        names.sequence == seq

        where:
        name              | group             | cluster          | app      | stack     | detail   | push   | seq
        null              | null              | null             | null     | null      | null     | null   | null
        'actor'           | 'actor'           | 'actor'          | 'actor'  | null      | null     | null   | null
        'actor-v003'      | 'actor-v003'      | 'actor'          | 'actor'  | null      | null     | 'v003' | 3
        'actor--v003'     | 'actor--v003'     | 'actor-'         | 'actor'  | null      | null     | 'v003' | 3
        'actor---v003'    | 'actor---v003'    | 'actor--'        | 'actor'  | null      | null     | 'v003' | 3
        'api-test-A'      | 'api-test-A'      | 'api-test-A'     | 'api'    | 'test'    | 'A'      | null   | null
        'api-test-A-v406' | 'api-test-A-v406' | 'api-test-A'     | 'api'    | 'test'    | 'A'      | 'v406' | 406
        'api-test101'     | 'api-test101'     | 'api-test101'    | 'api'    | 'test101' | null     | null   | null
        'chip_1'          | 'chip_1'          | 'chip_1'         | 'chip_1' | null      | null     | null   | null
        'chip_1-v889'     | 'chip_1-v889'     | 'chip_1'         | 'chip_1' | null      | null     | 'v889' | 889
        'disc-dev'        | 'disc-dev'        | 'disc-dev'       | 'disc'   | 'dev'     | null     | null   | null
        'disc-us-e-1d'    | 'disc-us-e-1d'    | 'disc-us-e-1d'   | 'disc'   | 'us'      | 'e-1d'   | null   | null
        'disc-us-e-1d-0'  | 'disc-us-e-1d-0'  | 'disc-us-e-1d-0' | 'disc'   | 'us'      | 'e-1d-0' | null   | null
        'd-us-e-1-0-v223' | 'd-us-e-1-0-v223' | 'd-us-e-1-0'     | 'd'      | 'us'      | 'e-1-0'  | 'v223' | 223
    }

    void 'should parse names of auto scaling groups with labeled variables'() {

        when:
        Names names = Relationships.dissectCompoundName("actiondrainer")

        then:
        "actiondrainer" == names.group
        "actiondrainer" == names.cluster
        "actiondrainer" == names.app
        null == names.stack
        null == names.detail
        null == names.push
        null == names.sequence
        null == names.countries
        null == names.devPhase
        null == names.hardware
        null == names.partners
        null == names.revision
        null == names.usedBy
        null == names.redBlackSwap
        null == names.zone

        when:
        names = Relationships.dissectCompoundName(
                'cass-nccpint-random-junk-c0america-d0prod-h0xbox-p0vizio-r027-u0nccp-w0A-z0useast1a-v003')

        then:
        'cass-nccpint-random-junk-c0america-d0prod-h0xbox-p0vizio-r027-u0nccp-w0A-z0useast1a-v003' == names.group
        'cass-nccpint-random-junk-c0america-d0prod-h0xbox-p0vizio-r027-u0nccp-w0A-z0useast1a' == names.cluster
        'cass' == names.app
        'nccpint' == names.stack
        'random-junk' == names.detail
        'v003' == names.push
        3 == names.sequence
        'america' == names.countries
        'prod' == names.devPhase
        'xbox' == names.hardware
        'vizio' == names.partners
        '27' == names.revision
        'nccp' == names.usedBy
        'A' == names.redBlackSwap
        'useast1a' == names.zone

        when:
        names = Relationships.dissectCompoundName('cass-nccpintegration-c0northamerica-d0prod')

        then:
        names.group == 'cass-nccpintegration-c0northamerica-d0prod'
        names.cluster == 'cass-nccpintegration-c0northamerica-d0prod'
        names.app == 'cass'
        names.stack == 'nccpintegration'
        names.detail == null
        names.push == null
        names.sequence == null
        names.countries == 'northamerica'
        names.devPhase == 'prod'
        names.hardware == null
        names.partners == null
        names.revision == null
        names.usedBy == null
        names.redBlackSwap == null
        names.zone == null

        when:
        names = Relationships.dissectCompoundName('cass--my-stuff-c0northamerica-d0prod')

        then:
        names.group == 'cass--my-stuff-c0northamerica-d0prod'
        names.cluster == 'cass--my-stuff-c0northamerica-d0prod'
        names.app == 'cass'
        names.stack == null
        names.detail == 'my-stuff'
        names.push == null
        names.sequence == null
        names.countries == 'northamerica'
        names.devPhase == 'prod'
        names.hardware == null
        names.partners == null
        names.revision == null
        names.usedBy == null
        names.redBlackSwap == null
        names.zone == null

        when:
        names = Relationships.dissectCompoundName('cass-c0northamerica-d0prod')

        then:
        names.group == 'cass-c0northamerica-d0prod'
        names.cluster == 'cass-c0northamerica-d0prod'
        names.app == 'cass'
        names.stack == null
        names.detail == null
        names.push == null
        names.sequence == null
        names.countries == 'northamerica'
        names.devPhase == 'prod'
        names.hardware == null
        names.partners == null
        names.revision == null
        names.usedBy == null
        names.redBlackSwap == null
        names.zone == null

        when:
        names = Relationships.dissectCompoundName('cass-c0northamerica-d0prod-v102')

        then:
        names.group == 'cass-c0northamerica-d0prod-v102'
        names.cluster == 'cass-c0northamerica-d0prod'
        names.app == 'cass'
        names.stack == null
        names.detail == null
        names.push == 'v102'
        names.sequence == 102
        names.countries == 'northamerica'
        names.devPhase == 'prod'
        names.hardware == null
        names.partners == null
        names.revision == null
        names.usedBy == null
        names.redBlackSwap == null
        names.zone == null

        when:
        names = Relationships.dissectCompoundName('cass-v102')

        then:
        names.group == 'cass-v102'
        names.cluster == 'cass'
        names.app == 'cass'
        names.stack == null
        names.detail == null
        names.push == 'v102'
        names.sequence == 102
        names.countries == null
        names.devPhase == null
        names.hardware == null
        names.partners == null
        names.revision == null
        names.usedBy == null
        names.redBlackSwap == null
        names.zone == null
    }

    void 'should parse appversion string'() {

        when:
        AppVersion appVersion = Relationships.dissectAppVersion(appversion)

        then:
        appVersion.packageName == pack
        appVersion.version == ver
        appVersion.commit == commit
        appVersion.buildNumber == buildNum
        appVersion.buildJobName == job

        where:
        appversion                                      | pack        | ver     | commit   | buildNum | job
        "hello-1.0.0-592112"                            | "hello"     | "1.0.0" | "592112" | null     | null
        "hello-1.0.0-592112.h154"                       | "hello"     | "1.0.0" | "592112" | "154"    | null
        "hello-int-1.0.0-592112.h154/WE-WAPP-hello/154" | "hello-int" | "1.0.0" | "592112" | "154"    | "WE-WAPP-hello"
        "hello-1.0.0-592112.h154/WE-WAPP-hello/154"     | "hello"     | "1.0.0" | "592112" | "154"    | "WE-WAPP-hello"
    }

    void 'should fail to parse invalid appversion string'() {
        expect:
        Relationships.dissectAppVersion(appversion) == null

        where:
        appversion << [null, '', 'blah', 'blah blah blah']
    }

    void 'should extract package name from appversion string'() {
        expect:
        Relationships.packageFromAppVersion(appversion) == pack

        where:
        appversion                                            | pack
        'dfjsdfkjsdjf sd'                                     | null
        ''                                                    | null
        null                                                  | null
        'helloworld-1.0.0-592112.h154'                        | 'helloworld'
        'helloworld-1.0.0-592112.h154/WE-WAPP-helloworld/154' | 'helloworld'
    }

    void 'should extract app name from group name'() {
        expect:
        Relationships.appNameFromGroupName(group) == app

        where:
        group                                         | app
        "actiondrainer"                               | "actiondrainer"
        "merchweb--loadtest"                          | "merchweb"
        "merchweb-loadtest"                           | "merchweb"
        "discovery-us-east-1d"                        | "discovery"
        "discovery--us-east-1d"                       | "discovery"
        "api-test-A"                                  | "api"
        "evcache-us-east-1d-0"                        | "evcache"
        "evcache-us----east-1d-0"                     | "evcache"
        "videometadata-navigator-integration-240-CAN" | "videometadata"
    }

    void testAppNameFromLaunchConfigName() {
        expect:
        Relationships.appNameFromLaunchConfigName("actiondrainer-201010231745") == "actiondrainer"

        where:
        launch                                                     | app
        "actiondrainer-201010231745"                               | "actiondrainer"
        "merchweb--loadtest-201010231745"                          | "merchweb"
        "discovery--us-east-1d-201010231745"                       | "discovery"
        "merchweb-loadtest-201010231745"                           | "merchweb"
        "api-test-A-201010231745"                                  | "api"
        "discovery-dev-201010231745"                               | "discovery"
        "discovery-us-east-1d-201010231745"                        | "discovery"
        "evcache-us-east-1d-0-201010231745"                        | "evcache"
        "evcache-us----east-1d-0-201010231745"                     | "evcache"
        "videometadata-navigator-integration-240-CAN-201010231745" | "videometadata"
    }

    void testAppNameFromLoadBalancerName() {
        expect:
        Relationships.appNameFromLoadBalancerName(loadBal) == app

        where:
        app             | loadBal
        "actiondrainer" | "actiondrainer-frontend"
        "merchweb"      | "merchweb--loadtest-frontend"
        "discovery"     | "discovery--us-east-1d-frontend"
        "merchweb"      | "merchweb-loadtest-frontend"
        "api"           | "api-test-A-frontend"
        "discovery"     | "discovery-dev-frontend"
        "discovery"     | "discovery-us-east-1d-frontend"
        "evcache"       | "evcache-us-east-1d-0-frontend"
        "evcache"       | "evcache-us----east-1d-0-frontend"
        "videometadata" | "videometadata-navigator-integration-240-CAN-frontend"
    }

    void 'should extract stack name from group name'() {
        expect:
        Relationships.stackNameFromGroupName(group) == stack

        where:
        stack       | group
        ""          | "actiondrainer"
        ""          | "merchweb--loadtest"
        ""          | "discovery--us-east-1d"
        "test"      | "api-test-A"
        "dev"       | "discovery-dev"
        "us"        | "discovery-us-east-1d"
        "us"        | "evcache-us-east-1d-0"
        "us"        | "evcache-us----east-1d-0"
        "navigator" | "videometadata-navigator-integration-240-CAN"
    }

    void 'should extract cluster from group name'() {
        expect:
        Relationships.clusterFromGroupName(group) == cluster

        where:
        cluster                                       | group
        "actiondrainer"                               | "actiondrainer"
        "actiondrainer"                               | "actiondrainer-v301"
        "merchweb--loadtest"                          | "merchweb--loadtest"
        "discovery--us-east-1d-v"                     | "discovery--us-east-1d-v"
        "discovery--us-east-1d-v1"                    | "discovery--us-east-1d-v1"
        "discovery--us-east-1d-v11"                   | "discovery--us-east-1d-v11"
        "discovery--us-east-1d"                       | "discovery--us-east-1d-v111"
        "discovery--us-east-1d-v1111"                 | "discovery--us-east-1d-v1111"
        "merchweb-loadtest"                           | "merchweb-loadtest"
        "api-test-A"                                  | "api-test-A"
        "evcache-us-east-1d-0"                        | "evcache-us-east-1d-0"
        "evcache-us----east-1d-0"                     | "evcache-us----east-1d-0"
        "videometadata-navigator-integration-240-CAN" | "videometadata-navigator-integration-240-CAN"
    }

    @SuppressWarnings("GroovyAssignabilityCheck")
    void 'should check whether a name contains a reserved format'() {
        expect:
        Relationships.usesReservedFormat(groupName) == result

        where:
        result | groupName
        false  | "abha"
        true   | "abha-v999"
        false  | "abha-v9999999"
        true   | "integration-240-usa-iphone-v001"
        false  | "integration-240-usa-iphone-v22"
        true   | 'cass-nccpint-random-junk-c0northamerica-d0prod-h0gamesystems-p0vizio-r027-u0nccp-x0A-z0useast1a-v003'
        true   | 'c0northamerica'
        true   | 'junk-c0northamerica'
        true   | 'random-c0northamerica-junk'
        false  | 'random-abc0northamerica-junk'
    }

    void "should check whether a name follows the rules for a strict name"() {
        expect:
        Relationships.checkStrictName(name) == result

        where:
        name            | result
        "abha"          | true
        "account_batch" | false
        "account.batch" | false
        ""              | false
        null            | false
    }

    void "should check whether an app name is suitable for use in load balancer names"() {
        expect:
        Relationships.checkAppNameForLoadBalancer(app) == result

        where:
        app             | result
        "abha"          | true
        "account_batch" | false
        "account.batch" | false
        "account#batch" | false
        ""              | false
        null            | false
        "abhav309"      | false
        "abhav309787"   | true
        "v309"          | false
        "v3111111"      | true
    }

    void "should check if a name is okay to use for an application"() {
        expect:
        Relationships.checkName(name) == result

        where:
        name            | result
        "abha"          | true
        "account_batch" | true
        "account.batch" | true
        "account#batch" | false
        ""              | false
        null            | false
    }

    @SuppressWarnings("GroovyAssignabilityCheck")
    void "should check whether a details string is valid"() {
        expect:
        Relationships.checkDetail(detail) == result

        where:
        result | detail
        true   | "A"
        true   | "0"
        true   | "east-1c-0"
        true   | "230CAN-next-A"
        true   | "integration-240-USA"
        true   | "integration-240-usa-iphone-ipad-ios5-even-numbered-days-not-weekends"
        true   | "----"
        true   | "__._._--_.."
        false  | "230CAN#next-A"
        false  | ""
        false  | null
    }

    void "should build auto scaling group name from basic parts"() {
        expect:
        Relationships.buildGroupName([appName: app, stack: stack, detail: detail]) == group

        where:
        app          | stack        | detail       | group
        "helloworld" | "asgardtest" | null         | "helloworld-asgardtest"
        "helloworld" | "asgardtest" | ""           | "helloworld-asgardtest"
        "helloworld" | "asgardtest" | "2"          | "helloworld-asgardtest-2"
        "helloworld" | ""           | ""           | "helloworld"
        "helloworld" | null         | null         | "helloworld"
        "discovery"  | "us"         | "east-1d"    | "discovery-us-east-1d"
        "discovery"  | ""           | "us-east-1d" | "discovery--us-east-1d"
        "discovery"  | null         | "us-east-1d" | "discovery--us-east-1d"
        "merchweb"   | ""           | "loadtest"   | "merchweb--loadtest"
        "merchweb"   | null         | "loadtest"   | "merchweb--loadtest"
        "merchweb"   | null         | "loadtest"   | "merchweb--loadtest"
    }

    void "should build auto scaling group from many parts including labeled properties"() {

        expect:
        Relationships.buildGroupName(appName: "cass", stack: "nccpint", detail: "random-junk",
                countries: "northamerica", devPhase: "prod", hardware: "gamesystems", partners: "vizio", revision: "27",
                usedBy: "nccp", redBlackSwap: "A", zoneVar: "useast1a"
        ) == 'cass-nccpint-random-junk-c0northamerica-d0prod-h0gamesystems-p0vizio-r027-u0nccp-w0A-z0useast1a'

        Relationships.buildGroupName(appName: "cass", stack: "", detail: "random-junk", countries: null, devPhase: "",
                hardware: "gamesystems", partners: "", redBlackSwap: "A"
        ) == 'cass--random-junk-h0gamesystems-w0A'

        Relationships.buildGroupName(appName: "cass", stack: null, detail: null, devPhase: "", hardware: "gamesystems",
                partners: "", redBlackSwap: "A"
        ) == 'cass-h0gamesystems-w0A'
    }

    void "should fail to build an auto scaling group name based on invalid parts"() {

        when:
        Relationships.buildGroupName([appName: app, stack: "asgardtest", detail: "2"])

        then:
        thrown(exception)

        where:
        app  | exception
        ""   | IllegalArgumentException
        null | NullPointerException
    }

    void "should build launch configuration name"() {
        expect:
        Relationships.buildLaunchConfigurationName(group) ==~ ~launch

        where:
        group                        | launch
        "helloworld"                 | /helloworld-[0-9]{14}/
        "integration-240-usa-iphone" | /integration-240-usa-iphone-[0-9]{14}/
    }

    void "should build load balancer name from parts"() {
        expect:
        Relationships.buildLoadBalancerName(app, stack, detail) == loadBal

        where:
        app          | stack        | detail       | loadBal
        "helloworld" | "asgardtest" | null         | "helloworld-asgardtest"
        "helloworld" | "asgardtest" | ""           | "helloworld-asgardtest"
        "helloworld" | "asgardtest" | "frontend"   | "helloworld-asgardtest-frontend"
        "helloworld" | ""           | ""           | "helloworld"
        "helloworld" | null         | null         | "helloworld"
        "discovery"  | "us"         | "east-1d"    | "discovery-us-east-1d"
        "discovery"  | ""           | "frontend"   | "discovery--frontend"
        "discovery"  | null         | "us-east-1d" | "discovery--us-east-1d"
        "merchweb"   | ""           | "frontend"   | "merchweb--frontend"
        "merchweb"   | null         | "frontend"   | "merchweb--frontend"
    }

    void "should fail to build load balancer name from invalid parts"() {
        when:
        Relationships.buildLoadBalancerName(app, "asgardtest", "frontend")

        then:
        thrown(exception)

        where:
        app  | exception
        ""   | IllegalArgumentException
        null | NullPointerException
    }

    void "should parse base AMI ID from AMI description"() {
        expect:
        Relationships.baseAmiIdFromDescription(desc) == baseAmiId

        where:
        baseAmiId      | desc
        null           | ''
        null           | null
        'ami-50886239' | 'base_ami_id=ami-50886239,base_ami_name=servicenet-roku-qadd.dc.81210.10.44'
        'ami-1eb75c77' | 'base_ami_id=ami-1eb75c77,base_ami_name=servicenet-roku-qadd.dc.81210.10.44'
        'ami-1eb75c77' | 'base_ami_name=servicenet-roku-qadd.dc.81210.10.44,base_ami_id=ami-1eb75c77'
        'ami-7b4eb912' | 'store=ebs,ancestor_name=ebs-centosbase-x86_64-20101124,ancestor_id=ami-7b4eb912'
    }

    void "should parse base AMI name from AMI description"() {
        expect:
        Relationships.baseAmiNameFromDescription(desc) == baseAmiName

        where:
        baseAmiName                     | desc
        'servicenet-roku-qadd.dc.81210' | 'base_ami_id=ami-50886239,base_ami_name=servicenet-roku-qadd.dc.81210'
        'servicenet-roku-qadd.dc.81210' | 'base_ami_id=ami-1eb75c77,base_ami_name=servicenet-roku-qadd.dc.81210'
        'servicenet-roku-qadd.dc.81210' | 'base_ami_name=servicenet-roku-qadd.dc.81210,base_ami_id=ami-1eb75c77'
        'ebs-centosbase-x86_64-2010'    | 'store=ebs,ancestor_name=ebs-centosbase-x86_64-2010,ancestor_id=ami-7b4eb912'
    }

    void "should parse base AMI date from description"() {
        expect:
        Relationships.baseAmiDateFromDescription(desc) == dateTime

        where:
        desc                                                                         | dateTime
        'base_ami_id=ami-50886239,base_ami_name=servicenet-roku-qadd.dc.81210.10.44' | null
        'base_ami_id=ami-1eb75c77,base_ami_name=servicenet-roku-qadd.dc.81210.10.44' | null
        'base_ami_name=servicenet-roku-qadd.dc.81210.10.44,base_ami_id=ami-1eb75c77' | null
        'store=ebs,ancestor_name=centos-x86_64-20101124,ancestor_id=ami-7b4eb912'    | new DateTime(2010, 11, 24, 0, 0)
        'ancestor_name=centos-x86_64-20101124,ancestor_id=ami-7b4eb912'              | new DateTime(2010, 11, 24, 0, 0)
        'ancestor_id=ami-7b4eb912,ancestor_name=centos-x86_64-20101124'              | new DateTime(2010, 11, 24, 0, 0)
        'store=ebs,ancestor_name=centos-x86_64-20101124'                             | new DateTime(2010, 11, 24, 0, 0)
    }

    void "should build an alarm name for a scaling policy based on an auto scaling group name"() {
        expect:
        Relationships.buildAlarmName('helloworld--test-v000', '99999') == 'helloworld--test-v000-99999'
    }

    void "should build a scaling policy name based on an auto scaling group name"() {
        expect:
        Relationships.buildScalingPolicyName('helloworld--test-v000', '99999') == 'helloworld--test-v000-99999'
    }

    void "should create labeled environment variables"() {

        when:
        Names names = new Names('test')
        names.partners = 'sony'
        names.devPhase = 'stage'
        List<String> envVars = Relationships.labeledEnvironmentVariables(names, 'NETFLIX_')

        then:
        envVars == ['export NETFLIX_DEV_PHASE=stage', 'export NETFLIX_PARTNERS=sony']
    }

    void "should create labeled environment variables map"() {

        when:
        Names names = new Names('test')
        names.partners = 'sony'
        names.devPhase = 'stage'
        Map<String, String> envVars = Relationships.labeledEnvVarsMap(names, 'NETFLIX_')

        then:
        envVars == ['NETFLIX_DEV_PHASE': 'stage', 'NETFLIX_PARTNERS': 'sony']
    }

    void "should show pretty-formatted map keys for labeled variables"() {
        when:
        Names names = new Names('test-p0sony-d0stage')

        then:
        Relationships.parts(names) == ['Dev Phase': 'stage', 'Partners': 'sony']
    }
}
