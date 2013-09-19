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
  <title>Create New Application</title>
</head>
<body>
  <div class="body">
    <h1>Create New Application</h1>
    <g:if test="${flash.message}">
      <div class="message">${flash.message}</div>
    </g:if>
    <g:hasErrors bean="${cmd}">
      <div class="errors">
        <g:renderErrors bean="${cmd}" as="list"/>
      </div>
    </g:hasErrors>
    <g:form action="save" method="post" class="validate">
      <g:hiddenField name="requestedFromGui" value="true" />
      <div class="dialog">
        <table>
          <tbody>
          <tr class="prop">
            <td valign="top" class="name">
              <label for="name">Name:</label>
            </td>
            <td valign="top">
              <input type="text" id="name" name="name" value="${params.name}" class="required"/>
            </td>
          </tr>
          <tr class="prop">
            <td valign="top" class="name">
              <label for="group">App Group:</label>
            </td>
            <td valign="top">
              <input type="text" id="group" name="group" value="${params.group}"/>
            </td>
          </tr>
          <tr class="prop">
            <td class="name">
              <label for="type">Type:</label>
            </td>
            <td class="value">
              <select id="type" name="type">
                <g:each var="t" in="${typeList}">
                  <g:if test="${params.type == t || (!params.type && t == 'Web Service')}">
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
              <textarea cols="40" rows="3" id="description" name="description">${params.description}</textarea>
            </td>
          </tr>
          <tr class="prop">
            <td class="name">
              <label for="owner">Owner:</label>
            </td>
            <td class="value">
              <input type="text" id="owner" name="owner" value="${params.owner}"/>
            </td>
          </tr>
          <tr class="prop">
            <td class="name">
              <label for="email">Email:</label>
            </td>
            <td class="value">
              <input type="email" id="email" name="email" value="${params.email}"/>
            </td>
          </tr>
          <tr class="prop">
            <td class="name">
              <label for="monitorBucketType">Monitor<br/>Bucket Type:</label>
            </td>
            <td class="value">
              <select id="monitorBucketType" name="monitorBucketType">
                <g:each var="bucketType" in="${MonitorBucketType.values()}">
                  <option ${MonitorBucketType.getDefaultForNewApps() == bucketType.name() ? 'selected' : ''} value="${bucketType.name()}">${bucketType.description}</option>
                </g:each>
              </select>
            </td>
          </tr>
          <g:render template="/common/chaosMonkeyOptions" />
          </tbody>
        </table>
      </div>
      <div class="buttons">
        <g:buttonSubmit class="save" value="save">Create New Application</g:buttonSubmit>
      </div>
    </g:form>
  </div>
</body>
</html>
