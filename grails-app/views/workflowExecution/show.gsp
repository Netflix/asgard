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
  <title>Execution History</title>
</head>
<body>
<div class="body">
  <h1>Workflow Execution History</h1>
  <g:if test="${flash.message}">
    <div class="message">${flash.message}</div>
  </g:if>
  <div class="buttons"></div>
  <table>
    <tr class="prop">
      <td class="name">Description:</td>
      <td class="value">${workflowDescription}</td>
    </tr>
    <tr class="prop">
      <td class="name">Workflow ID:</td>
      <td class="value">${swfWorkflowId}</td>
    </tr>
    <tr class="prop">
      <td class="name">Run ID:</td>
      <td class="value">${swfRunId}</td>
    </tr>
  </table>
  <div class="list">
    <table class="sortable">
      <thead>
      <tr>
        <th>Event</th>
        <th>Scheduled</th>
        <th>Elapsed Time</th>
        <th>Event Type</th>
        <th>Version</th>
        <th>Name</th>
        <th>Input/Result/Context</th>
        <th>Identity</th>
      </tr>
      </thead>
      <tbody>
      <g:each var="eventAttributes" in="${filteredEvents}" status="i">
        <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
          <td>${eventAttributes.event.eventId}</td>
          <td>${eventAttributes.scheduledEventId}</td>
          <td>+${historyAnalyzer.getElapsedSeconds(eventAttributes.event)}s</td>
          <td>${eventAttributes.event.eventType}</td>
          <td>${eventAttributes.workflowType?.version}${eventAttributes.activityType?.version}</td>
          <td>${eventAttributes.workflowType?.name}${eventAttributes.activityType?.name}</td>
          <td>${eventAttributes.executionContext}${eventAttributes.input}${eventAttributes.result}<span class="error">${eventAttributes.reason}</span></td>
          <td>${eventAttributes.identity}</td>
        </tr>
      </g:each>
      </tbody>
    </table>
  </div>
</div>
</body>
</html>
