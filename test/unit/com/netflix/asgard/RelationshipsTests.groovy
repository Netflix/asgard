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

import com.amazonaws.services.autoscaling.model.AutoScalingGroup
import com.netflix.asgard.mock.Mocks
import com.netflix.asgard.model.AutoScalingGroupData
import com.netflix.frigga.Names
import com.netflix.frigga.ami.AppVersion
import grails.test.GrailsUnitTestCase
import org.joda.time.DateTime

@SuppressWarnings("GroovyAccessibility")
class RelationshipsTests extends GrailsUnitTestCase {

    void setUp() {
        Mocks.createDynamicMethods()
    }

    private void assertPushSequenceSortResult(List<String> expectedResult, List<String> input) {
        assert expectedResult == input.collect {
            AutoScalingGroupData.from(new AutoScalingGroup().withAutoScalingGroupName(it), null, null, null, [])
        }.sort(Relationships.PUSH_SEQUENCE_COMPARATOR).collect { it.autoScalingGroupName }
    }

    void testPushSequenceComparator() {

        Mocks.awsAutoScalingService()

        assertPushSequenceSortResult(["discovery-dev",
                "discovery-dev-v997",
                "discovery-dev-v998",
                "discovery-dev-v999",
                "discovery-dev-v000",
                "discovery-dev-v001",
                "discovery-dev-v002",
                "discovery-dev-v003"
        ], [
                "discovery-dev-v997",
                "discovery-dev-v003",
                "discovery-dev-v999",
                "discovery-dev-v001",
                "discovery-dev",
                "discovery-dev-v998",
                "discovery-dev-v002",
                "discovery-dev-v000"
        ])

        assertPushSequenceSortResult(["discovery-dev", "discovery-dev-v000"], ["discovery-dev", "discovery-dev-v000"])
        assertPushSequenceSortResult(["discovery-dev", "discovery-dev-v000"], ["discovery-dev-v000", "discovery-dev"])
        assertPushSequenceSortResult(
                ["discovery-dev", "discovery-dev-v000", "discovery-dev-v001"],
                ["discovery-dev-v001", "discovery-dev", "discovery-dev-v000"])
        assertPushSequenceSortResult([
                "discovery-dev-v001", "discovery-dev-v002"],
                ["discovery-dev-v001", "discovery-dev-v002"])
        assertPushSequenceSortResult(
                ["discovery-dev-v001", "discovery-dev-v002"],
                ["discovery-dev-v002", "discovery-dev-v001"])
        assertPushSequenceSortResult(
                ["discovery-dev-v563", "discovery-dev-v564", "discovery-dev-v565"],
                ["discovery-dev-v563", "discovery-dev-v565", "discovery-dev-v564"])
        assertPushSequenceSortResult(
                ["discovery-dev-v998", "discovery-dev-v999", "discovery-dev-v000"],
                ["discovery-dev-v000", "discovery-dev-v998", "discovery-dev-v999"])
        assertPushSequenceSortResult(
                ["discovery-dev-v998", "discovery-dev-v999", "discovery-dev-v000"],
                ["discovery-dev-v000", "discovery-dev-v999", "discovery-dev-v998"])
        assertPushSequenceSortResult(
                ["discovery-dev-v999", "discovery-dev-v000", "discovery-dev-v001"],
                ["discovery-dev-v000", "discovery-dev-v999", "discovery-dev-v001"])
    }

    void testBuildNextAutoScalingGroupName() {
        assert "discovery-dev-v000" == Relationships.buildNextAutoScalingGroupName("discovery-dev")
        assert "discovery-dev-v000" == Relationships.buildNextAutoScalingGroupName("discovery-dev-v999")
        assert "discovery-dev-v999" == Relationships.buildNextAutoScalingGroupName("discovery-dev-v998")
        assert "discovery-dev-v998" == Relationships.buildNextAutoScalingGroupName("discovery-dev-v997")
        assert "discovery-dev-v001" == Relationships.buildNextAutoScalingGroupName("discovery-dev-v000")
        assert "discovery-dev-v002" == Relationships.buildNextAutoScalingGroupName("discovery-dev-v001")
        assert "discovery-dev-v003" == Relationships.buildNextAutoScalingGroupName("discovery-dev-v002")
        assert "discovery-dev-v522" == Relationships.buildNextAutoScalingGroupName("discovery-dev-v521")
    }

