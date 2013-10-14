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

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.autoscaling.model.AutoScalingGroup
import com.amazonaws.services.autoscaling.model.ScalingPolicy
import com.amazonaws.services.cloudwatch.model.MetricAlarm
import com.amazonaws.services.ec2.model.AvailabilityZone
import com.amazonaws.services.ec2.model.Image
import com.amazonaws.services.ec2.model.Instance
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription
import com.amazonaws.services.simpledb.model.Attribute
import com.amazonaws.services.simpledb.model.DomainMetadataResult
import com.amazonaws.services.simpledb.model.Item
import com.netflix.asgard.model.AutoScalingGroupMixin
import com.netflix.asgard.model.MetricAlarmMixin
import com.netflix.asgard.model.ScalingPolicyMixin
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.apache.commons.io.FileUtils
import org.apache.commons.lang.StringUtils
import org.springframework.beans.factory.InitializingBean

/**
 * Legacy system for adding behavior to existing Java classes from libraries. Over time, as much of this as possible
 * should get migrated to unit-tested mixins, and the mixins should get directly initialized from Bootstrap.groovy.
 */
class MonkeyPatcherService implements InitializingBean {

    static transactional = false

    private static final originalASEToString = AmazonServiceException.metaClass.pickMethod('toString', [] as Class[])

    def grailsApplication

    @Override
    void afterPropertiesSet() {
        createDynamicMethods()
        addClassNameToStringOutputForAmazonServiceException()
    }

    void createDynamicMethods() {

        // For faster development cycles, hack the region enum on start up so only certain regions are used.
        List<Region> limitedRegions = Region.limitedRegions
        if (limitedRegions) {
            Region.metaClass.'static'.defaultRegion = { -> limitedRegions[0] }
            Region.metaClass.'static'.values = { -> limitedRegions as Region[] }
            // Warning, this will not affect services or Spring wiring that occur prior to the init of this service.
        }

        // Requests and cookies
        HttpServletRequest.metaClass.getCookie = { String name -> Cookies.get(name) }
        HttpServletResponse.metaClass.setCookie = { String name, String value, Integer maxAge ->
            Cookies.set(delegate, name, value, maxAge)
        }
        HttpServletResponse.metaClass.deleteCookie = { String name -> Cookies.delete(delegate, name) }

        monkeyPatchLoadBalancerDescription()
        monkeyPatchSimpleDbClasses()
        monkeyPatchAutoScalingGroup()

        AutoScalingGroup.mixin AutoScalingGroupMixin
        ScalingPolicy.mixin ScalingPolicyMixin
        MetricAlarm.mixin MetricAlarmMixin

        if (!(AutoScalingGroup.methods as List).contains("findInServiceInstanceIds")) {
            AutoScalingGroup.metaClass.findInServiceInstanceIds = { ->
                new TreeSet<String>(delegate.instances.findAll { it.lifecycleState == 'InService' }.collect { it.instanceId })
            }
        }
        // autoscaling Instances
        if (!(com.amazonaws.services.autoscaling.model.Instance.methods as List).contains("copy")) {
            com.amazonaws.services.autoscaling.model.Instance.metaClass.copy = { -> Meta.copy(delegate) }
        }
        if (!(AutoScalingGroup.methods as List).contains("copy")) {
            AutoScalingGroup.metaClass.copy = { ->
                AutoScalingGroup asgCopy = Meta.copy(delegate as AutoScalingGroup)
                // Deep copy the instances
                asgCopy.instances = asgCopy.instances.collect { it.copy() }
                asgCopy
            }
        }
        monkeyPatchAvailabilityZone()
        monkeyPatchImage()
        monkeyPatchInstance()
    }

    private void monkeyPatchInstance() {
        if (!(Instance.methods as List).contains('getTag')) {
            Instance.metaClass['getTag'] = { String key -> delegate.tags?.find({ it.key == key })?.value }
        }
        addInstanceTagGetterMethod('getApp', 'app')
        addInstanceTagGetterMethod('getOwner', 'owner')
    }

    private void monkeyPatchImage() {
        if (!(Image.methods as List).contains('getTag')) {
            Image.metaClass['getTag'] = { String key -> delegate.tags?.find({ it.key == key })?.value }
        }
        addImageTagGetterMethod('getCreator', 'creator')
        addImageTagGetterMethod('getCreationTime', 'creation_time')
        addImageTagGetterMethod('getAppVersion', 'appversion')
        addImageTagGetterMethod('getLastReferencedTime', 'last_referenced_time')
        if (!(Image.methods as List).contains('getParsedAppVersion')) {
            Image.metaClass['getParsedAppVersion'] = { -> Relationships.dissectAppVersion(delegate.appVersion) }
        }
        if (!(Image.methods as List).contains('getPackageName')) {
            Image.metaClass['getPackageName'] = { -> Relationships.packageFromAppVersion(delegate.appVersion) }
        }
        if (!(Image.methods as List).contains('getBaseAmiId')) {
            Image.metaClass['getBaseAmiId'] = { -> Relationships.baseAmiIdFromDescription(delegate.description) }
        }
        if (!(Image.methods as List).contains('getBaseAmiName')) {
            Image.metaClass['getBaseAmiName'] = { -> Relationships.baseAmiNameFromDescription(delegate.description) }
        }
        if (!(Image.methods as List).contains('getBaseAmiDate')) {
            Image.metaClass['getBaseAmiDate'] = { -> Relationships.baseAmiDateFromDescription(delegate.description) }
        }
        if (!(Image.methods as List).contains('isKeepForever')) {
            Image.metaClass['isKeepForever'] = { -> delegate.getTag('expiration_time') == 'never' }
        }
    }

