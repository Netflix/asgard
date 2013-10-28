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
  <title>Tasks</title>
</head>
<body>
  <div class="body">
    <g:if test="${flash.message}">
      <div class="message">${flash.message}</div>
    </g:if>
    <h1>Running Tasks</h1>
    <g:form method="post">
      <div class="list">
        <div class="buttons">
          <g:buttonSubmit class="stop" data-warning="Really stop the selected task?" action="cancel" value="Stop Selected Task"/>
        </div>
        <table class="sortable">
          <thead>
          <tr>
            <th>&thinsp;x</th>
            <th>Name</th>
            <th>Region</th>
            <th>Started</th>
            <th>Updated</th>
            <th>Duration</th>
            <th>${ticketLabel}</th>
            <g:if test="${authenticationEnabled}">
              <th>Username</th>
            </g:if>
            <th class="wrappable">Client (best guess, may be wrong)</th>
            <th>Status</th>
            <th>Operation</th>
          </tr>
          </thead>
          <tbody>
          <g:each var="rti" status="i" in="${runningTaskList}">
            <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
              <td><g:radio name="id" value="${rti.id}"/></td>
              <td>
                <g:link class="task" action="show" params="[id: rti.id]"
                        title="Show details of this task">${rti.name}</g:link>
              </td>
              <td>${rti.userContext?.region}</td>
              <td><g:formatDate date="${rti.startTime}"/></td>
              <td><g:formatDate date="${rti.updateTime}"/></td>
              <td>${rti.durationString}</td>
              <td>${rti.userContext?.ticket}</td>
              <g:if test="${authenticationEnabled}">
                <td>${rti.userContext?.username}</td>
              </g:if>
              <td>${rti.userContext?.clientHostName} ${rti.userContext?.clientIpAddress}</td>
              <td>${rti.status}</td>
              <td>${rti.operation}</td>
            </tr>
          </g:each>
          </tbody>
        </table>
      </div>
      <div class="paginateButtons">
      </div>
    </g:form>
  </div>
  <div class="body">
    <h1>Completed Tasks</h1>
    <div class="list">
      <div class="buttons"></div>
      <table class="sortable">
        <thead>
        <tr>
          <th>&thinsp;x</th>
          <th>Name</th>
          <th>Region</th>
          <th>Started</th>
          <th>Finished</th>
          <th class="sorttable_nosort">Duration</th>
          <th>${ticketLabel}</th>
          <g:if test="${authenticationEnabled}">
            <th>Username</th>
          </g:if>
          <th class="wrappable">Client (best guess, may be wrong)</th>
          <th>Status</th>
        </tr>
        </thead>
        <tbody>
        <g:each var="cti" status="i" in="${completedTaskList}">
          <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
            <td><g:radio name="id" value="${cti.id}"/></td>
            <td>
              <g:link class="task" action="show" params="${cti.id ? [id: cti.id] : [runId: cti.workflowExecution.runId]}"
                      title="Show details of this task">${cti.name}</g:link>
            </td>
            <td>${cti.userContext?.region}</td>
            <td><g:formatDate date="${cti.startTime}"/></td>
            <td><g:formatDate date="${cti.updateTime}"/></td>
            <td>${cti.durationString}</td>
            <td>${cti.userContext?.ticket}</td>
            <g:if test="${authenticationEnabled}">
              <td>${cti.userContext?.username}</td>
            </g:if>
            <td>${cti.userContext?.clientHostName} ${cti.userContext?.clientIpAddress}</td>
            <td class="${cti.status == 'failed' ? 'error' : ''}">${cti.status}</td>
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
