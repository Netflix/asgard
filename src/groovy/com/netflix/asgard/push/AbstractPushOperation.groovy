package com.netflix.asgard.push

import com.amazonaws.services.autoscaling.model.AutoScalingGroup
import com.netflix.asgard.From
import com.netflix.asgard.Task
import com.netflix.asgard.UserContext

abstract class AbstractPushOperation {

    def applicationService
    def awsAutoScalingService
    def awsLoadBalancerService
    def taskService
    Task task

    protected final AutoScalingGroup checkGroupStillExists(UserContext userContext, String autoScalingGroupName,
            From from=From.AWS) {
        AutoScalingGroup group = awsAutoScalingService.getAutoScalingGroup(userContext, autoScalingGroupName, from)
        if (!group) {
            abortBecauseGroupDisappeared(autoScalingGroupName)
        }
        group
    }

    private void abortBecauseGroupDisappeared(String autoScalingGroupName) {
        throw new PushException("Group '${autoScalingGroupName}' can no longer be found.")
    }

}
