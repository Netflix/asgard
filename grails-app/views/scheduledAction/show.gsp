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
<%@ page import="com.netflix.asgard.model.AlarmData" %>
<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  <meta name="layout" content="main"/>
  <title>${scheduledAction.scheduledActionName} Scheduled Action</title>
</head>
<body>
  <div class="body">
    <h1>Action Details</h1>
    <g:if test="${flash.message}">
      <div class="message">${flash.message}</div>
    </g:if>
    <div class="buttons">
      <g:form class="validate">
        <input type="hidden" name="id" value="${scheduledAction.scheduledActionName}"/>
        <g:link class="edit" action="edit" id="${scheduledAction.scheduledActionName}"
                params="[group: scheduledAction.autoScalingGroupName]">Edit Scheduled Action</g:link>
        <g:buttonSubmit class="delete" data-warning="Really delete Scheduled Action '${scheduledAction.scheduledActionName}'?"
                        action="delete" value="Delete Scheduled Action"/>
      </g:form>
    </div>
    <div>
      <table>
        <tbody>
        <tr class="prop">
          <td class="name">Name:</td>
          <td class="value">${scheduledAction.scheduledActionName}</td>
        </tr>
        <tr class="prop">
          <td class="name">ARN:</td>
          <td class="value">${scheduledAction.scheduledActionARN}</td>
        </tr>
        <tr class="prop">
          <td class="name">Auto Scaling Group:</td>
          <td class="value"><g:linkObject type="autoScaling" name="${scheduledAction.autoScalingGroupName}"/></td>
        </tr>
        <tr class="prop">
          <td class="name">Start Time:</td>
          <td class="value">${scheduledAction.startTime}</td>
        </tr>
        <tr class="prop">
          <td class="name">End Time:</td>
          <td class="value">${scheduledAction.endTime}</td>
        </tr>
        <tr class="prop">
          <td class="name">Recurrence:</td>
          <td class="value">${scheduledAction.recurrence}</td>
        </tr>
        <tr class="prop">
          <td class="name">Minimum Size:</td>
          <td class="value">${scheduledAction.minSize}</td>
        </tr>
        <tr class="prop">
          <td class="name">Maximum Size:</td>
          <td class="value">${scheduledAction.maxSize}</td>
        </tr>
        <tr class="prop">
          <td class="name">Desired Capacity:</td>
          <td class="value">${scheduledAction.desiredCapacity}</td>
        </tr>
        </tbody>
      </table>
    </div>
  </div>
</body>
</html>