    void testDissectGroupNameWithDot() {

        Names names = Relationships.dissectCompoundName("chukwa.collector_1-v889")
        assert "chukwa.collector_1-v889" == names.group
        assert "chukwa.collector_1" == names.cluster
        assert "chukwa.collector_1" == names.app
        assert null == names.stack
        assert null == names.detail
        assert "v889" == names.push
        assert 889 == names.sequence
    }

    void testDissectGroupNameInvalid() {

        Names names = Relationships.dissectCompoundName('nccp-moviecontrol%27')
        assert null == names.group
        assert null == names.cluster
        assert null == names.app
        assert null == names.stack
        assert null == names.detail
        assert null == names.push
        assert null == names.sequence
    }

    void testDissectGroupName() {

        Names names = Relationships.dissectCompoundName(null)
        assert null == names.group
        assert null == names.cluster
        assert null == names.app
        assert null == names.stack
        assert null == names.detail
        assert null == names.push
        assert null == names.sequence

        names = Relationships.dissectCompoundName("actiondrainer")
        assert "actiondrainer" == names.group
        assert "actiondrainer" == names.cluster
        assert "actiondrainer" == names.app
        assert null == names.stack
        assert null == names.detail
        assert null == names.push
        assert null == names.sequence

        names = Relationships.dissectCompoundName("actiondrainer-v003")
        assert "actiondrainer-v003" == names.group
        assert "actiondrainer" == names.cluster
        assert "actiondrainer" == names.app
        assert null == names.stack
        assert null == names.detail
        assert "v003" == names.push
        assert 3 == names.sequence

        names = Relationships.dissectCompoundName("actiondrainer--v003")
        assert "actiondrainer--v003" == names.group
        assert "actiondrainer-" == names.cluster
        assert "actiondrainer" == names.app
        assert null == names.stack
        assert null == names.detail
        assert "v003" == names.push
        assert 3 == names.sequence

        names = Relationships.dissectCompoundName("actiondrainer---v003")
        assert "actiondrainer---v003" == names.group
        assert "actiondrainer--" == names.cluster
        assert "actiondrainer" == names.app
        assert null == names.stack
        assert null == names.detail
        assert "v003" == names.push
        assert 3 == names.sequence

        names = Relationships.dissectCompoundName("api-test-A")
        assert "api-test-A" == names.group
        assert "api-test-A" == names.cluster
        assert "api" == names.app
        assert "test" == names.stack
        assert "A" == names.detail
        assert null == names.push

        names = Relationships.dissectCompoundName("api-test-A-v406")
        assert "api-test-A-v406" == names.group
        assert "api-test-A" == names.cluster
        assert "api" == names.app
        assert "test" == names.stack
        assert "A" == names.detail
        assert "v406" == names.push
        assert 406 == names.sequence

        names = Relationships.dissectCompoundName("api-test101")
        assert "api-test101" == names.group
        assert "api-test101" == names.cluster
        assert "api" == names.app
        assert "test101" == names.stack
        assert null == names.detail
        assert null == names.push
        assert null == names.sequence

        names = Relationships.dissectCompoundName("chukwacollector_1")
        assert "chukwacollector_1" == names.group
        assert "chukwacollector_1" == names.cluster
        assert "chukwacollector_1" == names.app
        assert null == names.stack
        assert null == names.detail
        assert null == names.push
        assert null == names.sequence

        names = Relationships.dissectCompoundName("chukwacollector_1-v889")
        assert "chukwacollector_1-v889" == names.group
        assert "chukwacollector_1" == names.cluster
        assert "chukwacollector_1" == names.app
        assert null == names.stack
        assert null == names.detail
        assert "v889" == names.push
        assert 889 == names.sequence

        names = Relationships.dissectCompoundName("api-test-A")
        assert "api-test-A" == names.group
        assert "api-test-A" == names.cluster
        assert "api" == names.app
        assert "test" == names.stack
        assert "A" == names.detail
        assert null == names.push
        assert null == names.sequence

        names = Relationships.dissectCompoundName("discovery-dev")
        assert "discovery-dev" == names.group
        assert "discovery-dev" == names.cluster
        assert "discovery" == names.app
        assert "dev" == names.stack
        assert null == names.detail
        assert null == names.push
        assert null == names.sequence

        names = Relationships.dissectCompoundName("discovery-us-east-1d")
        assert "discovery-us-east-1d" == names.group
        assert "discovery-us-east-1d" == names.cluster
        assert "discovery" == names.app
        assert "us" == names.stack
        assert "east-1d" == names.detail
        assert null == names.push
        assert null == names.sequence

        names = Relationships.dissectCompoundName("evcache-us-east-1d-0")
        assert "evcache-us-east-1d-0" == names.group
        assert "evcache-us-east-1d-0" == names.cluster
        assert "evcache" == names.app
        assert "us" == names.stack
        assert "east-1d-0" == names.detail
        assert null == names.push
        assert null == names.sequence

        names = Relationships.dissectCompoundName("evcache-us-east-1d-0-v223")
        assert "evcache-us-east-1d-0-v223" == names.group
        assert "evcache-us-east-1d-0" == names.cluster
        assert "evcache" == names.app
        assert "us" == names.stack
        assert "east-1d-0" == names.detail
        assert "v223" == names.push
        assert 223 == names.sequence

        names = Relationships.dissectCompoundName("videometadata-navigator-integration-240-CAN")
        assert "videometadata-navigator-integration-240-CAN" == names.group
        assert "videometadata-navigator-integration-240-CAN" == names.cluster
        assert "videometadata" == names.app
        assert "navigator" == names.stack
        assert "integration-240-CAN" == names.detail
        assert null == names.push
        assert null == names.sequence
    }

