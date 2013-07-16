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
<%@ page import="org.apache.commons.lang.StringEscapeUtils" %>
<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  <meta name="layout" content="main"/>
  <title>${task.name}</title>
</head>
<body>
  <div class="body">
    <h1>Task Details</h1>
    <g:if test="${flash.message}">
      <div class="message">${flash.message}</div>
    </g:if>
    <g:if test="${!task.isDone()}">
      <g:form method="post">
        <input type="hidden" name="id" value="${task.id}"/>
        <input type="hidden" name="runId" value="${task.runId}"/>
        <input type="hidden" name="workflowId" value="${task.workflowId}"/>
        <div class="buttons" id="taskCancellationForm">
          <g:if test="${params.taskToken}">
            <g:link class="deploy" controller="cluster" action="proceedWithDeployment"
                    params="[taskToken: params.taskToken, proceed: true, runId: task.runId, workflowId: task.workflowId]">Proceed With Deployment</g:link>
            <g:link class="deploy" controller="cluster" action="proceedWithDeployment"
                    params="[taskToken: params.taskToken, proceed: false, runId: task.runId, workflowId: task.workflowId]">Stop Deployment</g:link>
          </g:if>
          <g:else>
            <g:buttonSubmit class="stop" data-warning="Really stop task ${StringEscapeUtils.escapeJavaScript(task.name)} ?" action="cancel" value="Stop Task"/>
          </g:else>
        </div>
      </g:form>
    </g:if>
    <div class="task">
      <table>
        <tbody>
        <g:render template="returnLink"/>
        <tr class="prop">
          <td class="name">Name:</td>
          <td class="value">${task.name}</td>
        </tr>
        <tr class="prop">
          <td class="name">Region:</td>
          <td class="value">${task.userContext?.region}</td>
        </tr>
        <tr class="prop">
          <td class="name">Status:</td>
          <td class="value"><span id="taskStatus">${task.status}</span></td>
        </tr>
        <tr class="prop">
          <td class="name">Start Time:</td>
          <td class="value date"><g:formatDate date="${task.startTime}"/></td>
        </tr>
        <tr class="prop">
          <td class="name">${task.isDone() ? "Finished" : "Update"} Time:</td>
          <td class="value date"><span id="taskUpdateTime"><g:formatDate date="${task.updateTime}"/></span></td>
        </tr>
        <tr class="prop">
          <td class="name">Duration:</td>
          <td class="value"><span id="taskDurationString">${task.durationString}</span></td>
        </tr>
        <tr class="prop">
          <td class="name">Operation:</td>
          <td class="value"><span id="taskOperation">${task.operation}</span></td>
        </tr>
        <tr class="prop">
          <td class="name">${ticketLabel}:</td>
          <td class="value">${task.userContext?.ticket}</td>
        </tr>
        <g:if test="${authenticationEnabled}">
          <tr class="prop">
            <td class="name">Username:</td>
            <td class="value">${task.userContext?.username}</td>
          </tr>
        </g:if>
        <tr class="prop">
          <td class="name">Client Host Name<br/>(best guess,<br/>may be wrong):</td>
          <td class="value">${task.userContext?.clientHostName}</td>
        </tr>
        <tr class="prop">
          <td class="name">Client IP Address:</td>
          <td class="value">${task.userContext?.clientIpAddress}</td>
        </tr>
        <tr class="prop">
          <td class="name">Log:</td>
          <td class="value">
            <g:textArea class="resizable" name="log" rows="20" cols="160" readonly="true"><g:each var="statement" in="${task.log}">${statement}${"\n"}</g:each></g:textArea>
          </td>
        </tr>
        <g:render template="returnLink"/>
        </tbody>
      </table>
    </div>
  </div>
</body>
</html>
