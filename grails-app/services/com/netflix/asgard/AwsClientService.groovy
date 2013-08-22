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
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient
import com.amazonaws.services.ec2.AmazonEC2Client
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient
import com.amazonaws.services.rds.AmazonRDSClient
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.simpledb.AmazonSimpleDBClient
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient
import com.amazonaws.services.sns.AmazonSNSClient
import com.amazonaws.services.sqs.AmazonSQSClient
import com.netflix.asgard.mock.MockAmazonAutoScalingClient
import com.netflix.asgard.mock.MockAmazonCloudWatchClient
import com.netflix.asgard.mock.MockAmazonEC2Client
import com.netflix.asgard.mock.MockAmazonElasticLoadBalancingClient
import com.netflix.asgard.mock.MockAmazonRDSClient
import com.netflix.asgard.mock.MockAmazonS3Client
import com.netflix.asgard.mock.MockAmazonSimpleDBClient
import com.netflix.asgard.mock.MockAmazonSimpleWorkflowClient
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
    def serverService
    def configService

    /**
     * Interface names mapped to ClientTypes wrapper objects. For each interface name, a real and fake concrete class
     * type should be provided.
     */
    private Map<String, Class> interfaceSimpleNamesToAwsClientClasses

    private ClientConfiguration clientConfiguration

    void afterPropertiesSet() {
        interfaceSimpleNamesToAwsClientClasses = [
                AmazonAutoScaling: concrete(AmazonAutoScalingClient, MockAmazonAutoScalingClient),
                AmazonCloudWatch: concrete(AmazonCloudWatchClient, MockAmazonCloudWatchClient),
                AmazonEC2: concrete(AmazonEC2Client, MockAmazonEC2Client),
                AmazonElasticLoadBalancing: concrete(AmazonElasticLoadBalancingClient,
                        MockAmazonElasticLoadBalancingClient),
                AmazonRDS: concrete(AmazonRDSClient, MockAmazonRDSClient),
                AmazonS3: concrete(AmazonS3Client, MockAmazonS3Client),
                AmazonSimpleDB: concrete(AmazonSimpleDBClient, MockAmazonSimpleDBClient),
                AmazonSimpleWorkflow: concrete(AmazonSimpleWorkflowClient, MockAmazonSimpleWorkflowClient),
                AmazonSNS: concrete(AmazonSNSClient, MockAmazonSnsClient),
                AmazonSQS: concrete(AmazonSQSClient, MockAmazonSqsClient)
        ]
        clientConfiguration = new ClientConfiguration()
        clientConfiguration.proxyHost = configService.proxyHost
        clientConfiguration.proxyPort = configService.proxyPort
        clientConfiguration.userAgent = 'asgard-' + serverService.version
    }

    public <T> T create(Class<T> interfaceType) {
        Class implementationType = interfaceSimpleNamesToAwsClientClasses[interfaceType.simpleName]
        createImpl(implementationType)
    }

    public <T> T createImpl(Class<T> implementationType) {
        implementationType.newInstance(secretService.awsCredentials, clientConfiguration) as T
    }

    Class concrete(Class real, Class fake) {
        grailsApplication.config.server.online ? real : fake
    }
}