    void testDissectGroupNameWithLabeledVariables() {

        Names names = Relationships.dissectCompoundName("actiondrainer")
        assert "actiondrainer" == names.group
        assert "actiondrainer" == names.cluster
        assert "actiondrainer" == names.app
        assert null == names.stack
        assert null == names.detail
        assert null == names.push
        assert null == names.sequence
        assert null == names.countries
        assert null == names.devPhase
        assert null == names.hardware
        assert null == names.partners
        assert null == names.revision
        assert null == names.usedBy
        assert null == names.redBlackSwap
        assert null == names.zone

        names = Relationships.dissectCompoundName(
                'cass-nccpintegration-random-junk-c0northamerica-d0prod-h0gamesystems-p0vizio-r027-u0nccp-w0A-z0useast1a-v003')
        assert 'cass-nccpintegration-random-junk-c0northamerica-d0prod-h0gamesystems-p0vizio-r027-u0nccp-w0A-z0useast1a-v003' == names.group
        assert 'cass-nccpintegration-random-junk-c0northamerica-d0prod-h0gamesystems-p0vizio-r027-u0nccp-w0A-z0useast1a' == names.cluster
        assert 'cass' == names.app
        assert 'nccpintegration' == names.stack
        assert 'random-junk' == names.detail
        assert 'v003' == names.push
        assert 3 == names.sequence
        assert 'northamerica' == names.countries
        assert 'prod' == names.devPhase
        assert 'gamesystems' == names.hardware
        assert 'vizio' == names.partners
        assert '27' == names.revision
        assert 'nccp' == names.usedBy
        assert 'A' == names.redBlackSwap
        assert 'useast1a' == names.zone

        names = Relationships.dissectCompoundName('cass-nccpintegration-c0northamerica-d0prod')
        assert 'cass-nccpintegration-c0northamerica-d0prod' == names.group
        assert 'cass-nccpintegration-c0northamerica-d0prod' == names.cluster
        assert 'cass' == names.app
        assert 'nccpintegration' == names.stack
        assert null == names.detail
        assert null == names.push
        assert null == names.sequence
        assert 'northamerica' == names.countries
        assert 'prod' == names.devPhase
        assert null == names.hardware
        assert null == names.partners
        assert null == names.revision
        assert null == names.usedBy
        assert null == names.redBlackSwap
        assert null == names.zone

        names = Relationships.dissectCompoundName('cass--my-stuff-c0northamerica-d0prod')
        assert 'cass--my-stuff-c0northamerica-d0prod' == names.group
        assert 'cass--my-stuff-c0northamerica-d0prod' == names.cluster
        assert 'cass' == names.app
        assert null == names.stack
        assert 'my-stuff' == names.detail
        assert null == names.push
        assert null == names.sequence
        assert 'northamerica' == names.countries
        assert 'prod' == names.devPhase
        assert null == names.hardware
        assert null == names.partners
        assert null == names.revision
        assert null == names.usedBy
        assert null == names.redBlackSwap
        assert null == names.zone

        names = Relationships.dissectCompoundName('cass-c0northamerica-d0prod')
        assert 'cass-c0northamerica-d0prod' == names.group
        assert 'cass-c0northamerica-d0prod' == names.cluster
        assert 'cass' == names.app
        assert null == names.stack
        assert null == names.detail
        assert null == names.push
        assert null == names.sequence
        assert 'northamerica' == names.countries
        assert 'prod' == names.devPhase
        assert null == names.hardware
        assert null == names.partners
        assert null == names.revision
        assert null == names.usedBy
        assert null == names.redBlackSwap
        assert null == names.zone

        names = Relationships.dissectCompoundName('cass-c0northamerica-d0prod-v102')
        assert 'cass-c0northamerica-d0prod-v102' == names.group
        assert 'cass-c0northamerica-d0prod' == names.cluster
        assert 'cass' == names.app
        assert null == names.stack
        assert null == names.detail
        assert 'v102' == names.push
        assert 102 == names.sequence
        assert 'northamerica' == names.countries
        assert 'prod' == names.devPhase
        assert null == names.hardware
        assert null == names.partners
        assert null == names.revision
        assert null == names.usedBy
        assert null == names.redBlackSwap
        assert null == names.zone

        names = Relationships.dissectCompoundName('cass-v102')
        assert 'cass-v102' == names.group
        assert 'cass' == names.cluster
        assert 'cass' == names.app
        assert null == names.stack
        assert null == names.detail
        assert 'v102' == names.push
        assert 102 == names.sequence
        assert null == names.countries
        assert null == names.devPhase
        assert null == names.hardware
        assert null == names.partners
        assert null == names.revision
        assert null == names.usedBy
        assert null == names.redBlackSwap
        assert null == names.zone
    }

