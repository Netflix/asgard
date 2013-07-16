package com.netflix.asgard

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow
import com.amazonaws.services.simpleworkflow.flow.ActivityWorker
import com.amazonaws.services.simpleworkflow.flow.DataConverter
import com.amazonaws.services.simpleworkflow.flow.ManualActivityCompletionClient
import com.amazonaws.services.simpleworkflow.flow.ManualActivityCompletionClientFactory
import com.amazonaws.services.simpleworkflow.flow.ManualActivityCompletionClientFactoryImpl
import com.amazonaws.services.simpleworkflow.flow.StartWorkflowOptions
import com.amazonaws.services.simpleworkflow.flow.WorkflowClientExternal
import com.amazonaws.services.simpleworkflow.flow.WorkflowClientExternalBase
import com.amazonaws.services.simpleworkflow.flow.WorkflowClientFactoryExternalBase
import com.amazonaws.services.simpleworkflow.flow.WorkflowWorker
import com.amazonaws.services.simpleworkflow.flow.generic.GenericWorkflowClientExternal
import com.amazonaws.services.simpleworkflow.model.WorkflowExecution
import com.amazonaws.services.simpleworkflow.model.WorkflowType
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import com.netflix.asgard.flow.InterfaceBasedWorkflowClient
import com.netflix.asgard.flow.WorkflowDescriptionTemplate
import com.netflix.asgard.flow.WorkflowMetaAttributes
import com.netflix.asgard.model.SwfWorkflowTags
import org.springframework.beans.factory.InitializingBean

/**
 * This service handles AWS SWF concerns at the level of the Flow framework. It primarily controls pollers (decision
 * and activity) as well as giving the means to execute or inspect workflows.
 */
class FlowService implements InitializingBean {

    AwsClientService awsClientService
    AmazonSimpleWorkflow simpleWorkflow
    ConfigService configService

    WorkflowWorker workflowWorker
    ActivityWorker activityWorker

    final ImmutableSet<Class<?>> workflowImplementationTypes = ImmutableSet.of()
    final ImmutableMap<Class<?>, WorkflowDescriptionTemplate> workflowToDescriptionTemplate = ImmutableMap.
            copyOf([:] as Map)
    final ImmutableSet<Object> activityImplementations = ImmutableSet.of()

    /* The AWS SWF domain that will be used in this service for polling and scheduling workflows */
    private String domain

    void afterPropertiesSet() {
        domain = configService.simpleWorkflowDomain
        simpleWorkflow = awsClientService.create(AmazonSimpleWorkflow)
        activityImplementations.each { Spring.autowire(it) }
        workflowWorker = new WorkflowWorker(simpleWorkflow, domain, domain)
        workflowWorker.setWorkflowImplementationTypes(workflowImplementationTypes)
        workflowWorker.start()
        log.info('Workflow Host Service Started...')
        activityWorker = activityWorker ?: new ActivityWorker(simpleWorkflow, domain, domain)
        activityWorker.addActivitiesImplementations(activityImplementations)
        activityWorker.start()
        log.info("Activity Worker Started for Task List: ${activityWorker.getTaskListToPoll()}")
    }

    /**
     * Creates a workflow client that allows you to schedule a workflow by calling methods on the workflow interface.
     *
     * @param userContext who, where, why
     * @param workflow the interface for this workflow
     * @param link to the relevant object
     * @return the workflow client
     */
    public <T> InterfaceBasedWorkflowClient<T> getNewWorkflowClient(UserContext userContext,
            Class<T> workflow, Link link = null) {
        WorkflowDescriptionTemplate workflowDescriptionTemplate = workflowToDescriptionTemplate[workflow]
        WorkflowType workflowType = new WorkflowMetaAttributes(workflow).workflowType
        def factory = new WorkflowClientFactoryExternalBase<InterfaceBasedWorkflowClient>(simpleWorkflow, domain) {
            @Override
            protected InterfaceBasedWorkflowClient createClientInstance(WorkflowExecution workflowExecution,
                    StartWorkflowOptions options, DataConverter dataConverter,
                    GenericWorkflowClientExternal genericClient) {
                new InterfaceBasedWorkflowClient(workflow, workflowDescriptionTemplate, workflowExecution, workflowType,
                        options, dataConverter, genericClient, new SwfWorkflowTags())
            }
        }
        SwfWorkflowTags workflowTags = new SwfWorkflowTags()
        workflowTags.user = userContext
        workflowTags.link = link
        factory.startWorkflowOptions = new StartWorkflowOptions(tagList: workflowTags.constructTags())
        factory.client
    }

    /**
     * Gets an existing workflow. Useful for canceling, terminating or just looking at attributes of the workflow.
     *
     * @param workflowIdentification ids for a specific existing workflow execution
     * @return the workflow client
     */
    public WorkflowClientExternal getWorkflowClient(WorkflowExecution workflowIdentification) {
        def factory = new WorkflowClientFactoryExternalBase<WorkflowClientExternal>(simpleWorkflow, domain) {
            @Override
            protected WorkflowClientExternal createClientInstance(WorkflowExecution workflowExecution,
                    StartWorkflowOptions options, DataConverter dataConverter,
                    GenericWorkflowClientExternal genericClient) {
                new WorkflowClientExternalBase(workflowExecution, null, options, dataConverter, genericClient) {}
            }
        }
        factory.getClient(workflowIdentification)
    }

    /**
     * Returns a client for the manual activity. Manual activities do not complete automatically. With a
     * ManualActivityCompletionClient you can revisit an executing manual activity and complete it as you see fit.
     *
     * @param taskToken a token generated by the manual activity
     * @return the workflow client
     */
    ManualActivityCompletionClient getManualActivityCompletionClient(String taskToken) {
        ManualActivityCompletionClientFactory manualCompletionClientFactory =
            new ManualActivityCompletionClientFactoryImpl(simpleWorkflow)
        manualCompletionClientFactory.getClient(taskToken)
    }

}
