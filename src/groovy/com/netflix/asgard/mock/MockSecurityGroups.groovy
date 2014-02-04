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

class MockSecurityGroups {

    static final String DATA = '''
[
  {
    "class": "com.amazonaws.services.ec2.model.SecurityGroup",
    "description": "Amazon Key Management Service",
    "groupId": "sg-8cb5d7e5",
    "groupName": "akms",
    "ipPermissions":
    [
      {
        "class": "com.amazonaws.services.ec2.model.IpPermission",
        "fromPort": 7102,
        "ipProtocol": "tcp",
        "ipRanges":
        [
          "184.72.105.251/32"
        ],
        "toPort": 7102,
        "userIdGroupPairs":
        [
          {
            "class": "com.amazonaws.services.ec2.model.UserIdGroupPair",
            "groupId": "sg-4eab0428",
            "groupName": "nf-infrastructure",
            "userId": "179000000000"
          }
        ]
      }
    ],
    "ipPermissionsEgress":
    [
    ],
    "ownerId": "179000000000",
    "tags":
    [
    ],
    "vpcId": null
  },
  {
    "class": "com.amazonaws.services.ec2.model.SecurityGroup",
    "description": "Hello World App",
    "groupId": "sg-b7f73ade",
    "groupName": "helloworld",
    "ipPermissions":
    [
    ],
    "ipPermissionsEgress":
    [
    ],
    "ownerId": "179000000000",
    "tags":
    [
    ],
    "vpcId": null
  },
  {
    "class": "com.amazonaws.services.ec2.model.SecurityGroup",
    "description": "Frontend for hello world app",
    "groupId": "sg-8bf73ae2",
    "groupName": "helloworld-frontend",
    "ipPermissions":
    [
      {
        "class": "com.amazonaws.services.ec2.model.IpPermission",
        "fromPort": 7001,
        "ipProtocol": "tcp",
        "ipRanges":
        [
          "10.0.0.0/8"
        ],
        "toPort": 7001,
        "userIdGroupPairs":
        [
        ]
      }
    ],
    "ipPermissionsEgress":
    [
    ],
    "ownerId": "179000000000",
    "tags":
    [
    ],
    "vpcId": null
  },
  {
    "class": "com.amazonaws.services.ec2.model.SecurityGroup",
    "description": "For Asgard testing.",
    "groupId": "sg-e87caf81",
    "groupName": "helloworld-asgardtest",
    "ipPermissions":
    [
      {
        "class": "com.amazonaws.services.ec2.model.IpPermission",
        "fromPort": 48000,
        "ipProtocol": "tcp",
        "ipRanges":
        [
        ],
        "toPort": 48025,
        "userIdGroupPairs":
        [
          {
            "class": "com.amazonaws.services.ec2.model.UserIdGroupPair",
            "groupId": "sg-e1804688",
            "groupName": "nimsoft",
            "userId": "179000000000"
          }
        ]
      },
      {
        "class": "com.amazonaws.services.ec2.model.IpPermission",
        "fromPort": 7001,
        "ipProtocol": "tcp",
        "ipRanges":
        [
        ],
        "toPort": 7001,
        "userIdGroupPairs":
        [
          {
            "class": "com.amazonaws.services.ec2.model.UserIdGroupPair",
            "groupId": "sg-541fce3d",
            "groupName": "account_batch",
            "userId": "179000000000"
          },
          {
            "class": "com.amazonaws.services.ec2.model.UserIdGroupPair",
            "groupId": "sg-0486596d",
            "groupName": "abcache",
            "userId": "179000000000"
          }
        ]
      }
    ],
    "ipPermissionsEgress":
    [
    ],
    "ownerId": "179000000000",
    "tags":
    [
    ],
    "vpcId": null
  },
  {
    "class": "com.amazonaws.services.ec2.model.SecurityGroup",
    "description": "Temp sec group for Asgard testing",
    "groupId": "sg-08c76d61",
    "groupName": "helloworld-tmp",
    "ipPermissions":
    [
    ],
    "ipPermissionsEgress":
    [
    ],
    "ownerId": "179000000000",
    "tags":
    [
    ],
    "vpcId": null
  },
  {
    "class": "com.amazonaws.services.ec2.model.SecurityGroup",
    "description": "uiboot for NTS",
    "groupId": "sg-3ef0a057",
    "groupName": "ntsuiboot",
    "ipPermissions":
    [
      {
        "class": "com.amazonaws.services.ec2.model.IpPermission",
        "fromPort": 7001,
        "ipProtocol": "tcp",
        "ipRanges":
        [
        ],
        "toPort": 7001,
        "userIdGroupPairs":
        [
          {
            "class": "com.amazonaws.services.ec2.model.UserIdGroupPair",
            "groupId": "sg-843f59ed",
            "groupName": "amazon-elb-sg",
            "userId": "amazon-elb"
          },
          {
            "class": "com.amazonaws.services.ec2.model.UserIdGroupPair",
            "groupId": "sg-238aa74a",
            "groupName": "ntsjavaharness",
            "userId": "179000000000"
          },
          {
            "class": "com.amazonaws.services.ec2.model.UserIdGroupPair",
            "groupId": "sg-f71f2a9e",
            "groupName": "refappcloud",
            "userId": "179000000000"
          }
        ]
      }
    ],
    "ipPermissionsEgress":
    [
    ],
    "ownerId": "179000000000",
    "tags":
    [
    ],
    "vpcId": null
  }
]
'''
}
