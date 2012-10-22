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
import com.amazonaws.services.ec2.model.Image
import com.amazonaws.services.ec2.model.Instance
import com.amazonaws.services.ec2.model.Reservation
import com.amazonaws.services.ec2.model.Tag
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription
import com.google.common.collect.Multiset
import com.netflix.asgard.model.ApplicationInstance
import com.netflix.asgard.text.TextLink
import com.netflix.asgard.text.TextLinkTemplate
import com.netflix.grails.contextParam.ContextParam
import grails.converters.JSON
import grails.converters.XML

@ContextParam('region')
class InstanceController {

    final static allowedMethods = [terminate: 'POST', terminateAndShrinkGroup: 'POST']

    def index = { redirect(action: 'list', params:params) }

    def awsAutoScalingService
    def awsEc2Service
    def awsLoadBalancerService
    def configService
    def discoveryService
    def mergedInstanceGroupingService

    def list = {
        UserContext userContext = UserContext.of(request)
        List<MergedInstance> instances = []
        Set<String> appNames = Requests.ensureList(params.id).collect { it.split(',') }.flatten() as Set<String>
        if (appNames) {
            instances = appNames.collect { mergedInstanceGroupingService.getMergedInstances(userContext, it) }.flatten()
        } else {
            instances = mergedInstanceGroupingService.getMergedInstances(userContext)
        }
        instances = instances.sort { it.appName ? it.appName.toLowerCase() : "~${it.instanceId}".toString() }
        withFormat {
            html { ['instanceList' : instances, 'appNames': appNames] }
            xml { new XML(instances).render(response) }
            json { new JSON(instances).render(response) }
        }
    }

    def find = {
        UserContext userContext = UserContext.of(request)
        String fieldName = params.by
        List<String> fieldValues = Requests.ensureList(params.value).collect { it.split(',') }.flatten()
        List<MergedInstance> matchingMergedInstances =
                mergedInstanceGroupingService.findByFieldValue(userContext, fieldName, fieldValues)
        if (!matchingMergedInstances) {
            flash.message = 'No results. For field names and values look at ' +
                    "${Requests.getBaseUrl(request)}/instance/find.json?by=appName&value=helloworld,cloudmonkey"
        }
        withFormat {
            html { render(view: 'list', model: ['instanceList' : matchingMergedInstances]) }
            xml { new XML(matchingMergedInstances).render(response) }
            json { new JSON(matchingMergedInstances).render(response) }
        }
    }

    def appversions = {
        UserContext userContext = UserContext.of(request)
        Set<Multiset.Entry<AppVersion>> avs = awsEc2Service.getCountedAppVersions(userContext).entrySet()
        withFormat {
            xml {
                render() {
                    apps(count: avs.size()) {
                        avs.each { Multiset.Entry<AppVersion> entry ->
                            app(name: entry.element.packageName,
                                version: entry.element.version,
                                count: entry.count,
                                cl: entry.element.changelist,
                                buildJob: entry.element.buildJobName,
                                buildNum: entry.element.buildNumber)
                        }
                    }
                }
            }
        }
    }

    def audit = {
        UserContext userContext = UserContext.of(request)
        String filter = params.id
        List<MergedInstance> instances = mergedInstanceGroupingService.getMergedInstances(userContext, '')
        List<MergedInstance> taggedInstances = instances.findAll { it.ec2Instance?.tags }
        Map<String, List<MergedInstance>> ownersToInstanceLists = new TreeMap<String, List<MergedInstance>>()
        taggedInstances.each { MergedInstance mergedInstance ->
            Tag ownerTag = mergedInstance.ec2Instance.tags.find { it.key == 'owner' }
            if (ownerTag) {
                String owner = ownerTag.value
                if (!filter || filter == owner) {
                    if (!ownersToInstanceLists[owner]) {
                        ownersToInstanceLists[owner] = []
                    }
                    List<MergedInstance> ownedInstances = ownersToInstanceLists[owner]
                    ownedInstances << mergedInstance
                }
            }
        }
        ownersToInstanceLists.values().each { it.sort { it.launchTime } }
        withFormat {
            html { ['ownersToInstanceLists' : ownersToInstanceLists] }
            xml { new XML(ownersToInstanceLists).render(response) }
            json { new JSON(ownersToInstanceLists).render(response) }
        }
    }

