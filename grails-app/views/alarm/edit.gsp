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
  <title>Edit Alarm</title>
</head>
<body>
<div class="body">
  <h1>Edit Alarm</h1>
  <g:if test="${flash.message}">
    <div class="message">${flash.message}</div>
  </g:if>
  <g:hasErrors bean="${cmd}">
    <div class="errors">
      <g:renderErrors bean="${cmd}" as="list"/>
    </div>
  </g:hasErrors>
  <g:form action="update" method="post" class="validate">
    <div class="dialog">
      <table>
        <tbody>
        <tr class="prop">
          <td class="name">
            <label>Name:</label>
          </td>
          <td class="value">${alarmName}</td>
        </tr>
        <g:render template="alarmOptions"/>
        <tr>
          <td colspan="2">
            <a href="http://docs.aws.amazon.com/AmazonCloudWatch/latest/DeveloperGuide/CW_Support_For_AWS.html">Metric and Dimension Documentation</a>
          </td>
        </tr>
        <tr class="prop">
          <td class="name">Metric:</td>
          <input type="hidden" name="namespace" value="${currentMetric.namespace}"/>
          <input type="hidden" name="metric" value="${currentMetric.metricName}"/>
          <td class="value">${currentMetric.displayText}</td>
        </tr>
        <g:each var="dimension" in="${dimensions}">
          <tr class="prop">
            <td class="name">
              <label for="${dimension}">${dimension}:</label>
            </td>
            <td class="value">
              <g:textField name="${dimension}" value="${params[dimension] ?: dimensionValues[dimension]}"/>
            </td>
          </tr>
        </g:each>
        </tbody>
      </table>
    </div>
    <div class="buttons">
      <input type="hidden" name="alarmName" value="${alarmName}"/>
      <g:buttonSubmit class="save" value="update">Update Alarm</g:buttonSubmit>
    </div>
  </g:form>
</div>
</body>
</html>
