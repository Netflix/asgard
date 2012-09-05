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
  <title>Create New Scheduled Action</title>
</head>

<body>
<div class="body">
  <h1>Create New Scheduled Action</h1>
  <g:if test="${flash.message}">
    <div class="message">${flash.message}</div>
  </g:if>
  <g:hasErrors bean="${cmd}">
    <div class="errors">
      <g:renderErrors bean="${cmd}" as="list"/>
    </div>
  </g:hasErrors>
  <g:form action="save" method="post" class="validate">
    <div class="dialog">
      <table>
        <tbody>
        <tr>
          <td colspan="2">
            <h2>Scheduled Action</h2>
          </td>
        </tr>
        <g:render template="actionOptions"/>
        <tr class="prop" title="The regular schedule that an action occurs. When Start Time and End Time are specified they form the boundaries of the recurring action.">
          <td class="name"><label for="recurrence">Recurrence:</label></td>
          <td class="value"><g:textField name="recurrence" value="${recurrence}"/>&nbsp;<a href="http://en.wikipedia.org/wiki/Cron">What's this?</a></td>
        </tr>
        </tbody>
      </table>
    </div>
    <div class="buttons">
      <input type="hidden" name="group" value="${group}"/>
      <g:buttonSubmit class="save" value="save">Create New Scheduled Action</g:buttonSubmit>
    </div>
  </g:form>
</div>
</body>
</html>