    def diagnose = {
        UserContext userContext = UserContext.of(request)
        String instanceId = EntityType.instance.ensurePrefix(params.instanceId ?: params.id)
        ApplicationInstance appInst = discoveryService.getAppInstance(userContext, instanceId)
        Map details = [
            'discInstance' : appInst,
            'healthCheck' : runHealthCheck(appInst)
        ]
        withFormat {
            html { return details }
            xml { new XML(details).render(response) }
            json { new JSON(details).render(response) }
        }
    }

    /* can show instance info given: instanceId, appName+instanceId, appName+hostName */
    def show = {
        UserContext userContext = UserContext.of(request)
        String instanceId = EntityType.instance.ensurePrefix(params.instanceId ?: params.id)
        String appName
        ApplicationInstance appInst
        if (params.appName) {
            appName = params.appName
            String instName = instanceId ?: params.hostName
            appInst = discoveryService.getAppInstance(userContext, appName, instName)
        } else {
            appInst = discoveryService.getAppInstance(userContext, instanceId)
            appName = appInst?.appName
        }
        Reservation instRsrv = instanceId ? awsEc2Service.getInstanceReservation(userContext, instanceId) : null
        Instance instance = instRsrv ? instRsrv.instances[0] : null
        if (!appInst && !instance) {
            String identifier = instanceId ?: "${params.appName}/${params.hostName}"
            Requests.renderNotFound('Instance', identifier, this)
        } else {
            String healthCheck = runHealthCheck(appInst)
            instance?.tags?.sort { it.key }
            Image image = instance ? awsEc2Service.getImage(userContext, instance.imageId) : null
            AutoScalingGroup group = awsAutoScalingService.getAutoScalingGroupFor(userContext, instance?.instanceId)
            String clusterName = Relationships.clusterFromGroupName(group?.autoScalingGroupName)
            List<LoadBalancerDescription> loadBalancers = awsLoadBalancerService.getLoadBalancersFor(userContext,
                    instance?.instanceId)

            Map<String, List<TextLink>> linkGroupingsToListsOfTextLinks = [:]
            String baseServer = instance?.publicDnsName ?: appInst?.hostName
            if (baseServer) {
                Map<String, List<TextLinkTemplate>> groupingsToLinkTemplateLists =
                        configService.instanceLinkGroupingsToLinkTemplateLists
                groupingsToLinkTemplateLists.each { String grouping, List<TextLinkTemplate> linkTemplates ->
                    List<TextLink> textLinks = linkTemplates.collect { it.makeLinkForServer(baseServer) }
                    linkGroupingsToListsOfTextLinks[grouping] = textLinks
                }
            }

            Map details = [
                    appName: appName,
                    discInstance: appInst,
                    healthCheck: healthCheck,  // TODO: do this in JavaScript on the page?
                    baseServer: baseServer,
                    instance: instance,
                    linkGroupingsToListsOfTextLinks: linkGroupingsToListsOfTextLinks,
                    securityGroups: instance?.securityGroups?.sort(),
                    image: image,
                    cluster: clusterName,
                    group: group,
                    loadBalancers: loadBalancers
            ]
            withFormat {
                html { return details }
                xml { new XML(details).render(response) }
                json { new JSON(details).render(response) }
            }
        }
    }

    def terminate = {
        UserContext userContext = UserContext.of(request)
        List<String> instanceIds = Requests.ensureList(params.selectedInstances ?: params.instanceId)

        // All this deregister-before-terminate logic is complicated because it needs to be done in large batches to
        // reduce Amazon errors. When Amazon fixes their ELB bugs a lot of this code should be removed for simplicity.
        Map<String, Collection<String>> asgNamesToInstanceIdSets = new HashMap<String, Collection<String>>()
        for (String instanceId in instanceIds) {
            String asg = awsAutoScalingService.getAutoScalingGroupFor(userContext, instanceId)?.autoScalingGroupName
            if (asg) {
                if (!asgNamesToInstanceIdSets.containsKey(asg)) {
                    asgNamesToInstanceIdSets.put(asg, new HashSet<String>())
                }
                asgNamesToInstanceIdSets[asg].add(instanceId)
            }
        }
        for (String asg in asgNamesToInstanceIdSets.keySet()) {
            Collection<String> instanceIdsForAsg = asgNamesToInstanceIdSets[asg]
            awsAutoScalingService.deregisterInstancesInAutoScalingGroupFromLoadBalancers(userContext, asg,
                    instanceIdsForAsg)
        }

        awsEc2Service.terminateInstances(userContext, instanceIds)
        flash.message = "Terminated ${instanceIds.size()} instance${instanceIds.size() == 1 ? '' : 's'}: ${instanceIds}"
        chooseRedirect(params.autoScalingGroupName, instanceIds, params.appNames)
    }

