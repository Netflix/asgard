/*
 * Copyright 2013 Netflix, Inc.
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
package com.netflix.asgard.model

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap

/**
 * This class makes Cloudwatch metrics and dimensions available by their namespace. It handles known AWS metrics as
 * well as your own custom metrics supplied on initialization.
 * The relationships between AWS Cloudwatch metrics, dimensions and namespaces was created according to the AWS
 * documentation referenced below.
 * http://docs.amazonwebservices.com/AmazonCloudWatch/latest/DeveloperGuide/CW_Support_For_AWS.html
 */
class MetricNamespaces {

    private static final ImmutableList<MetricNamespace> AWS_NAMESPACES = ImmutableList.of(
            MetricNamespace.of('AWS/Billing', [
                    'EstimatedCharges'
            ], [
                    'ServiceName',
                    'LinkedAccount'
            ]),
            MetricNamespace.of('AWS/DynamoDB', [
                    'SuccessfulRequestLatency',
                    'UserErrors',
                    'SystemErrors',
                    'ThrottledRequests',
                    'ProvisionedReadCapacityUnits',
                    'ProvisionedWriteCapacityUnits',
                    'ConsumedReadCapacityUnits',
                    'ConsumedWriteCapacityUnits',
                    'ReturnedItemCount'
            ], [
                    'TableName',
                    'Operation'
            ]),
            MetricNamespace.of('AWS/ElastiCache', [
                    'CPUUtilization',
                    'SwapUsage',
                    'FreeableMemory',
                    'NetworkBytesIn',
                    'NetworkBytesOut'
            ], [
                    'BytesUsedForCacheItems',
                    'BytesReadIntoMemcached',
                    'CasBadval',
                    'CasHits',
                    'CasMisses',
                    'CmdFlush',
                    'CmdGet',
                    'CmdSet',
                    'CPUUtilization',
                    'CurrConnections',
                    'CurrItems',
                    'DecrHits',
                    'DecrMisses',
                    'DeleteHits',
                    'DeleteMisses',
                    'Evictions',
                    'GetHits',
                    'GetMisses',
                    'IncrHits',
                    'IncrMisses',
                    'Reclaimed',
                    'NewConnections',
                    'NewItems',
                    'UnusedMemory'
            ]),
            MetricNamespace.of('AWS/EBS', [
                    'VolumeReadBytes',
                    'VolumeWriteBytes',
                    'VolumeReadOps',
                    'VolumeWriteOps',
                    'VolumeTotalReadTime',
                    'VolumeTotalWriteTime',
                    'VolumeIdleTime',
                    'VolumeQueueLength',
                    'VolumeThroughputPercentage',
                    'VolumeConsumedReadWriteOps'
            ], [
                    'VolumeId'
            ]),
            MetricNamespace.of('AWS/EC2', [
                    'CPUUtilization',
                    'DiskReadOps',
                    'DiskWriteOps',
                    'DiskReadBytes',
                    'DiskWriteBytes',
                    'NetworkIn',
                    'NetworkOut',
                    'StatusCheckFailed',
                    'StatusCheckFailed_Instance',
                    'StatusCheckFailed_System'
            ], [
                    'AutoScalingGroupName',
                    'ImageId',
                    'InstanceId',
                    'InstanceType'
            ]),
            MetricNamespace.of('AWS/ElasticMapReduce', [
                    'CoreNodesPending',
                    'CoreNodesRunning',
                    'HBaseBackupFailed',
                    'HBaseMostRecentBackupDuration',
                    'HBaseTimeSinceLastSuccessfulBackup',
                    'HDFSBytesRead',
                    'HDFSBytesWritten',
                    'HDFSUtilization',
                    'IsIdle',
                    'JobsFailed',
                    'JobsRunning',
                    'LiveDataNodes',
                    'LiveTaskTrackers',
                    'MapSlotsOpen',
                    'MissingBlocks',
                    'ReduceSlotsOpen',
                    'RemainingMapTasks',
                    'RemainingMapTasksPerSlot',
                    'RemainingReduceTasks',
                    'RunningMapTasks',
                    'RunningReduceTasks',
                    'S3BytesRead',
                    'S3BytesWritten',
                    'TaskNodesPending',
                    'TaskNodesRunning',
                    'TotalLoad'
            ], [
                    'JobFlowId',
                    'JobId'
            ]),
            MetricNamespace.of('AWS/RDS', [
                    'BinLogDiskUsage',
                    'CPUUtilization',
                    'DatabaseConnections',
                    'FreeableMemory',
                    'FreeStorageSpace',
                    'ReplicaLag',
                    'SwapUsage',
                    'ReadIOPS',
                    'WriteIOPS',
                    'ReadLatency',
                    'WriteLatency',
                    'ReadThroughput',
                    'WriteThroughput'
            ], [
                    'DBInstanceIdentifier',
                    'DatabaseClass',
                    'EngineName'
            ]),
            MetricNamespace.of('AWS/SNS', [
                    'NumberOfMessagesPublished',
                    'PublishSize',
                    'NumberOfNotificationsDelivered',
                    'NumberOfNotificationsFailed'
            ], [
                    'TopicName'
            ]),
            MetricNamespace.of('AWS/SQS', [
                    'NumberOfMessagesSent',
                    'SentMessageSize',
                    'NumberOfMessagesReceived',
                    'NumberOfEmptyReceives',
                    'NumberOfMessagesDeleted',
                    'ApproximateNumberOfMessagesDelayed',
                    'ApproximateNumberOfMessagesVisible',
                    'ApproximateNumberOfMessagesNotVisible'
            ], [
                    'QueueName'
            ]),
            MetricNamespace.of('AWS/StorageGateway', [
                    'ReadBytes',
                    'WriteBytes',
                    'ReadTime',
                    'WriteTime',
                    'QueuedWrites',
                    'CloudBytesDownloaded',
                    'CloudBytesUploaded',
                    'CloudDownloadLatency',
                    'WorkingStoragePercentUsed',
                    'WorkingStorageUsed',
                    'WorkingStorageFree'
            ], [
                    'GatewayId',
                    'GatewayName',
                    'VolumeId'
            ]),
            MetricNamespace.of('AWS/AutoScaling', [
                    'GroupMinSize',
                    'GroupMaxSize',
                    'GroupDesiredCapacity',
                    'GroupInServiceInstances',
                    'GroupPendingInstances',
                    'GroupTerminatingInstances',
                    'GroupTotalInstances'
            ], [
                    'AutoScalingGroupName'
            ]),
            MetricNamespace.of('AWS/ELB', [
                    'Latency',
                    'RequestCount',
                    'HealthyHostCount',
                    'UnHealthyHostCount',
                    'HTTPCode_ELB_4XX',
                    'HTTPCode_ELB_5XX',
                    'HTTPCode_Backend_2XX',
                    'HTTPCode_Backend_3XX',
                    'HTTPCode_Backend_4XX',
                    'HTTPCode_Backend_5XX',
            ], [
                    'LoadBalancerName',
                    'AvailabilityZone'
            ])
    )

