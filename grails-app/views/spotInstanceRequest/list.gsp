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
  <title>Spot Instance Requests in ${region.description}</title>
</head>
<body>
  <div class="body">
    <h1>Spot Instance Requests in ${region.description} (Beta)</h1>
    <g:if test="${flash.message}">
      <div class="message">${flash.message}</div>
    </g:if>
    <g:form method="post" class="validate">
      <input type="hidden" name="appNames" value="${params.id}"/>
      <div class="list">
        <div class="buttons">
          <g:buttonSubmit class="stop" value="Cancel Spot Instance Request(s)" action="cancel"
                  data-warning="Really cancel spot instance request(s)?"/>
        </div>
        <table class="sortable">
          <thead>
          <tr>
            <th>x</th>
            <th>Spot Instance<br/>Request ID</th>
            <th>Spot<br/>Price<br/>Max</th>
            <th>Type</th>
            <th>State</th>
            <th>AZ</th>
            <th>Instance</th>
            <th>Create Time</th>
          </tr>
          </thead>
          <tbody>
          <g:each var="sir" in="${spotInstanceRequests}" status="i">
            <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
              <td><g:checkBox class="requireLogin" name="selectedSpotInstanceRequests" value="${sir.spotInstanceRequestId}" checked="false"/></td>
              <td><g:linkObject name="${sir.spotInstanceRequestId}" /></td>
              <td>${sir.spotPrice}</td>
              <td>${sir.type}</td>
              <td>${sir.state}</td>
              <td><g:availabilityZone value="${sir.launchSpecification.placement?.availabilityZone}"/></td>
              <td><g:linkObject type="instance" name="${sir.instanceId}"/></td>
              <td>${sir.createTime}</td>
            </tr>
          </g:each>
          </tbody>
        </table>
      </div>
    </g:form>
  </div>
</body>
</html>
