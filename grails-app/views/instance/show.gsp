<%--

    Copyright 2012 Netflix, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

--%>
<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  <meta name="layout" content="main"/>
  <title>${appName} ${instance?.instanceId} Instance</title>
</head>
<body>
  <div class="body">
    <h1>Instance Details</h1>
    <g:if test="${flash.message}">
      <div class="message">${flash.message}</div>
    </g:if>
    <g:if test="${instance}">
      <g:form class="validate">
        <input type="hidden" name="instanceId" value="${instance.instanceId}"/>
        <g:if test="${group?.desiredCapacity}">
          <div class="buttons">
            <h3>ASG Decrement:</h3>
            <g:buttonSubmit class="stop"
                    data-warning="Really Terminate instance ${instance.instanceId} and decrement size of auto scaling group ${group.autoScalingGroupName} to ${group.desiredCapacity - 1}?"
                    action="terminateAndShrinkGroup"
                    value="Shrink ASG ${group.autoScalingGroupName} to Size ${group.desiredCapacity - 1} and Terminate Instance"
                    title="Terminate this instance and decrement the size of its auto scaling group." />
          </div>
        </g:if>
        <div class="buttons">
          <h3>Operating System:</h3>
          <g:buttonSubmit class="stop" data-warning="Really Terminate: ${instance.instanceId}?"
                  action="terminate" value="Terminate Instance" title="Shut down and delete this instance." />
          <g:buttonSubmit class="shutdown" data-warning="Really Reboot: ${instance.instanceId}?"
                  action="reboot" value="Reboot Instance" title="Restart the OS of the instance." />
          <g:link class="cli" action="raw" params="[instanceId: instance.instanceId]" title="Display the operating system console output log.">Console Output (Raw)</g:link>
          <g:link class="userData" action="userDataHtml" params="[id: instance.instanceId]" title="Display the user data executed by the instance on startup.">User Data</g:link>
        </div>
        <div class="buttons">
          <h3>Load Balancing:</h3>
          <g:buttonSubmit class="requireLogin removeBalance"
                  action="deregister" value="Deregister Instance from LB" title="Remove this instance from the auto scaling group's load balancers." />
          <g:buttonSubmit class="requireLogin instanceBalance"
                  action="register" value="Register Instance with ASG's LB" title="Add this instance to the auto scaling group's load balancers." />
          <g:link class="attachElastic" action="associate" params="[instanceId:instance.instanceId]"
                    title="Choose an elastic IP address to use for this instance.">Associate Elastic IP with Instance</g:link>
        </div>
        <g:if test="${appName}">
          <input type="hidden" name="appName" value="${appName}"/>
          <div class="buttons">
            <h3>Eureka:</h3>
            <g:buttonSubmit class="requireLogin outOfService"
                    action="takeOutOfService" value="Deactivate in Eureka" title="Prevent Eureka from listing this instance for use by other applications." />
            <g:buttonSubmit class="requireLogin inService"
                    action="putInService" value="Activate in Eureka" title="Allow Eureka to list this instance for use by other applications." />
          </div>
        </g:if>
      </g:form>
    </g:if>
    <div class="dialog">
      <table>
        <tbody>
        <tr class="prop" title="Application name from Cloud Application Registry">
          <td class="name">Application:</td>
          <td class="value">
            <g:if test="${appName}">
              <g:linkObject type="application" name="${appName}"/>
            </g:if>
          </td>
        </tr>
        <tr class="prop" title="Effective public hostname from Eureka or EC2">
          <td class="name">DNS Name:</td>
          <td class="value">${baseServer}</td>
        </tr>
        <g:each in="${linkGroupingsToListsOfTextLinks}" var="groupingToTextLinks">
          <tr>
            <td class="name">${groupingToTextLinks.key}:</td>
            <td class="value">
              <g:each in="${groupingToTextLinks.value}" var="textLink">
                <a href="${textLink.url}">${textLink.text}</a><br/>
              </g:each>
            </td>
          </tr>
        </g:each>
        <g:if test="${discoveryExists}">
          <tr class="prop">
            <td><h2 title="Information from Eureka">Eureka</h2></td>
          </tr>
          <g:if test="${discInstance}">
            <tr class="prop">
              <td class="name">DNS Name/IP:</td>
              <td class="value">${discInstance.hostName} | ${discInstance.ipAddr}</td>
            </tr>
            <tr class="prop">
              <td class="name">Port:</td>
              <td class="value">${discInstance.port}</td>
            </tr>
            <tr class="prop">
              <td class="name">Status Page:</td>
              <td class="value"><a href="${discInstance.statusPageUrl}">${discInstance.statusPageUrl}</a></td>
            </tr>
            <tr class="prop">
              <td class="name">Health Check:</td>
              <td class="value"><a href="${discInstance.healthCheckUrl}">${discInstance.healthCheckUrl}</a> : (${healthCheck})</td>
            </tr>
            <tr class="prop">
              <td class="name">VIP Address:</td>
              <td class="value">${discInstance.vipAddress}</td>
            </tr>
            <tr class="prop">
              <td class="name">Lease Info</td>
              <td>
                <table>
                  <g:each var="data" in="${discInstance.leaseInfo}" status="i">
                    <tr class="prop">
                      <td class="name">${data.key}:</td>
                      <td class="value">${data.value}</td>
                    </tr>
                  </g:each>
                </table>
              </td>
            </tr>
            <tr class="prop">
              <td class="name">Status:</td>
              <td class="value ${discInstance.status == "UP" ? "inService" : "outOfService"}">${discInstance.status}</td>
            </tr>
          </g:if>
          <g:else>
            <tr><td>Not found in Eureka</td></tr>
          </g:else>
        </g:if>
        <tr class="prop">
          <td><h2 title="Information from AWS EC2">EC2</h2></td>
        </tr>
        <g:if test="${instance}">
          <tr class="prop">
            <td class="name">Instance ID:</td>
            <td class="value">${instance.instanceId}</td>
          </tr>
          <g:if test="${instance.spotInstanceRequestId}">
            <tr class="prop">
              <td class="name">Spot Instance Request:</td>
              <td class="value"><g:linkObject type="spotInstanceRequest" name="${instance.spotInstanceRequestId}" /></td>
            </tr>
          </g:if>
          <tr class="prop">
            <td class="name">Public DNS/IP:</td>
            <td class="value">${instance.publicDnsName} | ${instance.publicIpAddress}</td>
          </tr>
          <tr class="prop">
            <td class="name">Private DNS/IP:</td>
            <td class="value">${instance.privateDnsName} | ${instance.privateIpAddress}</td>
          </tr>
          <tr class="prop">
            <td class="name">Image:</td>
            <td class="value">
              <g:linkObject type="image" name="${instance.imageId}"/>${image ? ' | ' + image.architecture + ' | ' + image.imageLocation : ''}
            </td>
          </tr>
          <tr class="prop">
            <td class="name"><g:link controller="instanceType" action="list">Instance Type:</g:link></td>
            <td class="value">${instance.instanceType}</td>
          </tr>
          <tr class="prop">
            <td class="name">Zone:</td>
            <td class="value"><g:availabilityZone value="${instance.placement.availabilityZone}"/></td>
          </tr>
          <tr class="prop">
            <td class="name">State (Transition Reason):</td>
            <td class="value">${instance.state.name} | ${instance.stateTransitionReason} | ${instance.stateReason?.message}</td>
          </tr>
          <tr class="prop">
            <td class="name">Launch Time:</td>
            <td class="value"><g:formatDate date="${instance.launchTime}"/></td>
          </tr>
          <tr class="prop">
            <td class="name">Key Name:</td>
            <td class="value">${instance.keyName}</td>
          </tr>
          <tr class="prop">
            <td class="name">Ami Launch Index:</td>
            <td class="value">${instance.amiLaunchIndex}</td>
          </tr>
          <tr class="prop">
            <td class="name">KernelId:</td>
            <td class="value">${instance.kernelId}</td>
          </tr>
          <tr class="prop">
            <td class="name">RamdiskId:</td>
            <td class="value">${instance.ramdiskId}</td>
          </tr>
          <tr class="prop">
            <td class="name">Platform:</td>
            <td class="value">${instance.platform}</td>
          </tr>
          <tr class="prop">
            <td class="name">Monitoring:</td>
            <td class="value">${instance.monitoring.state}</td>
          </tr>
          <tr class="prop">
            <td class="name">Subnet ID:</td>
            <td class="value">${instance.subnetId}</td>
          </tr>
          <tr class="prop">
            <td class="name">VPC ID:</td>
            <td class="value">${instance.vpcId}</td>
          </tr>
          <tr class="prop">
            <td class="name">Root Device Type:</td>
            <td class="value">${instance.rootDeviceType}</td>
          </tr>
          <tr class="prop">
            <td class="name">Root Device Name:</td>
            <td class="value">${instance.rootDeviceName}</td>
          </tr>
          <tr class="prop">
            <td class="name">Block Device Mappings:</td>
            <td class="name">
              <table>
                <g:each var="mapping" in="${instance.blockDeviceMappings}">
                  <tr class="prop"><td class="value">${mapping.deviceName} : ${mapping.ebs.volumeId}</td></tr>
                </g:each>
              </table>
            </td>
          </tr>
          <tr class="prop">
            <td class="name">Product Code:</td>
            <td class="name">
              <table>
                <g:each var="data" in="${instance.productCodes}">
                  <tr class="prop"><td class="value">${data}</td></tr>
                </g:each>
              </table>
            </td>
          </tr>
          <tr class="prop">
            <td class="name">Security Groups:</td>
            <td class="value">
              <table>
                <g:each var="g" in="${securityGroups}">
                  <tr class="prop">
                    <td class="value"><g:linkObject type="security" name="${g.groupId}">${g.groupName}</g:linkObject></td>
                  </tr>
                </g:each>
              </table>
            </td>
          </tr>
          <g:render template="/common/showTags" model="[entity: instance]"/>
        </g:if>

        <tr class="prop">
          <td><h2 title="Information cross-referenced from other objects">Referenced From</h2></td>
        </tr>
        <tr class="prop">
          <td class="name">Cluster:</td>
          <td class="value">
            <g:if test="${cluster}">
              <g:linkObject type="cluster" name="${cluster}"/>
            </g:if>
          </td>
        </tr>
        <tr class="prop">
          <td class="name">AutoScaling Group:</td>
          <td class="value">
            <g:if test="${group}">
              <g:linkObject type="autoScaling" name="${group?.autoScalingGroupName}"/>
            </g:if>
          </td>
        </tr>
        <tr class="prop">
          <td class="name">Load Balancers:</td>
          <td class="value">
            <table>
              <g:each var="loadBalancer" in ="${loadBalancers}">
                <tr class="prop">
                  <td class="value"><g:linkObject type="loadBalancer" name="${loadBalancer.loadBalancerName}"/></td>
                </tr>
              </g:each>
            </table>
          </td>
        </tr>
        </tbody>
      </table>
    </div>
  </div>
</body>
</html>
