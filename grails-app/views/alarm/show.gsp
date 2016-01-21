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
  <title>${alarm.alarmName} Alarm</title>
</head>
<body>
  <div class="body">
    <h1>Alarm Details</h1>
    <g:if test="${flash.message}">
      <div class="message">${flash.message}</div>
    </g:if>
    <div class="buttons">
      <g:form class="validate">
        <input type="hidden" name="id" value="${alarm.alarmName}"/>
        <g:link class="edit keep" action="edit" id="${alarm.alarmName}">Edit Alarm</g:link>
        <g:buttonSubmit class="delete" action="delete" value="Delete Alarm" data-warning="Really delete Alarm '${alarm.alarmName}'?"/>
      </g:form>
    </div>
    <div>
      <table>
        <tbody>
        <tr class="prop">
          <td class="name">Name:</td>
          <td class="value">${alarm.alarmName}</td>
        </tr>
        <tr class="prop">
          <td class="name">Policy:</td>
          <td class="value">
            <g:each var="policy" in="${policies}">
              <div><g:linkObject type="scalingPolicy" name="${policy}"/></div>
            </g:each>
          </td>
        </tr>
        <tr class="prop">
          <td class="name">Dimensions:</td>
          <td class="value">
            <g:render template="dimensions" model="[dimensions: alarm.dimensions,showAsgText: true]"/>
          </td>
        </tr>
        <g:render template="alarmDetails" />
        <tr class="prop">
          <td class="name">State Value:</td>
          <td class="value">${alarm.stateValue}</td>
        </tr>
        <tr class="prop">
          <td class="name">State Reason:</td>
          <td class="value">${alarm.stateReason}</td>
        </tr>
        <tr class="prop">
          <td class="name">State Reason Data:</td>
          <td class="value">${alarm.stateReasonData}</td>
        </tr>
        <tr class="prop">
          <td class="name">State Updated Time:</td>
          <td class="value"><g:formatDate date="${alarm.stateUpdatedTimestamp}"/></td>
        </tr>
        <tr class="prop">
          <td class="name">Unit:</td>
          <td class="value">${alarm.unit}</td>
        </tr>
        <tr class="prop">
          <td class="name">Amazon Resource Locator (ARN):</td>
          <td class="value">${alarm.alarmArn}</td>
        </tr>
        <tr class="prop">
          <td class="name">Actions Enabled:</td>
          <td class="value">${alarm.actionsEnabled}</td>
        </tr>
        <tr class="prop">
          <td class="name">Actions:</td>
          <td class="value">
            <ul>
              <g:each var="alarmAction" in="${alarm.alarmActions}">
                <li>${alarmAction}</li>
              </g:each>
            </ul>
          </td>
        </tr>
        <tr class="prop">
          <td class="name">OK Actions:</td>
          <td class="value">${alarm.getOKActions()}</td>
        </tr>
        <tr class="prop">
          <td class="name">Insufficient Data Actions:</td>
          <td class="value">${alarm.insufficientDataActions}</td>
        </tr>
        <tr class="prop">
          <td class="name">Config Updated Time:</td>
          <td class="value"><g:formatDate date="${alarm.alarmConfigurationUpdatedTimestamp}"/></td>
        </tr>
        </tbody>
      </table>
    </div>
  </div>
</body>
</html>
