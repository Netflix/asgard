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

class MockSnapshots {

    @SuppressWarnings('LineLength')
    static final String DATA = '''
[
  {
    "class": "com.amazonaws.services.ec2.model.Snapshot",
    "description": "name=helloworld-1.0.0-592112.h155_x86_64-201012232159,arch=x86_64,ancestor_name=ebs-centosbase-x86_64-20101124,ancestor_id=ami-7b4eb912,ancestor_snapshot=snap-af297fc5",
    "ownerAlias": null,
    "ownerId": "179000000000",
    "progress": "100%",
    "snapshotId": "snap-017eb46c",
    "startTime": "2010-12-24T05:59:39Z",
    "state": "completed",
    "tags":
    [
    ],
    "volumeId": "vol-4e409a26",
    "volumeSize": 10
  },
  {
    "class": "com.amazonaws.services.ec2.model.Snapshot",
    "description": "name=helloworld-1.0.0-592112.h154_x86_64-201012191352,arch=x86_64,ancestor_name=ebs-centosbase-x86_64-20101124,ancestor_id=ami-7b4eb912,ancestor_snapshot=snap-af297fc5",
    "ownerAlias": null,
    "ownerId": "179000000000",
    "progress": "100%",
    "snapshotId": "snap-0749b06a",
    "startTime": "2010-12-19T21:53:18Z",
    "state": "completed",
    "tags":
    [
    ],
    "volumeId": "vol-a27da2ca",
    "volumeSize": 10
  },
  {
    "class": "com.amazonaws.services.ec2.model.Snapshot",
    "description": "name=helloworld-1.4.0-1041348.h379-test0,arch=i386,ancestor_name=centosbase-i386-20110914-ebs,ancestor_id=ami-8ba163e2,ancestor_snapshot=snap-3e41725e",
    "ownerAlias": null,
    "ownerId": "179000000000",
    "progress": "100%",
    "snapshotId": "snap-077d9664",
    "startTime": "2011-09-22T18:30:06Z",
    "state": "completed",
    "tags":
    [
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "appversion",
        "value": "helloworld-1.4.0-1041348.h379/WE-WAPP-helloworld/379"
      }
    ],
    "volumeId": "vol-6e1b6504",
    "volumeSize": 10
  },
  {
    "class": "com.amazonaws.services.ec2.model.Snapshot",
    "description": "name=helloworld-1.4.0-829980.h2-i386-201105232334,arch=i386,ancestor_name=ebs-centosbase-i386-20101124,ancestor_id=ami-6340b70a,ancestor_snapshot=snap-d96334b3",
    "ownerAlias": null,
    "ownerId": "179000000000",
    "progress": "100%",
    "snapshotId": "snap-0b617864",
    "startTime": "2011-05-23T23:34:47Z",
    "state": "completed",
    "tags":
    [
    ],
    "volumeId": "vol-ab64d7c0",
    "volumeSize": 10
  },
  {
    "class": "com.amazonaws.services.ec2.model.Snapshot",
    "description": "name=helloworld-1.0.0-535075_x86_64-201011221324,arch=x86_64,ancestor_name=ebs-centosbase-x86_64-20101122,ancestor_id=ami-715ea918,ancestor_snapshot=snap-e9e1a983",
    "ownerAlias": null,
    "ownerId": "179000000000",
    "progress": "100%",
    "snapshotId": "snap-11e6ae7b",
    "startTime": "2010-11-22T21:24:49Z",
    "state": "completed",
    "tags":
    [
    ],
    "volumeId": "vol-f9817191",
    "volumeSize": 10
  },
  {
    "class": "com.amazonaws.services.ec2.model.Snapshot",
    "description": "name=helloworld-1.4.0-1083825.h397-x86_64-201110150236,arch=x86_64,ancestor_name=centosbase-x86_64-20111004-ebs,ancestor_id=ami-bf569bd6,ancestor_snapshot=snap-4c2dad2f",
    "ownerAlias": null,
    "ownerId": "179000000000",
    "progress": "100%",
    "snapshotId": "snap-22561c41",
    "startTime": "2011-10-15T02:38:19Z",
    "state": "completed",
    "tags":
    [
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "appversion",
        "value": "helloworld-1.4.0-1083825.h397/WE-WAPP-helloworld/397"
      }
    ],
    "volumeId": "vol-af6c5cc5",
    "volumeSize": 10
  },
  {
    "class": "com.amazonaws.services.ec2.model.Snapshot",
    "description": "name=helloworld-1.0.0-624032.h174_x86_64-201101251700,arch=x86_64,ancestor_name=ebs-centosbase-x86_64-20101124,ancestor_id=ami-7b4eb912,ancestor_snapshot=",
    "ownerAlias": null,
    "ownerId": "179000000000",
    "progress": "100%",
    "snapshotId": "snap-2d1a4340",
    "startTime": "2011-01-26T01:01:19Z",
    "state": "completed",
    "tags":
    [
    ],
    "volumeId": "vol-3c77f954",
    "volumeSize": 10
  },
  {
    "class": "com.amazonaws.services.ec2.model.Snapshot",
    "description": "name=helloworld-1.0.0-624032.h173_x86_64-201101171821,arch=x86_64,ancestor_name=ebs-centosbase-x86_64-20101124,ancestor_id=ami-7b4eb912,ancestor_snapshot=snap-af297fc5",
    "ownerAlias": null,
    "ownerId": "179000000000",
    "progress": "100%",
    "snapshotId": "snap-335f235e",
    "startTime": "2011-01-18T02:21:39Z",
    "state": "completed",
    "tags":
    [
    ],
    "volumeId": "vol-5cf17434",
    "volumeSize": 10
  },
  {
    "class": "com.amazonaws.services.ec2.model.Snapshot",
    "description": "name=helloworld-1.0.0-592112.h154_x86_64-201012180022,arch=x86_64,ancestor_name=ebs-centosbase-x86_64-20101124,ancestor_id=ami-7b4eb912,ancestor_snapshot=snap-af297fc5",
    "ownerAlias": null,
    "ownerId": "179000000000",
    "progress": "100%",
    "snapshotId": "snap-3733c05a",
    "startTime": "2010-12-18T08:22:47Z",
    "state": "completed",
    "tags":
    [
    ],
    "volumeId": "vol-3cc71b54",
    "volumeSize": 10
  },
  {
    "class": "com.amazonaws.services.ec2.model.Snapshot",
    "description": "name=ntsuiboot-1.0.0-1091191.h1-x86_64-201110181917,arch=x86_64,ancestor_name=centosbase-x86_64-20111004-ebs,ancestor_id=ami-bf569bd6,ancestor_snapshot=snap-4c2dad2f",
    "ownerAlias": null,
    "ownerId": "179000000000",
    "progress": "100%",
    "snapshotId": "snap-58ecc33b",
    "startTime": "2011-10-18T19:19:43Z",
    "state": "completed",
    "tags":
    [
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "appversion",
        "value": "ntsuiboot-1.0.0-1091191.h1"
      }
    ],
    "volumeId": "vol-7fd6ed15",
    "volumeSize": 10
  },
  {
    "class": "com.amazonaws.services.ec2.model.Snapshot",
    "description": "name=helloworld-1.4.0-898154.h61-x86_64-201106272151,arch=x86_64,ancestor_name=ebs-centosbase-x86_64-20110610,ancestor_id=ami-d646bfbf,ancestor_snapshot=snap-a4e265ca",
    "ownerAlias": null,
    "ownerId": "179000000000",
    "progress": "100%",
    "snapshotId": "snap-5c1e3332",
    "startTime": "2011-06-27T21:53:28Z",
    "state": "completed",
    "tags":
    [
    ],
    "volumeId": "vol-af5913c4",
    "volumeSize": 10
  },
  {
    "class": "com.amazonaws.services.ec2.model.Snapshot",
    "description": "name=helloworld-1.0.0-503690_x86_64-201010201415,arch=x86_64,ancestor_name=ebs-centosbase-x86_64-20101017,ancestor_id=ami-de22d6b7,ancestor_snapshot=snap-c7448aad",
    "ownerAlias": null,
    "ownerId": "179000000000",
    "progress": "100%",
    "snapshotId": "snap-5fc71735",
    "startTime": "2010-10-20T21:15:47Z",
    "state": "completed",
    "tags":
    [
    ],
    "volumeId": "vol-02f1f86b",
    "volumeSize": 10
  },
  {
    "class": "com.amazonaws.services.ec2.model.Snapshot",
    "description": "name=helloworld-1.4.0-829092.h20-x86_64-201105162334,arch=x86_64,ancestor_name=ebs-centosbase-x86_64-20101124,ancestor_id=ami-7b4eb912,ancestor_snapshot=snap-af297fc5",
    "ownerAlias": null,
    "ownerId": "179000000000",
    "progress": "100%",
    "snapshotId": "snap-633f0f0c",
    "startTime": "2011-05-16T23:35:13Z",
    "state": "completed",
    "tags":
    [
    ],
    "volumeId": "vol-b13290da",
    "volumeSize": 10
  },
  {
    "class": "com.amazonaws.services.ec2.model.Snapshot",
    "description": "name=helloworld-test-x86_64-20101215,arch=x86_64,ancestor_name=ebs-centosbase-x86_64-20101124,ancestor_id=ami-7b4eb912,ancestor_snapshot=snap-af297fc5",
    "ownerAlias": null,
    "ownerId": "179000000000",
    "progress": "100%",
    "snapshotId": "snap-6507ef08",
    "startTime": "2010-12-16T06:25:20Z",
    "state": "completed",
    "tags":
    [
    ],
    "volumeId": "vol-e45b888c",
    "volumeSize": 10
  },
  {
    "class": "com.amazonaws.services.ec2.model.Snapshot",
    "description": "name=helloworld-1.0.0-586499.h151_x86_64-201012151805,arch=x86_64,ancestor_name=ebs-centosbase-x86_64-20101124,ancestor_id=ami-7b4eb912,ancestor_snapshot=snap-af297fc5",
    "ownerAlias": null,
    "ownerId": "179000000000",
    "progress": "100%",
    "snapshotId": "snap-6f658c02",
    "startTime": "2010-12-16T02:05:41Z",
    "state": "completed",
    "tags":
    [
    ],
    "volumeId": "vol-d400d3bc",
    "volumeSize": 10
  },
  {
    "class": "com.amazonaws.services.ec2.model.Snapshot",
    "description": "name=helloworld-1.0.0-502435_x86_64-201010181437,arch=x86_64,ancestor_name=ebs-centosbase-x86_64-20101017,ancestor_id=ami-de22d6b7,ancestor_snapshot=snap-c7448aad",
    "ownerAlias": null,
    "ownerId": "179000000000",
    "progress": "100%",
    "snapshotId": "snap-7727ec1d",
    "startTime": "2010-10-18T21:38:18Z",
    "state": "completed",
    "tags":
    [
    ],
    "volumeId": "vol-5e626d37",
    "volumeSize": 10
  },
  {
    "class": "com.amazonaws.services.ec2.model.Snapshot",
    "description": "name=helloworld-1.0.0-570978.h147_x86_64-201012081809,arch=x86_64,ancestor_name=ebs-centosbase-x86_64-20101124,ancestor_id=ami-7b4eb912,ancestor_snapshot=snap-af297fc5",
    "ownerAlias": null,
    "ownerId": "179000000000",
    "progress": "100%",
    "snapshotId": "snap-7f3d3715",
    "startTime": "2010-12-09T02:09:55Z",
    "state": "completed",
    "tags":
    [
    ],
    "volumeId": "vol-85824ced",
    "volumeSize": 10
  },
  {
    "class": "com.amazonaws.services.ec2.model.Snapshot",
    "description": "name=helloworld-1.0.0-586499.h152_x86_64-201012151834,arch=x86_64,ancestor_name=ebs-centosbase-x86_64-20101124,ancestor_id=ami-7b4eb912,ancestor_snapshot=snap-af297fc5",
    "ownerAlias": null,
    "ownerId": "179000000000",
    "progress": "100%",
    "snapshotId": "snap-83739aee",
    "startTime": "2010-12-16T02:34:32Z",
    "state": "completed",
    "tags":
    [
    ],
    "volumeId": "vol-4e09da26",
    "volumeSize": 10
  },
  {
    "class": "com.amazonaws.services.ec2.model.Snapshot",
    "description": "name=helloworld-1.4.0-841258.h261-x86_64-201105241712,arch=x86_64,ancestor_name=ebs-centosbase-x86_64-20101124,ancestor_id=ami-7b4eb912,ancestor_snapshot=snap-af297fc5",
    "ownerAlias": null,
    "ownerId": "179000000000",
    "progress": "100%",
    "snapshotId": "snap-8a06e3e4",
    "startTime": "2011-05-24T17:13:11Z",
    "state": "completed",
    "tags":
    [
    ],
    "volumeId": "vol-5bf14330",
    "volumeSize": 10
  },
  {
    "class": "com.amazonaws.services.ec2.model.Snapshot",
    "description": "name=helloworld-1.4.0-829980.h2-i386-201105232051,arch=i386,ancestor_name=ebs-centosbase-i386-20101124,ancestor_id=ami-6340b70a,ancestor_snapshot=snap-d96334b3",
    "ownerAlias": null,
    "ownerId": "179000000000",
    "progress": "100%",
    "snapshotId": "snap-8ff5ebe0",
    "startTime": "2011-05-23T20:51:55Z",
    "state": "completed",
    "tags":
    [
    ],
    "volumeId": "vol-c9f65ea2",
    "volumeSize": 10
  },
  {
    "class": "com.amazonaws.services.ec2.model.Snapshot",
    "description": "name=helloworld-vvn-1.0.0-882109.h17-x86_64-201106171933,arch=x86_64,ancestor_name=ebs-centosbase-x86_64-20110610,ancestor_id=ami-d646bfbf,ancestor_snapshot=snap-a4e265ca",
    "ownerAlias": null,
    "ownerId": "179000000000",
    "progress": "100%",
    "snapshotId": "snap-902648fe",
    "startTime": "2011-06-17T19:35:11Z",
    "state": "completed",
    "tags":
    [
    ],
    "volumeId": "vol-e993ee82",
    "volumeSize": 10
  },
  {
    "class": "com.amazonaws.services.ec2.model.Snapshot",
    "description": "name=helloworld-1.4.0-964516.h13-x86_64-201108051930,arch=x86_64,ancestor_name=ebs-centosbase-x86_64-20110610,ancestor_id=ami-d646bfbf,ancestor_snapshot=snap-a4e265ca",
    "ownerAlias": null,
    "ownerId": "179000000000",
    "progress": "100%",
    "snapshotId": "snap-930d2af2",
    "startTime": "2011-08-05T19:32:08Z",
    "state": "completed",
    "tags":
    [
    ],
    "volumeId": "vol-e153568a",
    "volumeSize": 10
  },
  {
    "class": "com.amazonaws.services.ec2.model.Snapshot",
    "description": "name=helloworld-1.4.0-1030199.h375-x86_64-201109151949,arch=x86_64,ancestor_name=centosbase-x86_64-20110914-ebs,ancestor_id=ami-8fa163e6,ancestor_snapshot=snap-dc4172bc",
    "ownerAlias": null,
    "ownerId": "179000000000",
    "progress": "100%",
    "snapshotId": "snap-94a59df4",
    "startTime": "2011-09-15T19:50:39Z",
    "state": "completed",
    "tags":
    [
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "appversion",
        "value": "helloworld-1.4.0-1030199.h375/WE-WAPP-helloworld/375"
      }
    ],
    "volumeId": "vol-1c137e76",
    "volumeSize": 10
  },
  {
    "class": "com.amazonaws.services.ec2.model.Snapshot",
    "description": "name=helloworld-1.0.0-735764.h224-x86_64-201103181559,arch=x86_64,ancestor_name=ebs-centosbase-x86_64-20101124,ancestor_id=ami-7b4eb912,ancestor_snapshot=snap-af297fc5",
    "ownerAlias": null,
    "ownerId": "179000000000",
    "progress": "100%",
    "snapshotId": "snap-a996d0c5",
    "startTime": "2011-03-18T16:00:27Z",
    "state": "completed",
    "tags":
    [
    ],
    "volumeId": "vol-1747667f",
    "volumeSize": 10
  },
  {
    "class": "com.amazonaws.services.ec2.model.Snapshot",
    "description": "name=helloworld-1.0.0-741015.h229-x86_64-201103250106,arch=x86_64,ancestor_name=ebs-centosbase-x86_64-20101124,ancestor_id=ami-7b4eb912,ancestor_snapshot=snap-af297fc5",
    "ownerAlias": null,
    "ownerId": "179000000000",
    "progress": "100%",
    "snapshotId": "snap-ab7652c7",
    "startTime": "2011-03-25T01:06:17Z",
    "state": "completed",
    "tags":
    [
    ],
    "volumeId": "vol-b93d0cd1",
    "volumeSize": 10
  },
  {
    "class": "com.amazonaws.services.ec2.model.Snapshot",
    "description": "name=helloworld-1.4.0-829980.h258-x86_64-201105190016,arch=x86_64,ancestor_name=ebs-centosbase-x86_64-20101124,ancestor_id=ami-7b4eb912,ancestor_snapshot=snap-af297fc5",
    "ownerAlias": null,
    "ownerId": "179000000000",
    "progress": "100%",
    "snapshotId": "snap-b73431d8",
    "startTime": "2011-05-19T00:17:06Z",
    "state": "completed",
    "tags":
    [
    ],
    "volumeId": "vol-0f40e964",
    "volumeSize": 10
  },
  {
    "class": "com.amazonaws.services.ec2.model.Snapshot",
    "description": "name=helloworld-1.4.0-985478.h335-x86_64-201108182221,arch=x86_64,ancestor_name=ebs-centosbase-x86_64-20110610,ancestor_id=ami-d646bfbf,ancestor_snapshot=snap-a4e265ca",
    "ownerAlias": null,
    "ownerId": "179000000000",
    "progress": "100%",
    "snapshotId": "snap-ba8d72da",
    "startTime": "2011-08-18T22:22:46Z",
    "state": "completed",
    "tags":
    [
    ],
    "volumeId": "vol-eaf62180",
    "volumeSize": 10
  },
  {
    "class": "com.amazonaws.services.ec2.model.Snapshot",
    "description": "name=helloworld-1.0.0-624032.h174_x86_64-201101191002,arch=x86_64,ancestor_name=ebs-centosbase-x86_64-20101124,ancestor_id=ami-7b4eb912,ancestor_snapshot=",
    "ownerAlias": null,
    "ownerId": "179000000000",
    "progress": "100%",
    "snapshotId": "snap-bf5613d2",
    "startTime": "2011-01-19T18:03:36Z",
    "state": "completed",
    "tags":
    [
    ],
    "volumeId": "vol-8a2cabe2",
    "volumeSize": 10
  },
  {
    "class": "com.amazonaws.services.ec2.model.Snapshot",
    "description": "name=helloworld-1.0.0-503690_x86_64-201010190908,arch=x86_64,ancestor_name=ebs-centosbase-x86_64-20101017,ancestor_id=ami-de22d6b7,ancestor_snapshot=snap-c7448aad",
    "ownerAlias": null,
    "ownerId": "179000000000",
    "progress": "100%",
    "snapshotId": "snap-dba377b1",
    "startTime": "2010-10-19T16:09:05Z",
    "state": "completed",
    "tags":
    [
    ],
    "volumeId": "vol-287a7441",
    "volumeSize": 10
  },
  {
    "class": "com.amazonaws.services.ec2.model.Snapshot",
    "description": "name=ntsuiboot-1.0.0-939580.h3-x86_64-201107211703,arch=x86_64,ancestor_name=ebs-centosbase-x86_64-20110610,ancestor_id=ami-d646bfbf,ancestor_snapshot=snap-a4e265ca",
    "ownerAlias": null,
    "ownerId": "179000000000",
    "progress": "100%",
    "snapshotId": "snap-f3a21992",
    "startTime": "2011-07-21T17:06:03Z",
    "state": "completed",
    "tags":
    [
    ],
    "volumeId": "vol-bd1313d6",
    "volumeSize": 10
  }
]
'''
}
