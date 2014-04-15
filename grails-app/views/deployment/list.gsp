<%--

    Copyright 2014 Netflix, Inc.

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
  <title>Deployments</title>
</head>
<body>
<div class="body">
  <g:if test="${flash.message}">
    <div class="message">${flash.message}</div>
  </g:if>
  <h1>Deployments</h1>
  <div class="list">
    <div class="buttons"></div>
    <table class="sortable">
      <thead>
      <tr>
        <th>Started</th>
        <th>Status</th>
        <th>Cluster</th>
        <th>Owner</th>
        <th>Region</th>
      </tr>
      </thead>
      <tbody>
      <g:each var="deployment" in="${deployments}" status="i">
        <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
          <td>
            <g:link class="deploy" controller="ng" action=" " fragment="deployment/detail/${deployment.id}">${deployment.startTime}</g:link>
          </td>
          <td>${deployment.status}</td>
          <td><g:linkObject type="cluster" id="${deployment.clusterName}" name="${deployment.clusterName}"/></td>
          <td>${deployment.owner}</td>
          <td>${deployment.region}</td>
        </tr>
      </g:each>
      </tbody>
    </table>
  </div>
</div>
</body>
</html>