    void testDissectAppVersion() {

        AppVersion appVersion = Relationships.dissectAppVersion("helloworld-1.0.0-592112.h154/WE-WAPP-helloworld/154")
        assert "helloworld" == appVersion.packageName
        assert "1.0.0" == appVersion.version
        assert "592112" == appVersion.commit
        assert "154" == appVersion.buildNumber
        assert "WE-WAPP-helloworld" == appVersion.buildJobName

        appVersion = Relationships.dissectAppVersion("helloworld-server-1.0.0-592112.h154/WE-WAPP-helloworld/154")
        assert "helloworld-server" == appVersion.packageName
        assert "1.0.0" == appVersion.version
        assert "592112" == appVersion.commit
        assert "154" == appVersion.buildNumber
        assert "WE-WAPP-helloworld" == appVersion.buildJobName

        appVersion = Relationships.dissectAppVersion("helloworld-1.0.0-592112.h154")
        assert "helloworld" == appVersion.packageName
        assert "1.0.0" == appVersion.version
        assert "592112" == appVersion.commit
        assert "154" == appVersion.buildNumber
        assertNull appVersion.buildJobName

        appVersion = Relationships.dissectAppVersion("helloworld-1.0.0-592112")
        assert "helloworld" == appVersion.packageName
        assert "1.0.0" == appVersion.version
        assert "592112" == appVersion.commit
        assertNull appVersion.buildNumber
        assertNull appVersion.buildJobName

        assertNull Relationships.dissectAppVersion(null)
        assertNull Relationships.dissectAppVersion("")
        assertNull Relationships.dissectAppVersion("blah blah blah")
    }

    void testPackageFromAppVersion() {
        assert 'helloworld' == Relationships.packageFromAppVersion('helloworld-1.0.0-592112.h154/WE-WAPP-helloworld/154')
        assert null == Relationships.packageFromAppVersion(null)
        assert null == Relationships.packageFromAppVersion('')
        assert null == Relationships.packageFromAppVersion('dfjsdfkjsdfkjsd fkjsdf kljsdf ksjdf klsdjf sd')
    }

