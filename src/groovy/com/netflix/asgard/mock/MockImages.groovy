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

class MockImages {

    // http://localhost:8080/us-east-1/image/list/akms,helloworld,ntsuiboot.json
    @SuppressWarnings('LineLength')
    static final String DATA = '''
[
  {
    "architecture": "x86_64",
    "blockDeviceMappings":
    [
      {
        "class": "com.amazonaws.services.ec2.model.BlockDeviceMapping",
        "deviceName": "/dev/sda1",
        "ebs": {
          "class": "com.amazonaws.services.ec2.model.EbsBlockDevice",
          "deleteOnTermination": true,
          "snapshotId": "snap-2d1a4340",
          "volumeSize": 10
        },
        "noDevice": null,
        "virtualName": null
      }
    ],
    "class": "com.amazonaws.services.ec2.model.Image",
    "description": "ancestor_id=ami-7b4eb912,ancestor_name=ebs-centosbase-x86_64-20101124",
    "hypervisor": "xen",
    "imageId": "ami-8ceb1be5",
    "imageLocation": "179000000000/helloworld-1.0.0-624032.h174_x86_64-201101251700",
    "imageOwnerAlias": null,
    "imageType": "machine",
    "kernelId": "aki-460bfe2f",
    "name": "helloworld-1.0.0-624032.h174_x86_64-201101251700",
    "ownerId": "179000000000",
    "platform": null,
    "productCodes":
    [
    ],
    "public": false,
    "ramdiskId": "ari-580bfe31",
    "rootDeviceName": "/dev/sda1",
    "rootDeviceType": "ebs",
    "state": "available",
    "stateReason": null,
    "tags":
    [
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "last_referenced_time",
        "value": "2011-10-22 12:01:23 PDT"
      },
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "appversion",
        "value": "helloworld-1.0.0-624032.h174/WE-WAPP-helloworld/174"
      },
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "creation_time",
        "value": "2011-01-25 17:03:25 PST"
      },
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "creator",
        "value": "awstest"
      }
    ],
    "virtualizationType": "paravirtual"
  },
  {
    "architecture": "x86_64",
    "blockDeviceMappings":
    [
      {
        "class": "com.amazonaws.services.ec2.model.BlockDeviceMapping",
        "deviceName": "/dev/sda1",
        "ebs": {
          "class": "com.amazonaws.services.ec2.model.EbsBlockDevice",
          "deleteOnTermination": true,
          "snapshotId": "snap-94a59df4",
          "volumeSize": 10
        },
        "noDevice": null,
        "virtualName": null
      }
    ],
    "class": "com.amazonaws.services.ec2.model.Image",
    "description": "name=helloworld-1.4.0-1030199.h375-x86_64-201109151949-ebs,arch=x86_64,ancestor_name=centosbase-x86_64-20110914-ebs,ancestor_id=ami-8fa163e6,ancestor_version=nflx-base-1.1-1028909.h145",
    "hypervisor": "xen",
    "imageId": "ami-83bd7fea",
    "imageLocation": "179000000000/helloworld-1.4.0-1030199.h375-x86_64-201109151949-ebs",
    "imageOwnerAlias": null,
    "imageType": "machine",
    "kernelId": "aki-460bfe2f",
    "name": "helloworld-1.4.0-1030199.h375-x86_64-201109151949-ebs",
    "ownerId": "179000000000",
    "platform": null,
    "productCodes":
    [
    ],
    "public": false,
    "ramdiskId": "ari-580bfe31",
    "rootDeviceName": "/dev/sda1",
    "rootDeviceType": "ebs",
    "state": "available",
    "stateReason": null,
    "tags":
    [
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "last_referenced_time",
        "value": "2011-10-22 12:01:23 PDT"
      },
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "creation_time",
        "value": "2011-09-15 19:51:47 UTC"
      },
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "creator",
        "value": "jsmith"
      },
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "appversion",
        "value": "helloworld-1.4.0-1030199.h375/WE-WAPP-helloworld/375"
      },
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "base_ami_version",
        "value": "nflx-base-1.1-1028909.h145"
      }
    ],
    "virtualizationType": "paravirtual"
  },
  {
    "architecture": "i386",
    "blockDeviceMappings":
    [
      {
        "class": "com.amazonaws.services.ec2.model.BlockDeviceMapping",
        "deviceName": "/dev/sda1",
        "ebs": {
          "class": "com.amazonaws.services.ec2.model.EbsBlockDevice",
          "deleteOnTermination": true,
          "snapshotId": "snap-077d9664",
          "volumeSize": 10
        },
        "noDevice": null,
        "virtualName": null
      }
    ],
    "class": "com.amazonaws.services.ec2.model.Image",
    "description": "name=helloworld-1.4.0-1041348.h379-test0-ebs,arch=i386,ancestor_name=centosbase-i386-20110914-ebs,ancestor_id=ami-8ba163e2,ancestor_version=nflx-base-1.1-1028909.h145",
    "hypervisor": "xen",
    "imageId": "ami-875193ee",
    "imageLocation": "179000000000/helloworld-1.4.0-1041348.h379-test0-ebs",
    "imageOwnerAlias": null,
    "imageType": "machine",
    "kernelId": "aki-1c669375",
    "name": "helloworld-1.4.0-1041348.h379-test0-ebs",
    "ownerId": "179000000000",
    "platform": null,
    "productCodes":
    [
    ],
    "public": false,
    "ramdiskId": "ari-0ccd3965",
    "rootDeviceName": "/dev/sda1",
    "rootDeviceType": "ebs",
    "state": "available",
    "stateReason": null,
    "tags":
    [
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "appversion",
        "value": "helloworld-1.4.0-1041348.h379/WE-WAPP-helloworld/379"
      },
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "base_ami_version",
        "value": "nflx-base-1.1-1028909.h145"
      },
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "creator",
        "value": "mjones"
      },
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "creation_time",
        "value": "2011-09-22 18:31:35 UTC"
      }
    ],
    "virtualizationType": "paravirtual"
  },
  {
    "architecture": "x86_64",
    "blockDeviceMappings":
    [
      {
        "class": "com.amazonaws.services.ec2.model.BlockDeviceMapping",
        "deviceName": "/dev/sda1",
        "ebs": {
          "class": "com.amazonaws.services.ec2.model.EbsBlockDevice",
          "deleteOnTermination": true,
          "snapshotId": "snap-22561c41",
          "volumeSize": 10
        },
        "noDevice": null,
        "virtualName": null
      }
    ],
    "class": "com.amazonaws.services.ec2.model.Image",
    "description": "name=helloworld-1.4.0-1083825.h397-x86_64-201110150236-ebs,arch=x86_64,ancestor_name=centosbase-x86_64-20111004-ebs,ancestor_id=ami-bf569bd6,ancestor_version=nflx-base-1.1-1064993.h172",
    "hypervisor": "xen",
    "imageId": "ami-ed599584",
    "imageLocation": "179000000000/helloworld-1.4.0-1083825.h397-x86_64-201110150236-ebs",
    "imageOwnerAlias": null,
    "imageType": "machine",
    "kernelId": "aki-460bfe2f",
    "name": "helloworld-1.4.0-1083825.h397-x86_64-201110150236-ebs",
    "ownerId": "179000000000",
    "platform": null,
    "productCodes":
    [
    ],
    "public": false,
    "ramdiskId": "ari-580bfe31",
    "rootDeviceName": "/dev/sda1",
    "rootDeviceType": "ebs",
    "state": "available",
    "stateReason": null,
    "tags":
    [
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "last_referenced_time",
        "value": "2011-10-22 12:01:23 PDT"
      },
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "base_ami_version",
        "value": "nflx-base-1.1-1064993.h172"
      },
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "creation_time",
        "value": "2011-10-15 02:42:41 UTC"
      },
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "creator",
        "value": "ezbakery"
      },
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "appversion",
        "value": "helloworld-1.4.0-1083825.h397/WE-WAPP-helloworld/397"
      }
    ],
    "virtualizationType": "paravirtual"
  },
  {
    "architecture": "x86_64",
    "blockDeviceMappings":
    [
      {
        "class": "com.amazonaws.services.ec2.model.BlockDeviceMapping",
        "deviceName": "/dev/sda1",
        "ebs": {
          "class": "com.amazonaws.services.ec2.model.EbsBlockDevice",
          "deleteOnTermination": true,
          "snapshotId": "snap-633f0f0c",
          "volumeSize": 10
        },
        "noDevice": null,
        "virtualName": null
      }
    ],
    "class": "com.amazonaws.services.ec2.model.Image",
    "description": "name=helloworld-1.4.0-829092.h20-x86_64-201105162334,arch=x86_64,ancestor_name=ebs-centosbase-x86_64-20101124,ancestor_id=ami-7b4eb912",
    "hypervisor": "xen",
    "imageId": "ami-caed13a3",
    "imageLocation": "179000000000/helloworld-1.4.0-829092.h20-x86_64-201105162334",
    "imageOwnerAlias": null,
    "imageType": "machine",
    "kernelId": "aki-460bfe2f",
    "name": "helloworld-1.4.0-829092.h20-x86_64-201105162334",
    "ownerId": "179000000000",
    "platform": null,
    "productCodes":
    [
    ],
    "public": false,
    "ramdiskId": "ari-580bfe31",
    "rootDeviceName": "/dev/sda1",
    "rootDeviceType": "ebs",
    "state": "available",
    "stateReason": null,
    "tags":
    [
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "creation_time",
        "value": "2011-05-16 23:38:14 UTC"
      },
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "last_referenced_time",
        "value": "2011-10-22 12:01:23 PDT"
      },
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "appversion",
        "value": "helloworld-1.4.0-829092.h20"
      },
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "creator",
        "value": "bmiller"
      }
    ],
    "virtualizationType": "paravirtual"
  },
  {
    "architecture": "i386",
    "blockDeviceMappings":
    [
      {
        "class": "com.amazonaws.services.ec2.model.BlockDeviceMapping",
        "deviceName": "/dev/sda1",
        "ebs": {
          "class": "com.amazonaws.services.ec2.model.EbsBlockDevice",
          "deleteOnTermination": true,
          "snapshotId": "snap-8ff5ebe0",
          "volumeSize": 10
        },
        "noDevice": null,
        "virtualName": null
      }
    ],
    "class": "com.amazonaws.services.ec2.model.Image",
    "description": "name=helloworld-1.4.0-829980.h2-i386-201105232051,arch=i386,ancestor_name=ebs-centosbase-i386-20101124,ancestor_id=ami-6340b70a",
    "hypervisor": "xen",
    "imageId": "ami-147d837d",
    "imageLocation": "179000000000/helloworld-1.4.0-829980.h2-i386-201105232051",
    "imageOwnerAlias": null,
    "imageType": "machine",
    "kernelId": "aki-1c669375",
    "name": "helloworld-1.4.0-829980.h2-i386-201105232051",
    "ownerId": "179000000000",
    "platform": null,
    "productCodes":
    [
    ],
    "public": false,
    "ramdiskId": "ari-0ccd3965",
    "rootDeviceName": "/dev/sda1",
    "rootDeviceType": "ebs",
    "state": "available",
    "stateReason": null,
    "tags":
    [
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "creator",
        "value": "ezbakery"
      },
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "last_referenced_time",
        "value": "2011-08-18 00:00:49 PDT"
      },
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "appversion",
        "value": "helloworld-1.4.0-829980.h2"
      },
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "creation_time",
        "value": "2011-05-23 20:53:07 UTC"
      }
    ],
    "virtualizationType": "paravirtual"
  },
  {
    "architecture": "i386",
    "blockDeviceMappings":
    [
      {
        "class": "com.amazonaws.services.ec2.model.BlockDeviceMapping",
        "deviceName": "/dev/sda1",
        "ebs": {
          "class": "com.amazonaws.services.ec2.model.EbsBlockDevice",
          "deleteOnTermination": true,
          "snapshotId": "snap-0b617864",
          "volumeSize": 10
        },
        "noDevice": null,
        "virtualName": null
      }
    ],
    "class": "com.amazonaws.services.ec2.model.Image",
    "description": "name=helloworld-1.4.0-829980.h2-i386-201105232334,arch=i386,ancestor_name=ebs-centosbase-i386-20101124,ancestor_id=ami-6340b70a",
    "hypervisor": "xen",
    "imageId": "ami-40788629",
    "imageLocation": "179000000000/helloworld-1.4.0-829980.h2-i386-201105232334",
    "imageOwnerAlias": null,
    "imageType": "machine",
    "kernelId": "aki-1c669375",
    "name": "helloworld-1.4.0-829980.h2-i386-201105232334",
    "ownerId": "179000000000",
    "platform": null,
    "productCodes":
    [
    ],
    "public": false,
    "ramdiskId": "ari-0ccd3965",
    "rootDeviceName": "/dev/sda1",
    "rootDeviceType": "ebs",
    "state": "available",
    "stateReason": null,
    "tags":
    [
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "creator",
        "value": "ezbakery"
      },
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "last_referenced_time",
        "value": "2011-08-18 00:00:49 PDT"
      },
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "appversion",
        "value": "helloworld-1.4.0-829980.h2"
      },
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "creation_time",
        "value": "2011-05-23 23:36:14 UTC"
      }
    ],
    "virtualizationType": "paravirtual"
  },
  {
    "architecture": "x86_64",
    "blockDeviceMappings":
    [
      {
        "class": "com.amazonaws.services.ec2.model.BlockDeviceMapping",
        "deviceName": "/dev/sda1",
        "ebs": {
          "class": "com.amazonaws.services.ec2.model.EbsBlockDevice",
          "deleteOnTermination": true,
          "snapshotId": "snap-5c1e3332",
          "volumeSize": 10
        },
        "noDevice": null,
        "virtualName": null
      }
    ],
    "class": "com.amazonaws.services.ec2.model.Image",
    "description": "name=helloworld-1.4.0-898154.h61-x86_64-201106272151,arch=x86_64,ancestor_name=ebs-centosbase-x86_64-20110610,ancestor_id=ami-d646bfbf,ancestor_version=nflx-base-1.1-881005.h55",
    "hypervisor": "xen",
    "imageId": "ami-6c8d7605",
    "imageLocation": "179000000000/helloworld-1.4.0-898154.h61-x86_64-201106272151",
    "imageOwnerAlias": null,
    "imageType": "machine",
    "kernelId": "aki-460bfe2f",
    "name": "helloworld-1.4.0-898154.h61-x86_64-201106272151",
    "ownerId": "179000000000",
    "platform": null,
    "productCodes":
    [
    ],
    "public": false,
    "ramdiskId": "ari-580bfe31",
    "rootDeviceName": "/dev/sda1",
    "rootDeviceType": "ebs",
    "state": "available",
    "stateReason": null,
    "tags":
    [
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "appversion",
        "value": "helloworld-1.4.0-898154.h61/WE-WAPP-helloworld-BUILDBETA/61"
      },
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "creation_time",
        "value": "2011-06-27 21:55:06 UTC"
      },
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "last_referenced_time",
        "value": "2011-10-22 12:01:23 PDT"
      },
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "creator",
        "value": "bmiller"
      }
    ],
    "virtualizationType": "paravirtual"
  },
  {
    "architecture": "x86_64",
    "blockDeviceMappings":
    [
      {
        "class": "com.amazonaws.services.ec2.model.BlockDeviceMapping",
        "deviceName": "/dev/sda1",
        "ebs": {
          "class": "com.amazonaws.services.ec2.model.EbsBlockDevice",
          "deleteOnTermination": true,
          "snapshotId": "snap-930d2af2",
          "volumeSize": 10
        },
        "noDevice": null,
        "virtualName": null
      }
    ],
    "class": "com.amazonaws.services.ec2.model.Image",
    "description": "name=helloworld-1.4.0-964516.h13-x86_64-201108051930,arch=x86_64,ancestor_name=ebs-centosbase-x86_64-20110610,ancestor_id=ami-d646bfbf,ancestor_version=nflx-base-1.1-937357.h57",
    "hypervisor": "xen",
    "imageId": "ami-4775b32e",
    "imageLocation": "179000000000/helloworld-1.4.0-964516.h13-x86_64-201108051930",
    "imageOwnerAlias": null,
    "imageType": "machine",
    "kernelId": "aki-460bfe2f",
    "name": "helloworld-1.4.0-964516.h13-x86_64-201108051930",
    "ownerId": "179000000000",
    "platform": null,
    "productCodes":
    [
    ],
    "public": false,
    "ramdiskId": "ari-580bfe31",
    "rootDeviceName": "/dev/sda1",
    "rootDeviceType": "ebs",
    "state": "available",
    "stateReason": null,
    "tags":
    [
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "appversion",
        "value": "helloworld-1.4.0-964516.h13/ZZ-WAPP-hellodsheahan/13"
      },
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "last_referenced_time",
        "value": "2011-10-22 12:01:23 PDT"
      },
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "creator",
        "value": "bmiller"
      },
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "creation_time",
        "value": "2011-08-05 19:33:50 UTC"
      }
    ],
    "virtualizationType": "paravirtual"
  },
  {
    "architecture": "x86_64",
    "blockDeviceMappings":
    [
      {
        "class": "com.amazonaws.services.ec2.model.BlockDeviceMapping",
        "deviceName": "/dev/sda1",
        "ebs": {
          "class": "com.amazonaws.services.ec2.model.EbsBlockDevice",
          "deleteOnTermination": true,
          "snapshotId": "snap-ba8d72da",
          "volumeSize": 10
        },
        "noDevice": null,
        "virtualName": null
      }
    ],
    "class": "com.amazonaws.services.ec2.model.Image",
    "description": "name=helloworld-1.4.0-985478.h335-x86_64-201108182221,arch=x86_64,ancestor_name=ebs-centosbase-x86_64-20110610,ancestor_id=ami-d646bfbf,ancestor_version=nflx-base-1.1-937357.h57",
    "hypervisor": "xen",
    "imageId": "ami-3178b958",
    "imageLocation": "179000000000/helloworld-1.4.0-985478.h335-x86_64-201108182221",
    "imageOwnerAlias": null,
    "imageType": "machine",
    "kernelId": "aki-460bfe2f",
    "name": "helloworld-1.4.0-985478.h335-x86_64-201108182221",
    "ownerId": "179000000000",
    "platform": null,
    "productCodes":
    [
    ],
    "public": false,
    "ramdiskId": "ari-580bfe31",
    "rootDeviceName": "/dev/sda1",
    "rootDeviceType": "ebs",
    "state": "available",
    "stateReason": null,
    "tags":
    [
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "last_referenced_time",
        "value": "2011-10-22 12:01:23 PDT"
      },
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "creator",
        "value": "mwhite"
      },
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "creation_time",
        "value": "2011-08-18 22:24:44 UTC"
      },
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "appversion",
        "value": "helloworld-1.4.0-985478.h335/WE-WAPP-helloworld/335"
      }
    ],
    "virtualizationType": "paravirtual"
  },
  {
    "architecture": "x86_64",
    "blockDeviceMappings":
    [
      {
        "class": "com.amazonaws.services.ec2.model.BlockDeviceMapping",
        "deviceName": "/dev/sda1",
        "ebs": {
          "class": "com.amazonaws.services.ec2.model.EbsBlockDevice",
          "deleteOnTermination": true,
          "snapshotId": "snap-58ecc33b",
          "volumeSize": 10
        },
        "noDevice": null,
        "virtualName": null
      }
    ],
    "class": "com.amazonaws.services.ec2.model.Image",
    "description": "name=ntsuiboot-1.0.0-1091191.h1-x86_64-201110181917-ebs,arch=x86_64,ancestor_name=centosbase-x86_64-20111004-ebs,ancestor_id=ami-bf569bd6,ancestor_version=nflx-base-1.1-1064993.h172",
    "hypervisor": "xen",
    "imageId": "ami-fba96692",
    "imageLocation": "179000000000/ntsuiboot-1.0.0-1091191.h1-x86_64-201110181917-ebs",
    "imageOwnerAlias": null,
    "imageType": "machine",
    "kernelId": "aki-460bfe2f",
    "name": "ntsuiboot-1.0.0-1091191.h1-x86_64-201110181917-ebs",
    "ownerId": "179000000000",
    "platform": null,
    "productCodes":
    [
    ],
    "public": false,
    "ramdiskId": "ari-580bfe31",
    "rootDeviceName": "/dev/sda1",
    "rootDeviceType": "ebs",
    "state": "available",
    "stateReason": null,
    "tags":
    [
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "last_referenced_time",
        "value": "2011-10-22 12:01:23 PDT"
      },
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "appversion",
        "value": "ntsuiboot-1.0.0-1091191.h1"
      },
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "creation_time",
        "value": "2011-10-18 19:21:36 UTC"
      },
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "base_ami_version",
        "value": "nflx-base-1.1-1064993.h172"
      },
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "creator",
        "value": "jhounddog"
      }
    ],
    "virtualizationType": "paravirtual"
  },
  {
    "architecture": "x86_64",
    "blockDeviceMappings":
    [
      {
        "class": "com.amazonaws.services.ec2.model.BlockDeviceMapping",
        "deviceName": "/dev/sda1",
        "ebs": {
          "class": "com.amazonaws.services.ec2.model.EbsBlockDevice",
          "deleteOnTermination": true,
          "snapshotId": "snap-f3a21992",
          "volumeSize": 10
        },
        "noDevice": null,
        "virtualName": null
      }
    ],
    "class": "com.amazonaws.services.ec2.model.Image",
    "description": "name=ntsuiboot-1.0.0-939580.h3-x86_64-201107211703,arch=x86_64,ancestor_name=ebs-centosbase-x86_64-20110610,ancestor_id=ami-d646bfbf,ancestor_version=nflx-base-1.1-937357.h57",
    "hypervisor": "xen",
    "imageId": "ami-dfdd1ab6",
    "imageLocation": "179000000000/ntsuiboot-1.0.0-939580.h3-x86_64-201107211703",
    "imageOwnerAlias": null,
    "imageType": "machine",
    "kernelId": "aki-460bfe2f",
    "name": "ntsuiboot-1.0.0-939580.h3-x86_64-201107211703",
    "ownerId": "179000000000",
    "platform": null,
    "productCodes":
    [
    ],
    "public": false,
    "ramdiskId": "ari-580bfe31",
    "rootDeviceName": "/dev/sda1",
    "rootDeviceType": "ebs",
    "state": "available",
    "stateReason": null,
    "tags":
    [
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "creator",
        "value": "jhounddog"
      },
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "last_referenced_time",
        "value": "2011-10-22 12:01:23 PDT"
      },
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "appversion",
        "value": "ntsuiboot-1.0.0-939580.h3"
      },
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "creation_time",
        "value": "2011-07-21 17:08:11 UTC"
      }
    ],
    "virtualizationType": "paravirtual"
  },
  {
    "architecture": "x86_64",
    "blockDeviceMappings":
    [
    ],
    "class": "com.amazonaws.services.ec2.model.Image",
    "description": "ancestor_id=ami-8fa163e6,ancestor_name=centosbase-x86_64-20110914-ebs,ancestor_version=nflx-base-1.1-1028909.h145",
    "hypervisor": "xen",
    "imageId": "ami-7541831c",
    "imageLocation": "nflx-amis/helloworld-1.4.0-1041348.h379_x86_64-201109211847.manifest.xml",
    "imageOwnerAlias": null,
    "imageType": "machine",
    "kernelId": "aki-460bfe2f",
    "name": "helloworld-1.4.0-1041348.h379_x86_64-201109211847",
    "ownerId": "179000000000",
    "platform": null,
    "productCodes":
    [
    ],
    "public": false,
    "ramdiskId": "ari-580bfe31",
    "rootDeviceName": null,
    "rootDeviceType": "instance-store",
    "state": "available",
    "stateReason": null,
    "tags":
    [
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "appversion",
        "value": "helloworld-1.4.0-1041348.h379/WE-WAPP-helloworld/379"
      },
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "creation_time",
        "value": "2011-09-21 18:57:05 PDT"
      },
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "creator",
        "value": "mthompson"
      }
    ],
    "virtualizationType": "paravirtual"
  },
  {
    "architecture": "x86_64",
    "blockDeviceMappings":
    [
    ],
    "class": "com.amazonaws.services.ec2.model.Image",
    "description": "ancestor_id=ami-8fa163e6,ancestor_name=centosbase-x86_64-20110914-ebs,ancestor_version=nflx-base-1.1-1028909.h145",
    "hypervisor": "xen",
    "imageId": "ami-4b478522",
    "imageLocation": "nflx-amis/helloworld-1.4.0-1041348.h379_x86_64-201109212057.manifest.xml",
    "imageOwnerAlias": null,
    "imageType": "machine",
    "kernelId": "aki-460bfe2f",
    "name": "helloworld-1.4.0-1041348.h379_x86_64-201109212057",
    "ownerId": "179000000000",
    "platform": null,
    "productCodes":
    [
    ],
    "public": false,
    "ramdiskId": "ari-580bfe31",
    "rootDeviceName": null,
    "rootDeviceType": "instance-store",
    "state": "available",
    "stateReason": null,
    "tags":
    [
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "appversion",
        "value": "helloworld-1.4.0-1041348.h379/WE-WAPP-helloworld/379"
      },
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "creation_time",
        "value": "2011-09-21 21:05:33 PDT"
      },
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "creator",
        "value": "mthompson"
      }
    ],
    "virtualizationType": "paravirtual"
  },
  {
    "architecture": "i386",
    "blockDeviceMappings":
    [
    ],
    "class": "com.amazonaws.services.ec2.model.Image",
    "description": "ancestor_id=ami-40788629,ancestor_name=helloworld-1.4.0-829980.h2-i386-201105232334,ancestor_version=nflx-base-1.1-553441",
    "hypervisor": "xen",
    "imageId": "ami-5b9f5832",
    "imageLocation": "nflx-amis/helloworld-1.4.0-20110718.manifest.xml",
    "imageOwnerAlias": null,
    "imageType": "machine",
    "kernelId": "aki-1c669375",
    "name": "helloworld-1.4.0-20110718",
    "ownerId": "179000000000",
    "platform": null,
    "productCodes":
    [
    ],
    "public": false,
    "ramdiskId": "ari-0ccd3965",
    "rootDeviceName": null,
    "rootDeviceType": "instance-store",
    "state": "available",
    "stateReason": null,
    "tags":
    [
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "creator",
        "value": "jsmith"
      },
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "creation_time",
        "value": "2011-07-18 10:53:53 PDT"
      },
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "last_referenced_time",
        "value": "2011-08-18 00:00:49 PDT"
      },
      {
        "class": "com.amazonaws.services.ec2.model.Tag",
        "key": "appversion",
        "value": "helloworld-1.4.0-829980.h2"
      }
    ],
    "virtualizationType": "paravirtual"
  }
]
'''
}
