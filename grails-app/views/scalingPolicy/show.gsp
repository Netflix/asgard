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
  <title>${scalingPolicy.policyName} Scaling Policy</title>
</head>
<body>
  <div class="body">
    <h1>Policy Details</h1>
    <g:if test="${flash.message}">
      <div class="message">${flash.message}</div>
    </g:if>
    <div class="buttons">
      <g:form class="validate">
        <input type="hidden" name="id" value="${scalingPolicy.policyName}"/>
        <g:link class="edit keep" action="edit" id="${scalingPolicy.policyName}"
                params="[group: scalingPolicy.autoScalingGroupName]">Edit Scaling Policy</g:link>
        <g:buttonSubmit class="delete" data-warning="Really delete Scaling Policy '${scalingPolicy.policyName}'?"
                        action="delete" value="Delete Scaling Policy"/>
        <g:link class="create keep" controller="alarm" action="create"
                params="[id: scalingPolicy.policyName]">Add Alarm</g:link>
      </g:form>
    </div>
    <div>
      <table>
        <tbody>
        <tr class="prop">
          <td class="name">Name:</td>
          <td class="value">${scalingPolicy.policyName}</td>
        </tr>
        <tr class="prop">
          <td class="name">ARN:</td>
          <td class="value">${scalingPolicy.policyARN}</td>
        </tr>
        <tr class="prop">
          <td class="name">Auto Scaling Group:</td>
          <td class="value"><g:linkObject type="autoScaling" name="${scalingPolicy.autoScalingGroupName}"/></td>
        </tr>
        <tr class="prop">
          <td class="name">Adjustment Type:</td>
          <td class="value">${scalingPolicy.adjustmentType}</td>
        </tr>
        <tr class="prop">
          <td class="name">Adjustment:</td>
          <td class="value">${scalingPolicy.scalingAdjustment}</td>
        </tr>
        <tr class="prop">
          <td class="name">Minimum Adjustment:</td>
          <td class="value">${scalingPolicy.minAdjustmentStep}</td>
        </tr>
        <tr class="prop">
          <td class="name">Cooldown:</td>
          <td class="value">${scalingPolicy.cooldown} seconds</td>
        </tr>
        <tr class="prop">
          <td class="name">Alarms:</td>
          <td class="value">
            <g:if test="${alarms}">
              <ul class="links">
                <g:each var="alarm" in="${alarms}">
                  <li><g:linkObject type="alarm" name="${alarm.alarmName}">${alarm.toDisplayValue()}</g:linkObject></li>
                </g:each>
              </ul>
            </g:if>
            <g:else>None</g:else>
          </td>
        </tr>
        </tbody>
      </table>
    </div>
  </div>
</body>
</html>
