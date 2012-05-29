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

class MockApplications {

    // http://localhost:8080/us-east-1/application/list/akms,helloworld,ntsuiboot.json
    static final String DATA = '''
[
  {
    "class": "com.netflix.asgard.AppRegistration",
    "createTime": "2011-04-15T03:08:14Z",
    "description": "Amazon Key Management Service",
    "email": "rstover@company.com",
    "monitorBucketType": {
      "enumType": "com.netflix.asgard.model.MonitorBucketType",
      "name": "application"
    },
    "name": "akms",
    "owner": "Russel Stover",
    "type": "Web Service",
    "updateTime": "2011-04-15T03:08:14Z"
  },
  {
    "class": "com.netflix.asgard.AppRegistration",
    "createTime": "2010-03-31T02:48:32Z",
    "description": "Hello World sample cloud project.",
    "email": "vwilliams@company.com",
    "monitorBucketType": {
      "enumType": "com.netflix.asgard.model.MonitorBucketType",
      "name": "cluster"
    },
    "name": "helloworld",
    "owner": "Vanessa Williams",
    "type": "Web Service",
    "updateTime": "2011-08-22T17:43:58Z"
  },
  {
    "class": "com.netflix.asgard.AppRegistration",
    "createTime": "2011-07-08T00:24:35Z",
    "description": "uiboot for NTS",
    "email": "wsonoma@company.com",
    "monitorBucketType": {
      "enumType": "com.netflix.asgard.model.MonitorBucketType",
      "name": "application"
    },
    "name": "ntsuiboot",
    "owner": "NTS Team",
    "type": "Web Service",
    "updateTime": "2011-07-08T00:24:35Z"
  }
]
'''
}
