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
import com.amazonaws.services.autoscaling.model.LaunchConfiguration
import com.amazonaws.services.cloudwatch.model.MetricAlarm
import com.amazonaws.services.ec2.model.Image
import com.amazonaws.services.ec2.model.Instance
import com.amazonaws.services.ec2.model.SecurityGroup
import com.amazonaws.services.ec2.model.Snapshot
import com.amazonaws.services.ec2.model.Volume
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription
import com.amazonaws.services.rds.model.DBInstance
import com.amazonaws.services.rds.model.DBSecurityGroup
import com.amazonaws.services.rds.model.DBSnapshot
import com.google.common.base.CharMatcher
import com.google.common.base.Splitter
import com.netflix.asgard.model.SimpleQueue
import com.netflix.asgard.model.TopicData
import com.netflix.asgard.push.Cluster
import org.springframework.beans.factory.InitializingBean

class SearchService implements InitializingBean {

    static transactional = false

    def applicationService
    def awsEc2Service
    def awsAutoScalingService
    def awsCloudWatchService
    def awsLoadBalancerService
    def awsSnsService
    def awsSqsService
    def awsRdsService
    def simpleDbDomainService

    // Collect the known id prefixes to optimize for them
    Map<String, EntityType> prefixesToTypes = [:]

    void afterPropertiesSet() {
        // Refactor to use findResults() when the next Groovy on Grails gets released
        EntityType.values().findAll { it.idPrefix }.each { EntityType type ->
             prefixesToTypes.put(type.idPrefix, type)
        }
    }

    Map<Region, Map<EntityType, List>> findResults(UserContext userContext, String query) {

        Iterable<String> terms = Splitter.on(CharMatcher.anyOf(';, ')).split(query)
        Map<Region, Map<EntityType, List>> regionsToTypesToLists = [:]
        Map<String, EntityType> termsToGuessedTypes = [:]

        for (String term in terms) {
            termsToGuessedTypes[term] = guessType(term)
        }

        // Region-agnostic objects
        Map<EntityType, List> typesToGlobalLists = [:]
        for (String term in terms) {
            EntityType guessedType = termsToGuessedTypes[term]
            Collection<SearchResultItem> globalResults = findGlobalResultsForTerm(userContext, term, guessedType)
            for (SearchResultItem item in globalResults) {
                List entities = typesToGlobalLists[item.entityType] ?: []
                entities << item.entity
                typesToGlobalLists[item.entityType] = entities
            }
        }
        if (typesToGlobalLists) {
            regionsToTypesToLists[null] = typesToGlobalLists
        }

        // Region-specific objects
        for (Region region in Region.values()) {
            Map<EntityType, List> typesToRegionalLists = [:]
            for (String term in terms) {
                EntityType guessedType = termsToGuessedTypes[term]
                UserContext userContextForRegion = userContext.withRegion(region)
                Collection<SearchResultItem> regionalResults =
                        findRegionalResultsForTerm(userContextForRegion, term, guessedType)
                for (SearchResultItem item in regionalResults) {
                    List entities = typesToRegionalLists[item.entityType] ?: []
                    entities << item.entity
                    typesToRegionalLists[item.entityType] = entities
                }
            }
            if (typesToRegionalLists) {
                regionsToTypesToLists[region] = typesToRegionalLists
            }
        }

        regionsToTypesToLists
    }

    private EntityType guessType(String id) {
        prefixesToTypes.keySet().findResult {
            prefix -> id.startsWith(prefix) ? prefixesToTypes[prefix] : null
        } as EntityType
    }

    private Collection<SearchResultItem> findGlobalResultsForTerm(UserContext userContext, String term,
                                                                  EntityType guessedType) {
        Region region = userContext.region
        Collection<SearchResultItem> results = []
        if (!guessedType) {
            AppRegistration app = applicationService.getRegisteredApplication(userContext, term, From.CACHE)
            if (app) { results << new SearchResultItem(EntityType.application, region, app)}
        }
        results
    }

