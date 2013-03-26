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

import com.netflix.asgard.model.AutoScalingGroupData
import com.netflix.frigga.NameValidation
import com.netflix.frigga.Names
import com.netflix.frigga.ami.AppVersion
import com.netflix.frigga.ami.BaseAmiInfo
import com.netflix.frigga.autoscaling.AutoScalingGroupNameBuilder
import com.netflix.frigga.elb.LoadBalancerNameBuilder
import org.apache.commons.lang.WordUtils
import org.joda.time.DateTime

/**
 * Utility class for handling relationships between different cloud objects and Netflix concepts, including naming
 * rules.
 */
class Relationships {

    /**
     * Application names should not have excessively long names because the name is the root of other complex names
     * such as cluster, auto scaling group, load balancer, scaling policy, and alarm.
     */
    static final Integer APPLICATION_MAX_LENGTH = 28

    /**
     * Auto scaling groups and similarly named objects should not have excessively long names because the name will be
     * used in hostnames, instances identifiers in Tracer Central, monitoring tools, log files, and very complex names
     * such as scaling policy and alarm.
     */
    static final Integer GROUP_NAME_MAX_LENGTH = 96

    /** The maximum number of auto scaling groups allowed in a cluster spanning multiple push sequence numbers. */
    static final Integer CLUSTER_MAX_GROUPS = 3

    /** The maximum sequence number for an auto scaling group in a sequenced cluster before rolling over to 0. */
    static final Integer CLUSTER_MAX_SEQUENCE_NUMBER = 999

    static final Comparator<AutoScalingGroupData> PUSH_SEQUENCE_COMPARATOR = new Comparator<AutoScalingGroupData>() {
        int compare(AutoScalingGroupData a, AutoScalingGroupData b) {
            Names aNames = dissectCompoundName(a.autoScalingGroupName)
            Names bNames = dissectCompoundName(b.autoScalingGroupName)
            Check.equal(aNames.cluster, bNames.cluster)
            Integer aSequence = aNames.sequence
            Integer bSequence = bNames.sequence

            // The group with no push sequence number goes first
            if (aSequence == null) { return -1 }
            if (bSequence == null) { return 1 }

            // If a and b are very far apart then greater number goes first, to achieve 997, 998, 999, 000, 001, 002
            if (Math.abs(aSequence - bSequence) > CLUSTER_MAX_SEQUENCE_NUMBER - CLUSTER_MAX_GROUPS * 2) {
                return bSequence - aSequence
            }
            aSequence - bSequence
        }
    }

    static String appNameFromLoadBalancerName(String loadBalancerName) {
        dissectCompoundName(loadBalancerName).app ?: ''
    }

    static String appNameFromLaunchConfigName(String launchConfigName) {
        dissectCompoundName(launchConfigName).app ?: ''
    }

    static String appNameFromSecurityGroupName(String securityGroupName) {
        dissectCompoundName(securityGroupName).app ?: ''
    }

    static String appNameFromGroupName(String autoScalingGroupName) {
        dissectCompoundName(autoScalingGroupName).app ?: ''
    }

    static String stackNameFromGroupName(String autoScalingGroupName) {
        dissectCompoundName(autoScalingGroupName).stack ?: ''
    }

    static String clusterFromGroupName(String autoScalingGroupName) {
        dissectCompoundName(autoScalingGroupName).cluster ?: ''
    }

    /**
     * Breaks down the name of an auto scaling group or load balancer into its component parts.
     *
     * @param compoundName the name of an auto scaling group or load balancer
     * @return Names a data object containing the component parts of the compound name
     */
    static Names dissectCompoundName(String compoundName) {
        Names.parseName(compoundName)
    }

    static String buildGroupName(Map params, Boolean doValidation = false) {
        new AutoScalingGroupNameBuilder(
                appName: params.appName,
                stack: params.newStack ?: params.stack,
                detail: params.detail,
                countries: params.countries,
                devPhase: params.devPhase,
                hardware: params.hardware,
                partners: params.partners,
                revision: params.revision,
                usedBy: params.usedBy,
                redBlackSwap: params.redBlackSwap,
                zoneVar: params.zoneVar).buildGroupName()
    }

    /**
     * Validates that an app name is safe to use in a load balancer name, meaning it is alphanumeric and does not use
     * a text format reserved for other purposes.
     *
     * @param name to check
     * @return Boolean true if the name is safe to use in a load balancer
     */
    static Boolean checkAppNameForLoadBalancer(String name) {
        checkStrictName(name) && !usesReservedFormat(name)
    }

    /**
     * Determines whether a name ends with the reserved format -v000 where 0 represents any digit, or starts with the
     * reserved format z0 where z is any letter, or contains a hyphen-separated token that starts with the z0 format.
     *
     * @param name to inspect
     * @return Boolean true if the name ends with the reserved format
     */
    static Boolean usesReservedFormat(String name) {
        NameValidation.usesReservedFormat(name)
    }

    /**
     * Validates a strict name of a cloud object according to the rules in http://go/CloudModel
     * The strict name can contain letters and numbers only. It is suitable to be used in a load balancer name because
     * it will then be used safely and consistently in the load balancer's server name where punctuation characters are
     * forbidden.
     *
     * @param name to check
     * @return Boolean true if the name is purely alphanumeric
     */
    static Boolean checkStrictName(String name) {
        name ==~ /^[a-zA-Z0-9]+$/
    }

