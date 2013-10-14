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
import com.amazonaws.services.rds.AmazonRDS
import com.amazonaws.services.rds.model.AuthorizeDBSecurityGroupIngressRequest
import com.amazonaws.services.rds.model.CreateDBInstanceRequest
import com.amazonaws.services.rds.model.CreateDBSecurityGroupRequest
import com.amazonaws.services.rds.model.CreateDBSnapshotRequest
import com.amazonaws.services.rds.model.DBInstance
import com.amazonaws.services.rds.model.DBSecurityGroup
import com.amazonaws.services.rds.model.DBSnapshot
import com.amazonaws.services.rds.model.DeleteDBInstanceRequest
import com.amazonaws.services.rds.model.DeleteDBSecurityGroupRequest
import com.amazonaws.services.rds.model.DeleteDBSnapshotRequest
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest
import com.amazonaws.services.rds.model.DescribeDBSecurityGroupsRequest
import com.amazonaws.services.rds.model.DescribeDBSnapshotsRequest
import com.amazonaws.services.rds.model.DescribeDBSnapshotsResult
import com.amazonaws.services.rds.model.ModifyDBInstanceRequest
import com.amazonaws.services.rds.model.RestoreDBInstanceFromDBSnapshotRequest
import com.amazonaws.services.rds.model.RevokeDBSecurityGroupIngressRequest
import com.netflix.asgard.cache.CacheInitializer
import org.springframework.beans.factory.InitializingBean

class AwsRdsService implements CacheInitializer, InitializingBean {
    static final String SMALL = 'db.m1.small'
    static final String LARGE = 'db.m1.large'
    static final String EXTRA_LARGE = 'db.m1.xlarge'
    static final String DOUBLE_EXTRA_LARGE = 'db.m2.2xlarge'
    static final String QUADRUPLE_EXTRA_LARGE = 'db.m2.4xlarge'

    static final Integer DEFAULT_PORT = 3306

    enum Engine {
        MySQL("MySQL"),
        OracleSE1("oracle-se1"),
        OracleSE("oracle-se"),
        OracleEE("oracle-ee")

        final String awsValue

        Engine(String awsValue) {
            this.awsValue = awsValue
        }
    }

    static transactional = false

    def configService
    MultiRegionAwsClient<AmazonRDS> awsClient
    def awsClientService
    Caches caches
    def taskService
    List<String> accounts // Main account is accounts[0]

    void afterPropertiesSet() {
        awsClient = new MultiRegionAwsClient<AmazonRDS>( { Region region ->
            AmazonRDS client = awsClientService.create(AmazonRDS)
            client.setEndpoint("rds.${region}.amazonaws.com")
            client
        })
        accounts = configService.awsAccounts
    }

    void initializeCaches() {
        caches.allDBSecurityGroups.ensureSetUp({ Region region -> retrieveDBSecurityGroups(region) })
        caches.allDBInstances.ensureSetUp({ Region region -> retrieveDBInstances(region) })
        caches.allDBSnapshots.ensureSetUp({ Region region -> retrieveDBSnapshots(region) })
    }

    // Instances

    private List<DBInstance> retrieveDBInstances(Region region) {
        awsClient.by(region).describeDBInstances(new DescribeDBInstancesRequest()).getDBInstances()
    }

    Collection<DBInstance> getDBInstances(UserContext userContext) {
        caches.allDBInstances.by(userContext.region).list()
    }

    DBInstance getDBInstance(UserContext userContext, String dbInstanceId, From from = From.AWS) {
        Check.notNull(dbInstanceId, DBInstance)
        if (from == From.CACHE) {
            return caches.allDBInstances.by(userContext.region).get(dbInstanceId)
        }
        def result = awsClient.by(userContext.region).describeDBInstances(
                new DescribeDBInstancesRequest().withDBInstanceIdentifier(dbInstanceId))
        caches.allDBInstances.by(userContext.region).put(dbInstanceId, Check.lone(result.getDBInstances(),
                DBInstance))
    }

    void deleteDBInstance(UserContext userContext, String dbInstanceId) {
        taskService.runTask(userContext, "Deleting DB instance '${dbInstanceId}'", { task ->
            DBInstance instance = awsClient.by(userContext.region).deleteDBInstance(
                new DeleteDBInstanceRequest()
                    .withDBInstanceIdentifier(dbInstanceId)
                    .withFinalDBSnapshotIdentifier("${dbInstanceId}-final-snapshot")
            )
            caches.allDBInstances.by(userContext.region).put(instance.getDBInstanceIdentifier(), instance)
        }, Link.to(EntityType.rdsInstance, dbInstanceId))
    }

