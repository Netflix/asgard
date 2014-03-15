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

import com.amazonaws.services.simpleworkflow.flow.WorkflowClientExternal
import com.amazonaws.services.simpleworkflow.model.ChildPolicy
import com.amazonaws.services.simpleworkflow.model.WorkflowExecutionInfo
import com.netflix.asgard.deployment.DeploymentWorkflow
import com.netflix.asgard.deployment.DeploymentWorkflowOptions
import com.netflix.asgard.model.AutoScalingGroupBeanOptions
import com.netflix.asgard.model.Deployment
import com.netflix.asgard.model.LaunchConfigurationBeanOptions
import com.netflix.asgard.model.SwfWorkflow
import com.netflix.asgard.model.WorkflowExecutionBeanOptions

/**
 * Manages workflows that automate the deployment of a new ASG to an existing controller.
 */
class DeploymentService {

    def applicationService
    def awsEc2Service
    def awsSimpleWorkflowService
    def flowService

    /**
     * Get all running deployments.
     */
    List<Deployment> getRunningDeployments() {
        filterAndSortWorkflowExecutionInfos(awsSimpleWorkflowService.openWorkflowExecutions).
                collect { new WorkflowExecutionBeanOptions(it).asDeployment() }
    }

    /**
     * Get last 100 finished deployments.
     */
    List<Deployment> getFinishedDeployments() {
        filterAndSortWorkflowExecutionInfos(awsSimpleWorkflowService.closedWorkflowExecutions).take(100).
                collect { new WorkflowExecutionBeanOptions(it).asDeployment() }
    }

    private List<WorkflowExecutionInfo> filterAndSortWorkflowExecutionInfos(
            Collection<WorkflowExecutionInfo> workflowExecutionInfos) {
        workflowExecutionInfos.findAll { it.workflowType.name == 'DeploymentWorkflow.deploy' }.
                sort { it.startTimestamp }.reverse()
    }

    /**
     * Looks up a deployment by its ID.
     *
     * @param id for deployment
     * @return deployment or null if no task was found
     */
    Deployment getDeploymentById(String id) {
        if (!id) { return null }
        try {
            new Retriable<Deployment>(
                    work: {
                        Deployment deployment = awsSimpleWorkflowService.
                                getWorkflowExecutionInfoByTaskId(id)?.asDeployment()
                        if (!deployment) { throw new IllegalArgumentException("There is no deployment with id ${id}.") }
                        deployment
                    },
                    firstDelayMillis: 300
            ).performWithRetries()
        } catch (CollectedExceptions ignore) {
            return null
        }
    }

    /**
     * Cancels a deployment. It gets terminated immediately with no clean up.
     *
     * @param userContext who, where, why
     * @param deployment to be canceled
     */
    void cancelDeployment(UserContext userContext, Deployment deployment) {
        String cancelledByMessage = "Cancelled by ${userContext.username ?: 'user'}@${userContext.clientHostName}"
        WorkflowClientExternal client = flowService.getWorkflowClient(deployment.workflowExecution)
        client.terminateWorkflowExecution(cancelledByMessage, deployment.toString(), ChildPolicy.TERMINATE)
    }

    /**
     * Starts the deployment of a new Auto Scaling Group in an existing cluster.
     *
     * @param userContext who, where, why
     * @param clusterName the name of the cluster where the next ASG should be created
     * @param deploymentOptions dictate what the deployment will do
     * @param lcOverrides specify changes to the template launch configuration
     * @param asgOverrides specify changes to the template auto scaling group
     * @return the unique ID of the workflow execution that is starting
     */
    public String startDeployment(UserContext userContext, String clusterName,
            DeploymentWorkflowOptions deploymentOptions, LaunchConfigurationBeanOptions lcOverrides,
            AutoScalingGroupBeanOptions asgOverrides) {

        SwfWorkflow<DeploymentWorkflow> workflow = flowService.getNewWorkflowClient(userContext,
                DeploymentWorkflow, new Link(EntityType.cluster, clusterName))
        workflow.client.deploy(userContext, deploymentOptions, lcOverrides, asgOverrides)
        workflow.tags.id
    }

    /**
     * Checks whether the specified cluster has at least one open workflow execution.
     *
     * @param clusterName the name of the cluster in question
     * @return deployment that is currently operating on the cluster, or null if none found
     */
    Deployment getRunningDeploymentForCluster(String clusterName) {
        Link link = new Link(type: EntityType.cluster, id: clusterName)
        WorkflowExecutionInfo executionInfo = awsSimpleWorkflowService.getOpenWorkflowExecutionForObjectLink(link)
        if (executionInfo == null) { return null }
        new WorkflowExecutionBeanOptions(executionInfo).asDeployment()
    }
}
