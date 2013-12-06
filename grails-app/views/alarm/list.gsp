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
  <title>Alarms</title>
</head>
<body>
<div class="body">
  <h1>Alarms in ${region.description}</h1>
  <g:if test="${flash.message}">
    <div class="message">${flash.message}</div>
  </g:if>
  <g:form method="post">
    <div class="list">
      <div class="buttons"></div>
      <table class="sortable">
        <thead>
        <tr>
          <th>Alarm Name</th>
          <th>Dimensions</th>
          <th>Statistic</th>
          <th>Metric</th>
          <th>Comparison Operator</th>
          <th>Threshold</th>
          <th>Config Updated Time</th>
        </tr>
        </thead>
        <tbody>
        <g:each var="alarm" in="${alarms}" status="i">
          <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
            <td><g:linkObject type="alarm" name="${alarm.alarmName}"/></td>
            <td class="countAndList hideAdvancedItems">
              <g:if test="${alarm.dimensions.size() == 1}">
                <g:render template="dimensions" model="[dimensions: alarm.dimensions]"/>
              </g:if>
              <g:elseif test="${alarm.dimensions.size() > 1}">
                <span class="toggle fakeLink">${alarm.dimensions.size()}</span>
                <div class="advancedItems">
                  <g:render template="dimensions" model="[dimensions: alarm.dimensions]"/>
                </div>
              </g:elseif>
            </td>
            <td>${alarm.statistic}</td>
            <td>${alarm.metricName.encodeAsHTML()}</td>
            <td>${alarm.comparisonOperator}</td>
            <td><g:formatNumber number="${alarm.threshold}"/></td>
            <td><g:formatDate date="${alarm.alarmConfigurationUpdatedTimestamp}"/></td>
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
