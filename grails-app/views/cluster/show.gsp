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
<%@ page import="com.netflix.asgard.push.GroupResizeOperation" %>
<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  <meta name="layout" content="main"/>
  <title>${env} ${cluster.name} cluster</title>
</head>
<body>
  <div class="body cluster">
    <div class="intro">
      <h1>Manage Cluster of Sequential Auto Scaling Groups</h1>
      <g:if test="${flash.message}">
        <div class="message">${flash.message}</div>
      </g:if>
      <g:if test="${params.autoDeploy}">
        <div class="buttons">
          <g:link class="deploy" action="prepareDeployment"
                  params="[id: cluster.name]">Prepare Automated Deployment</g:link>
          <g:link class="create" action="prepareNextAsg" params="[id: cluster.name]">Create Next Group '${nextGroupName}'</g:link>
        </div>
      </g:if>
      <p>
        Recommended next step: <br/>
        <em>${recommendedNextStep}</em>
      </p>
      <g:if test="${runningTasks}">
        <h3>Running tasks:</h3>
        <ul class="tasks">
          <g:each var="task" in="${runningTasks}">
            <li><g:link class="task" controller="task" action="show" params="[id:task.id]">${task.name}</g:link></li>
          </g:each>
        </ul>
      </g:if>
    </div>
    <g:if test="${instanceType && zoneAvailabilities}">
      <table class="reservations" title="Reserved instances are cheaper and more likely to be available than on demand instances">
        <caption>Reservations for ${instanceType}</caption>
        <thead>
        <tr>
          <th>Zone</th>
          <th title="Reservations that are currently unused and available for use">Available</th>
        </tr>
        </thead>
        <tbody>
        <g:each var="za" in="${zoneAvailabilities}">
          <tr>
            <td><g:availabilityZone value="${za.zoneName}"/></td>
            <td title="${za.low ? 'Only ' : ''}${za.percentAvailable}% of ${za.totalReservations} reservations" ${za.low ? 'class="danger"' : ''}>${za.availableReservations}</td>
          </tr>
        </g:each>
        </tbody>
      </table>
    </g:if>
    <ul class="groupReplacingPush">
      <g:each var="autoScalingGroup" in="${cluster}">
        <li class="clusterAsgForm ${autoScalingGroup.seemsDisabled() ? 'disabledGroup' : ''}">
          <g:form method="post" class="validate">
            <g:hiddenField name="name" value="${autoScalingGroup.autoScalingGroupName}"/>
            <h2><g:linkObject type="autoScaling" name="${autoScalingGroup.autoScalingGroupName}"/></h2>
            <g:if test="${autoScalingGroup.expirationDurationString}">
              <div class="expiration">Expires in ${autoScalingGroup.expirationDurationString}</div>
            </g:if>
            <g:if test="${autoScalingGroup.suspendedPrimaryProcessTypes}">
              <div class="suspendProcess">${autoScalingGroup.suspendedPrimaryProcessTypes.join(' and ')} ${autoScalingGroup.suspendedPrimaryProcessTypes.size() == 1 ? 'is' : 'are'} disabled</div>
            </g:if>
            <g:if test="${autoScalingGroup.deleteInProgress}">
              <div class="deleting">${autoScalingGroup.status}</div>
            </g:if>
            <g:else>
              <div class="buttons requireLogin">
                <g:buttonSubmit class="resize" action="resize" value="Resize" /> <label for="minAndMaxSize_${autoScalingGroup.autoScalingGroupName}">to</label>
                <g:if test="${autoScalingGroup.minSize == autoScalingGroup.maxSize}"  >
                  <input type="text" size="2" class="groupSize number" id="minAndMaxSize_${autoScalingGroup.autoScalingGroupName}" name="minAndMaxSize" value="${autoScalingGroup.maxSize}"/>
                </g:if>
                <g:else>
                  <input type="text" size="2" class="groupSize number" id="minSize_${autoScalingGroup.autoScalingGroupName}" name="minSize" value="${autoScalingGroup.minSize}"/>
                  <label for="minSize_${autoScalingGroup.autoScalingGroupName}">min</label> /
                  <input type="text" size="2" class="groupSize number" id="maxSize_${autoScalingGroup.autoScalingGroupName}" name="maxSize" value="${autoScalingGroup.maxSize}"/>
                  <label for="maxSize_${autoScalingGroup.autoScalingGroupName}">max</label>
                </g:else>
                <div class="batchResizeContainer" id="batchResizeContainer_${autoScalingGroup.autoScalingGroupName}">
                  <label for="batchSize_${autoScalingGroup.autoScalingGroupName}">in batches of</label>
                  <input disabled="disabled" type="text" size="2" class="number" id="batchSize_${autoScalingGroup.autoScalingGroupName}" name="batchSize" value="${GroupResizeOperation.DEFAULT_BATCH_SIZE}"/>
                  every ${GroupResizeOperation.MINUTES_BETWEEN_BATCHES} minutes
                </div>
              </div>
              <div class="buttons">
                <g:buttonSubmit class="delete" action="delete" value="Delete"
                                data-warning="Terminate: ${autoScalingGroup.instances.size()} instances and delete Auto Scaling Group '${autoScalingGroup.autoScalingGroupName}'?"/>
                <g:buttonSubmit class="requireLogin trafficDisable" action="deactivate" value="Disable" />
                <g:buttonSubmit class="requireLogin trafficEnable" action="activate" value="Enable" />
              </div>
            </g:else>
            <table class="tiny">
              <g:set var="total" value="${autoScalingGroup.instances.size()}"/>
              <caption>${total} instance${total == 1 ? '' : 's grouped by state'}</caption>
              <tr>
                <th>Count</th>
                <th>State</th>
                <th>Build</th>
                <th>ELB</th>
                <g:if test="${discoveryExists}">
                  <th>Eureka</th>
                </g:if>
              </tr>
              <g:set var="statesToInstanceLists" value="${autoScalingGroup.statesToInstanceList}"/>
              <g:set var="instanceStates" value="${statesToInstanceLists.keySet()}"/>
              <g:each in="${instanceStates}" var="instanceState">
                <tbody class="countAndList hideAdvancedItems">
                <tr>
                  <td><span class="toggle fakeLink">${statesToInstanceLists[instanceState].size()}</span></td>
                  <td>${instanceState.lifecycleState}</td>
                  <td>
                    <g:if test="${instanceState.buildNumber && instanceState.buildJobName && buildServer}">
                      <a href="${buildServer}/job/${instanceState.buildJobName}/${instanceState.buildNumber}/"
                         class="builds">${instanceState.buildNumber}</a>
                    </g:if>
                    <g:else>
                      <g:linkObject type="image" name="${instanceState.imageId}"/>
                    </g:else>
                  </td>
                  <td>
                    <g:each in="${instanceState.loadBalancers}" var="elbName" status="i">
                      <g:linkObject type="loadBalancer" name="${elbName}" compact="true"/>
                      <g:if test="${i % 5 == 4}"><br/></g:if>
                    </g:each>
                  </td>
                  <g:if test="${discoveryExists}">
                    <td class="${instanceState.discoveryStatus == "UP" ? "inService" : "outOfService"}">${instanceState.discoveryStatus ?: 'N/A'}</td>
                  </g:if>
                </tr>
                <tr class="advancedItems">
                  <td colspan="100%">
                    <g:each in="${statesToInstanceLists[instanceState]}" var="instance">
                      <g:linkObject type="instance" name="${instance.instanceId}"/>
                    </g:each>
                  </td>
                </tr>
                </tbody>
              </g:each>
            </table>
          </g:form>
        </li>
      </g:each>
      <g:if test="${okayToCreateGroup}">
        <li class="clusterAsgForm create hideAdvancedItems">
          <g:if test="${!requireLoginForEdit}">
            <g:form method="post" class="validate" action="createNextGroup">
              <g:hiddenField name="name" value="${cluster.name}" />
              <g:hiddenField name="noOptionalDefaults" value="true" />
              <h2>Create Next Group:</h2>
              <span class="toggle fakeLink" id="showAdvancedOptionsToCreateNextGroup">Advanced Options</span>
              <div class="clear"></div>
              <h2>${nextGroupName}</h2>
              <table>
                <tr class="advanced"><td colspan="2"><h2>Auto Scaling</h2></td></tr>
                <g:render template="/autoScaling/autoScalingOptions" />
                <g:render template="/loadBalancer/selection"/>
                <g:render template="/launchConfiguration/launchConfigOptions" />
                <g:render template="/push/startupOptions" />
                <tr class="advanced">
                  <td>
                    <label for="trafficAllowed">Enable traffic?</label>
                  </td>
                  <td>
                    <input id="trafficAllowed" type="checkbox" name="trafficAllowed" checked="checked" />
                    <label for="trafficAllowed">Send client requests to new instances</label>
                  </td>
                </tr>
              </table>
              <div class="buttons">
                <g:buttonSubmit class="create" action="createNextGroup" value="Create Next Group ${nextGroupName}" />
              </div>
            </g:form>
          </g:if>
          <g:else>
            <g:link controller="auth" action="login" params="${[targetUri: targetUri]}">
              Login to enable Create Next Group.
            </g:link>
          </g:else>
        </li>
      </g:if>
    </ul>
  </div>
</body>
</html>
