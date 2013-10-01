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
package com.netflix.asgard.mock

import com.amazonaws.AmazonWebServiceRequest
import com.amazonaws.ClientConfiguration
import com.amazonaws.ResponseMetadata
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.rds.AmazonRDSClient
import com.amazonaws.services.rds.model.AuthorizeDBSecurityGroupIngressRequest
import com.amazonaws.services.rds.model.CreateDBInstanceReadReplicaRequest
import com.amazonaws.services.rds.model.CreateDBInstanceRequest
import com.amazonaws.services.rds.model.CreateDBParameterGroupRequest
import com.amazonaws.services.rds.model.CreateDBSecurityGroupRequest
import com.amazonaws.services.rds.model.CreateDBSnapshotRequest
import com.amazonaws.services.rds.model.DBInstance
import com.amazonaws.services.rds.model.DBParameterGroup
import com.amazonaws.services.rds.model.DBSecurityGroup
import com.amazonaws.services.rds.model.DBSnapshot
import com.amazonaws.services.rds.model.DeleteDBInstanceRequest
import com.amazonaws.services.rds.model.DeleteDBParameterGroupRequest
import com.amazonaws.services.rds.model.DeleteDBSecurityGroupRequest
import com.amazonaws.services.rds.model.DeleteDBSnapshotRequest
import com.amazonaws.services.rds.model.DescribeDBEngineVersionsRequest
import com.amazonaws.services.rds.model.DescribeDBEngineVersionsResult
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest
import com.amazonaws.services.rds.model.DescribeDBInstancesResult
import com.amazonaws.services.rds.model.DescribeDBParameterGroupsRequest
import com.amazonaws.services.rds.model.DescribeDBParameterGroupsResult
import com.amazonaws.services.rds.model.DescribeDBParametersRequest
import com.amazonaws.services.rds.model.DescribeDBParametersResult
import com.amazonaws.services.rds.model.DescribeDBSecurityGroupsRequest
import com.amazonaws.services.rds.model.DescribeDBSecurityGroupsResult
import com.amazonaws.services.rds.model.DescribeDBSnapshotsRequest
import com.amazonaws.services.rds.model.DescribeDBSnapshotsResult
import com.amazonaws.services.rds.model.DescribeEngineDefaultParametersRequest
import com.amazonaws.services.rds.model.DescribeEventsRequest
import com.amazonaws.services.rds.model.DescribeEventsResult
import com.amazonaws.services.rds.model.DescribeReservedDBInstancesOfferingsRequest
import com.amazonaws.services.rds.model.DescribeReservedDBInstancesOfferingsResult
import com.amazonaws.services.rds.model.DescribeReservedDBInstancesRequest
import com.amazonaws.services.rds.model.DescribeReservedDBInstancesResult
import com.amazonaws.services.rds.model.EngineDefaults
import com.amazonaws.services.rds.model.ModifyDBInstanceRequest
import com.amazonaws.services.rds.model.ModifyDBParameterGroupRequest
import com.amazonaws.services.rds.model.ModifyDBParameterGroupResult
import com.amazonaws.services.rds.model.PurchaseReservedDBInstancesOfferingRequest
import com.amazonaws.services.rds.model.RebootDBInstanceRequest
import com.amazonaws.services.rds.model.ReservedDBInstance
import com.amazonaws.services.rds.model.ResetDBParameterGroupRequest
import com.amazonaws.services.rds.model.ResetDBParameterGroupResult
import com.amazonaws.services.rds.model.RestoreDBInstanceFromDBSnapshotRequest
import com.amazonaws.services.rds.model.RestoreDBInstanceToPointInTimeRequest
import com.amazonaws.services.rds.model.RevokeDBSecurityGroupIngressRequest
import com.netflix.asgard.BeanState

class MockAmazonRDSClient extends AmazonRDSClient {

    private Collection<DBInstance> mockDbInstances
    private Collection<DBSecurityGroup> mockDbSecurityGroups
    private Collection<DBSnapshot> mockDbSnapshots

    private List<DBInstance> loadMockDbInstances() {
        [new DBInstance().withDBInstanceIdentifier('goofy')]
    }

    private List<DBSecurityGroup> loadMockDbSecurityGroups() {
        [new DBSecurityGroup().withDBSecurityGroupName('donaldduck')]
    }

    private List<DBSnapshot> loadMockDbSnapshots() {
        [new DBSnapshot().withDBSnapshotIdentifier('mickeymouse')]
    }

    MockAmazonRDSClient(BasicAWSCredentials awsCredentials, ClientConfiguration clientConfiguration) {
        super(awsCredentials as BasicAWSCredentials, clientConfiguration)
        mockDbInstances = loadMockDbInstances()
        mockDbSecurityGroups = loadMockDbSecurityGroups()
        mockDbSnapshots = loadMockDbSnapshots()
    }

    void setEndpoint(String endpoint) {

    }

    void deleteDBParameterGroup(DeleteDBParameterGroupRequest deleteDBParameterGroupRequest) {

    }

    DBSnapshot deleteDBSnapshot(DeleteDBSnapshotRequest deleteDBSnapshotRequest) {
        null
    }

    ModifyDBParameterGroupResult modifyDBParameterGroup(ModifyDBParameterGroupRequest modifyDBParameterGroupRequest) {
        null
    }

