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

class MockInstances {

    // http://localhostL8080/us-east-1/instance/list/akms,helloworld,ntsuiboot.json
    static final String DATA = '''
[
  {
    "amiId": null,
    "appInstance": {
      "appName": "akms",
      "class": "com.netflix.asgard.ApplicationInstance",
      "dataCenterInfo": null,
      "healthCheckUrl": "http://akmsqa100:7101/cryptexservice/healthcheck",
      "hostName": "akmsqa100",
      "instanceId": null,
      "ipAddr": "10.193.49.6",
      "leaseInfo": {
        "renewalIntervalInSecs": "30",
        "durationInSecs": "90",
        "registrationTimestamp": "2011-10-21T14:00:54.209-07:00",
        "lastRenewalTimestamp": "2011-10-22T23:31:46.121-07:00",
        "evictionTimestamp": "0",
        "clock": "14777"
      },
      "metadata": {
      },
      "port": "7101",
      "securePort": "7102",
      "status": "STARTING",
      "statusPageUrl": "http://akmsqa100:7101/cryptexservice/",
      "version": "v1.0",
      "vipAddress": "akmsqa100:7101"
    },
    "appName": "akms",
    "autoScalingGroupName": null,
    "class": "com.netflix.asgard.MergedInstance",
    "ec2Instance": null,
    "hostName": "akmsqa100",
    "instanceId": null,
    "instanceType": null,
    "launchTime": null,
    "port": "7101",
    "status": "STARTING",
    "version": "v1.0",
    "vipAddress": "akmsqa100:7101",
    "zone": null
  },
  {
    "amiId": null,
    "appInstance": {
      "appName": "akms",
      "class": "com.netflix.asgard.ApplicationInstance",
      "dataCenterInfo": null,
      "healthCheckUrl": "http://akmsqa101:7101/cryptexservice/healthcheck",
      "hostName": "akmsqa101",
      "instanceId": null,
      "ipAddr": "10.193.49.7",
      "leaseInfo": {
        "renewalIntervalInSecs": "30",
        "durationInSecs": "90",
        "registrationTimestamp": "2011-10-21T14:00:54.207-07:00",
        "lastRenewalTimestamp": "2011-10-22T23:31:40.027-07:00",
        "evictionTimestamp": "0",
        "clock": "14794"
      },
      "metadata": {
      },
      "port": "7101",
      "securePort": "7102",
      "status": "STARTING",
      "statusPageUrl": "http://akmsqa101:7101/cryptexservice/",
      "version": "v1.0",
      "vipAddress": "akmsqa101:7101"
    },
    "appName": "akms",
    "autoScalingGroupName": null,
    "class": "com.netflix.asgard.MergedInstance",
    "ec2Instance": null,
    "hostName": "akmsqa101",
    "instanceId": null,
    "instanceType": null,
    "launchTime": null,
    "port": "7101",
    "status": "STARTING",
    "version": "v1.0",
    "vipAddress": "akmsqa101:7101",
    "zone": null
  },
  {
    "amiId": "ami-fd5f9394",
    "appInstance": {
      "appName": "akms",
      "class": "com.netflix.asgard.ApplicationInstance",
      "dataCenterInfo": {
        "availability-zone": "us-east-1a",
        "public-ipv4": "107.20.52.4",
        "instance-id": "i-8fd134ec",
        "public-hostname": "ec2-107-20-52-4.compute-1.amazonaws.com",
        "local-ipv4": "10.124.99.210",
        "ami-id": "ami-fd5f9394",
        "instance-type": "m1.large"
      },
      "healthCheckUrl": "http://ec2-107-20-52-4.compute-1.amazonaws.com:7101/cryptexservice/healthcheck",
      "hostName": "ec2-107-20-52-4.compute-1.amazonaws.com",
      "instanceId": "i-8fd134ec",
      "ipAddr": "10.124.99.210",
      "leaseInfo": {
        "renewalIntervalInSecs": "30",
        "durationInSecs": "90",
        "registrationTimestamp": "2011-10-21T14:00:54.212-07:00",
        "lastRenewalTimestamp": "2011-10-22T23:31:20.825-07:00",
        "evictionTimestamp": "0",
        "clock": "4034"
      },
      "metadata": {
      },
      "port": "7101",
      "securePort": "7102",
      "status": "UP",
      "statusPageUrl": "http://ec2-107-20-52-4.compute-1.amazonaws.com:7101/cryptexservice/",
      "version": "v1.0",
      "vipAddress": "ec2-107-20-52-4.compute-1.amazonaws.com:7101"
    },
    "appName": "akms",
    "autoScalingGroupName": "akms-v002",
    "class": "com.netflix.asgard.MergedInstance",
    "ec2Instance": {
      "amiLaunchIndex": 0,
      "architecture": "x86_64",
      "blockDeviceMappings":
      [
        {
          "class": "com.amazonaws.services.ec2.model.InstanceBlockDeviceMapping",
          "deviceName": "/dev/sda1",
          "ebs": {
            "attachTime": "2011-10-21T20:54:41Z",
            "class": "com.amazonaws.services.ec2.model.EbsInstanceBlockDevice",
            "deleteOnTermination": true,
            "status": "attached",
            "volumeId": "vol-09a1a363"
          }
        }
      ],
      "class": "com.amazonaws.services.ec2.model.Instance",
      "clientToken": "a3e35f55-a3b5-4255-94e1-f9fc8b195192",
      "hypervisor": "xen",
      "imageId": "ami-fd5f9394",
      "instanceId": "i-8fd134ec",
      "instanceLifecycle": null,
      "instanceType": "m1.large",
      "kernelId": "aki-460bfe2f",
      "keyName": "nf-test-keypair-a",
      "launchTime": "2011-10-21T20:54:13Z",
      "license": null,
      "monitoring": {
        "class": "com.amazonaws.services.ec2.model.Monitoring",
        "state": "enabled"
      },
      "placement": {
        "availabilityZone": "us-east-1a",
        "class": "com.amazonaws.services.ec2.model.Placement",
        "groupName": "",
        "tenancy": "default"
      },
      "platform": null,
      "privateDnsName": "ip-10-124-99-210.ec2.internal",
      "privateIpAddress": "10.124.99.210",
      "productCodes":
      [
      ],
      "publicDnsName": "ec2-107-20-52-4.compute-1.amazonaws.com",
      "publicIpAddress": "107.20.52.4",
      "ramdiskId": "ari-580bfe31",
      "rootDeviceName": "/dev/sda1",
      "rootDeviceType": "ebs",
      "securityGroups":
      [
        {
          "class": "com.amazonaws.services.ec2.model.GroupIdentifier",
          "groupId": "sg-7fcd0716",
          "groupName": "nf-infrastructure"
        },
        {
          "class": "com.amazonaws.services.ec2.model.GroupIdentifier",
          "groupId": "sg-8cb5d7e5",
          "groupName": "akms"
        },
        {
          "class": "com.amazonaws.services.ec2.model.GroupIdentifier",
          "groupId": "sg-3e49b257",
          "groupName": "nf-datacenter"
        }
      ],
      "sourceDestCheck": null,
      "spotInstanceRequestId": null,
      "state": {
        "class": "com.amazonaws.services.ec2.model.InstanceState",
        "code": 16,
        "name": "running"
      },
      "stateReason": null,
      "stateTransitionReason": "",
      "subnetId": null,
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "aws:autoscaling:groupName",
          "value": "akms-v002"
        }
      ],
      "virtualizationType": "paravirtual",
      "vpcId": null
    },
    "hostName": "ec2-107-20-52-4.compute-1.amazonaws.com",
    "instanceId": "i-8fd134ec",
    "instanceType": "m1.large",
    "launchTime": "2011-10-21T20:54:13Z",
    "port": "7101",
    "status": "running",
    "version": "v1.0",
    "vipAddress": "ec2-107-20-52-4.compute-1.amazonaws.com:7101",
    "zone": "us-east-1a"
  },
  {
    "amiId": "ami-fd5f9394",
    "appInstance": {
      "appName": "akms",
      "class": "com.netflix.asgard.ApplicationInstance",
      "dataCenterInfo": {
        "availability-zone": "us-east-1c",
        "public-ipv4": "50.16.122.132",
        "instance-id": "i-a27867c2",
        "public-hostname": "ec2-50-16-122-132.compute-1.amazonaws.com",
        "local-ipv4": "10.207.38.53",
        "ami-id": "ami-fd5f9394",
        "instance-type": "m1.large"
      },
      "healthCheckUrl": "http://ec2-50-16-122-132.compute-1.amazonaws.com:7101/cryptexservice/healthcheck",
      "hostName": "ec2-50-16-122-132.compute-1.amazonaws.com",
      "instanceId": "i-a27867c2",
      "ipAddr": "10.207.38.53",
      "leaseInfo": {
        "renewalIntervalInSecs": "30",
        "durationInSecs": "90",
        "registrationTimestamp": "2011-10-21T14:00:54.214-07:00",
        "lastRenewalTimestamp": "2011-10-22T23:31:32.801-07:00",
        "evictionTimestamp": "0",
        "clock": "7426"
      },
      "metadata": {
      },
      "port": "7101",
      "securePort": "7102",
      "status": "UP",
      "statusPageUrl": "http://ec2-50-16-122-132.compute-1.amazonaws.com:7101/cryptexservice/",
      "version": "v1.0",
      "vipAddress": "ec2-50-16-122-132.compute-1.amazonaws.com:7101"
    },
    "appName": "akms",
    "autoScalingGroupName": "akms-v002",
    "class": "com.netflix.asgard.MergedInstance",
    "ec2Instance": {
      "amiLaunchIndex": 0,
      "architecture": "x86_64",
      "blockDeviceMappings":
      [
        {
          "class": "com.amazonaws.services.ec2.model.InstanceBlockDeviceMapping",
          "deviceName": "/dev/sda1",
          "ebs": {
            "attachTime": "2011-10-20T16:42:59Z",
            "class": "com.amazonaws.services.ec2.model.EbsInstanceBlockDevice",
            "deleteOnTermination": true,
            "status": "attached",
            "volumeId": "vol-f9101193"
          }
        }
      ],
      "class": "com.amazonaws.services.ec2.model.Instance",
      "clientToken": "3fba4d37-73ee-46b1-af0e-e54d2979ec30",
      "hypervisor": "xen",
      "imageId": "ami-fd5f9394",
      "instanceId": "i-a27867c2",
      "instanceLifecycle": null,
      "instanceType": "m1.large",
      "kernelId": "aki-460bfe2f",
      "keyName": "nf-test-keypair-a",
      "launchTime": "2011-10-20T16:42:28Z",
      "license": null,
      "monitoring": {
        "class": "com.amazonaws.services.ec2.model.Monitoring",
        "state": "enabled"
      },
      "placement": {
        "availabilityZone": "us-east-1c",
        "class": "com.amazonaws.services.ec2.model.Placement",
        "groupName": "",
        "tenancy": "default"
      },
      "platform": null,
      "privateDnsName": "domU-12-31-39-15-25-C7.compute-1.internal",
      "privateIpAddress": "10.207.38.53",
      "productCodes":
      [
      ],
      "publicDnsName": "ec2-50-16-122-132.compute-1.amazonaws.com",
      "publicIpAddress": "50.16.122.132",
      "ramdiskId": "ari-580bfe31",
      "rootDeviceName": "/dev/sda1",
      "rootDeviceType": "ebs",
      "securityGroups":
      [
        {
          "class": "com.amazonaws.services.ec2.model.GroupIdentifier",
          "groupId": "sg-7fcd0716",
          "groupName": "nf-infrastructure"
        },
        {
          "class": "com.amazonaws.services.ec2.model.GroupIdentifier",
          "groupId": "sg-8cb5d7e5",
          "groupName": "akms"
        },
        {
          "class": "com.amazonaws.services.ec2.model.GroupIdentifier",
          "groupId": "sg-3e49b257",
          "groupName": "nf-datacenter"
        }
      ],
      "sourceDestCheck": null,
      "spotInstanceRequestId": null,
      "state": {
        "class": "com.amazonaws.services.ec2.model.InstanceState",
        "code": 16,
        "name": "running"
      },
      "stateReason": null,
      "stateTransitionReason": "",
      "subnetId": null,
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "aws:autoscaling:groupName",
          "value": "akms-v002"
        }
      ],
      "virtualizationType": "paravirtual",
      "vpcId": null
    },
    "hostName": "ec2-50-16-122-132.compute-1.amazonaws.com",
    "instanceId": "i-a27867c2",
    "instanceType": "m1.large",
    "launchTime": "2011-10-20T16:42:28Z",
    "port": "7101",
    "status": "running",
    "version": "v1.0",
    "vipAddress": "ec2-50-16-122-132.compute-1.amazonaws.com:7101",
    "zone": "us-east-1c"
  },
  {
    "amiId": "ami-4775b32e",
    "appInstance": {
      "appName": "helloworld",
      "class": "com.netflix.asgard.ApplicationInstance",
      "dataCenterInfo": {
        "availability-zone": "us-east-1a",
        "public-ipv4": "50.17.58.139",
        "instance-id": "i-95fe1df6",
        "public-hostname": "ec2-50-17-58-139.compute-1.amazonaws.com",
        "local-ipv4": "10.124.221.243",
        "ami-id": "ami-4775b32e",
        "instance-type": "m1.large"
      },
      "healthCheckUrl": "http://ec2-50-17-58-139.compute-1.amazonaws.com:7001/healthcheck",
      "hostName": "ec2-50-17-58-139.compute-1.amazonaws.com",
      "instanceId": "i-95fe1df6",
      "ipAddr": "10.124.221.243",
      "leaseInfo": {
        "renewalIntervalInSecs": "30",
        "durationInSecs": "90",
        "registrationTimestamp": "2011-10-22T23:15:42.614-07:00",
        "lastRenewalTimestamp": "2011-10-22T23:31:43.319-07:00",
        "evictionTimestamp": "0",
        "clock": "32"
      },
      "metadata": {
      },
      "port": "7001",
      "securePort": "7002",
      "status": "UP",
      "statusPageUrl": "http://ec2-50-17-58-139.compute-1.amazonaws.com:7001/Status",
      "version": "1.1",
      "vipAddress": "ec2-50-17-58-139.compute-1.amazonaws.com:7001"
    },
    "appName": "helloworld",
    "autoScalingGroupName": "helloworld-example-v015",
    "class": "com.netflix.asgard.MergedInstance",
    "ec2Instance": {
      "amiLaunchIndex": 0,
      "architecture": "x86_64",
      "blockDeviceMappings":
      [
        {
          "class": "com.amazonaws.services.ec2.model.InstanceBlockDeviceMapping",
          "deviceName": "/dev/sda1",
          "ebs": {
            "attachTime": "2011-10-23T06:13:46Z",
            "class": "com.amazonaws.services.ec2.model.EbsInstanceBlockDevice",
            "deleteOnTermination": true,
            "status": "attached",
            "volumeId": "vol-bf7779d5"
          }
        }
      ],
      "class": "com.amazonaws.services.ec2.model.Instance",
      "clientToken": "d43fa901-dc96-4fab-9a20-e6ceb4fc47a0",
      "hypervisor": "xen",
      "imageId": "ami-4775b32e",
      "instanceId": "i-95fe1df6",
      "instanceLifecycle": null,
      "instanceType": "m1.large",
      "kernelId": "aki-460bfe2f",
      "keyName": "nf-test-keypair-a",
      "launchTime": "2011-10-23T06:13:02Z",
      "license": null,
      "monitoring": {
        "class": "com.amazonaws.services.ec2.model.Monitoring",
        "state": "enabled"
      },
      "placement": {
        "availabilityZone": "us-east-1a",
        "class": "com.amazonaws.services.ec2.model.Placement",
        "groupName": "",
        "tenancy": "default"
      },
      "platform": null,
      "privateDnsName": "ip-10-124-221-243.ec2.internal",
      "privateIpAddress": "10.124.221.243",
      "productCodes":
      [
      ],
      "publicDnsName": "ec2-50-17-58-139.compute-1.amazonaws.com",
      "publicIpAddress": "50.17.58.139",
      "ramdiskId": "ari-580bfe31",
      "rootDeviceName": "/dev/sda1",
      "rootDeviceType": "ebs",
      "securityGroups":
      [
        {
          "class": "com.amazonaws.services.ec2.model.GroupIdentifier",
          "groupId": "sg-7fcd0716",
          "groupName": "nf-infrastructure"
        },
        {
          "class": "com.amazonaws.services.ec2.model.GroupIdentifier",
          "groupId": "sg-b7f73ade",
          "groupName": "helloworld"
        },
        {
          "class": "com.amazonaws.services.ec2.model.GroupIdentifier",
          "groupId": "sg-8bf73ae2",
          "groupName": "helloworld-frontend"
        },
        {
          "class": "com.amazonaws.services.ec2.model.GroupIdentifier",
          "groupId": "sg-e87caf81",
          "groupName": "helloworld-asgardtest"
        },
        {
          "class": "com.amazonaws.services.ec2.model.GroupIdentifier",
          "groupId": "sg-3e49b257",
          "groupName": "nf-datacenter"
        }
      ],
      "sourceDestCheck": null,
      "spotInstanceRequestId": null,
      "state": {
        "class": "com.amazonaws.services.ec2.model.InstanceState",
        "code": 16,
        "name": "running"
      },
      "stateReason": null,
      "stateTransitionReason": "",
      "subnetId": null,
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "aws:autoscaling:groupName",
          "value": "helloworld-example-v015"
        }
      ],
      "virtualizationType": "paravirtual",
      "vpcId": null
    },
    "hostName": "ec2-50-17-58-139.compute-1.amazonaws.com",
    "instanceId": "i-95fe1df6",
    "instanceType": "m1.large",
    "launchTime": "2011-10-23T06:13:02Z",
    "port": "7001",
    "status": "running",
    "version": "1.1",
    "vipAddress": "ec2-50-17-58-139.compute-1.amazonaws.com:7001",
    "zone": "us-east-1a"
  },
  {
    "amiId": "ami-b3579ada",
    "appInstance": {
      "appName": "helloworld",
      "class": "com.netflix.asgard.ApplicationInstance",
      "dataCenterInfo": {
        "availability-zone": "us-east-1d",
        "public-ipv4": "107.20.50.5",
        "instance-id": "i-ce152dae",
        "public-hostname": "ec2-107-20-50-5.compute-1.amazonaws.com",
        "local-ipv4": "10.114.10.3",
        "ami-id": "ami-b3579ada",
        "instance-type": "m1.small"
      },
      "healthCheckUrl": "http://ec2-107-20-50-5.compute-1.amazonaws.com:7001/healthcheck",
      "hostName": "ec2-107-20-50-5.compute-1.amazonaws.com",
      "instanceId": "i-ce152dae",
      "ipAddr": "10.114.10.3",
      "leaseInfo": {
        "renewalIntervalInSecs": "30",
        "durationInSecs": "90",
        "registrationTimestamp": "2011-10-21T14:00:55.472-07:00",
        "lastRenewalTimestamp": "2011-10-22T23:31:30.120-07:00",
        "evictionTimestamp": "0",
        "clock": "24040"
      },
      "metadata": {
      },
      "port": "7001",
      "securePort": "7002",
      "status": "UP",
      "statusPageUrl": "http://ec2-107-20-50-5.compute-1.amazonaws.com:7001/Status",
      "version": "1.1",
      "vipAddress": "ec2-107-20-50-5.compute-1.amazonaws.com:7001"
    },
    "appName": "helloworld",
    "autoScalingGroupName": null,
    "class": "com.netflix.asgard.MergedInstance",
    "ec2Instance": {
      "amiLaunchIndex": 0,
      "architecture": "i386",
      "blockDeviceMappings":
      [
        {
          "class": "com.amazonaws.services.ec2.model.InstanceBlockDeviceMapping",
          "deviceName": "/dev/sda1",
          "ebs": {
            "attachTime": "2011-10-14T21:53:00Z",
            "class": "com.amazonaws.services.ec2.model.EbsInstanceBlockDevice",
            "deleteOnTermination": true,
            "status": "attached",
            "volumeId": "vol-f3eedf99"
          }
        }
      ],
      "class": "com.amazonaws.services.ec2.model.Instance",
      "clientToken": "",
      "hypervisor": "xen",
      "imageId": "ami-b3579ada",
      "instanceId": "i-ce152dae",
      "instanceLifecycle": null,
      "instanceType": "m1.small",
      "kernelId": "aki-1c669375",
      "keyName": "nf-test-keypair-a",
      "launchTime": "2011-10-14T21:52:27Z",
      "license": null,
      "monitoring": {
        "class": "com.amazonaws.services.ec2.model.Monitoring",
        "state": "disabled"
      },
      "placement": {
        "availabilityZone": "us-east-1d",
        "class": "com.amazonaws.services.ec2.model.Placement",
        "groupName": "",
        "tenancy": "default"
      },
      "platform": null,
      "privateDnsName": "ip-10-114-10-3.ec2.internal",
      "privateIpAddress": "10.114.10.3",
      "productCodes":
      [
      ],
      "publicDnsName": "ec2-107-20-50-5.compute-1.amazonaws.com",
      "publicIpAddress": "107.20.50.5",
      "ramdiskId": "ari-0ccd3965",
      "rootDeviceName": "/dev/sda1",
      "rootDeviceType": "ebs",
      "securityGroups":
      [
        {
          "class": "com.amazonaws.services.ec2.model.GroupIdentifier",
          "groupId": "sg-7fcd0716",
          "groupName": "nf-infrastructure"
        },
        {
          "class": "com.amazonaws.services.ec2.model.GroupIdentifier",
          "groupId": "sg-b7f73ade",
          "groupName": "helloworld"
        },
        {
          "class": "com.amazonaws.services.ec2.model.GroupIdentifier",
          "groupId": "sg-8bf73ae2",
          "groupName": "helloworld-frontend"
        },
        {
          "class": "com.amazonaws.services.ec2.model.GroupIdentifier",
          "groupId": "sg-3e49b257",
          "groupName": "nf-datacenter"
        }
      ],
      "sourceDestCheck": null,
      "spotInstanceRequestId": null,
      "state": {
        "class": "com.amazonaws.services.ec2.model.InstanceState",
        "code": 16,
        "name": "running"
      },
      "stateReason": null,
      "stateTransitionReason": "",
      "subnetId": null,
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "env",
          "value": "test"
        },
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "owner",
          "value": "kgrossoehme"
        },
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "pkg",
          "value": "helloworld-1.4.0-1083825.h397"
        },
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "app",
          "value": "helloworld"
        }
      ],
      "virtualizationType": "paravirtual",
      "vpcId": null
    },
    "hostName": "ec2-107-20-50-5.compute-1.amazonaws.com",
    "instanceId": "i-ce152dae",
    "instanceType": "m1.small",
    "launchTime": "2011-10-14T21:52:27Z",
    "port": "7001",
    "status": "running",
    "version": "1.1",
    "vipAddress": "ec2-107-20-50-5.compute-1.amazonaws.com:7001",
    "zone": "us-east-1d"
  },
  {
    "amiId": "ami-4775b32e",
    "appInstance": {
      "appName": "helloworld",
      "class": "com.netflix.asgard.ApplicationInstance",
      "dataCenterInfo": {
        "availability-zone": "us-east-1a",
        "public-ipv4": "174.129.143.61",
        "instance-id": "i-6ef9f30e",
        "public-hostname": "ec2-174-129-143-61.compute-1.amazonaws.com",
        "local-ipv4": "10.90.238.132",
        "ami-id": "ami-4775b32e",
        "instance-type": "m1.large"
      },
      "healthCheckUrl": "http://ec2-174-129-143-61.compute-1.amazonaws.com:7001/healthcheck",
      "hostName": "ec2-174-129-143-61.compute-1.amazonaws.com",
      "instanceId": "i-6ef9f30e",
      "ipAddr": "10.90.238.132",
      "leaseInfo": {
        "renewalIntervalInSecs": "30",
        "durationInSecs": "90",
        "registrationTimestamp": "2011-10-21T14:00:55.478-07:00",
        "lastRenewalTimestamp": "2011-10-22T23:31:35.357-07:00",
        "evictionTimestamp": "0",
        "clock": "15322"
      },
      "metadata": {
      },
      "port": "7001",
      "securePort": "7002",
      "status": "UP",
      "statusPageUrl": "http://ec2-174-129-143-61.compute-1.amazonaws.com:7001/Status",
      "version": "1.1",
      "vipAddress": "ec2-174-129-143-61.compute-1.amazonaws.com:7001"
    },
    "appName": "helloworld",
    "autoScalingGroupName": "helloworld-example-v015",
    "class": "com.netflix.asgard.MergedInstance",
    "ec2Instance": {
      "amiLaunchIndex": 0,
      "architecture": "x86_64",
      "blockDeviceMappings":
      [
        {
          "class": "com.amazonaws.services.ec2.model.InstanceBlockDeviceMapping",
          "deviceName": "/dev/sda1",
          "ebs": {
            "attachTime": "2011-10-18T17:49:43Z",
            "class": "com.amazonaws.services.ec2.model.EbsInstanceBlockDevice",
            "deleteOnTermination": true,
            "status": "attached",
            "volumeId": "vol-f3063d99"
          }
        }
      ],
      "class": "com.amazonaws.services.ec2.model.Instance",
      "clientToken": "bf0784bd-59c1-4f94-ad61-af8f5e54dfc6",
      "hypervisor": "xen",
      "imageId": "ami-4775b32e",
      "instanceId": "i-6ef9f30e",
      "instanceLifecycle": null,
      "instanceType": "m1.large",
      "kernelId": "aki-460bfe2f",
      "keyName": "nf-test-keypair-a",
      "launchTime": "2011-10-18T17:49:11Z",
      "license": null,
      "monitoring": {
        "class": "com.amazonaws.services.ec2.model.Monitoring",
        "state": "enabled"
      },
      "placement": {
        "availabilityZone": "us-east-1a",
        "class": "com.amazonaws.services.ec2.model.Placement",
        "groupName": "",
        "tenancy": "default"
      },
      "platform": null,
      "privateDnsName": "ip-10-90-238-132.ec2.internal",
      "privateIpAddress": "10.90.238.132",
      "productCodes":
      [
      ],
      "publicDnsName": "ec2-174-129-143-61.compute-1.amazonaws.com",
      "publicIpAddress": "174.129.143.61",
      "ramdiskId": "ari-580bfe31",
      "rootDeviceName": "/dev/sda1",
      "rootDeviceType": "ebs",
      "securityGroups":
      [
        {
          "class": "com.amazonaws.services.ec2.model.GroupIdentifier",
          "groupId": "sg-7fcd0716",
          "groupName": "nf-infrastructure"
        },
        {
          "class": "com.amazonaws.services.ec2.model.GroupIdentifier",
          "groupId": "sg-b7f73ade",
          "groupName": "helloworld"
        },
        {
          "class": "com.amazonaws.services.ec2.model.GroupIdentifier",
          "groupId": "sg-8bf73ae2",
          "groupName": "helloworld-frontend"
        },
        {
          "class": "com.amazonaws.services.ec2.model.GroupIdentifier",
          "groupId": "sg-e87caf81",
          "groupName": "helloworld-asgardtest"
        },
        {
          "class": "com.amazonaws.services.ec2.model.GroupIdentifier",
          "groupId": "sg-3e49b257",
          "groupName": "nf-datacenter"
        }
      ],
      "sourceDestCheck": null,
      "spotInstanceRequestId": null,
      "state": {
        "class": "com.amazonaws.services.ec2.model.InstanceState",
        "code": 16,
        "name": "running"
      },
      "stateReason": null,
      "stateTransitionReason": "",
      "subnetId": null,
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "elb_deregistered_time",
          "value": "2011-10-18 15:23:58 PDT"
        },
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "aws:autoscaling:groupName",
          "value": "helloworld-example-v015"
        }
      ],
      "virtualizationType": "paravirtual",
      "vpcId": null
    },
    "hostName": "ec2-174-129-143-61.compute-1.amazonaws.com",
    "instanceId": "i-6ef9f30e",
    "instanceType": "m1.large",
    "launchTime": "2011-10-18T17:49:11Z",
    "port": "7001",
    "status": "running",
    "version": "1.1",
    "vipAddress": "ec2-174-129-143-61.compute-1.amazonaws.com:7001",
    "zone": "us-east-1a"
  },
  {
    "amiId": "ami-6c8d7605",
    "appInstance": {
      "appName": "helloworld",
      "class": "com.netflix.asgard.ApplicationInstance",
      "dataCenterInfo": {
        "availability-zone": "us-east-1a",
        "public-ipv4": "184.73.36.185",
        "instance-id": "i-3ef8ba5e",
        "public-hostname": "ec2-184-73-36-185.compute-1.amazonaws.com",
        "local-ipv4": "10.90.9.249",
        "ami-id": "ami-6c8d7605",
        "instance-type": "m2.2xlarge"
      },
      "healthCheckUrl": "http://ec2-184-73-36-185.compute-1.amazonaws.com:7001/healthcheck",
      "hostName": "ec2-184-73-36-185.compute-1.amazonaws.com",
      "instanceId": "i-3ef8ba5e",
      "ipAddr": "10.90.9.249",
      "leaseInfo": {
        "renewalIntervalInSecs": "30",
        "durationInSecs": "90",
        "registrationTimestamp": "2011-10-21T14:00:55.541-07:00",
        "lastRenewalTimestamp": "2011-10-22T23:31:38.630-07:00",
        "evictionTimestamp": "0",
        "clock": "57603"
      },
      "metadata": {
      },
      "port": "7001",
      "securePort": "7002",
      "status": "UP",
      "statusPageUrl": "http://ec2-184-73-36-185.compute-1.amazonaws.com:7001/Status",
      "version": "1.1",
      "vipAddress": "ec2-184-73-36-185.compute-1.amazonaws.com:7001"
    },
    "appName": "helloworld",
    "autoScalingGroupName": null,
    "class": "com.netflix.asgard.MergedInstance",
    "ec2Instance": {
      "amiLaunchIndex": 0,
      "architecture": "x86_64",
      "blockDeviceMappings":
      [
        {
          "class": "com.amazonaws.services.ec2.model.InstanceBlockDeviceMapping",
          "deviceName": "/dev/sda1",
          "ebs": {
            "attachTime": "2011-10-04T01:16:25Z",
            "class": "com.amazonaws.services.ec2.model.EbsInstanceBlockDevice",
            "deleteOnTermination": true,
            "status": "attached",
            "volumeId": "vol-afe4b6c5"
          }
        }
      ],
      "class": "com.amazonaws.services.ec2.model.Instance",
      "clientToken": "5401b31d-8c43-45d3-89c6-ecd7a94b7acf",
      "hypervisor": "xen",
      "imageId": "ami-6c8d7605",
      "instanceId": "i-3ef8ba5e",
      "instanceLifecycle": null,
      "instanceType": "m2.2xlarge",
      "kernelId": "aki-460bfe2f",
      "keyName": "nf-test-keypair-a",
      "launchTime": "2011-10-04T01:15:58Z",
      "license": null,
      "monitoring": {
        "class": "com.amazonaws.services.ec2.model.Monitoring",
        "state": "enabled"
      },
      "placement": {
        "availabilityZone": "us-east-1a",
        "class": "com.amazonaws.services.ec2.model.Placement",
        "groupName": "",
        "tenancy": "default"
      },
      "platform": null,
      "privateDnsName": "ip-10-90-9-249.ec2.internal",
      "privateIpAddress": "10.90.9.249",
      "productCodes":
      [
      ],
      "publicDnsName": "ec2-184-73-36-185.compute-1.amazonaws.com",
      "publicIpAddress": "184.73.36.185",
      "ramdiskId": "ari-580bfe31",
      "rootDeviceName": "/dev/sda1",
      "rootDeviceType": "ebs",
      "securityGroups":
      [
        {
          "class": "com.amazonaws.services.ec2.model.GroupIdentifier",
          "groupId": "sg-7fcd0716",
          "groupName": "nf-infrastructure"
        },
        {
          "class": "com.amazonaws.services.ec2.model.GroupIdentifier",
          "groupId": "sg-ba2f42d3",
          "groupName": "tbradleytest"
        },
        {
          "class": "com.amazonaws.services.ec2.model.GroupIdentifier",
          "groupId": "sg-3e49b257",
          "groupName": "nf-datacenter"
        }
      ],
      "sourceDestCheck": null,
      "spotInstanceRequestId": null,
      "state": {
        "class": "com.amazonaws.services.ec2.model.InstanceState",
        "code": 16,
        "name": "running"
      },
      "stateReason": null,
      "stateTransitionReason": "",
      "subnetId": null,
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "aws:autoscaling:groupName",
          "value": "tbradleytest"
        }
      ],
      "virtualizationType": "paravirtual",
      "vpcId": null
    },
    "hostName": "ec2-184-73-36-185.compute-1.amazonaws.com",
    "instanceId": "i-3ef8ba5e",
    "instanceType": "m2.2xlarge",
    "launchTime": "2011-10-04T01:15:58Z",
    "port": "7001",
    "status": "running",
    "version": "1.1",
    "vipAddress": "ec2-184-73-36-185.compute-1.amazonaws.com:7001",
    "zone": "us-east-1a"
  },
  {
    "amiId": "ami-6c8d7605",
    "appInstance": {
      "appName": "helloworld",
      "class": "com.netflix.asgard.ApplicationInstance",
      "dataCenterInfo": {
        "availability-zone": "us-east-1a",
        "public-ipv4": "107.20.64.238",
        "instance-id": "i-965210f6",
        "public-hostname": "ec2-107-20-64-238.compute-1.amazonaws.com",
        "local-ipv4": "10.94.81.117",
        "ami-id": "ami-6c8d7605",
        "instance-type": "m2.2xlarge"
      },
      "healthCheckUrl": "http://ec2-107-20-64-238.compute-1.amazonaws.com:7001/healthcheck",
      "hostName": "ec2-107-20-64-238.compute-1.amazonaws.com",
      "instanceId": "i-965210f6",
      "ipAddr": "10.94.81.117",
      "leaseInfo": {
        "renewalIntervalInSecs": "30",
        "durationInSecs": "90",
        "registrationTimestamp": "2011-10-21T14:00:55.488-07:00",
        "lastRenewalTimestamp": "2011-10-22T23:31:27.236-07:00",
        "evictionTimestamp": "0",
        "clock": "44486"
      },
      "metadata": {
      },
      "port": "7001",
      "securePort": "7002",
      "status": "UP",
      "statusPageUrl": "http://ec2-107-20-64-238.compute-1.amazonaws.com:7001/Status",
      "version": "1.1",
      "vipAddress": "ec2-107-20-64-238.compute-1.amazonaws.com:7001"
    },
    "appName": "helloworld",
    "autoScalingGroupName": null,
    "class": "com.netflix.asgard.MergedInstance",
    "ec2Instance": {
      "amiLaunchIndex": 0,
      "architecture": "x86_64",
      "blockDeviceMappings":
      [
        {
          "class": "com.amazonaws.services.ec2.model.InstanceBlockDeviceMapping",
          "deviceName": "/dev/sda1",
          "ebs": {
            "attachTime": "2011-10-03T22:50:56Z",
            "class": "com.amazonaws.services.ec2.model.EbsInstanceBlockDevice",
            "deleteOnTermination": true,
            "status": "attached",
            "volumeId": "vol-07beec6d"
          }
        }
      ],
      "class": "com.amazonaws.services.ec2.model.Instance",
      "clientToken": "29f85f68-a785-401e-8b16-80b390986e68",
      "hypervisor": "xen",
      "imageId": "ami-6c8d7605",
      "instanceId": "i-965210f6",
      "instanceLifecycle": null,
      "instanceType": "m2.2xlarge",
      "kernelId": "aki-460bfe2f",
      "keyName": "nf-test-keypair-a",
      "launchTime": "2011-10-03T22:50:36Z",
      "license": null,
      "monitoring": {
        "class": "com.amazonaws.services.ec2.model.Monitoring",
        "state": "enabled"
      },
      "placement": {
        "availabilityZone": "us-east-1a",
        "class": "com.amazonaws.services.ec2.model.Placement",
        "groupName": "",
        "tenancy": "default"
      },
      "platform": null,
      "privateDnsName": "ip-10-94-81-117.ec2.internal",
      "privateIpAddress": "10.94.81.117",
      "productCodes":
      [
      ],
      "publicDnsName": "ec2-107-20-64-238.compute-1.amazonaws.com",
      "publicIpAddress": "107.20.64.238",
      "ramdiskId": "ari-580bfe31",
      "rootDeviceName": "/dev/sda1",
      "rootDeviceType": "ebs",
      "securityGroups":
      [
        {
          "class": "com.amazonaws.services.ec2.model.GroupIdentifier",
          "groupId": "sg-7fcd0716",
          "groupName": "nf-infrastructure"
        },
        {
          "class": "com.amazonaws.services.ec2.model.GroupIdentifier",
          "groupId": "sg-ba2f42d3",
          "groupName": "tbradleytest"
        },
        {
          "class": "com.amazonaws.services.ec2.model.GroupIdentifier",
          "groupId": "sg-3e49b257",
          "groupName": "nf-datacenter"
        }
      ],
      "sourceDestCheck": null,
      "spotInstanceRequestId": null,
      "state": {
        "class": "com.amazonaws.services.ec2.model.InstanceState",
        "code": 16,
        "name": "running"
      },
      "stateReason": null,
      "stateTransitionReason": "",
      "subnetId": null,
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "aws:autoscaling:groupName",
          "value": "tbradleytest"
        }
      ],
      "virtualizationType": "paravirtual",
      "vpcId": null
    },
    "hostName": "ec2-107-20-64-238.compute-1.amazonaws.com",
    "instanceId": "i-965210f6",
    "instanceType": "m2.2xlarge",
    "launchTime": "2011-10-03T22:50:36Z",
    "port": "7001",
    "status": "running",
    "version": "1.1",
    "vipAddress": "ec2-107-20-64-238.compute-1.amazonaws.com:7001",
    "zone": "us-east-1a"
  },
  {
    "amiId": null,
    "appInstance": {
      "appName": "helloworld",
      "class": "com.netflix.asgard.ApplicationInstance",
      "dataCenterInfo": null,
      "healthCheckUrl": "http://lgux-dyuan:7001/healthcheck",
      "hostName": "lgux-dyuan",
      "instanceId": null,
      "ipAddr": "10.2.245.59",
      "leaseInfo": {
        "renewalIntervalInSecs": "30",
        "durationInSecs": "90",
        "registrationTimestamp": "2011-10-21T14:00:55.494-07:00",
        "lastRenewalTimestamp": "2011-10-22T23:31:42.317-07:00",
        "evictionTimestamp": "0",
        "clock": "32023"
      },
      "metadata": {
      },
      "port": "7001",
      "securePort": "7002",
      "status": "UP",
      "statusPageUrl": "http://lgux-dyuan:7001/Status",
      "version": "1.1",
      "vipAddress": "lgux-dyuan:7001"
    },
    "appName": "helloworld",
    "autoScalingGroupName": null,
    "class": "com.netflix.asgard.MergedInstance",
    "ec2Instance": null,
    "hostName": "lgux-dyuan",
    "instanceId": null,
    "instanceType": null,
    "launchTime": null,
    "port": "7001",
    "status": "UP",
    "version": "1.1",
    "vipAddress": "lgux-dyuan:7001",
    "zone": null
  },
  {
    "amiId": "ami-10b84079",
    "appInstance": {
      "appName": "helloworld",
      "class": "com.netflix.asgard.ApplicationInstance",
      "dataCenterInfo": {
        "availability-zone": "us-east-1d",
        "public-ipv4": "50.17.175.102",
        "instance-id": "i-d9eaa8b7",
        "public-hostname": "ec2-50-17-175-102.compute-1.amazonaws.com",
        "local-ipv4": "10.114.90.25",
        "ami-id": "ami-10b84079",
        "instance-type": "m1.small"
      },
      "healthCheckUrl": "http://ec2-50-17-175-102.compute-1.amazonaws.com:7001/healthcheck",
      "hostName": "ec2-50-17-175-102.compute-1.amazonaws.com",
      "instanceId": "i-d9eaa8b7",
      "ipAddr": "10.114.90.25",
      "leaseInfo": {
        "renewalIntervalInSecs": "30",
        "durationInSecs": "90",
        "registrationTimestamp": "2011-10-21T14:00:55.466-07:00",
        "lastRenewalTimestamp": "2011-10-22T23:31:28.201-07:00",
        "evictionTimestamp": "0",
        "clock": "367544"
      },
      "metadata": {
      },
      "port": "7001",
      "securePort": "7002",
      "status": "UP",
      "statusPageUrl": "http://ec2-50-17-175-102.compute-1.amazonaws.com:7001/Status",
      "version": "1.1",
      "vipAddress": "ec2-50-17-175-102.compute-1.amazonaws.com:7001"
    },
    "appName": "helloworld",
    "autoScalingGroupName": null,
    "class": "com.netflix.asgard.MergedInstance",
    "ec2Instance": {
      "amiLaunchIndex": 0,
      "architecture": "i386",
      "blockDeviceMappings":
      [
        {
          "class": "com.amazonaws.services.ec2.model.InstanceBlockDeviceMapping",
          "deviceName": "/dev/sda1",
          "ebs": {
            "attachTime": "2011-06-17T15:51:14Z",
            "class": "com.amazonaws.services.ec2.model.EbsInstanceBlockDevice",
            "deleteOnTermination": true,
            "status": "attached",
            "volumeId": "vol-0764196c"
          }
        }
      ],
      "class": "com.amazonaws.services.ec2.model.Instance",
      "clientToken": "",
      "hypervisor": "xen",
      "imageId": "ami-10b84079",
      "instanceId": "i-d9eaa8b7",
      "instanceLifecycle": null,
      "instanceType": "m1.small",
      "kernelId": "aki-1c669375",
      "keyName": "nf-test-keypair-a",
      "launchTime": "2011-06-17T15:50:40Z",
      "license": null,
      "monitoring": {
        "class": "com.amazonaws.services.ec2.model.Monitoring",
        "state": "disabled"
      },
      "placement": {
        "availabilityZone": "us-east-1d",
        "class": "com.amazonaws.services.ec2.model.Placement",
        "groupName": "",
        "tenancy": "default"
      },
      "platform": null,
      "privateDnsName": "ip-10-114-90-25.ec2.internal",
      "privateIpAddress": "10.114.90.25",
      "productCodes":
      [
      ],
      "publicDnsName": "ec2-50-17-175-102.compute-1.amazonaws.com",
      "publicIpAddress": "50.17.175.102",
      "ramdiskId": "ari-0ccd3965",
      "rootDeviceName": "/dev/sda1",
      "rootDeviceType": "ebs",
      "securityGroups":
      [
        {
          "class": "com.amazonaws.services.ec2.model.GroupIdentifier",
          "groupId": "sg-7fcd0716",
          "groupName": "nf-infrastructure"
        },
        {
          "class": "com.amazonaws.services.ec2.model.GroupIdentifier",
          "groupId": "sg-b7f73ade",
          "groupName": "helloworld"
        },
        {
          "class": "com.amazonaws.services.ec2.model.GroupIdentifier",
          "groupId": "sg-8bf73ae2",
          "groupName": "helloworld-frontend"
        },
        {
          "class": "com.amazonaws.services.ec2.model.GroupIdentifier",
          "groupId": "sg-3e49b257",
          "groupName": "nf-datacenter"
        }
      ],
      "sourceDestCheck": null,
      "spotInstanceRequestId": null,
      "state": {
        "class": "com.amazonaws.services.ec2.model.InstanceState",
        "code": 16,
        "name": "running"
      },
      "stateReason": null,
      "stateTransitionReason": "",
      "subnetId": null,
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "env",
          "value": "test"
        },
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "pkg",
          "value": "helloworld"
        },
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "owner",
          "value": "josthomas"
        },
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "app",
          "value": "helloworld"
        }
      ],
      "virtualizationType": "paravirtual",
      "vpcId": null
    },
    "hostName": "ec2-50-17-175-102.compute-1.amazonaws.com",
    "instanceId": "i-d9eaa8b7",
    "instanceType": "m1.small",
    "launchTime": "2011-06-17T15:50:40Z",
    "port": "7001",
    "status": "running",
    "version": "1.1",
    "vipAddress": "ec2-50-17-175-102.compute-1.amazonaws.com:7001",
    "zone": "us-east-1d"
  },
  {
    "amiId": "ami-b3579ada",
    "appInstance": {
      "appName": "helloworld",
      "class": "com.netflix.asgard.ApplicationInstance",
      "dataCenterInfo": {
        "availability-zone": "us-east-1c",
        "public-ipv4": "107.22.37.83",
        "instance-id": "i-980a32f8",
        "public-hostname": "ec2-107-22-37-83.compute-1.amazonaws.com",
        "local-ipv4": "10.111.67.218",
        "ami-id": "ami-b3579ada",
        "instance-type": "m1.small"
      },
      "healthCheckUrl": "http://ec2-107-22-37-83.compute-1.amazonaws.com:7001/healthcheck",
      "hostName": "ec2-107-22-37-83.compute-1.amazonaws.com",
      "instanceId": "i-980a32f8",
      "ipAddr": "10.111.67.218",
      "leaseInfo": {
        "renewalIntervalInSecs": "30",
        "durationInSecs": "90",
        "registrationTimestamp": "2011-10-21T14:00:55.468-07:00",
        "lastRenewalTimestamp": "2011-10-22T23:31:38.515-07:00",
        "evictionTimestamp": "0",
        "clock": "24497"
      },
      "metadata": {
      },
      "port": "7001",
      "securePort": "7002",
      "status": "UP",
      "statusPageUrl": "http://ec2-107-22-37-83.compute-1.amazonaws.com:7001/Status",
      "version": "1.1",
      "vipAddress": "ec2-107-22-37-83.compute-1.amazonaws.com:7001"
    },
    "appName": "helloworld",
    "autoScalingGroupName": null,
    "class": "com.netflix.asgard.MergedInstance",
    "ec2Instance": {
      "amiLaunchIndex": 0,
      "architecture": "i386",
      "blockDeviceMappings":
      [
        {
          "class": "com.amazonaws.services.ec2.model.InstanceBlockDeviceMapping",
          "deviceName": "/dev/sda1",
          "ebs": {
            "attachTime": "2011-10-14T21:59:19Z",
            "class": "com.amazonaws.services.ec2.model.EbsInstanceBlockDevice",
            "deleteOnTermination": true,
            "status": "attached",
            "volumeId": "vol-b3d3e2d9"
          }
        }
      ],
      "class": "com.amazonaws.services.ec2.model.Instance",
      "clientToken": "",
      "hypervisor": "xen",
      "imageId": "ami-b3579ada",
      "instanceId": "i-980a32f8",
      "instanceLifecycle": null,
      "instanceType": "m1.small",
      "kernelId": "aki-1c669375",
      "keyName": "nf-test-keypair-a",
      "launchTime": "2011-10-14T21:58:46Z",
      "license": null,
      "monitoring": {
        "class": "com.amazonaws.services.ec2.model.Monitoring",
        "state": "disabled"
      },
      "placement": {
        "availabilityZone": "us-east-1c",
        "class": "com.amazonaws.services.ec2.model.Placement",
        "groupName": "",
        "tenancy": "default"
      },
      "platform": null,
      "privateDnsName": "ip-10-111-67-218.ec2.internal",
      "privateIpAddress": "10.111.67.218",
      "productCodes":
      [
      ],
      "publicDnsName": "ec2-107-22-37-83.compute-1.amazonaws.com",
      "publicIpAddress": "107.22.37.83",
      "ramdiskId": "ari-0ccd3965",
      "rootDeviceName": "/dev/sda1",
      "rootDeviceType": "ebs",
      "securityGroups":
      [
        {
          "class": "com.amazonaws.services.ec2.model.GroupIdentifier",
          "groupId": "sg-7fcd0716",
          "groupName": "nf-infrastructure"
        },
        {
          "class": "com.amazonaws.services.ec2.model.GroupIdentifier",
          "groupId": "sg-b7f73ade",
          "groupName": "helloworld"
        },
        {
          "class": "com.amazonaws.services.ec2.model.GroupIdentifier",
          "groupId": "sg-8bf73ae2",
          "groupName": "helloworld-frontend"
        },
        {
          "class": "com.amazonaws.services.ec2.model.GroupIdentifier",
          "groupId": "sg-3e49b257",
          "groupName": "nf-datacenter"
        }
      ],
      "sourceDestCheck": null,
      "spotInstanceRequestId": null,
      "state": {
        "class": "com.amazonaws.services.ec2.model.InstanceState",
        "code": 16,
        "name": "running"
      },
      "stateReason": null,
      "stateTransitionReason": "",
      "subnetId": null,
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "app",
          "value": "helloworld"
        },
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "pkg",
          "value": "helloworld-1.4.0-1083825.h397"
        },
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "owner",
          "value": "kgrossoehme"
        },
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "env",
          "value": "test"
        }
      ],
      "virtualizationType": "paravirtual",
      "vpcId": null
    },
    "hostName": "ec2-107-22-37-83.compute-1.amazonaws.com",
    "instanceId": "i-980a32f8",
    "instanceType": "m1.small",
    "launchTime": "2011-10-14T21:58:46Z",
    "port": "7001",
    "status": "running",
    "version": "1.1",
    "vipAddress": "ec2-107-22-37-83.compute-1.amazonaws.com:7001",
    "zone": "us-east-1c"
  },
  {
    "amiId": "ami-4775b32e",
    "appInstance": {
      "appName": "helloworld",
      "class": "com.netflix.asgard.ApplicationInstance",
      "dataCenterInfo": {
        "availability-zone": "us-east-1a",
        "public-ipv4": "50.16.106.171",
        "instance-id": "i-8ee4eeee",
        "public-hostname": "ec2-50-16-106-171.compute-1.amazonaws.com",
        "local-ipv4": "10.124.43.12",
        "ami-id": "ami-4775b32e",
        "instance-type": "m1.large"
      },
      "healthCheckUrl": "http://ec2-50-16-106-171.compute-1.amazonaws.com:7001/healthcheck",
      "hostName": "ec2-50-16-106-171.compute-1.amazonaws.com",
      "instanceId": "i-8ee4eeee",
      "ipAddr": "10.124.43.12",
      "leaseInfo": {
        "renewalIntervalInSecs": "30",
        "durationInSecs": "90",
        "registrationTimestamp": "2011-10-21T14:00:55.473-07:00",
        "lastRenewalTimestamp": "2011-10-22T23:31:17.855-07:00",
        "evictionTimestamp": "0",
        "clock": "15316"
      },
      "metadata": {
      },
      "port": "7001",
      "securePort": "7002",
      "status": "UP",
      "statusPageUrl": "http://ec2-50-16-106-171.compute-1.amazonaws.com:7001/Status",
      "version": "1.1",
      "vipAddress": "ec2-50-16-106-171.compute-1.amazonaws.com:7001"
    },
    "appName": "helloworld",
    "autoScalingGroupName": "helloworld-example-v015",
    "class": "com.netflix.asgard.MergedInstance",
    "ec2Instance": {
      "amiLaunchIndex": 0,
      "architecture": "x86_64",
      "blockDeviceMappings":
      [
        {
          "class": "com.amazonaws.services.ec2.model.InstanceBlockDeviceMapping",
          "deviceName": "/dev/sda1",
          "ebs": {
            "attachTime": "2011-10-18T17:45:46Z",
            "class": "com.amazonaws.services.ec2.model.EbsInstanceBlockDevice",
            "deleteOnTermination": true,
            "status": "attached",
            "volumeId": "vol-3f003b55"
          }
        }
      ],
      "class": "com.amazonaws.services.ec2.model.Instance",
      "clientToken": "37cd301a-1618-4702-ac51-ce5ca098bdf1",
      "hypervisor": "xen",
      "imageId": "ami-4775b32e",
      "instanceId": "i-8ee4eeee",
      "instanceLifecycle": null,
      "instanceType": "m1.large",
      "kernelId": "aki-460bfe2f",
      "keyName": "nf-test-keypair-a",
      "launchTime": "2011-10-18T17:45:11Z",
      "license": null,
      "monitoring": {
        "class": "com.amazonaws.services.ec2.model.Monitoring",
        "state": "enabled"
      },
      "placement": {
        "availabilityZone": "us-east-1a",
        "class": "com.amazonaws.services.ec2.model.Placement",
        "groupName": "",
        "tenancy": "default"
      },
      "platform": null,
      "privateDnsName": "ip-10-124-43-12.ec2.internal",
      "privateIpAddress": "10.124.43.12",
      "productCodes":
      [
      ],
      "publicDnsName": "ec2-50-16-106-171.compute-1.amazonaws.com",
      "publicIpAddress": "50.16.106.171",
      "ramdiskId": "ari-580bfe31",
      "rootDeviceName": "/dev/sda1",
      "rootDeviceType": "ebs",
      "securityGroups":
      [
        {
          "class": "com.amazonaws.services.ec2.model.GroupIdentifier",
          "groupId": "sg-7fcd0716",
          "groupName": "nf-infrastructure"
        },
        {
          "class": "com.amazonaws.services.ec2.model.GroupIdentifier",
          "groupId": "sg-b7f73ade",
          "groupName": "helloworld"
        },
        {
          "class": "com.amazonaws.services.ec2.model.GroupIdentifier",
          "groupId": "sg-8bf73ae2",
          "groupName": "helloworld-frontend"
        },
        {
          "class": "com.amazonaws.services.ec2.model.GroupIdentifier",
          "groupId": "sg-e87caf81",
          "groupName": "helloworld-asgardtest"
        },
        {
          "class": "com.amazonaws.services.ec2.model.GroupIdentifier",
          "groupId": "sg-3e49b257",
          "groupName": "nf-datacenter"
        }
      ],
      "sourceDestCheck": null,
      "spotInstanceRequestId": null,
      "state": {
        "class": "com.amazonaws.services.ec2.model.InstanceState",
        "code": 16,
        "name": "running"
      },
      "stateReason": null,
      "stateTransitionReason": "",
      "subnetId": null,
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "elb_deregistered_time",
          "value": "2011-10-18 15:23:58 PDT"
        },
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "aws:autoscaling:groupName",
          "value": "helloworld-example-v015"
        }
      ],
      "virtualizationType": "paravirtual",
      "vpcId": null
    },
    "hostName": "ec2-50-16-106-171.compute-1.amazonaws.com",
    "instanceId": "i-8ee4eeee",
    "instanceType": "m1.large",
    "launchTime": "2011-10-18T17:45:11Z",
    "port": "7001",
    "status": "running",
    "version": "1.1",
    "vipAddress": "ec2-50-16-106-171.compute-1.amazonaws.com:7001",
    "zone": "us-east-1a"
  },
  {
    "amiId": "ami-10b84079",
    "appInstance": {
      "appName": "helloworld",
      "class": "com.netflix.asgard.ApplicationInstance",
      "dataCenterInfo": {
        "availability-zone": "us-east-1c",
        "public-ipv4": "72.44.63.137",
        "instance-id": "i-27feb949",
        "public-hostname": "ec2-72-44-63-137.compute-1.amazonaws.com",
        "local-ipv4": "10.192.223.114",
        "ami-id": "ami-10b84079",
        "instance-type": "m1.small"
      },
      "healthCheckUrl": "http://ec2-72-44-63-137.compute-1.amazonaws.com:7001/healthcheck",
      "hostName": "ec2-72-44-63-137.compute-1.amazonaws.com",
      "instanceId": "i-27feb949",
      "ipAddr": "10.192.223.114",
      "leaseInfo": {
        "renewalIntervalInSecs": "30",
        "durationInSecs": "90",
        "registrationTimestamp": "2011-10-21T14:00:55.463-07:00",
        "lastRenewalTimestamp": "2011-10-22T23:31:19.338-07:00",
        "evictionTimestamp": "0",
        "clock": "370418"
      },
      "metadata": {
      },
      "port": "7001",
      "securePort": "7002",
      "status": "UP",
      "statusPageUrl": "http://ec2-72-44-63-137.compute-1.amazonaws.com:7001/Status",
      "version": "1.1",
      "vipAddress": "ec2-72-44-63-137.compute-1.amazonaws.com:7001"
    },
    "appName": "helloworld",
    "autoScalingGroupName": null,
    "class": "com.netflix.asgard.MergedInstance",
    "ec2Instance": {
      "amiLaunchIndex": 0,
      "architecture": "i386",
      "blockDeviceMappings":
      [
        {
          "class": "com.amazonaws.services.ec2.model.InstanceBlockDeviceMapping",
          "deviceName": "/dev/sda1",
          "ebs": {
            "attachTime": "2011-06-16T15:59:34Z",
            "class": "com.amazonaws.services.ec2.model.EbsInstanceBlockDevice",
            "deleteOnTermination": true,
            "status": "attached",
            "volumeId": "vol-4190e02a"
          }
        }
      ],
      "class": "com.amazonaws.services.ec2.model.Instance",
      "clientToken": "",
      "hypervisor": "xen",
      "imageId": "ami-10b84079",
      "instanceId": "i-27feb949",
      "instanceLifecycle": null,
      "instanceType": "m1.small",
      "kernelId": "aki-1c669375",
      "keyName": "nf-test-keypair-a",
      "launchTime": "2011-06-16T15:59:03Z",
      "license": null,
      "monitoring": {
        "class": "com.amazonaws.services.ec2.model.Monitoring",
        "state": "disabled"
      },
      "placement": {
        "availabilityZone": "us-east-1c",
        "class": "com.amazonaws.services.ec2.model.Placement",
        "groupName": "",
        "tenancy": "default"
      },
      "platform": null,
      "privateDnsName": "domU-12-31-39-0E-DC-84.compute-1.internal",
      "privateIpAddress": "10.192.223.114",
      "productCodes":
      [
      ],
      "publicDnsName": "ec2-72-44-63-137.compute-1.amazonaws.com",
      "publicIpAddress": "72.44.63.137",
      "ramdiskId": "ari-0ccd3965",
      "rootDeviceName": "/dev/sda1",
      "rootDeviceType": "ebs",
      "securityGroups":
      [
        {
          "class": "com.amazonaws.services.ec2.model.GroupIdentifier",
          "groupId": "sg-7fcd0716",
          "groupName": "nf-infrastructure"
        },
        {
          "class": "com.amazonaws.services.ec2.model.GroupIdentifier",
          "groupId": "sg-b7f73ade",
          "groupName": "helloworld"
        },
        {
          "class": "com.amazonaws.services.ec2.model.GroupIdentifier",
          "groupId": "sg-8bf73ae2",
          "groupName": "helloworld-frontend"
        },
        {
          "class": "com.amazonaws.services.ec2.model.GroupIdentifier",
          "groupId": "sg-3e49b257",
          "groupName": "nf-datacenter"
        }
      ],
      "sourceDestCheck": null,
      "spotInstanceRequestId": null,
      "state": {
        "class": "com.amazonaws.services.ec2.model.InstanceState",
        "code": 16,
        "name": "running"
      },
      "stateReason": null,
      "stateTransitionReason": "",
      "subnetId": null,
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "env",
          "value": "test"
        },
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "pkg",
          "value": "helloworld"
        },
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "app",
          "value": "helloworld"
        },
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "owner",
          "value": "abakthavatchalam"
        }
      ],
      "virtualizationType": "paravirtual",
      "vpcId": null
    },
    "hostName": "ec2-72-44-63-137.compute-1.amazonaws.com",
    "instanceId": "i-27feb949",
    "instanceType": "m1.small",
    "launchTime": "2011-06-16T15:59:03Z",
    "port": "7001",
    "status": "running",
    "version": "1.1",
    "vipAddress": "ec2-72-44-63-137.compute-1.amazonaws.com:7001",
    "zone": "us-east-1c"
  },
  {
    "amiId": null,
    "appInstance": {
      "appName": "ntsuiboot",
      "class": "com.netflix.asgard.ApplicationInstance",
      "dataCenterInfo": null,
      "healthCheckUrl": "http://ntsuiboot-i-f26f7a92:7001/healthcheck",
      "hostName": "ntsuiboot-i-f26f7a92",
      "instanceId": null,
      "ipAddr": "10.243.21.172",
      "leaseInfo": {
        "renewalIntervalInSecs": "30",
        "durationInSecs": "90",
        "registrationTimestamp": "2011-10-21T15:46:42.135-07:00",
        "lastRenewalTimestamp": "2011-10-22T23:31:41.095-07:00",
        "evictionTimestamp": "0",
        "clock": "3810"
      },
      "metadata": {
      },
      "port": "7001",
      "securePort": "7002",
      "status": "UP",
      "statusPageUrl": "http://ntsuiboot-i-f26f7a92:7001/Status",
      "version": "v1.0",
      "vipAddress": "ntsuiboot,ntsuiboot:7001"
    },
    "appName": "ntsuiboot",
    "autoScalingGroupName": null,
    "class": "com.netflix.asgard.MergedInstance",
    "ec2Instance": null,
    "hostName": "ntsuiboot-i-f26f7a92",
    "instanceId": null,
    "instanceType": null,
    "launchTime": null,
    "port": "7001",
    "status": "UP",
    "version": "v1.0",
    "vipAddress": "ntsuiboot,ntsuiboot:7001",
    "zone": null
  },
  {
    "amiId": "ami-dfdd1ab6",
    "appInstance": {
      "appName": "ntsuiboot",
      "class": "com.netflix.asgard.ApplicationInstance",
      "dataCenterInfo": {
        "availability-zone": "us-east-1d",
        "instance-id": "i-67b86a06",
        "public-ipv4": "50.17.172.64",
        "public-hostname": "ec2-50-17-172-64.compute-1.amazonaws.com",
        "local-ipv4": "10.116.247.53",
        "ami-id": "ami-dfdd1ab6",
        "instance-type": "m1.large"
      },
      "healthCheckUrl": "http://ec2-50-17-172-64.compute-1.amazonaws.com:7001/healthcheck",
      "hostName": "ec2-50-17-172-64.compute-1.amazonaws.com",
      "instanceId": "i-67b86a06",
      "ipAddr": "10.116.247.53",
      "leaseInfo": {
        "renewalIntervalInSecs": "30",
        "durationInSecs": "90",
        "registrationTimestamp": "2011-10-21T14:00:54.511-07:00",
        "lastRenewalTimestamp": "2011-10-22T23:31:23.915-07:00",
        "evictionTimestamp": "0",
        "clock": "269700"
      },
      "metadata": {
      },
      "port": "7001",
      "securePort": "7002",
      "status": "OUT_OF_SERVICE",
      "statusPageUrl": "http://ec2-50-17-172-64.compute-1.amazonaws.com:7001/Status",
      "version": "v1.0",
      "vipAddress": "ec2-50-17-172-64.compute-1.amazonaws.com:7001"
    },
    "appName": "ntsuiboot",
    "autoScalingGroupName": "ntsuiboot-v000",
    "class": "com.netflix.asgard.MergedInstance",
    "ec2Instance": {
      "amiLaunchIndex": 0,
      "architecture": "x86_64",
      "blockDeviceMappings":
      [
        {
          "class": "com.amazonaws.services.ec2.model.InstanceBlockDeviceMapping",
          "deviceName": "/dev/sda1",
          "ebs": {
            "attachTime": "2011-07-21T17:12:00Z",
            "class": "com.amazonaws.services.ec2.model.EbsInstanceBlockDevice",
            "deleteOnTermination": true,
            "status": "attached",
            "volumeId": "vol-bf6567d4"
          }
        }
      ],
      "class": "com.amazonaws.services.ec2.model.Instance",
      "clientToken": "c455e06b-79e4-423c-8fbf-fcf3c11b6113",
      "hypervisor": "xen",
      "imageId": "ami-dfdd1ab6",
      "instanceId": "i-67b86a06",
      "instanceLifecycle": null,
      "instanceType": "m1.large",
      "kernelId": "aki-460bfe2f",
      "keyName": "nf-test-keypair-a",
      "launchTime": "2011-07-21T17:11:31Z",
      "license": null,
      "monitoring": {
        "class": "com.amazonaws.services.ec2.model.Monitoring",
        "state": "enabled"
      },
      "placement": {
        "availabilityZone": "us-east-1d",
        "class": "com.amazonaws.services.ec2.model.Placement",
        "groupName": "",
        "tenancy": "default"
      },
      "platform": null,
      "privateDnsName": "ip-10-116-247-53.ec2.internal",
      "privateIpAddress": "10.116.247.53",
      "productCodes":
      [
      ],
      "publicDnsName": "ec2-50-17-172-64.compute-1.amazonaws.com",
      "publicIpAddress": "50.17.172.64",
      "ramdiskId": "ari-580bfe31",
      "rootDeviceName": "/dev/sda1",
      "rootDeviceType": "ebs",
      "securityGroups":
      [
        {
          "class": "com.amazonaws.services.ec2.model.GroupIdentifier",
          "groupId": "sg-3ef0a057",
          "groupName": "ntsuiboot"
        },
        {
          "class": "com.amazonaws.services.ec2.model.GroupIdentifier",
          "groupId": "sg-7fcd0716",
          "groupName": "nf-infrastructure"
        },
        {
          "class": "com.amazonaws.services.ec2.model.GroupIdentifier",
          "groupId": "sg-3e49b257",
          "groupName": "nf-datacenter"
        }
      ],
      "sourceDestCheck": null,
      "spotInstanceRequestId": null,
      "state": {
        "class": "com.amazonaws.services.ec2.model.InstanceState",
        "code": 16,
        "name": "running"
      },
      "stateReason": null,
      "stateTransitionReason": "",
      "subnetId": null,
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "elb_deregistered_time",
          "value": "2011-10-18 15:25:04 PDT"
        },
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "aws:autoscaling:groupName",
          "value": "ntsuiboot-v000"
        }
      ],
      "virtualizationType": "paravirtual",
      "vpcId": null
    },
    "hostName": "ec2-50-17-172-64.compute-1.amazonaws.com",
    "instanceId": "i-67b86a06",
    "instanceType": "m1.large",
    "launchTime": "2011-07-21T17:11:31Z",
    "port": "7001",
    "status": "running",
    "version": "v1.0",
    "vipAddress": "ec2-50-17-172-64.compute-1.amazonaws.com:7001",
    "zone": "us-east-1d"
  }
]
'''
}
