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
  <title>Auto Scaling Groups</title>
</head>
<body>
<div class="body">
  <h1>Auto Scaling Groups in ${region.description}${appNames ? ' for ' + appNames : ''}</h1>
  <g:if test="${flash.message}">
    <div class="message">${flash.message}</div>
  </g:if>
  <g:form method="post">
    <div class="list">
      <div class="buttons">
        <g:link class="create" action="create">Create New Auto Scaling Group</g:link>
      </div>
      <table class="sortable">
        <thead>
        <tr>
          <th>Group Name</th>
          <th>Cluster</th>
          <th>App</th>
          <th class="sorttable_alpha">Vars</th>
          <th>Launch Configuration</th>
          <th class="tiny">Min</th>
          <th class="tiny">Max</th>
          <th class="tiny">Des</th>
          <th class="tiny">Cool</th>
          <th title="Availability Zones">Av Zones</th>
          <th>Load Balancers</th>
          <th>Instances</th>
          <th class="sorttable_alpha">Scaling Policies</th>
          <th>Created Time</th>
        </tr>
        </thead>
        <tbody>
        <g:each in="${autoScalingGroups}" status="i" var="group">
          <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
            <td class="autoScaling"><g:linkObject type="autoScaling" name="${group.autoScalingGroupName}"/></td>
            <td class="cluster"><g:linkObject type="cluster" name="${group.clusterName}"/></td>
            <g:set var="appName" value="${groupNamesToAppNames[group.autoScalingGroupName]}"/>
            <g:if test="${groupsWithValidAppNames.contains(group.autoScalingGroupName)}">
              <td class="app"><g:linkObject type="application" name="${appName}"/></td>
            </g:if>
            <g:else>
              <td class="error app" title="The ${appName} app is not registered">${appName}</td>
            </g:else>
            <td class="variables">
              <g:each in="${group.variables}" var="${entry}">
                <span class="tagKey">${entry.key}:</span> ${entry.value}<br/>
              </g:each>
            </td>
            <td class="launchConfig"><g:linkObject type="launchConfiguration" name="${group.launchConfigurationName}"/></td>
            <td>${group.minSize}</td>
            <td>${group.maxSize}</td>
            <td>${group.desiredCapacity}</td>
            <td>${group.defaultCooldown}</td>
            <td class="availabilityZone">
              <g:each var="zone" in="${group.availabilityZones.sort()}">
                <div><g:availabilityZone value="${zone}"/></div>
              </g:each>
            </td>
            <td class="loadBalancer countAndList hideAdvancedItems">
              <g:if test="${group.loadBalancerNames.size() == 1}">
                <g:linkObject type="loadBalancer" name="${group.loadBalancerNames[0]}"/>
              </g:if>
              <g:elseif test="${group.loadBalancerNames.size() > 1}">
                <span class="toggle fakeLink">${group.loadBalancerNames.size()}</span>
                <div class="advancedItems">
                  <g:each var="balancer" in="${group.loadBalancerNames}">
                    <g:linkObject type="loadBalancer" name="${balancer}"/><br/>
                  </g:each>
                </div>
              </g:elseif>
            </td>
            <td class="countAndList hideAdvancedItems">
              <span class="toggle fakeLink">${group.instances.size()}</span>
              <div class="advancedItems tiny">
                <g:each var="ins" in="${group.instances}">
                  <g:linkObject name="${ins.instanceId}"/><br/>
                </g:each>
              </div>
            </td>
            <td class="countAndList hideAdvancedItems">
              <span class="toggle fakeLink">${groupNamesToScalingPolicies[group.autoScalingGroupName]?.size() ?: 0}</span>
              <div class="advancedItems tiny">
                <g:each var="scalingPolicy" in="${groupNamesToScalingPolicies[group.autoScalingGroupName]}">
                  <g:linkObject type="scalingPolicy" name="${scalingPolicy.policyName}">${scalingPolicy.toDisplayValue()}</g:linkObject><br/>
                </g:each>
              </div>
            </td>
            <td><g:formatDate date="${group.createdTime}"/></td>
          </tr>
        </g:each>
        </tbody>
      </table>
    </div>
    <footer/>
  </g:form>
</div>
</body>
</html>