    void testAppNameFromGroupName() {
        assert "actiondrainer" == Relationships.appNameFromGroupName("actiondrainer")
        assert "merchweb" == Relationships.appNameFromGroupName("merchweb--loadtest")
        assert "discovery" == Relationships.appNameFromGroupName("discovery--us-east-1d")
        assert "merchweb" == Relationships.appNameFromGroupName("merchweb-loadtest")
        assert "api" == Relationships.appNameFromGroupName("api-test-A")
        assert "discovery" == Relationships.appNameFromGroupName("discovery-dev")
        assert "discovery" == Relationships.appNameFromGroupName("discovery-us-east-1d")
        assert "evcache" == Relationships.appNameFromGroupName("evcache-us-east-1d-0")
        assert "evcache" == Relationships.appNameFromGroupName("evcache-us----east-1d-0")
        assert "videometadata" == Relationships.appNameFromGroupName("videometadata-navigator-integration-240-CAN")
    }

    void testAppNameFromLaunchConfigName() {
        assert "actiondrainer" == Relationships.appNameFromLaunchConfigName("actiondrainer-201010231745")
        assert "merchweb" == Relationships.appNameFromLaunchConfigName("merchweb--loadtest-201010231745")
        assert "discovery" == Relationships.appNameFromLaunchConfigName("discovery--us-east-1d-201010231745")
        assert "merchweb" == Relationships.appNameFromLaunchConfigName("merchweb-loadtest-201010231745")
        assert "api" == Relationships.appNameFromLaunchConfigName("api-test-A-201010231745")
        assert "discovery" == Relationships.appNameFromLaunchConfigName("discovery-dev-201010231745")
        assert "discovery" == Relationships.appNameFromLaunchConfigName("discovery-us-east-1d-201010231745")
        assert "evcache" == Relationships.appNameFromLaunchConfigName("evcache-us-east-1d-0-201010231745")
        assert "evcache" == Relationships.appNameFromLaunchConfigName("evcache-us----east-1d-0-201010231745")
        assert "videometadata" == Relationships.appNameFromLaunchConfigName("videometadata-navigator-integration-240-CAN-201010231745")
    }

    void testAppNameFromLoadBalancerName() {
        assert "actiondrainer" == Relationships.appNameFromLoadBalancerName("actiondrainer-frontend")
        assert "merchweb" == Relationships.appNameFromLoadBalancerName("merchweb--loadtest-frontend")
        assert "discovery" == Relationships.appNameFromLoadBalancerName("discovery--us-east-1d-frontend")
        assert "merchweb" == Relationships.appNameFromLoadBalancerName("merchweb-loadtest-frontend")
        assert "api" == Relationships.appNameFromLoadBalancerName("api-test-A-frontend")
        assert "discovery" == Relationships.appNameFromLoadBalancerName("discovery-dev-frontend")
        assert "discovery" == Relationships.appNameFromLoadBalancerName("discovery-us-east-1d-frontend")
        assert "evcache" == Relationships.appNameFromLoadBalancerName("evcache-us-east-1d-0-frontend")
        assert "evcache" == Relationships.appNameFromLoadBalancerName("evcache-us----east-1d-0-frontend")
        assert "videometadata" == Relationships.appNameFromLoadBalancerName("videometadata-navigator-integration-240-CAN-frontend")
    }

    void testStackNameFromGroupName() {
        assert "" == Relationships.stackNameFromGroupName("actiondrainer")
        assert "" == Relationships.stackNameFromGroupName("merchweb--loadtest")
        assert "" == Relationships.stackNameFromGroupName("discovery--us-east-1d")
        assert "loadtest" == Relationships.stackNameFromGroupName("merchweb-loadtest")
        assert "test" == Relationships.stackNameFromGroupName("api-test-A")
        assert "dev" == Relationships.stackNameFromGroupName("discovery-dev")
        assert "us" == Relationships.stackNameFromGroupName("discovery-us-east-1d")
        assert "us" == Relationships.stackNameFromGroupName("evcache-us-east-1d-0")
        assert "us" == Relationships.stackNameFromGroupName("evcache-us----east-1d-0")
        assert "navigator" == Relationships.stackNameFromGroupName("videometadata-navigator-integration-240-CAN")
    }

