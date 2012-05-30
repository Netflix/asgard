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
import com.amazonaws.services.autoscaling.model.ScalingPolicy
import com.amazonaws.services.cloudwatch.model.MetricAlarm
import com.amazonaws.services.ec2.model.AvailabilityZone
import com.amazonaws.services.ec2.model.Image
import com.amazonaws.services.ec2.model.Instance
import com.amazonaws.services.ec2.model.KeyPairInfo
import com.amazonaws.services.ec2.model.ReservedInstances
import com.amazonaws.services.ec2.model.SecurityGroup
import com.amazonaws.services.ec2.model.Snapshot
import com.amazonaws.services.ec2.model.SpotInstanceRequest
import com.amazonaws.services.ec2.model.Volume
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription
import com.amazonaws.services.elasticloadbalancing.model.SourceSecurityGroup
import com.amazonaws.services.rds.model.DBInstance
import com.amazonaws.services.rds.model.DBSecurityGroup
import com.amazonaws.services.rds.model.DBSnapshot
import com.google.common.collect.ImmutableBiMap
import com.google.common.collect.ImmutableSet
import com.netflix.asgard.model.ApplicationInstance
import com.netflix.asgard.model.ApplicationMetrics
import com.netflix.asgard.model.HardwareProfile
import com.netflix.asgard.model.InstanceTypeData
import com.netflix.asgard.model.SimpleQueue
import com.netflix.asgard.model.TopicData
import com.netflix.asgard.push.Cluster
import groovy.transform.Immutable
import java.lang.reflect.Field
import java.lang.reflect.Modifier

@Immutable class EntityType<T> {

    // By convention, entity names match corresponding controller names.
    static final EntityType<MetricAlarm> alarm = create('Metric Alarm', { it.alarmName })
    static final EntityType<AppRegistration> application = create('Application', { it.name })
    static final EntityType<ApplicationInstance> applicationInstance = create('App Instance', { it.hostName })
    static final EntityType<ApplicationMetrics> applicationMetric = create('Application Metric', { it.application })
    static final EntityType<AutoScalingGroup> autoScaling = create('Auto Scaling Group',
            { it.autoScalingGroupName })
    static final EntityType<AvailabilityZone> availabilityZone = create('Availability Zone', { it.zoneName })
    static final EntityType<Cluster> cluster = create('Cluster', { it.name }, '',
            'Show all the auto scaling groups in this cluster')
    static final EntityType<DBInstance> rdsInstance = create('Database Instance', { it.DBInstanceIdentifier })
    static final EntityType<DBSecurityGroup> dbSecurity = create('Database Security Group',
            { it.DBSecurityGroupName })
    static final EntityType<DBSnapshot> dbSnapshot = create('Database Snapshot', { it.DBSnapshotIdentifier })
    static final EntityType<String> domain = create('SimpleDB Domain', { it }, '',
            'Show metadata about this SimpleDB domain')
    static final EntityType<FastProperty> fastProperty = create('Fast Property', { it.id })
    static final EntityType<HardwareProfile> hardwareProfile = create('Hardware Profile',
            { it.instanceType.toString() })
    static final EntityType<Image> image = create('Image', { it.imageId }, 'ami-',
            'Show details of this Amazon machine image')
    static final EntityType<Instance> instance = create('Instance', { it.instanceId }, 'i-')
    static final EntityType<InstanceTypeData> instanceType = create('Instance Type', { it.name })
    static final EntityType<KeyPairInfo> keyPair = create('Key Pair', { it.keyName })
    static final EntityType<LaunchConfiguration> launchConfiguration = create('Launch Configuration',
            { it.launchConfigurationName })
    static final EntityType<LoadBalancerDescription> loadBalancer = create('Elastic Load Balancer',
            { it.loadBalancerName })
    static final EntityType<SimpleQueue> queue = create('Queue', { it.name })
    static final EntityType<ReservedInstances> reservation = create('Reservation', { it.reservedInstancesId })
    static final EntityType<ScalingPolicy> scalingPolicy = create('Scaling Policy', { it.policyName })
    static final EntityType<SecurityGroup> security = create('Security Group', { it.groupName })
    static final EntityType<Snapshot> snapshot = create('Storage Snapshot', { it.snapshotId }, 'snap-')
    static final EntityType<SourceSecurityGroup> sourceSecurityGroup = create('Source Security Group', { it.groupName })
    static final EntityType<SpotInstanceRequest> spotInstanceRequest = create('Spot Instance Request',
            { it.spotInstanceRequestId }, 'sir-')
    static final EntityType<Task> task = create('Task', { it.id })
    static final EntityType<TopicData> topic = create('Topic', { it.name })
    static final EntityType<Volume> volume = create('Volume', { it.volumeId }, 'vol-')

    /*
     * These two convenience constructors are explicit because of an IntelliJ bug that cannot handle the generics on a
     * method with optional parameters
     */
    static <T> EntityType<T> create(String displayName, Closure keyer) {
        create(displayName, keyer, '')
    }

    static <T> EntityType<T> create(String displayName, Closure keyer, String idPrefix) {
        create(displayName, keyer, idPrefix, '')
    }

    static <T> EntityType<T> create(String displayName, Closure keyer, String idPrefix, String linkPurpose) {
        new EntityType<T>(displayName, keyer, idPrefix, linkPurpose ?: "Show details of this ${displayName}")
    }

    private static final Collection<EntityType> allEntityTypes
    private static final ImmutableBiMap<String, EntityType> nameToEntityType
    static {
        Collection<Field> entityTypeFields = EntityType.declaredFields.findAll {
            Modifier.isStatic(it.modifiers) && it.type == EntityType
        }
        ImmutableBiMap.Builder<String, EntityType> nameToEntityTypeBuilder =
            new ImmutableBiMap.Builder<String, EntityType>()
        Collection<EntityType> entityTypes = []
        for (Field field : entityTypeFields) {
            EntityType type = EntityType[field.name] as EntityType
            entityTypes << type
            nameToEntityTypeBuilder.put(field.name, type)
        }
        allEntityTypes = ImmutableSet.copyOf(entityTypes)
        nameToEntityType = nameToEntityTypeBuilder.build()
    }

    static Collection<EntityType> values() {
        allEntityTypes
    }

    static EntityType fromName(String name) {
        nameToEntityType.get(name)
    }

    static String nameOf(EntityType entityType) {
        nameToEntityType.inverse().get(entityType)
    }

    static EntityType fromId(String id) {
        values().find { it.idPrefix && id?.startsWith(it.idPrefix) }
    }

    String displayName
    Closure keyer
    String idPrefix
    String linkPurpose

    /**
     * The unique String key for the value object.
     *
     * @param an entity of type T
     * @return The unique String key
     */
    String key(T entity) {
        keyer(entity)
    }

    String name() {
        EntityType.nameOf(this)
    }

    /**
     * If id is provided and it lacks the correct prefix, add the prefix. If the prefix is already present or id is
     * blank then make no changes to the id.
     *
     * @param input the provided id string
     * @return String the correct id
     */
    String ensurePrefix(String input) {
        if (!idPrefix) { return input }
        String id = input?.trim()
        id?.startsWith(idPrefix) || !id ? id : "${idPrefix}${id}"
    }
}
