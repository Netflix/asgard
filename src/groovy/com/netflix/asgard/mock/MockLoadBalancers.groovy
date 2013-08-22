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

class MockLoadBalancers {

    static final String DATA = '''
[
  {
    "DNSName": "helloworld--frontend-1444444444.us-east-1.elb.amazonaws.com",
    "VPCId": null,
    "availabilityZones":
    [
      "us-east-1a",
      "us-east-1c",
      "us-east-1d"
    ],
    "backendServerDescriptions":
    [
    ],
    "canonicalHostedZoneName": "helloworld--frontend-1444444444.us-east-1.elb.amazonaws.com",
    "canonicalHostedZoneNameID": "EQO5M30Q23N41P",
    "class": "com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription",
    "createdTime": "2010-11-08T21:50:33Z",
    "healthCheck": {
      "class": "com.amazonaws.services.elasticloadbalancing.model.HealthCheck",
      "healthyThreshold": 10,
      "interval": 10,
      "target": "HTTP:7001/healthcheck",
      "timeout": 5,
      "unhealthyThreshold": 2
    },
    "instances":
    [
      {
        "class": "com.amazonaws.services.elasticloadbalancing.model.Instance",
        "instanceId": "i-0b1c5b6a"
      },
      {
        "class": "com.amazonaws.services.elasticloadbalancing.model.Instance",
        "instanceId": "i-66ba8806"
      },
      {
        "class": "com.amazonaws.services.elasticloadbalancing.model.Instance",
        "instanceId": "i-8ee4eeee"
      },
      {
        "class": "com.amazonaws.services.elasticloadbalancing.model.Instance",
        "instanceId": "i-6ef9f30e"
      }
    ],
    "listenerDescriptions":
    [
      {
        "class": "com.amazonaws.services.elasticloadbalancing.model.ListenerDescription",
        "listener": {
          "SSLCertificateId": null,
          "class": "com.amazonaws.services.elasticloadbalancing.model.Listener",
          "instancePort": 7001,
          "instanceProtocol": "HTTP",
          "loadBalancerPort": 80,
          "protocol": "HTTP"
        },
        "policyNames":
        [
        ]
      }
    ],
    "loadBalancerName": "helloworld--frontend",
    "policies": {
      "LBCookieStickinessPolicies":
      [
      ],
      "appCookieStickinessPolicies":
      [
      ],
      "class": "com.amazonaws.services.elasticloadbalancing.model.Policies",
      "otherPolicies":
      [
      ]
    },
    "scheme": "internet-facing",
    "securityGroups":
    [
    ],
    "sourceSecurityGroup": {
      "class": "com.amazonaws.services.elasticloadbalancing.model.SourceSecurityGroup",
      "groupName": "amazon-elb-sg",
      "ownerAlias": "amazon-elb"
    },
    "subnets":
    [
    ]
  },
  {
    "DNSName": "ntsuiboot--frontend-2111111111.us-east-1.elb.amazonaws.com",
    "VPCId": null,
    "availabilityZones":
    [
      "us-east-1d"
    ],
    "backendServerDescriptions":
    [
    ],
    "canonicalHostedZoneName": "ntsuiboot--frontend-2111111111.us-east-1.elb.amazonaws.com",
    "canonicalHostedZoneNameID": "EQO5M30Q23N41P",
    "class": "com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription",
    "createdTime": "2011-07-08T00:26:16Z",
    "healthCheck": {
      "class": "com.amazonaws.services.elasticloadbalancing.model.HealthCheck",
      "healthyThreshold": 2,
      "interval": 60,
      "target": "HTTP:7001/healthcheck",
      "timeout": 5,
      "unhealthyThreshold": 2
    },
    "instances":
    [
      {
        "class": "com.amazonaws.services.elasticloadbalancing.model.Instance",
        "instanceId": "i-f26f7a92"
      }
    ],
    "listenerDescriptions":
    [
      {
        "class": "com.amazonaws.services.elasticloadbalancing.model.ListenerDescription",
        "listener": {
          "SSLCertificateId": null,
          "class": "com.amazonaws.services.elasticloadbalancing.model.Listener",
          "instancePort": 7001,
          "instanceProtocol": "HTTP",
          "loadBalancerPort": 80,
          "protocol": "HTTP"
        },
        "policyNames":
        [
        ]
      },
      {
        "class": "com.amazonaws.services.elasticloadbalancing.model.ListenerDescription",
        "listener": {
          "SSLCertificateId": "arn:aws:iam::179000000000:server-certificate/uiboot.company.com-CA2009",
          "class": "com.amazonaws.services.elasticloadbalancing.model.Listener",
          "instancePort": 7001,
          "instanceProtocol": "HTTP",
          "loadBalancerPort": 443,
          "protocol": "HTTPS"
        },
        "policyNames":
        [
        ]
      }
    ],
    "loadBalancerName": "ntsuiboot--frontend",
    "policies": {
      "LBCookieStickinessPolicies":
      [
      ],
      "appCookieStickinessPolicies":
      [
      ],
      "class": "com.amazonaws.services.elasticloadbalancing.model.Policies",
      "otherPolicies":
      [
        "SSLNegotiationPolicy",
        "CrossZoneLoadBalancingPolicy"
      ]
    },
    "scheme": "internet-facing",
    "securityGroups":
    [
    ],
    "sourceSecurityGroup": {
      "class": "com.amazonaws.services.elasticloadbalancing.model.SourceSecurityGroup",
      "groupName": "amazon-elb-sg",
      "ownerAlias": "amazon-elb"
    },
    "subnets":
    [
    ]
  }
]
'''
}
