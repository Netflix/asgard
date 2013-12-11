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
package com.netflix.asgard

import com.amazonaws.services.simpleworkflow.flow.StartWorkflowOptions
import com.amazonaws.services.simpleworkflow.flow.generic.GenericWorkflowClientExternal
import com.amazonaws.services.simpleworkflow.model.WorkflowExecution
import com.amazonaws.services.simpleworkflow.model.WorkflowExecutionInfo
import com.amazonaws.services.simpleworkflow.model.WorkflowType
import com.netflix.asgard.deployment.DeploymentWorkflow
import com.netflix.asgard.deployment.DeploymentWorkflowDescriptionTemplate
import com.netflix.asgard.deployment.DeploymentWorkflowOptions
import com.netflix.asgard.model.AutoScalingGroupBeanOptions
import com.netflix.asgard.model.LaunchConfigurationBeanOptions
import com.netflix.asgard.model.SwfWorkflowTags
import com.netflix.glisten.InterfaceBasedWorkflowClient
import spock.lang.Specification

@SuppressWarnings("GroovyAssignabilityCheck")
class PushServiceUnitSpec extends Specification {

    PushService pushService
    FlowService flowService
    AwsSimpleWorkflowService awsSimpleWorkflowService

    void setup() {
        flowService = Mock(FlowService)
        awsSimpleWorkflowService = Mock(AwsSimpleWorkflowService)
        pushService = new PushService(flowService: flowService, awsSimpleWorkflowService: awsSimpleWorkflowService)
    }

    def 'starting a deployment should make workflow client, call getter to update cache, and return task ID'() {
        UserContext userContext = UserContext.auto(Region.US_EAST_1)
        DeploymentWorkflowOptions deployOpts = new DeploymentWorkflowOptions()
        LaunchConfigurationBeanOptions lcOpts = new LaunchConfigurationBeanOptions()
        AutoScalingGroupBeanOptions asgOpts = new AutoScalingGroupBeanOptions()

        when:
        String taskId = pushService.startDeployment(userContext, 'Calysteral', deployOpts, lcOpts, asgOpts)

        then:
        taskId == '07700900461'
        1 * flowService.getNewWorkflowClient(userContext, DeploymentWorkflow,
                new Link(EntityType.cluster, 'Calysteral')) >> {
            WorkflowExecution workflowExecution = new WorkflowExecution(workflowId: '1716231163')
            GenericWorkflowClientExternal genericClient = Mock(GenericWorkflowClientExternal) {
                startWorkflow(_) >> workflowExecution
            }
            new InterfaceBasedWorkflowClient(DeploymentWorkflow, new DeploymentWorkflowDescriptionTemplate(),
                    workflowExecution, new WorkflowType(), new StartWorkflowOptions(), null,
                    genericClient, new SwfWorkflowTags(id: '07700900461'))
        }
        1 * awsSimpleWorkflowService.getWorkflowExecutionInfoByWorkflowExecution(
                new WorkflowExecution(workflowId: '1716231163'))
        0 * _
    }

    def 'should indicate that a workflow execution is in progress for the specified cluster'() {

        Link link = Link.to(EntityType.cluster, 'helloworld-example')

        when:
        WorkflowExecutionInfo info = pushService.getRunningDeploymentForCluster('helloworld-example')

        then:
        info == new WorkflowExecutionInfo()
        1 * awsSimpleWorkflowService.getOpenWorkflowExecutionForObjectLink(link) >> new WorkflowExecutionInfo()
    }

    def 'should indicate that a workflow execution is not in progress for the specified cluster'() {

        Link link = Link.to(EntityType.cluster, 'helloworld-example')

        when:
        WorkflowExecutionInfo info = pushService.getRunningDeploymentForCluster('helloworld-example')

        then:
        info == null
        1 * awsSimpleWorkflowService.getOpenWorkflowExecutionForObjectLink(link) >> null
    }
}