    private void monkeyPatchAvailabilityZone() {
        if (!(AvailabilityZone.methods as List).contains("shouldBePreselected")) {
            List<String> discouragedZones = grailsApplication?.config?.cloud?.discouragedAvailabilityZones ?: []
            AvailabilityZone.metaClass.shouldBePreselected = { selectedZones, autoScalingGroup ->
                String zoneName = delegate.getZoneName()
                Boolean isZoneNameDiscouraged = discouragedZones.contains(zoneName)
                Boolean zoneIsUsedByCurrentAsg = autoScalingGroup?.availabilityZones?.contains(zoneName)
                Boolean zoneHasAlreadyBeenSelected = Requests.ensureList(selectedZones).contains(zoneName)
                Boolean noAsgOrSelectionAndZoneIsFine = !selectedZones && !autoScalingGroup && !isZoneNameDiscouraged
                Boolean shouldBePreselected = zoneIsUsedByCurrentAsg || zoneHasAlreadyBeenSelected ||
                        noAsgOrSelectionAndZoneIsFine
                return shouldBePreselected
            }
        }
    }

    private void monkeyPatchAutoScalingGroup() {
        if (!(AutoScalingGroup.methods as List).contains("getAppName")) {
            AutoScalingGroup.metaClass.getAppName = { ->
                Relationships.appNameFromGroupName(delegate.autoScalingGroupName)
            }
        }
        if (!(AutoScalingGroup.methods as List).contains("getClusterName")) {
            AutoScalingGroup.metaClass.getClusterName = { ->
                Relationships.clusterFromGroupName(delegate.autoScalingGroupName)
            }
        }
        if (!(AutoScalingGroup.methods as List).contains("getStack")) {
            AutoScalingGroup.metaClass.getStack = { ->
                Relationships.stackNameFromGroupName(delegate.autoScalingGroupName)
            }
        }
        if (!(AutoScalingGroup.methods as List).contains("getVariables")) {
            AutoScalingGroup.metaClass.getVariables = { ->
                Relationships.parts(delegate.autoScalingGroupName as String)
            }
        }
    }

    private void monkeyPatchSimpleDbClasses() {
        if (!(Item.methods as List).contains('getAttribute')) {
            Item.metaClass.getAttribute = { String name ->
                delegate.attributes.find { Attribute attr -> attr.name == name }
            }
        }
        if (!(DomainMetadataResult.methods as List).contains("getItemNamesSize")) {
            DomainMetadataResult.metaClass.getItemNamesSize = { ->
                FileUtils.byteCountToDisplaySize(delegate.itemNamesSizeBytes as Integer)
            }
        }
        if (!(DomainMetadataResult.methods as List).contains("getAttributeNamesSize")) {
            DomainMetadataResult.metaClass.getAttributeNamesSize = { ->
                FileUtils.byteCountToDisplaySize(delegate.attributeNamesSizeBytes as Integer)
            }
        }
        if (!(DomainMetadataResult.methods as List).contains("getAttributeValuesSize")) {
            DomainMetadataResult.metaClass.getAttributeValuesSize = { ->
                FileUtils.byteCountToDisplaySize(delegate.attributeValuesSizeBytes as Integer)
            }
        }
    }

    private void monkeyPatchLoadBalancerDescription() {
        if (!(LoadBalancerDescription.methods as List).contains("getTargetTruncated")) {
            LoadBalancerDescription.metaClass.getTargetTruncated = { ->
                StringUtils.abbreviate(delegate.healthCheck.target, 25)
            }
        }
    }

    private void addImageTagGetterMethod(String getter, String key) {
        if (!(Image.methods as List).contains(getter)) {
            Image.metaClass[getter] = { -> delegate.getTag(key) }
        }
    }

    private void addInstanceTagGetterMethod(String getter, String key) {
        if (!(Instance.methods as List).contains(getter)) {
            Instance.metaClass[getter] = { -> delegate.getTag(key) }
        }
    }

    /**
     * Monkey patches AmazonServiceException to include the exception class name in the toString output
     */
    void addClassNameToStringOutputForAmazonServiceException() {
        AmazonServiceException.metaClass.toString = { ->
            "${delegate.getClass().getSimpleName()}: ${originalASEToString.invoke(delegate)}"
        }
    }
}