    def terminateAndShrinkGroup = {
        UserContext userContext = UserContext.of(request)
        String instanceId = params.instanceId
        try {
            AutoScalingGroup group = awsAutoScalingService.terminateInstanceAndDecrementAutoScalingGroup(userContext,
                   instanceId)
            if (group) {
                flash.message = "Terminated instance ${instanceId} and shrunk auto scaling group" +
                        " ${group.autoScalingGroupName} to ${group.desiredCapacity - 1}"
                redirect(controller: 'autoScaling', action: 'show', params: [id: group.autoScalingGroupName])
                return
            } else {
                flash.message = "Termination cancelled because instance ${instanceId} is not in an auto scaling group"
            }
        } catch (AmazonServiceException ase) {
            flash.message = ase.message
        }
        redirect(action: 'show', params:[instanceId: instanceId])
    }

    def reboot = {
        String instanceId = EntityType.instance.ensurePrefix(params.instanceId)
        UserContext userContext = UserContext.of(request)
        awsEc2Service.rebootInstance(userContext, instanceId)

        flash.message = "Rebooting instance '${instanceId}'."
        redirect(action: 'show', params:[instanceId:instanceId])
    }

    def raw = {
        UserContext userContext = UserContext.of(request)
        String instanceId = EntityType.instance.ensurePrefix(params.instanceId ?: params.id)
        try {
            String consoleOutput = awsEc2Service.getConsoleOutput(userContext, instanceId)
            return [ 'instanceId': instanceId, 'consoleOutput' : consoleOutput, 'now': new Date() ]
        } catch (AmazonServiceException ase) {
            Requests.renderNotFound('Instance', instanceId, this, ase.toString())
            return
        }
    }

    private void chooseRedirect(String autoScalingGroupName, List<String> instanceIds, String appName = null) {
        Map destination = [action: 'list']
        if (autoScalingGroupName) {
            destination = [controller: 'autoScaling', action: 'show', params: [id: autoScalingGroupName, runHealthChecks: true]]
        } else if (instanceIds.size() == 1) {
            destination = [action: 'show', params: [id: instanceIds[0]]]
        } else if (appName) {
            destination = [action: 'list', params: [id: appName]]
        }
        redirect destination
    }

    def deregister = {
        UserContext userContext = UserContext.of(request)
        List<String> instanceIds = Requests.ensureList(params.instanceId)
        String autoScalingGroupName = params.autoScalingGroupName

        Set<String> lbNames = new TreeSet<String>()
        instanceIds.each { instanceId ->
            lbNames.addAll(awsLoadBalancerService.getLoadBalancersFor(userContext, instanceId).collect { it.loadBalancerName })
        }

        if (lbNames.isEmpty()) {
            flash.message = "No load balancers found for instance${instanceIds.size() > 1 ? "s" : ""} '${instanceIds}'"
        } else {
            lbNames.each { lbName -> awsLoadBalancerService.removeInstances(userContext, lbName, instanceIds) }
            flash.message = "Deregistered instance${instanceIds.size() > 1 ? "s" : ""} '${instanceIds}' from " +
                "load balancer${lbNames.size() > 1 ? "s" : ""} '${lbNames}'."
        }
        chooseRedirect(autoScalingGroupName, instanceIds)
    }

