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
  <title>${name} Auto Scaling Activities</title>
</head>
<body>
<div class="body">
  <h1>${count} Auto Scaling Activities for <g:linkObject type="autoScaling" name="${name}"/> in ${region.description}</h1>
  <g:if test="${flash.message}">
    <div class="message">${flash.message}</div>
  </g:if>
  <g:form method="post">
    <div class="buttons">
    </div>
    <div class="list">
      <table class="sortable activities">
        <thead>
        <tr>
          <th>Description</th>
          <th>Cause</th>
          <th>Start Time</th>
          <th>End Time</th>
          <th>Status Code</th>
          <th>Status Message</th>
          <th>Progress</th>
          <th>Details</th>
          <th>ID</th>
        </tr>
        </thead>
        <tbody>
        <g:each in="${activities}" status="i" var="activity">
          <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
            <td>${activity.description}</td>
            <td>${activity.cause}</td>
            <td><g:formatDate date="${activity.startTime}"/></td>
            <td><g:formatDate date="${activity.endTime}"/></td>
            <td>${activity.statusCode}</td>
            <td>${activity.statusMessage}</td>
            <td>${activity.progress}%</td>
            <td class="details">${activity.details}</td>
            <td>${activity.activityId}</td>
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
