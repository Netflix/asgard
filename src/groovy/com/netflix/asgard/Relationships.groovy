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
import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target
import java.util.regex.Matcher
import java.util.regex.Pattern
import org.apache.commons.lang.WordUtils
import org.apache.commons.lang.builder.ToStringBuilder
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

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
        Matcher nameMatcher = NAME_PATTERN.matcher(autoScalingGroupName)
        if (nameMatcher.matches()) {
            return nameMatcher.group(1)
        }
        ''
    }

    static String stackNameFromGroupName(String autoScalingGroupName) {
        dissectCompoundName(autoScalingGroupName).stack ?: ''
    }

    static String clusterFromGroupName(String autoScalingGroupName) {
        dissectCompoundName(autoScalingGroupName).cluster ?: ''
    }

    private static final String NAME_CHARS = 'a-zA-Z0-9._'
    private static final String NAME_HYPHEN_CHARS = '-a-zA-Z0-9._'
    private static final String PUSH_FORMAT = /v([0-9]{3})/
    private static final String LABELED_VAR_SEPARATOR = '0'
    private static final String LABELED_VARIABLE = /[a-zA-Z][${LABELED_VAR_SEPARATOR}][a-zA-Z0-9]+/
    private static final Pattern NAME_PATTERN = Pattern.
            compile(/^([$NAME_CHARS]+)(?:-([$NAME_CHARS]*))?(?:-([$NAME_HYPHEN_CHARS]*?))?$/)

    static final String COUNTRIES_KEY = 'c'
    static final String DEV_PHASE_KEY = 'd'
    static final String HARDWARE_KEY = 'h'
    static final String PARTNERS_KEY = 'p'
    static final String REVISION_KEY = 'r'
    static final String USED_BY_KEY = 'u'
    static final String RED_BLACK_SWAP_KEY = 'w'
    static final String ZONE_KEY = 'z'

    /**
     * Breaks down the name of an auto scaling group or load balancer into its component parts.
     *
     * @param compoundName the name of an auto scaling group or load balancer
     * @return Names a data object containing the component parts of the compound name
     */
    static Names dissectCompoundName(String compoundName) {

        if (!compoundName) { return new Names([:]) }

        Matcher pushMatcher = compoundName =~ /^([$NAME_HYPHEN_CHARS]*)-($PUSH_FORMAT)$/
        Boolean hasPush = pushMatcher.matches()
        String cluster = hasPush ? pushMatcher[0][1] : compoundName
        String push = hasPush ? pushMatcher[0][2] : null
        Integer sequence = hasPush ? pushMatcher[0][3] as Integer : null

        Matcher labeledVarsMatcher = cluster =~ /^([$NAME_HYPHEN_CHARS]*?)((-$LABELED_VARIABLE)*)$/
        Boolean labeledAndUnlabeledMatches = labeledVarsMatcher.matches()
        if (!labeledAndUnlabeledMatches) {
            return new Names([:])
        }
        String unlabeledVars = labeledVarsMatcher[0][1]
        String labeledVariables = labeledVarsMatcher[0][2]

        Matcher nameMatcher = NAME_PATTERN.matcher(unlabeledVars)
        def parts = nameMatcher[0]
        String app = parts[1]
        String stack = parts[2] ?: null
        String detail = parts[3] ?: null

        String countries    = extractLabeledVariable(labeledVariables, COUNTRIES_KEY)
        String devPhase     = extractLabeledVariable(labeledVariables, DEV_PHASE_KEY)
        String hardware     = extractLabeledVariable(labeledVariables, HARDWARE_KEY)
        String partners     = extractLabeledVariable(labeledVariables, PARTNERS_KEY)
        String revision     = extractLabeledVariable(labeledVariables, REVISION_KEY)
        String usedBy       = extractLabeledVariable(labeledVariables, USED_BY_KEY)
        String redBlackSwap = extractLabeledVariable(labeledVariables, RED_BLACK_SWAP_KEY)
        String zone         = extractLabeledVariable(labeledVariables, ZONE_KEY)

        new Names(group: compoundName, cluster: cluster, app: app, stack: stack, detail: detail, push: push,
                sequence: sequence, countries: countries, devPhase: devPhase, hardware: hardware, partners: partners,
                revision: revision, usedBy: usedBy, redBlackSwap: redBlackSwap, zone: zone)
    }

    static String extractLabeledVariable(String labeledVariablesString, String labelKey) {
        if (labeledVariablesString && labelKey) {
            Matcher labelMatcher = labeledVariablesString =~
                    /.*?-${labelKey}${LABELED_VAR_SEPARATOR}([$NAME_CHARS]*).*?$/
            Boolean hasLabel = labelMatcher.matches()
            if (hasLabel) {
                def parts = labelMatcher[0]
                return parts[1]
            }
        }
        null
    }

    static String buildGroupName(Map params, Boolean doValidation = false) {
        String appName = params.appName
        Check.notEmpty(appName, "appName")

        String stack = params.newStack ?: params.stack
        String detail = params.detail
        String countries = params.countries
        String devPhase = params.devPhase
        String hardware = params.hardware
        String partners = params.partners
        String revision = params.revision
        String usedBy = params.usedBy
        String redBlackSwap = params.redBlackSwap
        String zoneVar = params.zoneVar

        if (doValidation) {
            if ([appName, stack, countries, devPhase, hardware, partners, revision, usedBy, redBlackSwap, zoneVar].
                    any { it && !Relationships.checkName(it) } || (detail && !Relationships.checkDetail(detail))) {
                throw new IllegalArgumentException('(Use alphanumeric characters only)')
            }
        }

        String sep = LABELED_VAR_SEPARATOR

        // Build the labeled variables for the end of the group name.
        String labeledVars = ''
        if (countries)    { labeledVars +=      "-${COUNTRIES_KEY}${sep}${countries}"    }
        if (devPhase)     { labeledVars +=      "-${DEV_PHASE_KEY}${sep}${devPhase}"     }
        if (hardware)     { labeledVars +=       "-${HARDWARE_KEY}${sep}${hardware}"     }
        if (partners)     { labeledVars +=       "-${PARTNERS_KEY}${sep}${partners}"     }
        if (revision)     { labeledVars +=       "-${REVISION_KEY}${sep}${revision}"     }
        if (usedBy)       { labeledVars +=        "-${USED_BY_KEY}${sep}${usedBy}"       }
        if (redBlackSwap) { labeledVars += "-${RED_BLACK_SWAP_KEY}${sep}${redBlackSwap}" }
        if (zoneVar)      { labeledVars +=           "-${ZONE_KEY}${sep}${zoneVar}"      }

        String result = combineAppStackDetail(appName, stack, detail) + labeledVars

        return result
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
        name ==~ /.*?$PUSH_FORMAT/ || name ==~ /^(.*?-)?$LABELED_VARIABLE.*?$/
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
        name ==~ /^[$NAME_CHARS]+$/
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
        detail ==~ /^[$NAME_HYPHEN_CHARS]+$/
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
        return combineAppStackDetail(appName, stack, detail)
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
        Check.notEmpty(autoScalingGroupName)
        Check.notEmpty(id)
        [autoScalingGroupName, id].join('-')
    }

    private static String combineAppStackDetail(String appName, String stack, String detail) {
        Check.notEmpty(appName, "appName")
        // Use empty strings, not null references that output "null"
        stack = stack ?: ""
        detail = detail ?: ""

        return detail ? "$appName-$stack-$detail" : stack ? "$appName-$stack" : appName
    }

    /**
     * All of these are valid:
     * subscriberha-1.0.0-586499
     * subscriberha-1.0.0-586499.h150
     * subscriberha-1.0.0-586499.h150/WE-WAPP-subscriberha/150
     */
    static final Pattern APP_VERSION_PATTERN =
            ~/([$NAME_HYPHEN_CHARS]+)-([0-9.]+)-([0-9]{5,7})(?:[.]h([0-9]+))?(?:\/([-a-zA-z0-9]+)\/([0-9]+))?/

    static String packageFromAppVersion(String appVersion) {
        dissectAppVersion(appVersion)?.packageName
    }

    static AppVersion dissectAppVersion(String appVersion) {
        Matcher matcher = appVersion =~ APP_VERSION_PATTERN
        if (matcher.matches()) {
            return new AppVersion(packageName: matcher[0][1], version: matcher[0][2], changelist: matcher[0][3],
                    buildNumber: matcher[0][4], buildJobName: matcher[0][5])
        }
        null
    }

    private final static def IMAGE_ID = /ami-[a-z0-9]{8}/

    static String baseAmiIdFromDescription(String imageDescription) {
        // base_ami_id=ami-1eb75c77,base_ami_name=servicenet-roku-qadd.dc.81210.10.44
        Matcher matcher = imageDescription =~ /^.*?base_ami_id=($IMAGE_ID).*?$/
        if (matcher.matches()) { return matcher[0][1] }
        // store=ebs,ancestor_name=ebs-centosbase-x86_64-20101124,ancestor_id=ami-7b4eb912
        matcher = imageDescription =~ /^.*?ancestor_id=($IMAGE_ID).*?$/
        if (matcher.matches()) { return matcher[0][1] }
        null
    }

    static String baseAmiNameFromDescription(String imageDescription) {
        // base_ami_id=ami-1eb75c77,base_ami_name=servicenet-roku-qadd.dc.81210.10.44
        Matcher matcher = imageDescription =~ /^.*?base_ami_name=([^,]+).*?$/
        if (matcher.matches()) { return matcher[0][1] }
        // store=ebs,ancestor_name=ebs-centosbase-x86_64-20101124,ancestor_id=ami-7b4eb912
        matcher = imageDescription =~ /^.*?ancestor_name=([^,]+).*?$/
        if (matcher.matches()) { return matcher[0][1] }
        null
    }

    static DateTime baseAmiDateFromDescription(String imageDescription) {
        String name = baseAmiNameFromDescription(imageDescription)
        Matcher matcher = name =~ /.*\-(20[0-9]{6})(\-.*)?/
        String dateString = null
        if (matcher.matches()) { dateString = matcher[0][1] }

        try {
            // Example: 20100823
            return dateString ? DateTimeFormat.forPattern("yyyyMMdd").parseDateTime(dateString) : null
        } catch (Exception ignored) {
            // Ignore failure.
            return null
        }
    }
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@interface LabeledEnvVar {}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@interface DisplayVar {}