    private static final ImmutableMap<String, MetricNamespace> AWS_NAMESPACES_BY_NAME = ImmutableMap.
            copyOf(AWS_NAMESPACES.collectEntries { [it.namespace, it] })

    private final ImmutableMap<String, MetricNamespace> allNamespacesByName


    /**
     * Construct MetricNamespaces with specified custom namespace metrics and dimensions.
     *
     * @param customNamespacesToDimensions describe dimensions available to each custom namespace
     * @param allCustomMetricIds specify all custom metrics across namespaces
     */
    MetricNamespaces(Map<String, Collection<String>> customNamespacesToDimensions = [:],
                     Collection<MetricId> allCustomMetricIds = []) {
        Map<String, List<MetricId>> namespacesToMetricIds = allCustomMetricIds?.groupBy { it.namespace }
        List<MetricNamespace> customMetricNamespace = customNamespacesToDimensions?.keySet()?.collect {
            List<String> metricNames = namespacesToMetricIds[it].collect { it.metricName }
            MetricNamespace.of(it, metricNames, customNamespacesToDimensions[it])
        } ?: []
        allNamespacesByName = ImmutableMap.copyOf(AWS_NAMESPACES_BY_NAME + customMetricNamespace.collectEntries { [ it.namespace, it] })
    }

    /**
     * @return all metrics across all namespaces
     */
    Set<MetricId> getAllMetricIds() {
        allNamespacesByName.values().collectMany { namespace ->
            namespace.getMetricIds()
        } as Set
    }

    /**
     * Gets the dimensions available for a metric namespace.
     *
     * @param namespace
     * @return dimension names
     */
    List<String> getDimensionsForNamespace(String namespace) {
        allNamespacesByName[namespace]?.dimensions?.sort()
    }
}