    def register = {
        UserContext userContext = UserContext.of(request)
        List<String> instanceIds = Requests.ensureList(params.instanceId)
        String autoScalingGroupName = params.autoScalingGroupName

        if (instanceIds) {
            AutoScalingGroup group = awsAutoScalingService.getAutoScalingGroup(userContext, autoScalingGroupName)
            if (!group) {
                group = awsAutoScalingService.getAutoScalingGroupFor(userContext, instanceIds[0])
            }

            // Ensure all instances are in the same group
            List<String> groupInstanceIds = group?.instances?.collect { it.instanceId } ?: []
            if (instanceIds.every { groupInstanceIds.contains(it) }) {
                List<String> elbNames = group.loadBalancerNames
                if (elbNames.isEmpty()) {
                    flash.message = "There are no load balancers on group '${group.autoScalingGroupName}'"
                } else {
                    elbNames.each { elbName -> awsLoadBalancerService.addInstances(userContext, elbName, instanceIds) }
                    flash.message = "Registered instance${instanceIds.size() == 1 ? '' : 's'} ${instanceIds} with " +
                            "load balancer${elbNames.size() == 1 ? '' : 's'} ${elbNames}"
                }
            } else {
                flash.message = "Error: Not all instances '${instanceIds}' are in group '${group?.autoScalingGroupName}'"
            }
        }

        chooseRedirect(autoScalingGroupName, instanceIds)
    }

    def associate = {
        UserContext userContext = UserContext.of(request)
        Instance instance = awsEc2Service.getInstance(userContext, EntityType.instance.ensurePrefix(params.instanceId))
        if (!instance) {
            flash.message = "EC2 Instance ${params.instanceId} not found."
            redirect(action: 'list')
            return
        } else {
            Map<String, String> publicIps = awsEc2Service.describeAddresses(userContext)
            log.debug "describeAddresses: ${publicIps}"
            return [
                    instance: instance,
                    publicIps: publicIps,
                    eipUsageMessage: grailsApplication.config.cloud.eipUsageMessage ?: null
            ]
        }
    }

    def associateDo = {
        //println "associateDo: ${params}"
        String publicIp = params.publicIp
        String instanceId = EntityType.instance.ensurePrefix(params.instanceId)
        UserContext userContext = UserContext.of(request)
        try {
            awsEc2Service.associateAddress(userContext, publicIp, instanceId)
            flash.message = "Elastic IP '${publicIp}' has been associated with '${instanceId}'."
        } catch (Exception e) {
            flash.message = "Could not associate Elastic IP '${publicIp}' with '${instanceId}': ${e}"
        }
        redirect(action: 'show', params:[instanceId:instanceId])
    }

    def takeOutOfService = {
        UserContext userContext = UserContext.of(request)
        String autoScalingGroupName = params.autoScalingGroupName
        List<String> instanceIds = Requests.ensureList(params.instanceId)
        discoveryService.disableAppInstances(userContext, params.appName, instanceIds)
        flash.message = "Instances of app '${params.appName}' taken out of service in discovery: '${instanceIds}'"
        chooseRedirect(autoScalingGroupName, instanceIds)
    }

    def putInService = {
        UserContext userContext = UserContext.of(request)
        String autoScalingGroupName = params.autoScalingGroupName
        List<String> instanceIds = Requests.ensureList(params.instanceId)
        discoveryService.enableAppInstances(userContext, params.appName, instanceIds)
        flash.message = "Instances of app '${params.appName}' put in service in discovery: '${instanceIds}'"
        chooseRedirect(autoScalingGroupName, instanceIds)
    }

    def addTag = {
        String instanceId = EntityType.instance.ensurePrefix(params.instanceId)
        UserContext userContext = UserContext.of(request)
        awsEc2Service.createInstanceTag(userContext, [instanceId], params.name, params.value)
        redirect(action: 'show', params:[instanceId:instanceId])
    }

    def removeTag = {
        String instanceId = EntityType.instance.ensurePrefix(params.instanceId)
        UserContext userContext = UserContext.of(request)
        awsEc2Service.deleteInstanceTag(userContext, instanceId, params.name)
        redirect(action: 'show', params:[instanceId:instanceId])
    }

    def userData = {
        UserContext userContext = UserContext.of(request)
        String instanceId = EntityType.instance.ensurePrefix(params.id ?: params.instanceId)
        render awsEc2Service.getUserDataForInstance(userContext, instanceId)
    }

    def userDataHtml = {
        UserContext userContext = UserContext.of(request)
        String instanceId = EntityType.instance.ensurePrefix(params.id ?: params.instanceId)
        render "<pre>${awsEc2Service.getUserDataForInstance(userContext, instanceId).encodeAsHTML()}</pre>"
    }

    private String runHealthCheck(ApplicationInstance appInst) {
        appInst?.healthCheckUrl ? (awsEc2Service.checkHostHealth(appInst?.healthCheckUrl) ? 'pass' : 'fail') : 'NA'
    }
}
