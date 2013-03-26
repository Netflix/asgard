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

import com.amazonaws.services.ec2.model.Instance
import com.amazonaws.services.ec2.model.Tag
import com.netflix.asgard.model.ApplicationInstance
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

/**
 * Generic Instance encapsulation for use in instance list. May be created from:
 *   - Discovery's Application*ApplicationInstance
 * and/or
 *   - EC2's RegisteredInstance
 */
@EqualsAndHashCode
@ToString
class MergedInstance {

    // General fields
    String appName
    String hostName    // ec2 public, or Netflix DC private host name
    String status

    // EC2 only
    String instanceType
    String instanceId
    String amiId
    String zone
    Date launchTime

    // Discovery only
    String version
    String port

    String autoScalingGroupName
    String launchConfigurationName

    Instance ec2Instance
    ApplicationInstance appInstance

    MergedInstance() {
    }

    MergedInstance(Instance ec2Instance, ApplicationInstance appInstance) {
        this.ec2Instance = ec2Instance
        this.appInstance = appInstance

        // Discovery app instance fields
        if (appInstance) {
            appName = appInstance.appName
            hostName = appInstance.hostName
            status = appInstance.status
            version = appInstance.version
            port = appInstance.port
            if (appInstance.dataCenterInfo) {
                instanceType = appInstance.dataCenterInfo['instance-type']
                instanceId = appInstance.dataCenterInfo['instance-id']
                amiId = appInstance.dataCenterInfo['ami-id']
                zone = appInstance.dataCenterInfo['availability-zone']
            }
        }

        // EC2 Instance fields
        if (ec2Instance) {
            hostName      = ec2Instance.publicDnsName
            status        = ec2Instance.state?.name
            instanceId    = ec2Instance.instanceId
            amiId         = ec2Instance.imageId
            instanceType  = ec2Instance.instanceType
            zone          = ec2Instance.placement?.availabilityZone
            launchTime    = ec2Instance.launchTime
        }
    }

    String getVipAddress() {
        appInstance?.vipAddress
    }

    List<Tag> listTags() {
        ec2Instance?.tags?.sort { it.key }
    }

    List listFieldContainers() {
        [
                this,
                appInstance,
                appInstance?.dataCenterInfo,
                appInstance?.leaseInfo,
                ec2Instance,
                ec2Instance?.placement,
                ec2Instance?.state,
                ec2Instance?.blockDeviceMappings?.collect { [it, it.ebs] }
        ].flatten().findAll { it != null }
    }

    List<String> listFieldNames() {
        List<String> keyNames = listFieldContainers().collect {
            it instanceof Map ? it.keySet() : it.metaClass?.properties*.name
        }.flatten().unique()
        keyNames.findAll { it && !BeanState.isMetaGarbagePropertyName(it) }.sort()
    }

    String getFieldValue(String fieldName) {
        if (!fieldName) { return null }
        def container = listFieldContainers().find { it.metaClass?.hasProperty(it, fieldName) ||
                (it instanceof Map && it.containsKey(fieldName))
        }
        return container ? container[fieldName] : null
    }

    def attributes() {
        [ami: amiId, app: appName, id: instanceId, launch: launchTime, status: status, version: version, zone: zone]
    }

}
