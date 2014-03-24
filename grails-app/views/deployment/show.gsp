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
<%@ page import="org.apache.commons.lang.StringEscapeUtils" %>
<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  <meta name="layout" content="main"/>
  <title>${deployment.description}</title>
</head>
<body>
  <div class="body">
    <h1>Deployment</h1>
    <g:if test="${flash.message}">
      <div class="message">${flash.message}</div>
    </g:if>
    <g:if test="${!deployment.isDone()}">
      <div class="buttons" id="taskCancellationForm">
        <g:form method="post">
          <input type="hidden" name="id" value="${deployment.id}"/>
          <g:buttonSubmit class="stop" data-warning="Really stop deployment ${StringEscapeUtils.escapeJavaScript(deployment.description)} ?" action="cancel" value="Stop Deployment"/>
        </g:form>
        <g:if test="${params.token}">
          <g:form method="post">
            <input type="hidden" name="id" value="${deployment.id}"/>
            <input type="hidden" name="token" value="${params.token}"/>
            <g:buttonSubmit class="rollback" action="rollback" value="Rollback Deployment"/>
            <g:buttonSubmit class="proceed" action="proceed" value="Proceed With Deployment"/>
          </g:form>
        </g:if>
      </div>
    </g:if>
    <div class="task">
      <table>
        <tbody>
        <tr class="prop">
          <td class="name">Return to<br/>Cluster:</td>
          <td class="value"><g:linkObject region="${deployment.region.code}" type="cluster" name="${deployment.clusterName}" /></td>
        </tr>
        <tr class="prop">
          <td class="name">Description:</td>
          <td class="value">${deployment.description}</td>
        </tr>
        <tr class="prop">
          <td class="name">Workflow Execution Details:</td>
          <td class="value"><g:link controller="workflowExecution" action="show" params="[runId: deployment.workflowExecution.runId, workflowId: deployment.workflowExecution.workflowId]">Workflow Execution</g:link></td>
        </tr>
        <tr class="prop">
          <td class="name">Region:</td>
          <td class="value">${deployment.region}</td>
        </tr>
        <tr class="prop">
          <td class="name">Status:</td>
          <td class="value"><span id="taskStatus">${deployment.status}</span></td>
        </tr>
        <tr class="prop">
          <td class="name">Start Time:</td>
          <td class="value date"><g:formatDate date="${deployment.startTime}"/></td>
        </tr>
        <tr class="prop">
          <td class="name">Duration:</td>
          <td class="value"><span id="taskDurationString">${deployment.durationString}</span></td>
        </tr>
        <tr class="prop">
          <td class="name">Owner:</td>
          <td class="value">${deployment.owner}</td>
        </tr>
        <tr class="prop">
          <td class="name">Log:</td>
          <td class="value">
            <g:textArea class="resizable" name="log" rows="20" cols="160" readonly="true"><g:each var="statement" in="${deployment.log}">${statement}${"\n"}</g:each></g:textArea>
          </td>
        </tr>
        </tbody>
      </table>
    </div>
  </div>
</body>
</html>
