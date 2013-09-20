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
<%@ page import="com.netflix.asgard.AwsAutoScalingService; java.text.NumberFormat" %>
<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  <meta name="layout" content="main"/>
  <title>${group.autoScalingGroupName} Auto Scaling Group</title>
</head>
<body>
  <div class="body">
    <h1>Auto Scaling Group Details</h1>
    <g:if test="${flash.message}">
      <div class="message">${flash.message}</div>
    </g:if>
    <g:set var="groupName" value="${group.autoScalingGroupName}"/>
    <g:form>
      <div class="buttons">
        <input type="hidden" name="name" value="${groupName}"/>
        <g:if test="${!group.deleteInProgress}">
          <g:link class="edit" action="edit" params="[id:groupName]">Edit Auto Scaling Group</g:link>
          <g:buttonSubmit class="delete" data-warning="Really delete Auto Scaling Group '${groupName}'?"
                          action="delete">Delete Auto Scaling Group</g:buttonSubmit>
          <g:link title="Set up a push operation that will replace instances within the existing group"
                  class="pushRolling" controller="push" action="editRolling" params="[id: groupName]">Prepare Rolling Push</g:link>
        </g:if>
        <g:link title="Set up a push operation that will create a new auto scaling group"
                class="pushReplace" controller="cluster" action="show" params="[id: clusterName]">Manage Cluster of Sequential ASGs</g:link>
      </div>
      <g:if test="${group.expirationDurationString}">
        <div class="buttons">
          <div class="expiration">Expires in ${group.expirationDurationString}</div>
          <g:if test="${showPostponeButton}">
            <g:buttonSubmit class="schedule" action="postpone">Postpone Deletion an Extra Day</g:buttonSubmit>
          </g:if>
        </div>
      </g:if>
    </g:form>
    <div>
      <table>
        <tbody>
        <g:if test="${group.status}">
          <tr class="prop">
            <td class="name">Status:</td>
            <td class="value ${group.deleteInProgress ? 'deleting' : ''}">${group.status}</td>
          </tr>
        </g:if>
        <tr class="prop">
          <td class="name">Auto Scaling Group:</td>
          <td class="value">${group.autoScalingGroupName}</td>
        </tr>
        <tr class="prop">
          <td class="name">Cluster:</td>
          <td class="value"><g:linkObject type="cluster" name="${clusterName}"/></td>
        </tr>
        <tr class="prop">
          <td class="name">Application:</td>
          <td class="value"><g:linkObject type="application" name="${app?.name}"/></td>
        </tr>
        <g:each in="${group.variables}" var="entry">
          <tr class="prop">
            <td class="name">${entry.key}:</td>
            <td class="value">${entry.value}</td>
          </tr>
        </g:each>
        <tr class="prop">
          <td class="name">Instance Bounds:</td>
          <td class="value subProperties"><label>Min</label> ${group.minSize} <label>Max</label> ${group.maxSize}</td>
        </tr>
        <tr class="prop">
          <td class="name">Desired Size:</td>
          <td class="value subProperties">${group.desiredCapacity} <label>instance${group.desiredCapacity == 1 ? '' : 's'}</label></td>
        </tr>
        <tr class="prop" title="The number of seconds after a scaling activity completes before any further scaling activities can start">
          <td class="name">Cooldown:</td>
          <td class="value">${group.defaultCooldown} second${group.defaultCooldown == 1 ? '' : 's'}</td>
        </tr>
        <tr class="prop" title="The method that the group will use to decide when to replace a problematic instance">
          <td class="name">ASG Health Check Type:</td>
          <td class="value">${group.healthCheckType} (${group.healthCheckType?.description})</td>
        </tr>
        <tr class="prop" title="The number of seconds to wait after instance launch before running the health check">
          <td class="name">ASG Health Check Grace Period:</td>
          <td class="value">${group.healthCheckGracePeriod} second${group.healthCheckGracePeriod == 1 ? '' : 's'}</td>
        </tr>
        <tr class="prop" title="The algorithm to use when selecting which instance to terminate">
          <td class="name">Termination Policies:</td>
          <td class="value">${group.terminationPolicies}</td>
        </tr>
        <tr class="prop">
          <td class="name">Availablility Zones:</td>
          <td class="value">
            <g:each var="zone" in="${group.availabilityZones}">
              <div><g:availabilityZone value="${zone}"/> (${zonesWithInstanceCounts.count(zone)})</div>
            </g:each>
          </td>
        </tr>
        <tr class="prop">
          <td class="name">VPC Purpose:</td>
          <td class="value">${subnetPurpose}</td>
        </tr>
        <tr class="prop" title="Comma-separated list that identifies VPC subnets per zone, if applicable.">
          <td class="name">VPC Zone Identifier:</td>
          <td class="value">${vpcZoneIdentifier}</td>
        </tr>
        <g:render template="/common/showTags" model="[entity: group]"/>
        <tr class="prop">
          <td class="name">AZ Rebalancing:</td>
          <td class="value">${azRebalanceStatus}</td>
        </tr>
        <tr class="prop">
          <td class="name">New Instance Launching:</td>
          <td class="value'}">${launchStatus}</td>
        </tr>
        <tr class="prop">
          <td class="name">Instance Terminating:</td>
          <td class="value'}">${terminateStatus}</td>
        </tr>
        <tr class="prop">
          <td class="name">Adding to Load Balancer:</td>
          <td class="value'}">${addToLoadBalancerStatus}</td>
        </tr>
        <g:if test="${isChaosMonkeyActive}">
          <tr class="prop">
            <td class="name">Chaos Monkey:</td>
            <td class="value"><a class="cloudready" href="${chaosMonkeyEditLink}">Edit in Cloudready</a></td>
          </tr>
        </g:if>
        <tr class="prop">
          <td class="name">Created Time:</td>
          <td class="value"><g:formatDate date="${group.createdTime}"/></td>
        </tr>
        <tr class="prop">
          <td class="name">Load Balancers:</td>
          <td class="value">
            <table>
              <g:each var="lbName" in="${group.loadBalancerNames}">
                <tr class="prop">
                  <td><g:linkObject type="loadBalancer" name="${lbName}"/></td>
                  <g:if test="${mismatchedElbNamesToZoneLists[lbName]}">
                    <td><g:render template="/common/zoneMismatch" model="[asgZones: group.availabilityZones, elbZones: mismatchedElbNamesToZoneLists[lbName]]"/></td>
                  </g:if>
                </tr>
              </g:each>
            </table>
          </td>
        </tr>
        <tr class="prop">
          <td class="name">Launch Configuration:</td>
          <td class="value"><g:linkObject type="launchConfiguration" name="${group.launchConfigurationName}"/></td>
        </tr>
        <g:render template="/launchConfiguration/launchTemplateFields" model="${[launchTemplate: launchConfiguration]}" />

        <tr class="prop">
          <td class="name">Auto Scaling Activities:</td>
          <td class="value">
            <g:if test="${activities.size >= 1}">
              <g:textArea class="resizable" name="log" rows="8" cols="160" readonly="true"><g:each var="a" in="${activities}">
