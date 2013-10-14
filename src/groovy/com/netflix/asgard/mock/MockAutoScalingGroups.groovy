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

class MockAutoScalingGroups {

    @SuppressWarnings('LineLength')
    static final String DATA = '''
[
  {
    "VPCZoneIdentifier": null,
    "autoScalingGroupARN": "arn:aws:autoscaling:us-east-1:179000000000:autoScalingGroup:ca6d1d52-4eed-490a-965e-dedb18db4d08:autoScalingGroupName/akms-v002",
    "autoScalingGroupName": "akms-v002",
    "availabilityZones":
    [
      "us-east-1c",
      "us-east-1a",
      "us-east-1d"
    ],
    "class": "com.amazonaws.services.autoscaling.model.AutoScalingGroup",
    "createdTime": "2011-09-30T17:57:27Z",
    "defaultCooldown": 10,
    "desiredCapacity": 2,
    "enabledMetrics":
    [
    ],
    "healthCheckGracePeriod": 600,
    "healthCheckType": "EC2",
    "instances":
    [
      {
        "availabilityZone": "us-east-1c",
        "class": "com.amazonaws.services.autoscaling.model.Instance",
        "healthStatus": "Healthy",
        "instanceId": "i-a27867c2",
        "launchConfigurationName": "akms-v002-20111018141009",
        "lifecycleState": "InService"
      },
      {
        "availabilityZone": "us-east-1a",
        "class": "com.amazonaws.services.autoscaling.model.Instance",
        "healthStatus": "Healthy",
        "instanceId": "i-8fd134ec",
        "launchConfigurationName": "akms-v002-20111018141009",
        "lifecycleState": "InService"
      }
    ],
    "launchConfigurationName": "akms-v002-20111018141009",
    "loadBalancerNames":
    [
    ],
    "maxSize": 2,
    "minSize": 2,
    "placementGroup": null,
    "status": null,
    "suspendedProcesses":
    [
    ]
  },
  {
    "VPCZoneIdentifier": null,
    "autoScalingGroupARN": "arn:aws:autoscaling:us-east-1:179000000000:autoScalingGroup:b1289d0d-229c-4a4a-bfb7-3b5a6f884f5a:autoScalingGroupName/helloworld-example-v015",
    "autoScalingGroupName": "helloworld-example-v015",
    "availabilityZones":
    [
      "us-east-1a"
    ],
    "class": "com.amazonaws.services.autoscaling.model.AutoScalingGroup",
    "createdTime": "2011-10-07T22:02:06Z",
    "defaultCooldown": 11,
    "desiredCapacity": 3,
    "enabledMetrics":
    [
    ],
    "healthCheckGracePeriod": 600,
    "healthCheckType": "EC2",
    "instances":
    [
      {
        "availabilityZone": "us-east-1a",
        "class": "com.amazonaws.services.autoscaling.model.Instance",
        "healthStatus": "Healthy",
        "instanceId": "i-8ee4eeee",
        "launchConfigurationName": "helloworld-example-v015-20111014165240",
        "lifecycleState": "InService"
      },
      {
        "availabilityZone": "us-east-1a",
        "class": "com.amazonaws.services.autoscaling.model.Instance",
        "healthStatus": "Healthy",
        "instanceId": "i-6ef9f30e",
        "launchConfigurationName": "helloworld-example-v015-20111014165240",
        "lifecycleState": "InService"
      },
      {
        "availabilityZone": "us-east-1a",
        "class": "com.amazonaws.services.autoscaling.model.Instance",
        "healthStatus": "Healthy",
        "instanceId": "i-95fe1df6",
        "launchConfigurationName": "helloworld-example-v015-20111014165240",
        "lifecycleState": "InService"
      }
    ],
    "launchConfigurationName": "helloworld-example-v015-20111014165240",
    "loadBalancerNames":
    [
      "helloworld--frontend"
    ],
    "maxSize": 3,
    "minSize": 3,
    "placementGroup": null,
    "status": null,
    "suspendedProcesses":
    [
      {
        "class": "com.amazonaws.services.autoscaling.model.SuspendedProcess",
        "processName": "AZRebalance",
        "suspensionReason": "User suspended at 2011-10-07T22:02:06Z"
      }
    ]
  },
  {
    "VPCZoneIdentifier": null,
    "autoScalingGroupARN": "arn:aws:autoscaling:us-east-1:179000000000:autoScalingGroup:e691cc9b-2c81-4c5a-8e61-be1c4bfbd065:autoScalingGroupName/ntsuiboot-v000",
    "autoScalingGroupName": "ntsuiboot-v000",
    "availabilityZones":
    [
      "us-east-1c",
      "us-east-1a",
      "us-east-1d"
    ],
    "class": "com.amazonaws.services.autoscaling.model.AutoScalingGroup",
    "createdTime": "2011-07-21T17:10:41Z",
    "defaultCooldown": 10,
    "desiredCapacity": 1,
    "enabledMetrics":
    [
    ],
    "healthCheckGracePeriod": 600,
    "healthCheckType": "EC2",
    "instances":
    [
      {
        "availabilityZone": "us-east-1d",
        "class": "com.amazonaws.services.autoscaling.model.Instance",
        "healthStatus": "Healthy",
        "instanceId": "i-67b86a06",
        "launchConfigurationName": "ntsuiboot-v000-20110721101039",
        "lifecycleState": "InService"
      }
    ],
    "launchConfigurationName": "ntsuiboot-v000-20110721101039",
    "loadBalancerNames":
    [
      "ntsuiboot--frontend"
    ],
    "maxSize": 1,
    "minSize": 1,
    "placementGroup": null,
    "status": null,
    "suspendedProcesses":
    [
      {
        "class": "com.amazonaws.services.autoscaling.model.SuspendedProcess",
        "processName": "Launch",
        "suspensionReason": "User suspended at 2011-10-18T22:25:03Z"
      }
    ]
  },
  {
    "VPCZoneIdentifier": null,
    "autoScalingGroupARN": "arn:aws:autoscaling:us-east-1:179000000000:autoScalingGroup:d0cb3cdb-fe4f-4b0f-9d40-17e291a79f89:autoScalingGroupName/ntsuiboot-v001",
    "autoScalingGroupName": "ntsuiboot-v001",
    "availabilityZones":
    [
      "us-east-1c",
      "us-east-1a",
      "us-east-1d"
    ],
    "class": "com.amazonaws.services.autoscaling.model.AutoScalingGroup",
    "createdTime": "2011-10-18T19:45:28Z",
    "defaultCooldown": 10,
    "desiredCapacity": 1,
    "enabledMetrics":
    [
    ],
    "healthCheckGracePeriod": 600,
    "healthCheckType": "EC2",
    "instances":
    [
      {
        "availabilityZone": "us-east-1d",
        "class": "com.amazonaws.services.autoscaling.model.Instance",
        "healthStatus": "Healthy",
        "instanceId": "i-f26f7a92",
        "launchConfigurationName": "ntsuiboot-v001-20111018124526",
        "lifecycleState": "InService"
      }
    ],
    "launchConfigurationName": "ntsuiboot-v001-20111018124526",
    "loadBalancerNames":
    [
      "ntsuiboot--frontend"
    ],
    "maxSize": 1,
    "minSize": 1,
    "placementGroup": null,
    "status": null,
    "suspendedProcesses":
    [
    ]
  }
]
'''
}