    DBInstance createDBInstance(UserContext userContext, DBInstance templateDbInstance, String masterUserPassword, Integer port) {
        final BeanState templateDbInstanceState = BeanState.ofSourceBean(templateDbInstance)
        final CreateDBInstanceRequest request = templateDbInstanceState.injectState(new CreateDBInstanceRequest())
        request.masterUserPassword = masterUserPassword
        if (port) { request.setPort(port) }
        taskService.runTask(userContext, "Creating DB instance '${templateDbInstance.DBInstanceIdentifier}'", { task ->
            final DBInstance createdInstance = awsClient.by(userContext.region).createDBInstance(request)
            caches.allDBInstances.by(userContext.region).put(createdInstance.getDBInstanceIdentifier(), createdInstance)
        }, Link.to(EntityType.rdsInstance, templateDbInstance.DBInstanceIdentifier))
        getDBInstance(userContext, templateDbInstance.DBInstanceIdentifier)
    }

    static List getDBInstanceClasses(){
        [SMALL, LARGE, EXTRA_LARGE, DOUBLE_EXTRA_LARGE, QUADRUPLE_EXTRA_LARGE]
    }

    static Collection<String> getLicenseModels() {
        ["general-public-license", "license-included", "bring-your-own-license"]
    }

    void updateDBInstance(UserContext userContext, Integer allocatedStorage, Integer backupRetentionPeriod,
                          String dbInstanceClass, String dbInstanceId, /*String dBParameterGroupName, */
                          Collection<String> dbSecurityGroups, String masterUserPassword, Boolean multiAZ,
                          String preferredBackupWindow, String preferredMaintenanceWindow){
        taskService.runTask(userContext, "Updating DB instance '${dbInstanceId}'", { task ->
            def request = new ModifyDBInstanceRequest()
                .withAllocatedStorage(allocatedStorage)
                .withApplyImmediately(true)
                .withBackupRetentionPeriod(backupRetentionPeriod)
                .withDBInstanceClass(dbInstanceClass)
                .withDBInstanceIdentifier(dbInstanceId)
                //.withDBParameterGroupName(dBParameterGroupName)
                .withDBSecurityGroups(dbSecurityGroups)
                .withMultiAZ(multiAZ)
                .withPreferredBackupWindow(preferredBackupWindow)
                .withPreferredMaintenanceWindow(preferredMaintenanceWindow)
            if (masterUserPassword) { request.setMasterUserPassword(masterUserPassword) }
            DBInstance instance = awsClient.by(userContext.region).modifyDBInstance(request)
            caches.allDBInstances.by(userContext.region).put(instance.getDBInstanceIdentifier(), instance)
        }, Link.to(EntityType.rdsInstance, dbInstanceId))
    }

    // Security

    Collection<DBSecurityGroup> getDBSecurityGroups(UserContext userContext) {
        caches.allDBSecurityGroups.by(userContext.region).list()
    }

    private List<DBSecurityGroup> retrieveDBSecurityGroups(Region region) {
        awsClient.by(region).describeDBSecurityGroups(new DescribeDBSecurityGroupsRequest()).getDBSecurityGroups()
    }

    DBSecurityGroup getDBSecurityGroup(UserContext userContext, String name, From from = From.AWS) {
        if (!name) { return null }
        if (from == From.CACHE) {
            return caches.allDBSecurityGroups.by(userContext.region).get(name)
        }
        DBSecurityGroup group
        try {
            def result = awsClient.by(userContext.region).describeDBSecurityGroups(
                    new DescribeDBSecurityGroupsRequest().withDBSecurityGroupName(name))
            group = Check.lone(result.getDBSecurityGroups(), DBSecurityGroup)
        } catch (AmazonServiceException ignored) {
            group = null
        }
        caches.allDBSecurityGroups.by(userContext.region).put(name, group)
    }

    // mutators

    DBSecurityGroup createDBSecurityGroup(UserContext userContext, String name, String description) {
        taskService.runTask(userContext, "Create DB Security Group '${name}'", { task ->
            awsClient.by(userContext.region).createDBSecurityGroup(
                new CreateDBSecurityGroupRequest().withDBSecurityGroupName(name).withDBSecurityGroupDescription(description))
        }, Link.to(EntityType.dbSecurity, name))
        getDBSecurityGroup(userContext, name)
    }