    void testClusterFromGroupName() {
        assert "actiondrainer" == Relationships.clusterFromGroupName("actiondrainer")
        assert "actiondrainer" == Relationships.clusterFromGroupName("actiondrainer-v301")
        assert "merchweb--loadtest" == Relationships.clusterFromGroupName("merchweb--loadtest")
        assert "discovery--us-east-1d-v" == Relationships.clusterFromGroupName("discovery--us-east-1d-v")
        assert "discovery--us-east-1d-v1" == Relationships.clusterFromGroupName("discovery--us-east-1d-v1")
        assert "discovery--us-east-1d-v11" == Relationships.clusterFromGroupName("discovery--us-east-1d-v11")
        assert "discovery--us-east-1d" == Relationships.clusterFromGroupName("discovery--us-east-1d-v111")
        assert "discovery--us-east-1d-v1111" == Relationships.clusterFromGroupName("discovery--us-east-1d-v1111")
        assert "merchweb-loadtest" == Relationships.clusterFromGroupName("merchweb-loadtest")
        assert "api-test-A" == Relationships.clusterFromGroupName("api-test-A")
        assert "evcache-us-east-1d-0" == Relationships.clusterFromGroupName("evcache-us-east-1d-0")
        assert "evcache-us----east-1d-0" == Relationships.clusterFromGroupName("evcache-us----east-1d-0")
        assert "videometadata-navigator-integration-240-CAN" == Relationships.clusterFromGroupName("videometadata-navigator-integration-240-CAN")
    }

    void testAvoidsReservedFormat() {
        assert !Relationships.usesReservedFormat("abha")
        assert Relationships.usesReservedFormat("abha-v999")
        assert !Relationships.usesReservedFormat("abha-v9999999")
        assert Relationships.usesReservedFormat("integration-240-usa-iphone-v001")
        assert !Relationships.usesReservedFormat("integration-240-usa-iphone-v22")

        assert Relationships.usesReservedFormat("integration-v001-usa-iphone")
        assert Relationships.usesReservedFormat('cass-nccpintegration-random-junk-c0northamerica-d0prod-h0gamesystems-p0vizio-r027-u0nccp-x0A-z0useast1a-v003')
        assert Relationships.usesReservedFormat('c0northamerica')
        assert Relationships.usesReservedFormat('junk-c0northamerica')
        assert Relationships.usesReservedFormat('c0northamerica')
        assert Relationships.usesReservedFormat('random-c0northamerica-junk')
        assert !Relationships.usesReservedFormat('random-abc0northamerica-junk')
    }

    void testCheckStrictName() {
        assert Relationships.checkStrictName("abha")
        assert !Relationships.checkStrictName("account_batch")
        assert !Relationships.checkStrictName("account.batch")
        assert !Relationships.checkStrictName("")
        assert !Relationships.checkStrictName(null)
    }

    void testCheckAppNameForLoadBalancer() {
        assert Relationships.checkAppNameForLoadBalancer("abha")
        assert !Relationships.checkAppNameForLoadBalancer("account_batch")
        assert !Relationships.checkAppNameForLoadBalancer("account.batch")
        assert !Relationships.checkAppNameForLoadBalancer("account#batch")
        assert !Relationships.checkAppNameForLoadBalancer("")
        assert !Relationships.checkAppNameForLoadBalancer(null)
        assert !Relationships.checkAppNameForLoadBalancer("abhav309")
        assert Relationships.checkAppNameForLoadBalancer("abhav309787")
        assert !Relationships.checkAppNameForLoadBalancer("v309")
        assert Relationships.checkAppNameForLoadBalancer("v3111111")
    }

    void testCheckName() {
        assert Relationships.checkName("abha")
        assert Relationships.checkName("account_batch")
        assert Relationships.checkName("account.batch")
        assert !Relationships.checkName("account#batch")
        assert !Relationships.checkName("")
        assert !Relationships.checkName(null)
    }

    void testDetail() {
        assert Relationships.checkDetail("A")
        assert Relationships.checkDetail("0")
        assert Relationships.checkDetail("east-1c-0")
        assert Relationships.checkDetail("230CAN-next-A")
        assert Relationships.checkDetail("integration-240-USA")
        assert Relationships.checkDetail("integration-240-usa-iphone-ipad-ios5-even-numbered-days-not-weekends")
        assert Relationships.checkDetail("----")
        assert Relationships.checkDetail("__._._--_..")
        assert !Relationships.checkDetail("230CAN#next-A")
        assert !Relationships.checkDetail("")
        assert !Relationships.checkDetail(null)
    }