${a.cause} : ${a.description} (${a.progress}% done) (Status: ${a.statusCode})
</g:each></g:textArea>
              <br/>
              <h3>${activities.size} ${activities.size == 1 ? 'activity' : 'activities'} shown.</h3>
              <h3>Click to show activities in a table:</h3>
              <g:each in="[100, 1000, AwsAutoScalingService.UNLIMITED]" var="count">
                <div><g:link class="activities" params="[id:group.autoScalingGroupName, activityCount: count]"
                        action="activities">${count == AwsAutoScalingService.UNLIMITED ? 'Unlimited' : NumberFormat.getInstance().format(count)} activities</g:link></div>
              </g:each>
            </g:if>
            <g:else>None</g:else>
          </td>
        </tr>

        <tr class="prop">
          <td><h2>Scaling Policies</h2></td>
        </tr>
        <tr class="prop">
          <td colspan="2">
            <table>
              <tr>
                <td colspan="100%" class="subitems">
                  <div class="buttons">
                    <span class="count">Total Policies: ${scalingPolicies.size()}</span>
                    <g:link class="create" controller="scalingPolicy" action="create"
                            params="[id: group.autoScalingGroupName]">Create New Scaling Policy</g:link>
                  </div>
                </td>
              </tr>
            </table>
            <g:if test="${scalingPolicies.size() >= 1}">
              <table id="policyTable" class="sortable list">
                <tr>
                  <th>Policy Name</th>
                  <th>Scaling<br/>Adjustment</th>
                  <th>Adjustment<br/>Type</th>
                  <th>Cooldown</th>
                  <th>Alarms</th>
                </tr>
                <g:each var="policy" in="${scalingPolicies}" status="i">
                  <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
                    <td><g:linkObject type="scalingPolicy" name="${policy.policyName}"/></td>
                    <td>${policy.scalingAdjustment}</td>
                    <td>${policy.adjustmentType}</td>
                    <td>${policy.cooldown}</td>
                    <td>
                      <ul class="links">
                        <g:each var="alarm" in="${policy.alarms}">
                          <li>
                            <g:linkObject type="alarm" name="${alarm.alarmName}">${alarmsByName[alarm.alarmName].toDisplayValue()}</g:linkObject>
                          </li>
                        </g:each>
                      </ul>
                    </td>
                  </tr>
                </g:each>
              </table>
            </g:if>
          </td>
        </tr>

        <tr class="prop">
          <td><h2>Scheduled Actions</h2></td>
        </tr>
        <tr class="prop">
          <td colspan="2">
            <table>
              <tr>
                <td colspan="100%" class="subitems">
                  <div class="buttons">
                    <span class="count">Total Actions: ${scheduledActions.size()}</span>
                    <g:link class="create" controller="scheduledAction" action="create"
                            params="[id: group.autoScalingGroupName]">Create New Scheduled Action</g:link>
                  </div>
                </td>
              </tr>
            </table>
            <g:if test="${scheduledActions.size() >= 1}">
              <table id="scheduledActionTable" class="sortable list">
                <tr>
                  <th>Action Name</th>
                  <th>Recurrence</th>
                  <th>Min</th>
                  <th>Max</th>
                  <th>Desired</th>
                </tr>
                <g:each var="action" in="${scheduledActions}" status="i">
                  <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
                    <td><g:linkObject type="scheduledAction" name="${action.scheduledActionName}"/></td>
                    <td>${action.recurrence}</td>
                    <td>${action.minSize}</td>
                    <td>${action.maxSize}</td>
                    <td>${action.desiredCapacity}</td>
                  </tr>
                </g:each>
              </table>
            </g:if>
          </td>
        </tr>

        <tr class="prop">
          <td><h2>Instances</h2></td>
        </tr>
        <tr class="prop">
          <td colspan="2">
            <g:if test="${group.instances.size() >= 1}">
              <g:form method="post" controller="instance" class="validate">
                <input type="hidden" name="autoScalingGroupName" value="${group.autoScalingGroupName}"/>
                <input type="hidden" name="appName" value="${app?.name}"/>
                <g:render template="/group/instanceControls"/>
                <table id="instanceTable" class="sortable list">
                  <tr class="prop">
                    <th class="sorttable_nosort"><input class="requireLogin" type="checkbox" id="allInstances"/></th>
                    <th>Instance</th>
                    <th>Zone</th>
                    <th>State</th>
                    <th>Launch Time</th>
                    <g:if test="${group.mostCommonAppVersion}">
                      <th>Package</th>
                      <th>Ver</th>
                      <th>Commit</th>
                      <th>Build</th>
                    </g:if>
                    <th>ELBs</th>
                    <g:if test="${discoveryExists}">
                      <th>Eureka</th>
                      <th>Health</th>
                    </g:if>
                  </tr>
                  <g:each var="ins" in="${group.instances}" status="i">
                    <tr class="${(i % 2) == 0 ? 'odd' : 'even'} ${ins.appVersion == group.mostCommonAppVersion ? '' : 'inconsistent'}" data-instanceid="${ins.instanceId}">
                      <td><g:checkBox class="requireLogin" name="instanceId" value="${ins.instanceId}" checked="0"/></td>
                      <td class="tiny"><g:linkObject type="instance" name="${ins.instanceId}" /></td>
                      <td><g:availabilityZone value="${ins.availabilityZone}" /></td>
                      <td class="${ins.lifecycleState == 'InService' ? '' : 'emphasized'}">
                        ${ins.lifecycleState}
                      </td>
                      <td><g:formatDate date="${ins.launchTime}"/></td>
                      <g:if test="${group.mostCommonAppVersion}">
                        <td class="appVersion">${ins.appVersion?.packageName}</td>
                        <td class="appVersion">${ins.appVersion?.version}</td>
                        <td class="appVersion">${ins.appVersion?.commit}</td>
                        <td class="appVersion">
                        <g:if test="${ins.appVersion?.buildJobName && buildServer}">
                          <a href="${buildServer}/job/${ins.appVersion.buildJobName}/${ins.appVersion.buildNumber}/"
                             class="builds">${ins.appVersion?.buildNumber}</a>
                        </g:if>
                        <g:else>${ins.appVersion?.buildNumber}</g:else>
                      </g:if>
                      <td>
                        <ul class="links">
                          <g:each var="loadBalancer" in="${ins.loadBalancers}">
                            <li><g:linkObject type="loadBalancer" name="${loadBalancer}" /></li>
                          </g:each>
                        </ul>
                      </td>
                      <g:if test="${discoveryExists}">
                        <g:render template="/group/discoveryAndHealthCells" model="[instance: ins]"/>
                      </g:if>
                    </tr>
                  </g:each>
                </table>
                <g:if test="${group.instances.size() > 10}">
                  <g:render template="/group/instanceControls"/>
                </g:if>
              </g:form>
            </g:if>
            <g:else>None</g:else>
          </td>
        </tr>
        </tbody>
      </table>
    </div>
  </div>
</body>
</html>