    void removeDBSecurityGroup(UserContext userContext, String name) {
        taskService.runTask(userContext, "Remove DB Security Group '${name}'", { task ->
            def request = new DeleteDBSecurityGroupRequest().withDBSecurityGroupName(name)
            awsClient.by(userContext.region).deleteDBSecurityGroup(request)  // no result
        }, Link.to(EntityType.dbSecurity, name))
        caches.allDBSecurityGroups.by(userContext.region).remove(name)
    }

    /** Hacky workaround for missing aws ability to modify a security group's description, or rename it. */
    DBSecurityGroup renameDBSecurityGroup(UserContext userContext, DBSecurityGroup group, String newName,
                                          String description) {
        String msg = "Rename DB Security Group '${group.getDBSecurityGroupName()}' to '${newName}'"
        taskService.runTask(userContext, msg, { Task task ->
            removeDBSecurityGroup(userContext, group.getDBSecurityGroupName())
            createDBSecurityGroup(userContext, newName, description)
            group.ipPermissions.each { perm ->
                perm.userIdGroupPairs.each { pair ->
                    authorizeDBSecurityGroupIngress(newName, pair.groupName, perm.ipProtocol, perm.fromPort, perm.toPort)
                }
            }
        }, Link.to(EntityType.dbSecurity, newName))
        getDBSecurityGroup(userContext, newName)
    }

    DBSecurityGroup authorizeDBSecurityGroupIngressForGroup(UserContext userContext, String groupName,
                                                            String ec2SecurityGroupName) {
        String sourceSecurityGroupOwnerId = accounts[0]
        taskService.runTask(userContext, "Authorize DB Security Group Ingress for '${ec2SecurityGroupName}'", { Task task ->
            awsClient.by(userContext.region).authorizeDBSecurityGroupIngress(
                new AuthorizeDBSecurityGroupIngressRequest()
                    .withDBSecurityGroupName(groupName)
                    .withEC2SecurityGroupName(ec2SecurityGroupName)
                    .withEC2SecurityGroupOwnerId(sourceSecurityGroupOwnerId)
            )
        }, Link.to(EntityType.dbSecurity, groupName))
        getDBSecurityGroup(userContext, groupName)
    }

    DBSecurityGroup authorizeDBSecurityGroupIngressForIP(UserContext userContext, String groupName, String cidrip) {
        String msg = "Authorize DB Security Group Ingress to '${groupName}' on ${cidrip}"
        taskService.runTask(userContext, msg, { Task task ->
            AuthorizeDBSecurityGroupIngressRequest request = new AuthorizeDBSecurityGroupIngressRequest().
                    withDBSecurityGroupName(groupName).withCIDRIP(cidrip)
            awsClient.by(userContext.region).authorizeDBSecurityGroupIngress(request)
        }, Link.to(EntityType.dbSecurity, groupName))
        getDBSecurityGroup(userContext, groupName)
    }

    DBSecurityGroup revokeDBSecurityGroupIngressForIP(UserContext userContext, String groupName, String cidrip) {
        taskService.runTask(userContext, "Revoke DB Security Group Ingress to ${groupName} on ${cidrip}", { Task task ->
            awsClient.by(userContext.region).revokeDBSecurityGroupIngress(
                new RevokeDBSecurityGroupIngressRequest()
                    .withDBSecurityGroupName(groupName)
                    .withCIDRIP(cidrip)
            )
        }, Link.to(EntityType.dbSecurity, groupName))
        getDBSecurityGroup(userContext, groupName)
    }

    DBSecurityGroup revokeDBSecurityGroupIngressForGroup(UserContext userContext, String groupName,
                                                         String ec2SecurityGroupName) {
        String sourceSecurityGroupOwnerId = accounts[0]
        taskService.runTask(userContext, "Revoke DB Security Group Ingress for ${ec2SecurityGroupName}", { task ->
            awsClient.by(userContext.region).revokeDBSecurityGroupIngress(
                new RevokeDBSecurityGroupIngressRequest()
                    .withDBSecurityGroupName(groupName)
                    .withEC2SecurityGroupName(ec2SecurityGroupName)
                    .withEC2SecurityGroupOwnerId(sourceSecurityGroupOwnerId)
            )
        }, Link.to(EntityType.dbSecurity, groupName))
        getDBSecurityGroup(userContext, groupName)
    }

    // Snapshots

    Collection<DBSnapshot> getDBSnapshots(UserContext userContext) {
        caches.allDBSnapshots.by(userContext.region).list()
    }

    private List<DBSnapshot> retrieveDBSnapshots(Region region) {
        awsClient.by(region).describeDBSnapshots(new DescribeDBSnapshotsRequest()).getDBSnapshots()
    }

