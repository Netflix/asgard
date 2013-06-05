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
  <title>Route53 Hosted Zones</title>
</head>
<body>
<div class="body">
  <h1>Route53 Hosted Zones</h1>
  <g:if test="${flash.message}">
    <div class="message">${flash.message}</div>
  </g:if>
  <g:form method="post">
    <div class="list">
      <div class="buttons">
        <g:link class="create" action="create">Create New Hosted Zone</g:link>
      </div>
      <table class="sortable">
        <thead>
        <tr>
          <th>Hosted Zone ID</th>
          <th>Name</th>
          <th>Resource Record<br/>Set Count</th>
          <th>Comment</th>
        </tr>
        </thead>
        <tbody>
        <g:each var="hostedZone" in="${hostedZones}" status="i">
          <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
            <td><g:linkObject type="hostedZone" name="${hostedZone.id}"/></td>
            <td>${hostedZone.name}</td>
            <td>${hostedZone.resourceRecordSetCount}</td>
            <td>${hostedZone.config.comment}</td>
          </tr>
        </g:each>
        </tbody>
      </table>
      <footer/>
    </div>
  </g:form>
</div>
</body>
</html>