    private Collection<SearchResultItem> findRegionalResultsForTerm(UserContext userContext, String term,
                                                                    EntityType guessedType) {
        Region region = userContext.region
        Collection<SearchResultItem> results = []
        if (guessedType == EntityType.instance) {
            Instance ec2Instance = awsEc2Service.getInstance(userContext, term, From.CACHE)
            if (ec2Instance) {
                return [new SearchResultItem(EntityType.instance, region, ec2Instance)]
            }
        } else if (guessedType == EntityType.snapshot) {
            Snapshot snapshot = awsEc2Service.getSnapshot(userContext, term, From.CACHE)
            if (snapshot) {
                return [new SearchResultItem(EntityType.snapshot, region, snapshot)]
            }
        } else if (guessedType == EntityType.volume) {
            Volume volume = awsEc2Service.getVolume(userContext, term, From.CACHE)
            if (volume) {
                return [new SearchResultItem(EntityType.volume, region, volume)]
            }
        } else if (guessedType == EntityType.image) {
            Image image = awsEc2Service.getImage(userContext, term, From.CACHE)
            if (image) {
                return [new SearchResultItem(EntityType.image, region, image)]
            }
        } else {
            SecurityGroup securityGroup = awsEc2Service.getSecurityGroup(userContext, term, From.CACHE)
            if (securityGroup) { results << new SearchResultItem(EntityType.security, region, securityGroup)}

            AutoScalingGroup autoScalingGroup = awsAutoScalingService.getAutoScalingGroup(userContext, term, From.CACHE)
            if (autoScalingGroup) { results << new SearchResultItem(EntityType.autoScaling, region, autoScalingGroup)}

            Cluster cluster = awsAutoScalingService.getCluster(userContext, term, From.CACHE)
            if (cluster) { results << new SearchResultItem(EntityType.cluster, region, cluster)}

            LaunchConfiguration launchConfig = awsAutoScalingService.getLaunchConfiguration(userContext, term, From.CACHE)
            if (launchConfig) { results << new SearchResultItem(EntityType.launchConfiguration, region, launchConfig)}

            MetricAlarm alarm = awsCloudWatchService.getAlarm(userContext, term, From.CACHE)
            if (alarm) { results << new SearchResultItem(EntityType.alarm, region, alarm)}

            LoadBalancerDescription loadBalancer = awsLoadBalancerService.getLoadBalancer(userContext, term, From.CACHE)
            if (loadBalancer) { results << new SearchResultItem(EntityType.loadBalancer, region, loadBalancer)}

            String domain = simpleDbDomainService.getDomainFromCache(userContext, term)
            if (domain) { results << new SearchResultItem(EntityType.domain, region, domain)}

            TopicData topic = awsSnsService.getTopic(userContext, term, From.CACHE)
            if (topic) { results << new SearchResultItem(EntityType.topic, region, topic)}

            SimpleQueue queue = awsSqsService.getQueue(userContext, term, From.CACHE)
            if (queue) { results << new SearchResultItem(EntityType.queue, region, queue)}

            DBInstance dbInstance = awsRdsService.getDBInstance(userContext, term, From.CACHE)
            if (dbInstance) { results << new SearchResultItem(EntityType.rdsInstance, region, dbInstance)}

            DBSecurityGroup dbSecurityGroup = awsRdsService.getDBSecurityGroup(userContext, term, From.CACHE)
            if (dbSecurityGroup) { results << new SearchResultItem(EntityType.dbSecurity, region, dbSecurityGroup)}

            DBSnapshot dbSnapshot = awsRdsService.getDBSnapshot(userContext, term, From.CACHE)
            if (dbSnapshot) { results << new SearchResultItem(EntityType.dbSnapshot, region, dbSnapshot)}

        }
        results
    }
}
