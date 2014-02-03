package com.netflix.asgard
import com.amazonaws.services.simpleworkflow.flow.WorkflowClientExternal
import com.amazonaws.services.simpleworkflow.model.ChildPolicy
import com.amazonaws.services.simpleworkflow.model.WorkflowExecution
import com.amazonaws.services.simpleworkflow.model.WorkflowExecutionInfo
import com.netflix.asgard.deployment.DeploymentWorkflow
import com.netflix.asgard.deployment.DeploymentWorkflowOptions
import com.netflix.asgard.model.AutoScalingGroupBeanOptions
import com.netflix.asgard.model.Deployment
import com.netflix.asgard.model.LaunchConfigurationBeanOptions
import com.netflix.asgard.model.SwfWorkflowTags
import com.netflix.asgard.model.WorkflowExecutionBeanOptions
import com.netflix.glisten.InterfaceBasedWorkflowClient
import com.netflix.glisten.WorkflowExecutionCreationCallback

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
     * The callback for Glisten to call after creating a WorkflowExecution, in order to update Asgard's
     * WorkflowExecution caches.
     */
    private WorkflowExecutionCreationCallback updateCaches = new WorkflowExecutionCreationCallback() {
        @Override
        void call(WorkflowExecution workflowExecution) {
            awsSimpleWorkflowService.getWorkflowExecutionInfoByWorkflowExecution(workflowExecution) // Update caches
        }
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

        InterfaceBasedWorkflowClient<DeploymentWorkflow> client = flowService.getNewWorkflowClient(userContext,
                DeploymentWorkflow, new Link(EntityType.cluster, clusterName))
        client.asWorkflow(updateCaches).deploy(userContext, deploymentOptions, lcOverrides, asgOverrides)
        SwfWorkflowTags tags = (SwfWorkflowTags) client.workflowTags
        tags.id
    }

    /**
     * Checks whether the specified cluster has at least one open workflow execution.
     *
     * @param clusterName the name of the cluster in question
     * @return info about the workflow execution that is currently operating on the cluster, or null if none found
     */
    WorkflowExecutionInfo getRunningDeploymentForCluster(String clusterName) {
        Link link = new Link(type: EntityType.cluster, id: clusterName)
        awsSimpleWorkflowService.getOpenWorkflowExecutionForObjectLink(link)
    }
}
