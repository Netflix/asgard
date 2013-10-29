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
<%@ page import="com.amazonaws.services.sqs.model.QueueAttributeName" %>
<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  <meta name="layout" content="main"/>
  <title>Edit Queue</title>
</head>
<body>
  <div class="body">
    <h1>Edit Queue</h1>
    <g:if test="${flash.message}">
      <div class="message">${flash.message}</div>
    </g:if>
    <g:hasErrors bean="${cmd}">
      <div class="errors">
        <g:renderErrors bean="${cmd}" as="list"/>
      </div>
    </g:hasErrors>
    <g:form action="save" method="post" class="validate">
      <input type="hidden" id="name" name="name" value="${queue.name}"/>
      <div>
        <table>
          <tbody>
          <tr>
            <td class="name"><label>Queue Name:</label></td>
            <td>${queue.name}</td>
          </tr>
          <tr>
            <td class="name"><label for="visibilityTimeout">Visibility Timeout:</label></td>
            <td>
              <input class="number" type="text" id="visibilityTimeout" name="visibilityTimeout"
                     value="${queue.attributes[QueueAttributeName.VisibilityTimeout.toString()]}"/> seconds
            </td>
          </tr>
          <tr>
            <td class="name"><label for="delay">Delay:</label></td>
            <td>
              <input class="number" type="text" id="delay" name="delay"
                     value="${queue.attributes[QueueAttributeName.DelaySeconds.toString()]}"/> seconds
            </td>
          </tr>
          <tr>
            <td class="name"><label for="retention">Message Retention Period:</label></td>
            <td>
              <input class="number" type="text" id="retention" name="retention"
                     value="${queue.attributes[QueueAttributeName.MessageRetentionPeriod.toString()]}"/> seconds
            </td>
          </tr>
          </tbody>
        </table>
      </div>
      <div class="buttons">
        <g:buttonSubmit class="save" action="update">Update Queue</g:buttonSubmit>
      </div>
    </g:form>
  </div>
</body>
</html>