    void testBuildAutoScalingGroupName() {

        assert "helloworld-asgardtest" == Relationships.buildGroupName([appName: "helloworld", stack: "asgardtest", detail: null])
        assert "helloworld-asgardtest" == Relationships.buildGroupName([appName: "helloworld", stack: "asgardtest", detail: ""])
        assert "helloworld-asgardtest-2" == Relationships.buildGroupName([appName: "helloworld", stack: "asgardtest", detail: "2"])
        assert "helloworld" == Relationships.buildGroupName([appName: "helloworld", stack: "", detail: ""])
        assert "helloworld" == Relationships.buildGroupName([appName: "helloworld", stack: null, detail: null])
        assert "discovery-us-east-1d" == Relationships.buildGroupName([appName: "discovery", stack: "us", detail: "east-1d"])
        assert "discovery--us-east-1d" == Relationships.buildGroupName([appName: "discovery", stack: "", detail: "us-east-1d"])
        assert "discovery--us-east-1d" == Relationships.buildGroupName([appName: "discovery", stack: null, detail: "us-east-1d"])
        assert "merchweb--loadtest" == Relationships.buildGroupName([appName: "merchweb", stack: "", detail: "loadtest"])
        assert "merchweb--loadtest" == Relationships.buildGroupName([appName: "merchweb", stack: null, detail: "loadtest"])

        def exceptionThrown = false
        try { Relationships.buildGroupName([appName: "", stack: "asgardtest", detail: "2"]) } catch (IllegalArgumentException ignored) { exceptionThrown = true }
        assert exceptionThrown

        def npeThrown = false
        try { Relationships.buildGroupName([appName: null, stack: "asgardtest", detail: "2"]) } catch (NullPointerException ignored) { npeThrown = true }
        assert npeThrown

        assert "helloworld-asgardtest" == Relationships.buildGroupName([appName: "helloworld", stack: "asgardtest", detail: null])

        assert 'cass-nccpintegration-random-junk-c0northamerica-d0prod-h0gamesystems-p0vizio-r027-u0nccp-w0A-z0useast1a' ==
                Relationships.buildGroupName(appName: "cass", stack: "nccpintegration",
                        detail: "random-junk", countries: "northamerica", devPhase: "prod",
                        hardware: "gamesystems", partners: "vizio", revision: "27", usedBy: "nccp", redBlackSwap: "A",
                        zoneVar: "useast1a")

        assert 'cass--random-junk-h0gamesystems-w0A' ==
                Relationships.buildGroupName(appName: "cass", stack: "",
                        detail: "random-junk", countries: null, devPhase: "",
                        hardware: "gamesystems", partners: "", redBlackSwap: "A")

        assert 'cass-h0gamesystems-w0A' ==
                Relationships.buildGroupName(appName: "cass", stack: null, detail: null, devPhase: "",
                        hardware: "gamesystems", partners: "", redBlackSwap: "A")
    }

    void testBuildLaunchConfigurationName() {
        assert Relationships.buildLaunchConfigurationName("helloworld") ==~ ~/helloworld-[0-9]{14}/
        assert Relationships.buildLaunchConfigurationName("integration-240-usa-iphone") ==~ ~/integration-240-usa-iphone-[0-9]{14}/
    }

    void testBuildLoadBalancerName() {
        assert "helloworld-asgardtest" == Relationships.buildLoadBalancerName("helloworld", "asgardtest", null)
        assert "helloworld-asgardtest" == Relationships.buildLoadBalancerName("helloworld", "asgardtest", "")
        assert "helloworld-asgardtest-frontend" == Relationships.buildLoadBalancerName("helloworld", "asgardtest", "frontend")
        assert "helloworld" == Relationships.buildLoadBalancerName("helloworld", "", "")
        assert "helloworld" == Relationships.buildLoadBalancerName("helloworld", null, null)
        assert "discovery-us-east-1d" == Relationships.buildLoadBalancerName("discovery", "us", "east-1d")
        assert "discovery--frontend" == Relationships.buildLoadBalancerName("discovery", "", "frontend")
        assert "discovery--us-east-1d" == Relationships.buildLoadBalancerName("discovery", null, "us-east-1d")
        assert "merchweb--frontend" == Relationships.buildLoadBalancerName("merchweb", "", "frontend")
        assert "merchweb--frontend" == Relationships.buildLoadBalancerName("merchweb", null, "frontend")

        def exceptionThrown = false
        try { Relationships.buildLoadBalancerName("", "asgardtest", "frontend") } catch (IllegalArgumentException ignored) { exceptionThrown = true }
        assert exceptionThrown

        def npeThrown = false
        try { Relationships.buildLoadBalancerName(null, "asgardtest", "frontend") } catch (NullPointerException ignored) { npeThrown = true }
        assert npeThrown
    }

