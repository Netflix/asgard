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
  <title>Launch Configurations</title>
</head>
<body>
<div class="body">
  <h1>Launch Configurations in ${region.description}${appNames ? ' for ' + appNames : ''}</h1>
  <g:if test="${flash.message}">
    <div class="message">${flash.message}</div>
  </g:if>
  <g:form method="post">
    <div class="list">
      <div class="buttons"></div>
      <table class="sortable">
        <thead>
        <tr>
          <th>Name</th>
          <th>Image ID</th>
          <th>Security Groups</th>
          <th>Instance Type</th>
          <th>Created Time</th>
        </tr>
        </thead>
        <tbody>
        <g:each var="config" in="${launchConfigurations}" status="i">
          <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
            <td><g:linkObject type="launchConfiguration" name="${config.launchConfigurationName}"/></td>
            <td><g:linkObject type="image" name="${config.imageId}"/></td>
            <td>
              <g:each var="secgroup" in="${config.securityGroups}">
                <g:linkObject type="security" name="${secgroup}"/><br>
              </g:each>
            </td>
            <td>${config.instanceType}</td>
            <td class="date"><g:formatDate date="${config.createdTime}"/></td>
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
