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

import com.amazonaws.ClientConfiguration
import com.amazonaws.regions.Regions
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient
import com.amazonaws.services.ec2.AmazonEC2Client
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient
import com.amazonaws.services.rds.AmazonRDSClient
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.simpledb.AmazonSimpleDBClient
import com.amazonaws.services.sns.AmazonSNSClient
import com.amazonaws.services.sqs.AmazonSQSClient
import com.netflix.asgard.mock.MockAmazonAutoScalingClient
import com.netflix.asgard.mock.MockAmazonCloudWatchClient
import com.netflix.asgard.mock.MockAmazonEC2Client
import com.netflix.asgard.mock.MockAmazonElasticLoadBalancingClient
import com.netflix.asgard.mock.MockAmazonRDSClient
import com.netflix.asgard.mock.MockAmazonS3Client
import com.netflix.asgard.mock.MockAmazonSimpleDBClient
import com.netflix.asgard.mock.MockAmazonSnsClient
import com.netflix.asgard.mock.MockAmazonSqsClient
import org.springframework.beans.factory.InitializingBean

/**
 * Service for getting real or mock amazon client objects.
 */
class AwsClientService implements InitializingBean {

    static transactional = false

    def grailsApplication
    def secretService
    def endpointService
    def serverService
    def configService

    /**
     * Interface names mapped to ClientTypes wrapper objects. For each interface name, a real and fake concrete class
     * type should be provided.
     */
    private Map<String, Map<String, Class>> interfaceSimpleNamesToAwsClientClasses = [:]

    private ClientConfiguration clientConfiguration

    void afterPropertiesSet() {
        Set<String> providers = Region.values().collect { it.provider } as Set
        Region.values().findAll { providers.remove(it.provider) }.each { Region region ->
            interfaceSimpleNamesToAwsClientClasses[region.provider] = [
                    AmazonAutoScaling: concrete(region, AmazonAutoScalingClient, MockAmazonAutoScalingClient),
                    AmazonCloudWatch: concrete(region, AmazonCloudWatchClient, MockAmazonCloudWatchClient),
                    AmazonEC2: concrete(region, AmazonEC2Client, MockAmazonEC2Client),
                    AmazonElasticLoadBalancing: concrete(region, AmazonElasticLoadBalancingClient, MockAmazonElasticLoadBalancingClient),
                    AmazonRDS: concrete(region, AmazonRDSClient, MockAmazonRDSClient),
                    AmazonS3: concrete(region, AmazonS3Client, MockAmazonS3Client),
                    AmazonSimpleDB: concrete(region, AmazonSimpleDBClient, MockAmazonSimpleDBClient),
                    AmazonSNS: concrete(region, AmazonSNSClient, MockAmazonSnsClient),
                    AmazonSQS: concrete(region, AmazonSQSClient, MockAmazonSqsClient)
            ]
        }
        clientConfiguration = new ClientConfiguration()
        clientConfiguration.proxyHost = configService.proxyHost
        clientConfiguration.proxyPort = configService.proxyPort
        clientConfiguration.userAgent = 'asgard-' + serverService.version
    }

    public <T> T create(Class<T> interfaceType, Region region = Region.US_EAST_1) {
        Class implementationType = interfaceSimpleNamesToAwsClientClasses[region.provider][interfaceType.simpleName]
        createImpl(implementationType, interfaceType, region)
    }

    public <T> T createImpl(Class<T> implementationType, Class<T> interfaceType = null, Region region = Region.US_EAST_1) {
        //GRZE:NOTE: just default to Region.US_EAST_1 if there is no endpoint, need to make sure and use right credentials then.
        region = endpointService.getEndpoint(region, interfaceType) != null ? region : Region.US_EAST_1
        T client = implementationType.newInstance(secretService.awsCredentials[region], clientConfiguration) as T
        client.setEndpoint(endpointService.getEndpoint(region, interfaceType))
        client
    }

    Class concrete(Region region, Class real, Class fake) {
        Class interfaceType = real.class.interfaces[0]
        Set<String> mocks = grailsApplication.config.grails?."${region.provider}"?.mockServices ?: []
        !mocks.contains(interfaceType.simpleName) && grailsApplication.config.server.online ? real : fake
    }
}
