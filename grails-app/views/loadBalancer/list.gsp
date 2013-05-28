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
  <title>Load Balancers</title>
</head>
<body>
<div class="body">
  <h1>Load Balancers in ${region.description}${appNames ? ' for ' + appNames : ''}</h1>
  <g:if test="${flash.message}">
    <div class="message">${flash.message}</div>
  </g:if>
  <g:form method="post">
    <div class="list">
      <div class="buttons">
        <g:link class="create" action="create">Create New Load Balancer</g:link>
      </div>
      <table class="sortable">
        <thead>
        <tr>
          <th>Name</th>
          <th>DNS</th>
          <th>Av Zones</th>
          <th>Policies</th>
          <th>Listeners</th>
          <th>Health Check</th>
          <th>Instances</th>
          <th>Created Time</th>
        </tr>
        </thead>
        <tbody>
        <g:each var="lb" in="${loadbalancers}" status="i">
          <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
            <td><g:linkObject type="loadBalancer" name="${lb.loadBalancerName}"/></td>
            <td>${lb.dNSName}</td>
            <td class="availabilityZone">
              <g:each var="zone" in="${lb.availabilityZones.sort()}">
                <div><g:availabilityZone value="${zone}"/></div>
              </g:each>
            </td>
            <td class="lbPolicies">
              <ul>
                <g:each var="policy" in="${lb.policies.otherPolicies}">
                  <li>${policy}</li>
                </g:each>
              </ul>
            <td>
              <g:each var="ld" in="${lb.listenerDescriptions}">
                ${ld.listener.protocol}:${ld.listener.loadBalancerPort} => ${ld.listener.instancePort}<br>
              </g:each>
            </td>
            <td>
              Target: <span title="${lb.healthCheck.target}">${lb.targetTruncated}</span><br>
              Interval: ${lb.healthCheck.interval}<br>
              Timeout: ${lb.healthCheck.timeout}<br>
              Unhealthy Threshold: ${lb.healthCheck.unhealthyThreshold}<br>
              Healthy Threshold: ${lb.healthCheck.healthyThreshold}<br>
            </td>
            <td class="countAndList hideAdvancedItems">
              <span class="toggle fakeLink">${lb.instances.size()}</span>
              <div class="advancedItems tiny">
                <g:each var="instance" in="${lb.instances}">
                  <g:linkObject type="instance" name="${instance.instanceId}"/><br/>
                </g:each>
              </div>
            </td>
            <td><g:formatDate date="${lb.createdTime}"/></td>
          </tr>
        </g:each>
        </tbody>
      </table>
    </div>
    <div class="paginateButtons">
    </div>
  </g:form>
</div>
</body>
</html>
