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
  <title>Edit Load Balancer</title>
</head>
<body>
  <div class="body">
    <h1>Edit Load Balancer</h1>
    <g:if test="${flash.message}">
      <div class="message">${flash.message}</div>
    </g:if>
    <g:hasErrors bean="${loadBalancer}">
      <div class="errors">
        <g:renderErrors bean="${loadBalancer}" as="list"/>
      </div>
    </g:hasErrors>
    <g:form method="post">
      <input type="hidden" name="name" value="${loadBalancer.loadBalancerName}"/>
      <div class="dialog">
        <table>
          <tbody>
          <tr class="prop">
            <td class="name">
              <label>Name:</label>
            </td>
            <td>${loadBalancer.loadBalancerName}</td>
          </tr>
          <tr class="prop">
            <td class="name">
              <label for="selectedZones">Availablity Zones:</label>
            </td>
            <td>
              <select multiple="true" size="5" id="selectedZones" name="selectedZones">
                <g:each var="z" in="${zoneList}">
                  <g:if test="${loadBalancer.availabilityZones.contains(z.zoneName)}">
                    <option selected value="${z.zoneName}">${z.zoneName}</option>
                  </g:if>
                  <g:else>
                    <option value="${z.zoneName}">${z.zoneName}</option>
                  </g:else>
                </g:each>
              </select>
            </td>
          </tr>
          <tr class="prop">
            <td class="name">
              <label>Health Check:</label>
            </td>
            <td class="numbers">
              <label for="target">Target:</label><input class="string" type="text" id="target" name="target" value="${loadBalancer.healthCheck.target}">
              <label for="interval">Interval:</label><input type="text" id="interval" name="interval" value="${loadBalancer.healthCheck.interval}">
              <label for="timeout">Timeout:</label><input type="text" id="timeout" name="timeout" value="${loadBalancer.healthCheck.timeout}">
              <label for="unhealthy">Unhealthy Threshold:</label><input type="text" id="unhealthy" name="unhealthy" value="${loadBalancer.healthCheck.unhealthyThreshold}">
              <label for="healthy">Healthy Threshold:</label><input type="text" id="healthy" name="healthy" value="${loadBalancer.healthCheck.healthyThreshold}">
            </td>
          </tr>
          </tbody>
        </table>
      </div>
      <div class="buttons">
        <g:buttonSubmit class="save" value="Update Load Balancer" action="update"/>
      </div>
    </g:form>
  </div>
</body>
</html>
