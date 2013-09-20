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
<%@ page import="com.netflix.asgard.model.MonitorBucketType" %>
<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  <meta name="layout" content="main"/>
  <title>Edit Application</title>
</head>
<body>
  <div class="body">
    <h1>Edit Application</h1>
    <g:if test="${flash.message}">
      <div class="message">${flash.message}</div>
    </g:if>
    <g:hasErrors bean="${group}">
      <div class="errors">
        <g:renderErrors bean="${group}" as="list"/>
      </div>
    </g:hasErrors>
    <g:form method="post">
      <input type="hidden" id="name" name="name" value="${app.name}"/>
      <div class="dialog">
        <table>
          <tbody>
          <tr class="prop">
            <td class="name">Name:</td>
            <td class="value">${app.name}</td>
          </tr>
          <tr class="prop">
            <td class="name">
              <label for="group">App Group:</label>
            </td>
            <td class="value">
              <input type="text" id="group" name="group" value="${app.group}"/>
            </td>
          </tr>
          <tr class="prop">
            <td class="name">
              <label for="type">Type:</label>
            </td>
            <td class="value">
              <select id="type" name="type">
                <g:each var="t" in="${typeList}">
                  <g:if test="${app.type == t}">
                    <option selected value="${t}">${t}</option>
                  </g:if>
                  <g:else><option value="${t}">${t}</option></g:else>
                </g:each>
              </select>
            </td>
          </tr>
          <tr class="prop">
            <td class="name">
              <label for="description">Description:</label>
            </td>
            <td class="value">
              <textarea cols="20" rows="3" id="description" name="description">${app.description}</textarea>
            </td>
          </tr>
          <tr class="prop">
            <td class="name">
              <label for="owner">Owner:</label>
            </td>
            <td class="value">
              <input type="text" id="owner" name="owner" value="${app.owner}"/>
            </td>
          </tr>
          <tr class="prop">
            <td class="name">
              <label for="email">Email:</label>
            </td>
            <td class="value">
              <input type="email" id="email" name="email" value="${app.email}"/>
            </td>
          </tr>
          <tr class="prop">
            <td class="name">
              <label for="monitorBucketType">Monitor<br/>Bucket Type:</label>
            </td>
            <td class="value">
              <select id="monitorBucketType" name="monitorBucketType">
                <g:each var="bucketType" in="${MonitorBucketType.values()}">
                  <option ${app.monitorBucketType?.name() == bucketType.name() ? 'selected' : ''} value="${bucketType.name()}">${bucketType.description}</option>
                </g:each>
              </select>
            </td>
          </tr>
          </tbody>
        </table>
      </div>
      <div class="buttons">
        <g:buttonSubmit class="save" value="Update Application" action="update"/>
      </div>
    </g:form>
  </div>
</body>
</html>
