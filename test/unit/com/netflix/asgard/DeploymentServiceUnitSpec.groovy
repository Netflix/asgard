/*
 * Copyright 2014 Netflix, Inc.
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

import com.amazonaws.services.simpledb.model.Attribute
import com.amazonaws.services.simpledb.model.Item
import com.amazonaws.services.simpleworkflow.flow.StartWorkflowOptions
import com.amazonaws.services.simpleworkflow.flow.WorkflowClientExternal
import com.amazonaws.services.simpleworkflow.flow.generic.GenericWorkflowClientExternal
import com.amazonaws.services.simpleworkflow.model.ChildPolicy
import com.amazonaws.services.simpleworkflow.model.WorkflowExecution
import com.amazonaws.services.simpleworkflow.model.WorkflowExecutionInfo
import com.amazonaws.services.simpleworkflow.model.WorkflowType
import com.netflix.asgard.deployment.DeploymentWorkflow
import com.netflix.asgard.deployment.DeploymentWorkflowDescriptionTemplate
import com.netflix.asgard.deployment.DeploymentWorkflowOptions
import com.netflix.asgard.model.AutoScalingGroupBeanOptions
import com.netflix.asgard.model.Deployment
import com.netflix.asgard.model.LaunchConfigurationBeanOptions
import com.netflix.asgard.model.SwfWorkflow
import com.netflix.asgard.model.SwfWorkflowTags
import com.netflix.asgard.model.WorkflowExecutionBeanOptions
import com.netflix.glisten.InterfaceBasedWorkflowClient
import spock.lang.Specification

class DeploymentServiceUnitSpec extends Specification {

    AwsSimpleWorkflowService awsSimpleWorkflowService = Mock(AwsSimpleWorkflowService)
    FlowService flowService = Mock(FlowService)
    DeploymentService deploymentService = new DeploymentService(awsSimpleWorkflowService: awsSimpleWorkflowService,
            flowService: flowService, awsSimpleDbService: Mock(AwsSimpleDbService))

    Closure<WorkflowExecutionInfo> newWorkflowExecutionInfo = { int sequenceNumber ->
        new WorkflowExecutionInfo(tagList: new SwfWorkflowTags(id: sequenceNumber as String).constructTags(),
                workflowType: new WorkflowType(name: 'DeploymentWorkflow.deploy'),
                startTimestamp: new Date(sequenceNumber))
    }

    Closure<Deployment> newDeployment = { int sequenceNumber ->
        new Deployment(sequenceNumber as String, null, null, null, null, null, new Date(sequenceNumber),
                new Date(sequenceNumber), 'running', [], [])
    }

    def setup() {
        Retriable.mixin(NoDelayRetriableMixin)
    }

	void 'should get running deployments'() {
        when:
        List<Deployment> deployments = deploymentService.runningDeployments

        then:
        deployments == [3, 2, 1].collect(newDeployment)

        and:
        1 * deploymentService.awsSimpleWorkflowService.getOpenWorkflowExecutions() >> [1, 2, 3].
                collect(newWorkflowExecutionInfo)
	}

    void 'should get finished deployments'() {
        when:
        List<Deployment> deployments = deploymentService.finishedDeployments

        then:
        deployments == [3, 2, 1].collect(newDeployment)

        and:
        1 * deploymentService.awsSimpleWorkflowService.getClosedWorkflowExecutions() >> [1, 2, 3].
                collect(newWorkflowExecutionInfo)
    }

    void 'should get last 100 finished deployments'() {
        when:
        List<Deployment> deployments = deploymentService.finishedDeployments

        then:
        deployments == (107..8).collect(newDeployment)

        and:
        1 * deploymentService.awsSimpleWorkflowService.getClosedWorkflowExecutions() >> (1..107).
                collect(newWorkflowExecutionInfo)
    }

    void 'should get deployment by id'() {
        when:
        Deployment deployment = deploymentService.getDeploymentById('1')

        then:
        deployment == newDeployment(1)

        and:
        1 * deploymentService.awsSimpleWorkflowService.getWorkflowExecutionInfoByTaskId('1') >>
                new WorkflowExecutionBeanOptions(newWorkflowExecutionInfo(1))
        1 * deploymentService.awsSimpleDbService.selectOne('ASGARD_SWF_TOKEN_FOR_DEPLOYMENT', '1') >> new Item(
                name: '1', attributes: [new Attribute(name: 'token', value: '1')])
        0 * _

    }

    void 'should not get deployment if it does not exist'() {
        when:
        Deployment deployment = deploymentService.getDeploymentById('1')

        then:
        deployment == null

        and:
        3 * deploymentService.awsSimpleWorkflowService.getWorkflowExecutionInfoByTaskId('1')
        0 * _
    }

    void 'should not get deployment without an id'() {
        expect:
        null == deploymentService.getDeploymentById(null)
    }

    void 'should cancel deployment'() {
        WorkflowExecution workflowExecution = new WorkflowExecution(workflowId: '1')
        deploymentService.flowService = Mock(FlowService)
        WorkflowClientExternal mockWorkflowClientExternal = Mock(WorkflowClientExternal)

        when:
        deploymentService.cancelDeployment(new UserContext(username: 'akiedis', clientHostName: 'rhcp.com'),
                new Deployment(null, null, null, workflowExecution))

        then:
        1 * deploymentService.flowService.getWorkflowClient(workflowExecution) >> mockWorkflowClientExternal
        1 * mockWorkflowClientExternal.terminateWorkflowExecution('Cancelled by akiedis@rhcp.com', _,
                ChildPolicy.TERMINATE)
    }

    def 'starting a deployment should make workflow client, call getter to update cache, and return task ID'() {
        UserContext userContext = UserContext.auto(Region.US_EAST_1)
        DeploymentWorkflowOptions deployOpts = new DeploymentWorkflowOptions(clusterName: 'Calysteral')
        LaunchConfigurationBeanOptions lcOpts = new LaunchConfigurationBeanOptions()
        AutoScalingGroupBeanOptions asgOpts = new AutoScalingGroupBeanOptions()

        when:
        String taskId = deploymentService.startDeployment(userContext, deployOpts, lcOpts, asgOpts)

        then:
        taskId == '07700900461'
        1 * flowService.getNewWorkflowClient(userContext, DeploymentWorkflow,
                new Link(EntityType.cluster, 'Calysteral')) >> {
            WorkflowExecution workflowExecution = new WorkflowExecution(workflowId: '1716231163')
            GenericWorkflowClientExternal genericClient = Mock(GenericWorkflowClientExternal) {
                startWorkflow(_) >> workflowExecution
            }
            new SwfWorkflow(new InterfaceBasedWorkflowClient(DeploymentWorkflow,
                    new DeploymentWorkflowDescriptionTemplate(),
                    workflowExecution, new WorkflowType(), new StartWorkflowOptions(), null,
                    genericClient, new SwfWorkflowTags(id: '07700900461')))
        }
        0 * _
    }

    def 'should indicate that a workflow execution is in progress for the specified cluster'() {
        Link link = Link.to(EntityType.cluster, 'helloworld-example')

        when:
        Deployment deployment = deploymentService.getRunningDeploymentForCluster(Region.US_WEST_1, 'helloworld-example')

        then:
        deployment == new Deployment('123', null, null, null, null, null, null, null, 'running', [], [])
        1 * awsSimpleWorkflowService.getOpenWorkflowExecutionForObjectLink(Region.US_WEST_1, link) >>
                new WorkflowExecutionInfo(tagList: new SwfWorkflowTags(id: '123').constructTags())
    }

    def 'should indicate that a workflow execution is not in progress for the specified cluster'() {
        Link link = Link.to(EntityType.cluster, 'helloworld-example')

        when:
        Deployment deployment = deploymentService.getRunningDeploymentForCluster(Region.US_WEST_1, 'helloworld-example')

        then:
        deployment == null
        1 * awsSimpleWorkflowService.getOpenWorkflowExecutionForObjectLink(Region.US_WEST_1, link) >> null
    }
}
