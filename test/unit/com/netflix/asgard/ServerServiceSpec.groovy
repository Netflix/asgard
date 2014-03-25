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

import com.amazonaws.services.ec2.model.Instance
import com.netflix.asgard.model.ApplicationInstance
import com.netflix.asgard.model.GroupedInstance
import com.netflix.asgard.push.Cluster
import spock.lang.Specification
import spock.lang.Unroll

@SuppressWarnings("GroovyAssignabilityCheck")
class ServerServiceSpec extends Specification {

    AwsAutoScalingService awsAutoScalingService = Mock(AwsAutoScalingService)
    ConfigService configService = Mock(ConfigService)
    EnvironmentService environmentService = Mock(EnvironmentService)
    FlagService flagService = Mock(FlagService)
    InitService initService = Mock(InitService)
    ServerService serverService = new ServerService(awsAutoScalingService: awsAutoScalingService,
            configService: configService, environmentService: environmentService, initService: initService,
            flagService: flagService)

    void setup() {
        new MonkeyPatcherService().createDynamicMethods()
    }

    @Unroll("it's #blocked that traffic gets blocked iff caches are filled is #filled and skip flag is off is #flagOff")
    void 'should indicate that user requests should be blocked only if caches are empty and skip flag is off'() {

        when:
        Boolean shouldBlock = serverService.shouldCacheLoadingBlockUserRequests()

        then:
        initService.cachesFilled() >> filled
        flagService.isOff(Flag.SKIP_CACHE_FILL) >> flagOff
        shouldBlock == blocked

        where:
        filled | flagOff | blocked
        true   | true    | false
        false  | true    | true
        true   | false   | false
        false  | false   | false
    }

    void 'should list zero other servers if not deployed with a known clustering strategy'() {

        expect:
        serverService.listRemoteServerNamesAndPorts() == []
    }

    void 'should list configured other server port combos'() {

        when:
        List<String> serverNamesAndPorts = serverService.listRemoteServerNamesAndPorts()

        then:
        serverNamesAndPorts == ['localhost:8081']
        1 * configService.otherServerNamePortCombos >> ['localhost:8081']
        0 * _
    }

    void 'should list other server port combos in cluster'() {

        String localInstanceId = 'i-bbbb'
        serverService.environmentService = environmentService
        Cluster cluster = Mock(Cluster) {
            getInstances() >> [
                    instanceWithEureka('i-aaaa', hostA, pubDnsA, pubIpA, priDnsA, priIpA, appPort),
                    instanceWithEureka(localInstanceId, 'ec2-b', null, null, null, null, '8080'),
                    instanceWithEureka('i-cccc', 'ec2-c', null, null, null, null, '9023'),
            ]
        }
        when:
        List<String> serverNamesAndPorts = serverService.listRemoteServerNamesAndPorts()

        then:
        serverNamesAndPorts == result
        1 * configService.otherServerNamePortCombos >> []
        1 * configService.userDataVarPrefix >> 'CLOUD_'
        1 * environmentService.instanceId >> localInstanceId
        1 * environmentService.getEnvironmentVariable('EC2_REGION') >> 'us-west-1'
        1 * environmentService.getEnvironmentVariable('CLOUD_CLUSTER') >> 'asgard'
        1 * configService.portForOtherServersInCluster >> confPort
        1 * awsAutoScalingService.getCluster(_, 'asgard') >> cluster

        where:
        confPort | appPort | hostA   | pubDnsA  | pubIpA      | priDnsA | priIpA     | result
        null     | '7001'  | 'ec2-a' | null     | null        | null    | null       | ['ec2-a:7001', 'ec2-c:9023']
        null     | '7001'  | 'ec2-a' | 'ec2-aa' | '203.2.8.5' | 'ip-aa' | '10.2.2.1' | ['ec2-a:7001', 'ec2-c:9023']
        null     | '7001'  | null    | 'ec2-aa' | '203.2.8.5' | 'ip-aa' | '10.2.2.1' | ['ec2-aa:7001', 'ec2-c:9023']
        null     | '7001'  | null    | null     | '203.2.8.5' | 'ip-aa' | '10.2.2.1' | ['203.2.8.5:7001', 'ec2-c:9023']
        null     | '7001'  | null    | null     | null        | 'ip-aa' | '10.2.2.1' | ['ip-aa:7001', 'ec2-c:9023']
        null     | '7001'  | null    | null     | null        | null    | '10.2.2.1' | ['10.2.2.1:7001', 'ec2-c:9023']
        null     | '7001'  | 'ec2-a' | null     | null        | 'ip-aa' | '10.2.2.1' | ['ec2-a:7001', 'ec2-c:9023']
        '7102'   | '7001'  | 'ec2-a' | null     | null        | null    | null       | ['ec2-a:7102', 'ec2-c:7102']
    }

    private GroupedInstance instanceWithEureka(String instanceId, String hostName, String publicDnsName,
            String publicIpAddress, String privateDnsName, String privateIpAddress, String port) {

        Instance ec2Instance = new Instance(instanceId: instanceId, publicDnsName: publicDnsName,
                publicIpAddress: publicIpAddress, privateDnsName: privateDnsName, privateIpAddress: privateIpAddress)
        ApplicationInstance appInst = new ApplicationInstance(instanceId: instanceId, hostName: hostName, port: port)
        def asgInstance = new com.amazonaws.services.autoscaling.model.Instance(instanceId: instanceId)
        MergedInstance mergedInstance = new MergedInstance(ec2Instance, appInst)
        GroupedInstance.from(asgInstance, [], mergedInstance, null)
    }
}
