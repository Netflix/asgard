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

/**
 * Data about imaginary EBS volumes for offline development of related graphic user interfaces, and for functional
 * testing.
 */
class MockVolumes {

    /**
     * Shortened and sanitized JSON string output from /volume/list.json
     */
    @SuppressWarnings('LineLength')
    static final String DATA = '''
  [
    {
      "attachments":
      [
        {
          "attachTime": "2012-06-29T22:23:23Z",
          "class": "com.amazonaws.services.ec2.model.VolumeAttachment",
          "deleteOnTermination": true,
          "device": "/dev/sda1",
          "instanceId": "i-11049e69",
          "state": "attached",
          "volumeId": "vol-00189a6e"
        }
      ],
      "availabilityZone": "us-east-1a",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2012-06-29T22:21:43Z",
      "size": 10,
      "snapshotId": "snap-e635e198",
      "state": "in-use",
      "tags":
      [
      ],
      "volumeId": "vol-deadbeef"
    },
    {
      "attachments":
      [
        {
          "attachTime": "2011-11-16T05:54:04Z",
          "class": "com.amazonaws.services.ec2.model.VolumeAttachment",
          "deleteOnTermination": true,
          "device": "/dev/sda1",
          "instanceId": "i-53649e30",
          "state": "attached",
          "volumeId": "vol-00189a6d"
        }
      ],
      "availabilityZone": "us-east-1d",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2011-11-16T05:53:42Z",
      "size": 10,
      "snapshotId": "snap-3575e157",
      "state": "in-use",
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "janitor_metadata",
          "value": "{\\"instance\\":\\"i-53649e30\\",\\"owner\\":\\"pvenkman@whoyagonnacall.com\\"}"
        }
      ],
      "volumeId": "vol-00189a6d"
    },
    {
      "attachments":
      [
      ],
      "availabilityZone": "us-east-1c",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2012-04-05T21:06:16Z",
      "size": 10,
      "snapshotId": "snap-bed5e1c5",
      "state": "available",
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "janitor_metadata",
          "value": "{\\"instance\\":\\"\\",\\"owner\\":\\"\\",\\"detachTime\\":\\"2012-04-08T21:09:27\\"}"
        }
      ],
      "volumeId": "vol-00289a6f"
    },
    {
      "attachments":
      [
        {
          "attachTime": "2012-06-26T00:01:26Z",
          "class": "com.amazonaws.services.ec2.model.VolumeAttachment",
          "deleteOnTermination": false,
          "device": "/dev/sdf2",
          "instanceId": "i-89d49eef",
          "state": "attached",
          "volumeId": "vol-00589a6e"
        }
      ],
      "availabilityZone": "us-east-1d",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2012-06-26T00:01:10Z",
      "size": 10,
      "snapshotId": "snap-e325e19d",
      "state": "in-use",
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "immutable_metadata",
          "value": "{\\"ami\\":\\"ami-3af49e53\\",\\"ami-nam49e:\\"centosbase-x86_64-20120625-ebs\\",\\"arch\\":\\"x86_64\\",\\"purpose\\":\\"prebake\\",\\"status\\":\\"free\\"}"
        },
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "janitor_metadata",
          "value": "{\\"instance\\":\\"i-89d49eef\\",\\"owner\\":\\"pvenkman@whoyagonnacall.com\\"}"
        }
      ],
      "volumeId": "vol-00589a6e"
    },
    {
      "attachments":
      [
        {
          "attachTime": "2012-07-01T19:59:29Z",
          "class": "com.amazonaws.services.ec2.model.VolumeAttachment",
          "deleteOnTermination": true,
          "device": "/dev/sda1",
          "instanceId": "i-e0249e98",
          "state": "attached",
          "volumeId": "vol-00589a61"
        }
      ],
      "availabilityZone": "us-east-1e",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2012-07-01T19:58:21Z",
      "size": 10,
      "snapshotId": "snap-b8d5e1c7",
      "state": "in-use",
      "tags":
      [
      ],
      "volumeId": "vol-00589a61"
    },
    {
      "attachments":
      [
        {
          "attachTime": "2012-06-29T16:36:19Z",
          "class": "com.amazonaws.services.ec2.model.VolumeAttachment",
          "deleteOnTermination": true,
          "device": "/dev/sda1",
          "instanceId": "i-7b449e03",
          "state": "attached",
          "volumeId": "vol-00d89a6e"
        }
      ],
      "availabilityZone": "us-east-1c",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2012-06-29T16:35:36Z",
      "size": 10,
      "snapshotId": "snap-e485e19b",
      "state": "in-use",
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "janitor_metadata",
          "value": "{\\"instance\\":\\"i-7b449e03\\",\\"owner\\":\\"rstanz@whoyagonnacall.com\\"}"
        }
      ],
      "volumeId": "vol-00d89a6e"
    },
    {
      "attachments":
      [
        {
          "attachTime": "2012-06-29T16:41:43Z",
          "class": "com.amazonaws.services.ec2.model.VolumeAttachment",
          "deleteOnTermination": true,
          "device": "/dev/sda1",
          "instanceId": "i-b3549ecb",
          "state": "attached",
          "volumeId": "vol-00f89a6e"
        }
      ],
      "availabilityZone": "us-east-1c",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2012-06-29T16:40:50Z",
      "size": 10,
      "snapshotId": "snap-f6a5e189",
      "state": "in-use",
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "janitor_metadata",
          "value": "{\\"instance\\":\\"i-b3549ecb\\",\\"owner\\":\\"rstanz@whoyagonnacall.com\\"}"
        }
      ],
      "volumeId": "vol-00f89a6e"
    },
    {
      "attachments":
      [
      ],
      "availabilityZone": "us-east-1d",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2012-01-26T02:38:57Z",
      "size": 10,
      "snapshotId": "snap-7fa5e11a",
      "state": "available",
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "immutable_metadata",
          "value": "{\\"ami\\":\\"ami-fd549e94\\",\\"ami-nam49e:\\"centosbase-x86_64-20111206-ebs\\",\\"arch\\":\\"x86_64\\",\\"purpose\\":\\"prebake\\",\\"status\\":\\"free\\"}"
        },
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "janitor_metadata",
          "value": "{\\"instance\\":\\"\\",\\"owner\\":\\"\\",\\"detachTime\\":\\"2012-03-06T04:47:03\\"}"
        }
      ],
      "volumeId": "vol-01289a6c"
    },
    {
      "attachments":
      [
      ],
      "availabilityZone": "us-east-1c",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2012-05-23T20:53:03Z",
      "size": 10,
      "snapshotId": "snap-3765e14b",
      "state": "available",
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "janitor_metadata",
          "value": "{\\"instance\\":\\"\\",\\"owner\\":\\"\\",\\"detachTime\\":\\"2012-05-25T17:45:26\\"}"
        }
      ],
      "volumeId": "vol-01389a6f"
    },
    {
      "attachments":
      [
      ],
      "availabilityZone": "us-east-1c",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2012-06-19T14:37:24Z",
      "size": 10,
      "snapshotId": "snap-9225e1ed",
      "state": "available",
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "janitor_metadata",
          "value": "{\\"instance\\":\\"\\",\\"owner\\":\\"\\",\\"detachTime\\":\\"2012-06-20T17:58:01\\"}"
        }
      ],
      "volumeId": "vol-01389a6f"
    },
    {
      "attachments":
      [
        {
          "attachTime": "2012-06-30T05:09:18Z",
          "class": "com.amazonaws.services.ec2.model.VolumeAttachment",
          "deleteOnTermination": true,
          "device": "/dev/sda1",
          "instanceId": "i-1ce49e64",
          "state": "attached",
          "volumeId": "vol-01689a6f"
        }
      ],
      "availabilityZone": "us-east-1e",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2012-06-30T05:08:39Z",
      "size": 10,
      "snapshotId": "snap-e135e19f",
      "state": "in-use",
      "tags":
      [
      ],
      "volumeId": "vol-01689a6f"
    },
    {
      "attachments":
      [
        {
          "attachTime": "2012-06-21T17:38:15Z",
          "class": "com.amazonaws.services.ec2.model.VolumeAttachment",
          "deleteOnTermination": false,
          "device": "/dev/sdh1",
          "instanceId": "i-0ce49e75",
          "state": "attached",
          "volumeId": "vol-01789a6f"
        }
      ],
      "availabilityZone": "us-east-1e",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2012-06-21T17:22:01Z",
      "size": 100,
      "snapshotId": "snap-0755e179",
      "state": "in-use",
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "rating_metadata",
          "value": "{\\"desc\\":\\"1339483743620\\",\\"device\\":\\"/dev/sdh1\\",\\"index\\":\\"7\\",\\"mount point\\":\\"/DB/RATEPROD/data02\\",\\"name\\":\\"RATEPROD\\",\\"role\\":\\"MASTER\\",\\"zone\\":\\"us-east-1a\\"}"
        },
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "janitor_metadata",
          "value": "{\\"instance\\":\\"i-0ce49e75\\",\\"owner\\":\\"rstanz@whoyagonnacall.com\\"}"
        }
      ],
      "volumeId": "vol-01789a6f"
    },
    {
      "attachments":
      [
      ],
      "availabilityZone": "us-east-1e",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2012-06-23T15:18:45Z",
      "size": 100,
      "snapshotId": "snap-9395e1ed",
      "state": "available",
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "janitor_metadata",
          "value": "{\\"instance\\":\\"\\",\\"owner\\":\\"\\",\\"detachTime\\":\\"2012-06-25T20:14:19\\"}"
        }
      ],
      "volumeId": "vol-01989a6f"
    },
    {
      "attachments":
      [
      ],
      "availabilityZone": "us-east-1d",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2011-11-30T21:41:11Z",
      "size": 10,
      "snapshotId": "snap-87f5e1e2",
      "state": "available",
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "janitor_metadata",
          "value": "{\\"instance\\":\\"\\",\\"owner\\":\\"\\",\\"detachTime\\":\\"2012-03-06T04:43:28\\"}"
        }
      ],
      "volumeId": "vol-01a89a6c"
    },
    {
      "attachments":
      [
      ],
      "availabilityZone": "us-east-1e",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2012-06-09T18:41:08Z",
      "size": 100,
      "snapshotId": "snap-e6f5e199",
      "state": "available",
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "janitor_metadata",
          "value": "{\\"instance\\":\\"\\",\\"owner\\":\\"\\",\\"detachTime\\":\\"2012-06-12T17:14:21\\"}"
        }
      ],
      "volumeId": "vol-01a89a6f"
    },
    {
      "attachments":
      [
      ],
      "availabilityZone": "us-east-1d",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2012-01-19T21:04:47Z",
      "size": 10,
      "snapshotId": "snap-c675e1a2",
      "state": "available",
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "janitor_metadata",
          "value": "{\\"instance\\":\\"\\",\\"owner\\":\\"\\",\\"detachTime\\":\\"2012-03-06T04:44:16\\"}"
        }
      ],
      "volumeId": "vol-01b89a6c"
    },
    {
      "attachments":
      [
        {
          "attachTime": "2012-06-08T01:37:38Z",
          "class": "com.amazonaws.services.ec2.model.VolumeAttachment",
          "deleteOnTermination": true,
          "device": "/dev/sda1",
          "instanceId": "i-05849e60",
          "state": "attached",
          "volumeId": "vol-01c89a6d"
        }
      ],
      "availabilityZone": "us-east-1c",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2012-02-06T18:59:37Z",
      "size": 8,
      "snapshotId": "snap-53b5e131",
      "state": "in-use",
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "janitor_metadata",
          "value": "{\\"instance\\":\\"i-05849e60\\",\\"owner\\":\\"rstanz@whoyagonnacall.com\\"}"
        }
      ],
      "volumeId": "vol-01c89a6d"
    },
    {
      "attachments":
      [
        {
          "attachTime": "2012-06-25T17:52:52Z",
          "class": "com.amazonaws.services.ec2.model.VolumeAttachment",
          "deleteOnTermination": true,
          "device": "/dev/sda1",
          "instanceId": "i-b4589acd",
          "state": "attached",
          "volumeId": "vol-01d89a6f"
        }
      ],
      "availabilityZone": "us-east-1c",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2012-06-25T17:52:06Z",
      "size": 10,
      "snapshotId": "snap-e2d5e19d",
      "state": "in-use",
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "janitor_metadata",
          "value": "{\\"instance\\":\\"i-b4549ecd\\",\\"owner\\":\\"espengler@whoyagonnacall.com\\"}"
        }
      ],
      "volumeId": "vol-01d89a6f"
    },
    {
      "attachments":
      [
      ],
      "availabilityZone": "us-east-1c",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2011-12-22T18:05:45Z",
      "size": 10,
      "snapshotId": "snap-7fa5e11a",
      "state": "available",
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "janitor_metadata",
          "value": "{\\"instance\\":\\"\\",\\"owner\\":\\"\\",\\"detachTime\\":\\"2012-03-06T04:40:21\\"}"
        }
      ],
      "volumeId": "vol-01e89a6c"
    },
    {
      "attachments":
      [
      ],
      "availabilityZone": "us-east-1a",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2012-01-07T16:24:10Z",
      "size": 100,
      "snapshotId": "snap-d885e1bc",
      "state": "available",
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "janitor_metadata",
          "value": "{\\"instance\\":\\"\\",\\"owner\\":\\"\\",\\"detachTime\\":\\"2012-03-06T04:43:39\\"}"
        }
      ],
      "volumeId": "vol-01e89a6c"
    },
    {
      "attachments":
      [
        {
          "attachTime": "2012-06-22T20:37:32Z",
          "class": "com.amazonaws.services.ec2.model.VolumeAttachment",
          "deleteOnTermination": true,
          "device": "/dev/sda1",
          "instanceId": "i-d4b89aad",
          "state": "attached",
          "volumeId": "vol-01f89a6f"
        }
      ],
      "availabilityZone": "us-east-1d",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2012-06-22T20:36:45Z",
      "size": 10,
      "snapshotId": "snap-bcf5e1c1",
      "state": "in-use",
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "janitor_metadata",
          "value": "{\\"instance\\":\\"i-d4b49ead\\",\\"owner\\":\\"espengler@whoyagonnacall.com\\"}"
        }
      ],
      "volumeId": "vol-01f89a6f"
    },
    {
      "attachments":
      [
        {
          "attachTime": "2012-06-19T17:31:37Z",
          "class": "com.amazonaws.services.ec2.model.VolumeAttachment",
          "deleteOnTermination": false,
          "device": "/dev/sdh4",
          "instanceId": "i-74989a0d",
          "state": "attached",
          "volumeId": "vol-01f89a6f"
        }
      ],
      "availabilityZone": "us-east-1e",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2012-06-19T17:22:28Z",
      "size": 100,
      "snapshotId": "snap-dd25e1a3",
      "state": "in-use",
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "immutable_metadata",
          "value": "{\\"desc\\":\\"1337451486800\\",\\"device\\":\\"/dev/sdh4\\",\\"index\\":\\"6\\",\\"mount point\\":\\"/DB/RATEPROD/data05\\",\\"name\\":\\"RATEPROD\\",\\"role\\":\\"MASTER\\",\\"\\":\\"\\",\\"zone\\":\\"us-east-1a\\"}"
        },
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "janitor_metadata",
          "value": "{\\"instance\\":\\"i-74949e0d\\",\\"owner\\":\\"espengler@whoyagonnacall.com\\"}"
        }
      ],
      "volumeId": "vol-01f89a6f"
    },
    {
      "attachments":
      [
        {
          "attachTime": "2012-06-29T21:09:32Z",
          "class": "com.amazonaws.services.ec2.model.VolumeAttachment",
          "deleteOnTermination": true,
          "device": "/dev/sda1",
          "instanceId": "i-c3889abb",
          "state": "attached",
          "volumeId": "vol-02089a6c"
        }
      ],
      "availabilityZone": "us-east-1e",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2012-06-29T21:08:52Z",
      "size": 10,
      "snapshotId": "snap-3235e15c",
      "state": "in-use",
      "tags":
      [
      ],
      "volumeId": "vol-02089a6c"
    },
    {
      "attachments":
      [
      ],
      "availabilityZone": "us-east-1a",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2012-05-08T20:56:46Z",
      "size": 100,
      "snapshotId": "snap-0d15e171",
      "state": "available",
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "janitor_metadata",
          "value": "{\\"instance\\":\\"\\",\\"owner\\":\\"\\",\\"detachTime\\":\\"2012-05-10T17:45:50\\"}"
        }
      ],
      "volumeId": "vol-02189a6d"
    },
    {
      "attachments":
      [
        {
          "attachTime": "2012-06-29T17:50:42Z",
          "class": "com.amazonaws.services.ec2.model.VolumeAttachment",
          "deleteOnTermination": true,
          "device": "/dev/sda1",
          "instanceId": "i-c9289ab1",
          "state": "attached",
          "volumeId": "vol-02589a6c"
        }
      ],
      "availabilityZone": "us-east-1c",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2012-06-29T17:49:48Z",
      "size": 10,
      "snapshotId": "snap-9895e1e6",
      "state": "in-use",
      "tags":
      [
      ],
      "volumeId": "vol-02589a6c"
    },
    {
      "attachments":
      [
        {
          "attachTime": "2012-07-01T20:02:12Z",
          "class": "com.amazonaws.services.ec2.model.VolumeAttachment",
          "deleteOnTermination": false,
          "device": "/dev/sdh5",
          "instanceId": "i-e8289a90",
          "state": "attached",
          "volumeId": "vol-02589a63"
        }
      ],
      "availabilityZone": "us-east-1e",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2012-07-01T19:53:10Z",
      "size": 100,
      "snapshotId": "snap-9d15e1e3",
      "state": "in-use",
      "tags":
      [
      ],
      "volumeId": "vol-02589a63"
    },
    {
      "attachments":
      [
        {
          "attachTime": "2012-07-01T19:59:16Z",
          "class": "com.amazonaws.services.ec2.model.VolumeAttachment",
          "deleteOnTermination": true,
          "device": "/dev/sda1",
          "instanceId": "i-ec249e94",
          "state": "attached",
          "volumeId": "vol-02589a63"
        }
      ],
      "availabilityZone": "us-east-1e",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2012-07-01T19:58:21Z",
      "size": 10,
      "snapshotId": "snap-b8d5e1c7",
      "state": "in-use",
      "tags":
      [
      ],
      "volumeId": "vol-02589a63"
    },
    {
      "attachments":
      [
        {
          "attachTime": "2012-06-29T17:55:01Z",
          "class": "com.amazonaws.services.ec2.model.VolumeAttachment",
          "deleteOnTermination": true,
          "device": "/dev/sda1",
          "instanceId": "i-97349eef",
          "state": "attached",
          "volumeId": "vol-02789a6c"
        }
      ],
      "availabilityZone": "us-east-1c",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2012-06-29T17:54:28Z",
      "size": 10,
      "snapshotId": "snap-b8d5e1c6",
      "state": "in-use",
      "tags":
      [
      ],
      "volumeId": "vol-02789a6c"
    },
    {
      "attachments":
      [
        {
          "attachTime": "2012-04-03T18:54:07Z",
          "class": "com.amazonaws.services.ec2.model.VolumeAttachment",
          "deleteOnTermination": true,
          "device": "/dev/sda1",
          "instanceId": "i-25149e42",
          "state": "attached",
          "volumeId": "vol-02c89a6d"
        }
      ],
      "availabilityZone": "us-east-1d",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2012-04-03T18:53:10Z",
      "size": 10,
      "snapshotId": "snap-7ee5e103",
      "state": "in-use",
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "janitor_metadata",
          "value": "{\\"instance\\":\\"i-25149e42\\",\\"owner\\":\\"wzeddemore@whoyagonnacall.com\\"}"
        }
      ],
      "volumeId": "vol-02c89a6d"
    },
    {
      "attachments":
      [
      ],
      "availabilityZone": "us-east-1a",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2012-01-09T06:00:28Z",
      "size": 100,
      "snapshotId": "snap-2215e146",
      "state": "available",
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "janitor_metadata",
          "value": "{\\"instance\\":\\"\\",\\"owner\\":\\"\\",\\"detachTime\\":\\"2012-03-06T04:45:56\\"}"
        }
      ],
      "volumeId": "vol-03089a6e"
    },
    {
      "attachments":
      [
      ],
      "availabilityZone": "us-east-1c",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2012-02-14T21:12:44Z",
      "size": 10,
      "snapshotId": "snap-8cc5e1e8",
      "state": "available",
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "janitor_metadata",
          "value": "{\\"instance\\":\\"\\",\\"owner\\":\\"\\",\\"detachTime\\":\\"2012-03-06T04:40:23\\"}"
        }
      ],
      "volumeId": "vol-03389a6f"
    },
    {
      "attachments":
      [
      ],
      "availabilityZone": "us-east-1c",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2012-05-25T22:11:18Z",
      "size": 10,
      "snapshotId": "snap-8ee5e1f3",
      "state": "available",
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "janitor_metadata",
          "value": "{\\"instance\\":\\"i-a9849ece\\",\\"owner\\":\\"pvenkman@whoyagonnacall.com\\",\\"detachTime\\":\\"2012-06-08T16:59:49\\"}"
        }
      ],
      "volumeId": "vol-03889a6d"
    },
    {
      "attachments":
      [
      ],
      "availabilityZone": "us-east-1a",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2011-11-30T21:41:11Z",
      "size": 10,
      "snapshotId": "snap-81f5e1e4",
      "state": "available",
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "janitor_metadata",
          "value": "{\\"instance\\":\\"\\",\\"owner\\":\\"\\",\\"detachTime\\":\\"2012-03-06T04:39:45\\"}"
        }
      ],
      "volumeId": "vol-03a89a6e"
    },
    {
      "attachments":
      [
        {
          "attachTime": "2012-06-25T17:51:22Z",
          "class": "com.amazonaws.services.ec2.model.VolumeAttachment",
          "deleteOnTermination": true,
          "device": "/dev/sda1",
          "instanceId": "i-8a549ef3",
          "state": "attached",
          "volumeId": "vol-03d89a6d"
        }
      ],
      "availabilityZone": "us-east-1c",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2012-06-25T17:50:41Z",
      "size": 10,
      "snapshotId": "snap-0a95e175",
      "state": "in-use",
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "janitor_metadata",
          "value": "{\\"instance\\":\\"i-8a549ef3\\",\\"owner\\":\\"rstanz@whoyagonnacall.com\\"}"
        }
      ],
      "volumeId": "vol-03d89a6d"
    },
    {
      "attachments":
      [
        {
          "attachTime": "2012-06-19T17:30:26Z",
          "class": "com.amazonaws.services.ec2.model.VolumeAttachment",
          "deleteOnTermination": false,
          "device": "/dev/sdh2",
          "instanceId": "i-76949e0f",
          "state": "attached",
          "volumeId": "vol-03f89a6d"
        }
      ],
      "availabilityZone": "us-east-1e",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2012-06-19T17:18:44Z",
      "size": 100,
      "snapshotId": "snap-2d35e153",
      "state": "in-use",
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "immutable_metadata",
          "value": "{\\"desc\\":\\"1337451521431\\",\\"device\\":\\"/dev/sdh2\\",\\"index\\":\\"8\\",\\"mount point\\":\\"/DB/RATEPROD/data03\\",\\"name\\":\\"RATEPROD\\",\\"role\\":\\"MASTER\\",\\"zone\\":\\"us-east-1a\\"}"
        },
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "janitor_metadata",
          "value": "{\\"instance\\":\\"i-76949e0f\\",\\"owner\\":\\"rstanz@whoyagonnacall.com\\"}"
        }
      ],
      "volumeId": "vol-03f89a6d"
    },
    {
      "attachments":
      [
        {
          "attachTime": "2012-06-19T17:29:39Z",
          "class": "com.amazonaws.services.ec2.model.VolumeAttachment",
          "deleteOnTermination": false,
          "device": "/dev/sdc1",
          "instanceId": "i-62949e1b",
          "state": "attached",
          "volumeId": "vol-03f89a6d"
        }
      ],
      "availabilityZone": "us-east-1e",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2012-06-19T17:20:08Z",
      "size": 100,
      "snapshotId": "snap-d1e5e1af",
      "state": "in-use",
      "tags":
      [
      ],
      "volumeId": "vol-03f89a6d"
    },
    {
      "attachments":
      [
        {
          "attachTime": "2012-06-19T17:30:25Z",
          "class": "com.amazonaws.services.ec2.model.VolumeAttachment",
          "deleteOnTermination": false,
          "device": "/dev/sdh",
          "instanceId": "i-6a949e13",
          "state": "attached",
          "volumeId": "vol-03f89a6d"
        }
      ],
      "availabilityZone": "us-east-1e",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2012-06-19T17:19:22Z",
      "size": 100,
      "snapshotId": "snap-1395e16d",
      "state": "in-use",
      "tags":
      [
      ],
      "volumeId": "vol-03f89a6d"
    },
    {
      "attachments":
      [
        {
          "attachTime": "2012-06-19T17:31:22Z",
          "class": "com.amazonaws.services.ec2.model.VolumeAttachment",
          "deleteOnTermination": false,
          "device": "/dev/sdh4",
          "instanceId": "i-68949e11",
          "state": "attached",
          "volumeId": "vol-03f89a6d"
        }
      ],
      "availabilityZone": "us-east-1e",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2012-06-19T17:23:12Z",
      "size": 100,
      "snapshotId": "snap-4195e13f",
      "state": "in-use",
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "immutable_metadata",
          "value": "{\\"desc\\":\\"1337451471203\\",\\"device\\":\\"/dev/sdh4\\",\\"index\\":\\"5\\",\\"mount point\\":\\"/DB/RATEPROD/data05\\",\\"name\\":\\"RATEPROD\\",\\"role\\":\\"MASTER\\",\\"zone\\":\\"us-east-1a\\"}"
        },
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "janitor_metadata",
          "value": "{\\"instance\\":\\"i-68949e11\\",\\"owner\\":\\"espengler@whoyagonnacall.com\\"}"
        }
      ],
      "volumeId": "vol-03f89a6d"
    },
    {
      "attachments":
      [
      ],
      "availabilityZone": "us-east-1c",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2012-06-18T19:39:17Z",
      "size": 10,
      "snapshotId": "snap-9225e1ed",
      "state": "available",
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "janitor_metadata",
          "value": "{\\"instance\\":\\"\\",\\"owner\\":\\"\\",\\"detachTime\\":\\"2012-06-20T17:58:18\\"}"
        }
      ],
      "volumeId": "vol-03f89a6d"
    },
    {
      "attachments":
      [
      ],
      "availabilityZone": "us-east-1a",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2011-12-01T21:50:34Z",
      "size": 100,
      "snapshotId": "snap-c1f5e1a4",
      "state": "available",
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "janitor_metadata",
          "value": "{\\"instance\\":\\"\\",\\"owner\\":\\"\\",\\"detachTime\\":\\"2012-03-06T04:39:01\\"}"
        }
      ],
      "volumeId": "vol-03f89a6e"
    },
    {
      "attachments":
      [
      ],
      "availabilityZone": "us-east-1a",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2012-05-08T20:56:45Z",
      "size": 100,
      "snapshotId": "snap-fd15e181",
      "state": "available",
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "janitor_metadata",
          "value": "{\\"instance\\":\\"\\",\\"owner\\":\\"\\",\\"detachTime\\":\\"2012-05-10T17:45:42\\"}"
        }
      ],
      "volumeId": "vol-04189a6b"
    },
    {
      "attachments":
      [
        {
          "attachTime": "2012-06-28T21:45:15Z",
          "class": "com.amazonaws.services.ec2.model.VolumeAttachment",
          "deleteOnTermination": true,
          "device": "/dev/sda1",
          "instanceId": "i-48349e31",
          "state": "attached",
          "volumeId": "vol-04589a6a"
        }
      ],
      "availabilityZone": "us-east-1c",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2012-06-28T21:44:00Z",
      "size": 10,
      "snapshotId": "snap-7c45e102",
      "state": "in-use",
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "janitor_metadata",
          "value": "{\\"instance\\":\\"i-48349e31\\",\\"owner\\":\\"espengler@whoyagonnacall.com\\"}"
        }
      ],
      "volumeId": "vol-04589a6a"
    },
    {
      "attachments":
      [
        {
          "attachTime": "2012-07-01T19:59:10Z",
          "class": "com.amazonaws.services.ec2.model.VolumeAttachment",
          "deleteOnTermination": true,
          "device": "/dev/sda1",
          "instanceId": "i-f2249e8a",
          "state": "attached",
          "volumeId": "vol-04589a65"
        }
      ],
      "availabilityZone": "us-east-1e",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2012-07-01T19:58:21Z",
      "size": 10,
      "snapshotId": "snap-b8d5e1c7",
      "state": "in-use",
      "tags":
      [
      ],
      "volumeId": "vol-04589a65"
    },
    {
      "attachments":
      [
        {
          "attachTime": "2012-06-29T17:55:44Z",
          "class": "com.amazonaws.services.ec2.model.VolumeAttachment",
          "deleteOnTermination": true,
          "device": "/dev/sda1",
          "instanceId": "i-e9349e91",
          "state": "attached",
          "volumeId": "vol-04789a6a"
        }
      ],
      "availabilityZone": "us-east-1c",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2012-06-29T17:55:11Z",
      "size": 10,
      "snapshotId": "snap-b8d5e1c6",
      "state": "in-use",
      "tags":
      [
      ],
      "volumeId": "vol-04789a6a"
    },
    {
      "attachments":
      [
        {
          "attachTime": "2012-06-26T19:50:40Z",
          "class": "com.amazonaws.services.ec2.model.VolumeAttachment",
          "deleteOnTermination": true,
          "device": "/dev/sda1",
          "instanceId": "i-fed49e87",
          "state": "attached",
          "volumeId": "vol-04889a6a"
        }
      ],
      "availabilityZone": "us-east-1d",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2012-06-26T19:49:36Z",
      "size": 10,
      "snapshotId": "snap-0225e17c",
      "state": "in-use",
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "janitor_metadata",
          "value": "{\\"instance\\":\\"i-fed49e87\\",\\"owner\\":\\"rstanz@whoyagonnacall.com\\"}"
        }
      ],
      "volumeId": "vol-04889a6a"
    },
    {
      "attachments":
      [
        {
          "attachTime": "2012-06-27T16:24:25Z",
          "class": "com.amazonaws.services.ec2.model.VolumeAttachment",
          "deleteOnTermination": true,
          "device": "/dev/sda1",
          "instanceId": "i-24c49e5d",
          "state": "attached",
          "volumeId": "vol-04989a6a"
        }
      ],
      "availabilityZone": "us-east-1d",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2012-06-27T16:23:21Z",
      "size": 10,
      "snapshotId": "snap-18a5e167",
      "state": "in-use",
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "janitor_metadata",
          "value": "{\\"instance\\":\\"i-24c49e5d\\",\\"owner\\":\\"rstanz@whoyagonnacall.com\\"}"
        }
      ],
      "volumeId": "vol-04989a6a"
    },
    {
      "attachments":
      [
        {
          "attachTime": "2012-06-22T04:31:51Z",
          "class": "com.amazonaws.services.ec2.model.VolumeAttachment",
          "deleteOnTermination": true,
          "device": "/dev/sda1",
          "instanceId": "i-28849e51",
          "state": "attached",
          "volumeId": "vol-05089a6b"
        }
      ],
      "availabilityZone": "us-east-1c",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2012-06-22T04:31:12Z",
      "size": 10,
      "snapshotId": "snap-92f5e1ef",
      "state": "in-use",
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "janitor_metadata",
          "value": "{\\"instance\\":\\"i-28849e51\\",\\"owner\\":\\"rstanz@whoyagonnacall.com\\"}"
        }
      ],
      "volumeId": "vol-05089a6b"
    },
    {
      "attachments":
      [
        {
          "attachTime": "2012-06-22T04:43:36Z",
          "class": "com.amazonaws.services.ec2.model.VolumeAttachment",
          "deleteOnTermination": true,
          "device": "/dev/sda1",
          "instanceId": "i-84b49efd",
          "state": "attached",
          "volumeId": "vol-05089a6b"
        }
      ],
      "availabilityZone": "us-east-1c",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2012-06-22T04:43:05Z",
      "size": 10,
      "snapshotId": "snap-92f5e1ef",
      "state": "in-use",
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "janitor_metadata",
          "value": "{\\"instance\\":\\"i-84b49efd\\",\\"owner\\":\\"rstanz@whoyagonnacall.com\\"}"
        }
      ],
      "volumeId": "vol-05089a6b"
    },
    {
      "attachments":
      [
      ],
      "availabilityZone": "us-east-1d",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2011-12-20T01:28:53Z",
      "size": 10,
      "snapshotId": "snap-d3a5e1b6",
      "state": "available",
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "immutable_metadata",
          "value": "{\\"ami-nam49e:\\"centosbase-i386-20111206-ebs\\",\\"ami\\":\\"ami-f1549e98\\",\\"arch\\":\\"i386\\",\\"purpose\\":\\"prebake\\",\\"status\\":\\"free\\"}"
        },
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "janitor_metadata",
          "value": "{\\"instance\\":\\"\\",\\"owner\\":\\"\\",\\"detachTime\\":\\"2012-03-06T04:46:05\\"}"
        }
      ],
      "volumeId": "vol-05189a68"
    },
    {
      "attachments":
      [
        {
          "attachTime": "2012-06-30T16:11:03Z",
          "class": "com.amazonaws.services.ec2.model.VolumeAttachment",
          "deleteOnTermination": false,
          "device": "/dev/sdg",
          "instanceId": "i-b4949ecc",
          "state": "attached",
          "volumeId": "vol-05389a69"
        }
      ],
      "availabilityZone": "us-east-1a",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2012-03-05T17:32:01Z",
      "size": 10,
      "snapshotId": "",
      "state": "in-use",
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "janitor_metadata",
          "value": "{\\"instance\\":\\"i-e6949e9f\\",\\"owner\\":\\"rstanz@whoyagonnacall.com\\"}"
        }
      ],
      "volumeId": "vol-05389a69"
    },
    {
      "attachments":
      [
      ],
      "availabilityZone": "us-east-1d",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2012-01-22T08:23:07Z",
      "size": 10,
      "snapshotId": "snap-7fa5e11a",
      "state": "available",
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "janitor_metadata",
          "value": "{\\"instance\\":\\"\\",\\"owner\\":\\"\\",\\"detachTime\\":\\"2012-03-06T04:43:24\\"}"
        }
      ],
      "volumeId": "vol-05589a68"
    },
    {
      "attachments":
      [
        {
          "attachTime": "2012-06-12T15:48:16Z",
          "class": "com.amazonaws.services.ec2.model.VolumeAttachment",
          "deleteOnTermination": true,
          "device": "/dev/sda1",
          "instanceId": "i-1e149e67",
          "state": "attached",
          "volumeId": "vol-05689a6b"
        }
      ],
      "availabilityZone": "us-east-1d",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2012-06-12T15:47:18Z",
      "size": 10,
      "snapshotId": "snap-2055e15f",
      "state": "in-use",
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "janitor_metadata",
          "value": "{\\"instance\\":\\"i-1e149e67\\",\\"owner\\":\\"espengler@whoyagonnacall.com\\"}"
        }
      ],
      "volumeId": "vol-05689a6b"
    },
    {
      "attachments":
      [
        {
          "attachTime": "2011-12-19T19:28:50Z",
          "class": "com.amazonaws.services.ec2.model.VolumeAttachment",
          "deleteOnTermination": true,
          "device": "/dev/sda1",
          "instanceId": "i-7c249e1e",
          "state": "attached",
          "volumeId": "vol-05789a68"
        }
      ],
      "availabilityZone": "us-east-1c",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2011-12-19T19:28:23Z",
      "size": 10,
      "snapshotId": "snap-9dc5e1f8",
      "state": "in-use",
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "janitor_metadata",
          "value": "{\\"instance\\":\\"i-7c249e1e\\",\\"owner\\":\\"espengler@whoyagonnacall.com\\"}"
        }
      ],
      "volumeId": "vol-05789a68"
    },
    {
      "attachments":
      [
        {
          "attachTime": "2012-06-21T17:36:12Z",
          "class": "com.amazonaws.services.ec2.model.VolumeAttachment",
          "deleteOnTermination": false,
          "device": "/dev/sdh13",
          "instanceId": "i-14e49e6d",
          "state": "attached",
          "volumeId": "vol-05789a6b"
        }
      ],
      "availabilityZone": "us-east-1e",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2012-06-21T17:20:08Z",
      "size": 100,
      "snapshotId": "snap-4935e137",
      "state": "in-use",
      "tags":
      [
      ],
      "volumeId": "vol-05789a6b"
    },
    {
      "attachments":
      [
      ],
      "availabilityZone": "us-east-1a",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2011-12-16T22:25:32Z",
      "size": 100,
      "snapshotId": "snap-a735e1c2",
      "state": "available",
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "janitor_metadata",
          "value": "{\\"instance\\":\\"\\",\\"owner\\":\\"\\",\\"detachTime\\":\\"2012-03-06T04:45:27\\"}"
        }
      ],
      "volumeId": "vol-05789a68"
    },
    {
      "attachments":
      [
        {
          "attachTime": "2012-06-21T17:37:11Z",
          "class": "com.amazonaws.services.ec2.model.VolumeAttachment",
          "deleteOnTermination": false,
          "device": "/dev/sdh2",
          "instanceId": "i-10e49e69",
          "state": "attached",
          "volumeId": "vol-05789a6b"
        }
      ],
      "availabilityZone": "us-east-1e",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2012-06-21T17:22:48Z",
      "size": 100,
      "snapshotId": "snap-d575e1ab",
      "state": "in-use",
      "tags":
      [
      ],
      "volumeId": "vol-05789a6b"
    },
    {
      "attachments":
      [
        {
          "attachTime": "2012-06-21T17:36:53Z",
          "class": "com.amazonaws.services.ec2.model.VolumeAttachment",
          "deleteOnTermination": false,
          "device": "/dev/sdh",
          "instanceId": "i-16e49e6f",
          "state": "attached",
          "volumeId": "vol-05789a6b"
        }
      ],
      "availabilityZone": "us-east-1e",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2012-06-21T17:24:11Z",
      "size": 100,
      "snapshotId": "snap-3595e14b",
      "state": "in-use",
      "tags":
      [
      ],
      "volumeId": "vol-05789a6b"
    },
    {
      "attachments":
      [
        {
          "attachTime": "2012-06-21T17:38:28Z",
          "class": "com.amazonaws.services.ec2.model.VolumeAttachment",
          "deleteOnTermination": false,
          "device": "/dev/sdh15",
          "instanceId": "i-12e49e6b",
          "state": "attached",
          "volumeId": "vol-05789a6b"
        }
      ],
      "availabilityZone": "us-east-1e",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2012-06-21T17:23:47Z",
      "size": 100,
      "snapshotId": "snap-d925e1a7",
      "state": "in-use",
      "tags":
      [
      ],
      "volumeId": "vol-05789a6b"
    },
    {
      "attachments":
      [
      ],
      "availabilityZone": "us-east-1d",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2012-01-09T17:24:41Z",
      "size": 10,
      "snapshotId": "snap-89f5e1ec",
      "state": "available",
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "janitor_metadata",
          "value": "{\\"instance\\":\\"\\",\\"owner\\":\\"\\",\\"detachTime\\":\\"2012-03-06T04:44:27\\"}"
        }
      ],
      "volumeId": "vol-05889a68"
    },
    {
      "attachments":
      [
      ],
      "availabilityZone": "us-east-1c",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2012-06-19T19:36:17Z",
      "size": 10,
      "snapshotId": "snap-9225e1ed",
      "state": "available",
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "janitor_metadata",
          "value": "{\\"instance\\":\\"\\",\\"owner\\":\\"\\",\\"detachTime\\":\\"2012-06-21T17:36:15\\"}"
        }
      ],
      "volumeId": "vol-05889a6b"
    },
    {
      "attachments":
      [
      ],
      "availabilityZone": "us-east-1e",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2012-06-23T15:18:44Z",
      "size": 100,
      "snapshotId": "snap-6f95e111",
      "state": "available",
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "janitor_metadata",
          "value": "{\\"instance\\":\\"\\",\\"owner\\":\\"\\",\\"detachTime\\":\\"2012-06-25T20:14:23\\"}"
        }
      ],
      "volumeId": "vol-05989a6b"
    },
    {
      "attachments":
      [
      ],
      "availabilityZone": "us-east-1c",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2012-01-10T06:05:40Z",
      "size": 80,
      "snapshotId": "",
      "state": "available",
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "janitor_metadata",
          "value": "{\\"instance\\":\\"\\",\\"owner\\":\\"\\",\\"detachTime\\":\\"2012-03-06T04:39:17\\"}"
        }
      ],
      "volumeId": "vol-05b89a68"
    },
    {
      "attachments":
      [
        {
          "attachTime": "2012-06-25T17:52:06Z",
          "class": "com.amazonaws.services.ec2.model.VolumeAttachment",
          "deleteOnTermination": true,
          "device": "/dev/sda1",
          "instanceId": "i-fc549e85",
          "state": "attached",
          "volumeId": "vol-05d89a6b"
        }
      ],
      "availabilityZone": "us-east-1e",
      "class": "com.amazonaws.services.ec2.model.Volume",
      "createTime": "2012-06-25T17:51:22Z",
      "size": 10,
      "snapshotId": "snap-9765e1e9",
      "state": "in-use",
      "tags":
      [
        {
          "class": "com.amazonaws.services.ec2.model.Tag",
          "key": "janitor_metadata",
          "value": "{\\"instance\\":\\"i-fc549e85\\",\\"owner\\":\\"espengler@whoyagonnacall.com\\"}"
        }
      ],
      "volumeId": "vol-05d89a6b"
    }
  ]
'''
}