    void testBaseAmiIdFromDescription() {
        assertNull Relationships.baseAmiIdFromDescription('')
        assertNull Relationships.baseAmiIdFromDescription(null)
        assert 'ami-50886239' == Relationships.baseAmiIdFromDescription('base_ami_id=ami-50886239,base_ami_name=servicenet-roku-qadd.dc.81210.10.44')
        assert 'ami-1eb75c77' == Relationships.baseAmiIdFromDescription('base_ami_id=ami-1eb75c77,base_ami_name=servicenet-roku-qadd.dc.81210.10.44')
        assert 'ami-1eb75c77' == Relationships.baseAmiIdFromDescription('base_ami_name=servicenet-roku-qadd.dc.81210.10.44,base_ami_id=ami-1eb75c77')
        assert 'ami-7b4eb912' == Relationships.baseAmiIdFromDescription('store=ebs,ancestor_name=ebs-centosbase-x86_64-20101124,ancestor_id=ami-7b4eb912')
    }

    void testBaseAmiNameFromDescription() {
        assert 'servicenet-roku-qadd.dc.81210.10.44' == Relationships.baseAmiNameFromDescription('base_ami_id=ami-50886239,base_ami_name=servicenet-roku-qadd.dc.81210.10.44')
        assert 'servicenet-roku-qadd.dc.81210.10.44' == Relationships.baseAmiNameFromDescription('base_ami_id=ami-1eb75c77,base_ami_name=servicenet-roku-qadd.dc.81210.10.44')
        assert 'servicenet-roku-qadd.dc.81210.10.44' == Relationships.baseAmiNameFromDescription('base_ami_name=servicenet-roku-qadd.dc.81210.10.44,base_ami_id=ami-1eb75c77')
        assert 'ebs-centosbase-x86_64-20101124' == Relationships.baseAmiNameFromDescription('store=ebs,ancestor_name=ebs-centosbase-x86_64-20101124,ancestor_id=ami-7b4eb912')
    }

    void testBaseAmiDateFromDescription() {
        assertNull Relationships.baseAmiDateFromDescription('base_ami_id=ami-50886239,base_ami_name=servicenet-roku-qadd.dc.81210.10.44')
        assertNull Relationships.baseAmiDateFromDescription('base_ami_id=ami-1eb75c77,base_ami_name=servicenet-roku-qadd.dc.81210.10.44')
        assertNull Relationships.baseAmiDateFromDescription('base_ami_name=servicenet-roku-qadd.dc.81210.10.44,base_ami_id=ami-1eb75c77')
        assert new DateTime(2010, 11, 24, 0, 0, 0, 0) == Relationships.baseAmiDateFromDescription('store=ebs,ancestor_name=ebs-centosbase-x86_64-20101124,ancestor_id=ami-7b4eb912')
    }

    void testBuildAlarmNameForScalingPolicy() {
        assert 'helloworld--scalingtest-v000-99999' == Relationships.buildAlarmName(
                'helloworld--scalingtest-v000', '99999')
    }

    void testBuildPolicyName() {
        assert 'helloworld--scalingtest-v000-99999' == Relationships.buildScalingPolicyName(
                'helloworld--scalingtest-v000', '99999')
    }

    void testLabeledEnvironmentVariables() {
        Names names = new Names('test')
        names.partners = 'sony'
        assert ['export NETFLIX_PARTNERS=sony'] == Relationships.labeledEnvironmentVariables(names, 'NETFLIX_')
        names.devPhase = 'stage'
        assert ['export NETFLIX_DEV_PHASE=stage', 'export NETFLIX_PARTNERS=sony'] ==
                Relationships.labeledEnvironmentVariables(names, 'NETFLIX_')
    }

    void testParts() {
        Names names = new Names('test')
        names.partners = 'sony'
        assert ['Partners': 'sony'] == Relationships.parts(names)
        names.devPhase = 'stage'
        assert ['Dev Phase': 'stage', 'Partners': 'sony'] == Relationships.parts(names)
    }

}