@Immutable final class Names {
    String group
    String cluster
    String app
    @DisplayVar String stack
    @DisplayVar String detail
    String push
    Integer sequence
    @LabeledEnvVar String countries
    @LabeledEnvVar String devPhase
    @LabeledEnvVar String hardware
    @LabeledEnvVar String partners
    @LabeledEnvVar String revision
    @LabeledEnvVar String usedBy
    @LabeledEnvVar String redBlackSwap
    @LabeledEnvVar String zone

    List<String> labeledEnvironmentVariables(String prefix) {
        Check.notNull(prefix, String, 'prefix')
        List<String> envVars = []

        this.class.declaredFields.findAll { it.declaredAnnotations.any { it instanceof LabeledEnvVar } }.
                collect { it.name }.sort().each { String name ->
            if (this[name]) {
                envVars << "export ${prefix}${Meta.splitCamelCase(name, "_").toUpperCase()}=${this[name]}"
            }
        }
        envVars
    }

    Map<String, String> parts() {
        Map<String, String> parts = [:]
        this.class.declaredFields.
                findAll { it.declaredAnnotations.any { it instanceof DisplayVar || it instanceof LabeledEnvVar } }.
                collect { it.name }.sort().each { String name ->
            if (this[name]) {
                parts[WordUtils.capitalize(Meta.splitCamelCase(name, " "))] = this[name]
            }
        }
        return parts
    }
}

@Immutable final class AppVersion implements Comparable {
    String packageName
    String version
    String buildJobName
    String buildNumber
    String changelist

    int compareTo(Object o) {
        if (equals (o)) { // if x.equals(y), then x.compareTo(y) should be 0
            return 0
        }

        if (o == null) {
            return 1  // equals(null) can never be true, so compareTo(null) should never be 0
        }

        AppVersion other = o as AppVersion // ClassCastException is the desired result here

        packageName <=> other.packageName ?:
        version <=> other.version ?:
        buildJobName <=> other.buildJobName ?:
        buildNumber <=> other.buildNumber ?:
        changelist <=> other.changelist
    }

    /**
     *  Had performance issues related to https://jira.codehaus.org/browse/GROOVY-5249 with @Immutable implementation
     */
    @Override
    String toString() {
        new ToStringBuilder(this)
            .append('packageName', packageName)
            .append('version', version)
            .append('buildJobName', buildJobName)
            .append('buildNumber', buildNumber)
            .append('changelist', changelist)
            .toString()
    }
}