    /**
     * Validates a name of a cloud object according to the rules in http://go/CloudModel
     * The name can contain letters, numbers, dots, and underscores.
     *
     * @param name the string to validate
     * @return true if the name is valid
     */
    static Boolean checkName(String name) {
        NameValidation.checkName(name)
    }

    /**
     * The detail part of an auto scaling group name can include letters, numbers, dots, underscores, and hyphens.
     * Restricting the ASG name this way allows safer assumptions in other code about ASG names, like a promise of no
     * spaces, hash marks, percent signs, or dollar signs.
     *
     * @param detail the detail string to validate
     * @return true if the detail is valid
     */
    static Boolean checkDetail(String detail) {
        NameValidation.checkDetail(detail)
    }

    static String buildLaunchConfigurationName(String autoScalingGroupName) {
        return "${autoScalingGroupName}-${new Date().format("yyyyMMddHHmmss")}"
    }

    static String buildNextAutoScalingGroupName(String previousGroupNameInCluster) {
        Names names = dissectCompoundName(previousGroupNameInCluster)
        Integer previous = names.sequence
        Integer next = (previous == null || previous >= CLUSTER_MAX_SEQUENCE_NUMBER) ? 0 : previous + 1
        String threeDigitNextNumber = String.format("%03d", next)
        "${names.cluster}-v${threeDigitNextNumber}"
    }

    static String buildLoadBalancerName(String appName, String stack, String detail) {
        new LoadBalancerNameBuilder(appName: appName, stack: stack, detail: detail).buildLoadBalancerName()
    }

    static String buildAppDetailName(String appName, String detail) {
        Check.notEmpty(appName, "appName")
        return detail ? "$appName-$detail" : appName
    }

    static String buildScalingPolicyName(String autoScalingGroupName, String id) {
        Check.notEmpty(autoScalingGroupName)
        Check.notEmpty(id)
        [autoScalingGroupName, id].join('-')
    }

    static String buildAlarmName(String autoScalingGroupName, String id) {
        Check.notEmpty(id)
        [autoScalingGroupName ?: 'alarm', id].join('-')
    }

    static String packageFromAppVersion(String appVersion) {
        dissectAppVersion(appVersion)?.packageName
    }

    static AppVersion dissectAppVersion(String appVersion) {
        AppVersion.parseName(appVersion)
    }

    static String baseAmiIdFromDescription(String imageDescription) {
        BaseAmiInfo.parseDescription(imageDescription).baseAmiId
    }

    static String baseAmiNameFromDescription(String imageDescription) {
        BaseAmiInfo.parseDescription(imageDescription).baseAmiName
    }

    static DateTime baseAmiDateFromDescription(String imageDescription) {
        Date date = BaseAmiInfo.parseDescription(imageDescription).baseAmiDate
        date ? new DateTime(date) : null
    }

    /**
     * Convert a VPC Zone Identifier into a list of subnet IDs.
     * A VPC Zone Identifier is really just a comma delimited list of subnet IDs.
     *
     * @param vpcZoneIdentifier the VPC Zone Identifier
     * @return list of subnet IDs
     */
    static List<String> subnetIdsFromVpcZoneIdentifier(String vpcZoneIdentifier) {
        vpcZoneIdentifier?.tokenize(',') ?: []
    }

    /**
     * Convert a list of subnet IDs into a VPC Zone Identifier.
     * A VPC Zone Identifier is really just a comma delimited list of subnet IDs.
     *
     * @param subnetIds the list of subnet IDs
     * @return the VPC Zone Identifier
     */
    static String vpcZoneIdentifierFromSubnetIds(List<String> subnetIds) {
        subnetIds.join(',')
    }

    private static List<String> LABELED_ENV_VAR_FIELDS = ['countries', 'devPhase', 'hardware', 'partners',
            'revision', 'usedBy', 'redBlackSwap', 'zone'].sort()

    static List<String> labeledEnvironmentVariables(String asgName, String prefix) {
        labeledEnvironmentVariables(dissectCompoundName(asgName), prefix)
    }

    static List<String> labeledEnvironmentVariables(Names names, String prefix) {
        Check.notNull(prefix, String, 'prefix')
        List<String> envVars = []

        LABELED_ENV_VAR_FIELDS.each { String field ->
            if (names[field]) {
                envVars << "export ${prefix}${Meta.splitCamelCase(field, "_").toUpperCase()}=${names[field]}"
            }
        }
        envVars
    }

    private static PARTS_FIELDS = (LABELED_ENV_VAR_FIELDS + ['stack', 'detail']).sort()

    static Map<String, String> parts(String asgName) {
        parts(dissectCompoundName(asgName))
    }

    static Map<String, String> parts(Names names) {
        Map<String, String> parts = [:]
        PARTS_FIELDS.each { String field ->
            if (names[field]) {
                parts[WordUtils.capitalize(Meta.splitCamelCase(field, " "))] = names[field]
            }
        }
        parts
    }
}


