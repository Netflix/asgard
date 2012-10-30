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
  <title>${lc.launchConfigurationName} Launch Configuration</title>
</head>
<body>
  <div class="body">
    <h1>Launch Configuration Details</h1>
    <g:if test="${flash.message}">
      <div class="message">${flash.message}</div>
    </g:if>
    <div class="buttons">
      <g:form>
        <input type="hidden" name="name" value="${lc.launchConfigurationName}"/>
        <g:buttonSubmit class="delete" action="delete" value="Delete Launch Configuration"
                data-warning="Really delete Launch Configuration '${lc.launchConfigurationName}'?" />
      </g:form>
    </div>
    <div class="dialog">
      <table>
        <tbody>
        <tr class="prop">
          <td class="name">Name:</td>
          <td class="value">${lc.launchConfigurationName}</td>
        </tr>
        <g:render template="launchTemplateFields" model="[launchTemplate: lc]"/>
        <tr class="prop">
          <td class="name">Created Time:</td>
          <td class="value"><g:formatDate date="${lc.createdTime}"/></td>
        </tr>
        <g:if test="${lc.spotPrice}">
          <tr class="prop">
            <td class="name">Spot Price:</td>
            <td class="value">${lc.spotPrice}</td>
          </tr>
        </g:if>
        <tr class="prop">
          <td><h2>Referenced From</h2></td>
        </tr>
        <tr class="prop">
          <td class="name">Cluster:</td>
          <td class="value">
            <g:linkObject type="cluster" name="${cluster}"/>
          </td>
        </tr>
        <tr class="prop">
          <td class="name">AutoScaling Group:</td>
          <td class="value">
            <g:linkObject type="autoScaling" name="${group?.autoScalingGroupName}"/>
          </td>
        </tr>
        <g:each in="${group?.variables}" var="entry">
          <tr class="prop">
            <td class="name">${entry.key}:</td>
            <td class="value">${entry.value}</td>
          </tr>
        </g:each>
        <tr class="prop">
          <td><h2>Pattern Matches</h2></td>
        </tr>
        <tr class="prop">
          <td class="name">Application:</td>
          <td class="value">
            <g:linkObject type="application" name="${app?.name}"/>
          </td>
        </tr>
        </tbody>
      </table>
    </div>
  </div>
</body>
</html>