    DBSnapshot deleteDBSnapshot(UserContext userContext, String dbSnapshotId) {
        String msg = "Deleting DB snapshot '${dbSnapshotId}'"
        taskService.runTask(userContext, msg, { Task task ->
            DeleteDBSnapshotRequest request = new DeleteDBSnapshotRequest().withDBSnapshotIdentifier(dbSnapshotId)
            awsClient.by(userContext.region).deleteDBSnapshot(request)
        }, Link.to(EntityType.dbSnapshot, dbSnapshotId))
        getDBSnapshot(userContext, dbSnapshotId)
    }

    DBSnapshot createDBSnapshot(UserContext userContext, String dbInstanceId, String dbSnapshotId) {
        String msg = "Restoring DB instance '${dbInstanceId}' from DB snapshot '${dbSnapshotId}'"
        DBSnapshot snapshot = taskService.runTask(userContext, msg, { Task task ->
            CreateDBSnapshotRequest request = new CreateDBSnapshotRequest().
                    withDBSnapshotIdentifier(dbSnapshotId).withDBInstanceIdentifier(dbInstanceId)
            awsClient.by(userContext.region).createDBSnapshot(request)
        }, Link.to(EntityType.dbSnapshot, dbSnapshotId)) as DBSnapshot
        getDBSnapshot(userContext, snapshot?.getDBSnapshotIdentifier())
    }

    DBSnapshot getDBSnapshot(UserContext userContext, String dbSnapshotId, From from = From.AWS) {
        if (!dbSnapshotId) { return null }
        if (from == From.CACHE) {
            return caches.allDBSnapshots.by(userContext.region).get(dbSnapshotId)
        }
        DescribeDBSnapshotsRequest request = new DescribeDBSnapshotsRequest().withDBSnapshotIdentifier(dbSnapshotId)
        DBSnapshot snapshot = null
        try {
            DescribeDBSnapshotsResult result = awsClient.by(userContext.region).describeDBSnapshots(request)
            snapshot = Check.loneOrNone(result.getDBSnapshots(), DBSnapshot)
        } catch (AmazonServiceException ase) {
            // Check for the specific code the RDS service uses when an object does not exist.
            if (ase.errorCode != 'DBSnapshotNotFound') {
                throw ase
            }
        }
        caches.allDBSnapshots.by(userContext.region).put(dbSnapshotId, snapshot)
        snapshot
    }

    List<DBSnapshot> getDBSnapshotsForInstance(UserContext userContext, String dbInstanceId) {
        DescribeDBSnapshotsRequest request = new DescribeDBSnapshotsRequest().withDBInstanceIdentifier(dbInstanceId)
        awsClient.by(userContext.region).describeDBSnapshots(request).getDBSnapshots()
    }

    DBInstance restoreFromSnapshot(UserContext userContext, String dbSnapshotId, String dbInstanceId) {
        String msg = "Restoring DB instance '${dbInstanceId}' from DB snapshot '${dbSnapshotId}'"
        taskService.runTask(userContext, msg, { Task task ->
            DBInstance instance = awsClient.by(userContext.region).restoreDBInstanceFromDBSnapshot(
                new RestoreDBInstanceFromDBSnapshotRequest()
                    .withDBSnapshotIdentifier(dbSnapshotId)
                    .withDBInstanceIdentifier(dbInstanceId)
            )
            caches.allDBInstances.by(userContext.region).put(instance.getDBInstanceIdentifier(), instance)
        }, Link.to(EntityType.rdsInstance, dbInstanceId))
        getDBInstance(userContext, dbInstanceId)
    }

    DBInstance restoreFromSnapshot(UserContext userContext, String dbSnapshotId, String dbInstanceId, String zone,
                                   String dbClass, boolean multiAZ, Integer port) {
        String msg = "Restoring DB instance '${dbInstanceId}' from DB snapshot '${dbSnapshotId}'"
        taskService.runTask(userContext, msg, { Task task ->
            DBInstance instance = awsClient.by(userContext.region).restoreDBInstanceFromDBSnapshot(
                new RestoreDBInstanceFromDBSnapshotRequest()
                    .withDBSnapshotIdentifier(dbSnapshotId)
                    .withDBInstanceIdentifier(dbInstanceId)
                    .withAvailabilityZone(zone)
                    .withDBInstanceClass(dbClass)
                    .withMultiAZ(multiAZ)
                    .withPort(port)
            )
            caches.allDBInstances.by(userContext.region).put(instance.getDBInstanceIdentifier(), instance)
        }, Link.to(EntityType.rdsInstance, dbInstanceId))
        getDBInstance(userContext, dbInstanceId)
    }

}
