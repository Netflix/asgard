<%@ page import="com.amazonaws.services.simpleworkflow.model.ActivityTypeInfo; com.netflix.asgard.EntityType" %>
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
  <title>Activity Types</title>
</head>
<body>
<div class="body">
  <h1>Activity Types</h1>
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
          <th>Version</th>
          <th>Status</th>
          <th>Description</th>
          <th>Creation Date</th>
          <th>Deprecation Date</th>
        </tr>
        </thead>
        <tbody>
        <g:each var="activityTypeInfo" in="${activityTypeInfos}" status="i">
          <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
            <td><g:link action="show" params="${[name: activityTypeInfo.activityType.name, version: activityTypeInfo.activityType.version]}">${activityTypeInfo.activityType.name}</g:link></td>
            <td>${activityTypeInfo.activityType.version}</td>
            <td>${activityTypeInfo.status}</td>
            <td>${activityTypeInfo.description}</td>
            <td><g:formatDate date="${activityTypeInfo.creationDate}"/></td>
            <td><g:formatDate date="${activityTypeInfo.deprecationDate}"/></td>
          </tr>
        </g:each>
        </tbody>
      </table>
    </div>
    <div class="paginateButtons"></div>
  </g:form>
</div>
</body>
</html>
