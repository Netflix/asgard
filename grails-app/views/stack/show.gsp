<%--

    Copyright 2013 Netflix, Inc.

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
  <title>Stack</title>
</head>
<body>
<div class="body">
  <h1>Stack '${params.id}' in ${region.description}</h1>
  <g:if test="${flash.message}">
    <div class="message">${flash.message}</div>
  </g:if>
  <div class="list">
    <table class="sortable">
      <thead>
      <tr>
        <th>App</th>
        <g:if test="${isSignificantStack}">
          <th>Healthy<br />Instances</th>
        </g:if>
        <th>All Instances</th>
        <th>Group Name</th>
        <th title="Availability Zones">Av Zones</th>
        <th>Build</th>
        <th>Last Push</th>
      </tr>
      </thead>
      <tbody>
      <g:each in="${stackAsgs}" status="i" var="stackAsg">
        <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
          <g:if test="${stackAsg.appName in registeredAppNames}">
            <td class="app"><g:linkObject type="application" name="${stackAsg.appName}"/></td>
          </g:if>
          <g:else>
            <td class="error app" title="The ${stackAsg.appName} app is not registered">${stackAsg.appName}</td>
          </g:else>
          <g:if test="${isSignificantStack}">
            <td><div class="${stackAsg.healthDescription}">${stackAsg.healthyInstances}</div></td>
          </g:if>
          <td class="countAndList hideAdvancedItems">
            <span class="toggle fakeLink">${stackAsg.group.instances.size()}</span>
            <div class="advancedItems tiny">
              <g:each var="ins" in="${stackAsg.group.instances}">
                <g:linkObject name="${ins.instanceId}"/><br/>
              </g:each>
            </div>
          </td>
          <td class="autoScaling"><g:linkObject type="autoScaling" name="${stackAsg.group.autoScalingGroupName}"/></td>
          <td class="availabilityZone">
            <g:each var="zone" in="${stackAsg.group.availabilityZones.sort()}">
              <div><g:availabilityZone value="${zone}"/></div>
            </g:each>
          </td>
          <td>
            <g:if test="${stackAsg.appVersion?.buildJobName && buildServer}">
              <a href="${buildServer}/job/${stackAsg.appVersion.buildJobName}/${stackAsg.appVersion.buildNumber}/"
                 class="builds">${stackAsg.appVersion?.buildNumber}</a>
            </g:if>
            <g:else>${stackAsg.appVersion?.buildNumber}</g:else>
          </td>
          <td><g:formatDate date="${stackAsg.launchConfig.createdTime}"/></td>
        </tr>
      </g:each>
      </tbody>
    </table>
  </div>
  <div class="paginateButtons">
  </div>
</div>
</body>
</html>