    DBSecurityGroup revokeDBSecurityGroupIngress(
            RevokeDBSecurityGroupIngressRequest revokeDBSecurityGroupIngressRequest) {
        null
    }

    DescribeDBParametersResult describeDBParameters(DescribeDBParametersRequest describeDBParametersRequest) {
        null
    }

    DescribeEventsResult describeEvents(DescribeEventsRequest describeEventsRequest) {
        null
    }

    DBSecurityGroup createDBSecurityGroup(CreateDBSecurityGroupRequest createDBSecurityGroupRequest) {
        null
    }

    DescribeDBInstancesResult describeDBInstances(DescribeDBInstancesRequest describeDBInstancesRequest) {
        new DescribeDBInstancesResult().withDBInstances(mockDbInstances)
    }

    DescribeDBParameterGroupsResult describeDBParameterGroups(
            DescribeDBParameterGroupsRequest describeDBParameterGroupsRequest) {
        null
    }

    DBSnapshot createDBSnapshot(CreateDBSnapshotRequest createDBSnapshotRequest) {
        null
    }

    DescribeDBEngineVersionsResult describeDBEngineVersions(
            DescribeDBEngineVersionsRequest describeDBEngineVersionsRequest) {
        null
    }

    DBInstance rebootDBInstance(RebootDBInstanceRequest rebootDBInstanceRequest) {
        null
    }

    DBSecurityGroup authorizeDBSecurityGroupIngress(
            AuthorizeDBSecurityGroupIngressRequest authorizeDBSecurityGroupIngressRequest) {
        null
    }

    DBInstance restoreDBInstanceToPointInTime(
            RestoreDBInstanceToPointInTimeRequest restoreDBInstanceToPointInTimeRequest) {
        null
    }

    DescribeDBSnapshotsResult describeDBSnapshots(DescribeDBSnapshotsRequest describeDBSnapshotsRequest) {
        new DescribeDBSnapshotsResult().withDBSnapshots(mockDbSnapshots)
    }

    DescribeReservedDBInstancesOfferingsResult describeReservedDBInstancesOfferings(
            DescribeReservedDBInstancesOfferingsRequest describeReservedDBInstancesOfferingsRequest) {
        null
    }

    EngineDefaults describeEngineDefaultParameters(
            DescribeEngineDefaultParametersRequest describeEngineDefaultParametersRequest) {
        null
    }

    DBInstance deleteDBInstance(DeleteDBInstanceRequest deleteDBInstanceRequest) {
        null
    }

    DescribeDBSecurityGroupsResult describeDBSecurityGroups(
            DescribeDBSecurityGroupsRequest describeDBSecurityGroupsRequest) {
        new DescribeDBSecurityGroupsResult().withDBSecurityGroups(mockDbSecurityGroups)
    }

    DBInstance createDBInstance(CreateDBInstanceRequest createDBInstanceRequest) {
        BeanState.ofSourceBean(createDBInstanceRequest).injectState(new DBInstance())
    }

    ResetDBParameterGroupResult resetDBParameterGroup(ResetDBParameterGroupRequest resetDBParameterGroupRequest) {
        null
    }

    DBInstance modifyDBInstance(ModifyDBInstanceRequest modifyDBInstanceRequest) {
        null
    }

    DBInstance restoreDBInstanceFromDBSnapshot(
            RestoreDBInstanceFromDBSnapshotRequest restoreDBInstanceFromDBSnapshotRequest) {
        null
    }

    DescribeReservedDBInstancesResult describeReservedDBInstances(
            DescribeReservedDBInstancesRequest describeReservedDBInstancesRequest) {
        null
    }

    DBParameterGroup createDBParameterGroup(
            CreateDBParameterGroupRequest createDBParameterGroupRequest) {
        null
    }

    void deleteDBSecurityGroup(DeleteDBSecurityGroupRequest deleteDBSecurityGroupRequest) {

    }

    DBInstance createDBInstanceReadReplica(CreateDBInstanceReadReplicaRequest createDBInstanceReadReplicaRequest) {
        null
    }

    ReservedDBInstance purchaseReservedDBInstancesOffering(
            PurchaseReservedDBInstancesOfferingRequest purchaseReservedDBInstancesOfferingRequest) {
        null
    }

    DescribeEventsResult describeEvents() {
        null
    }

    DescribeDBInstancesResult describeDBInstances() {
        new DescribeDBInstancesResult().withDBInstances(mockDbInstances)
    }

    DescribeDBParameterGroupsResult describeDBParameterGroups() {
        null
    }

    DescribeDBEngineVersionsResult describeDBEngineVersions() {
        null
    }

    DescribeDBSnapshotsResult describeDBSnapshots() {
        new DescribeDBSnapshotsResult().withDBSnapshots(mockDbSnapshots)
    }

    DescribeReservedDBInstancesOfferingsResult describeReservedDBInstancesOfferings() {
        null
    }

    DescribeDBSecurityGroupsResult describeDBSecurityGroups() {
        new DescribeDBSecurityGroupsResult().withDBSecurityGroups(mockDbSecurityGroups)
    }

    DescribeReservedDBInstancesResult describeReservedDBInstances() {
        null
    }

    void shutdown() {

    }

    ResponseMetadata getCachedResponseMetadata(AmazonWebServiceRequest request) {
        null
    }

}
