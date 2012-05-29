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
  <title>Create New Topic</title>
</head>
<body>
<div class="body">
  <h1>Create New Topic</h1>
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
        <tr class="prop">
          <td class="name">
            <label for="appName">Application:</label>
          </td>
          <td>
            <g:select class="required" name="appName"
                      noSelection="['':'-Choose application-']" value="${appName}" from="${applications}"
                      optionKey="name" optionValue="name"/>
          </td>
        </tr>
        <tr class="prop">
          <td class="name">
            <label for="detail">Detail (optional):</label>
          </td>
          <td>
            <g:textField name="detail" value="${detail}"/>
          </td>
        </tr>
        </tbody>
      </table>
    </div>
    <div class="buttons">
      <g:buttonSubmit class="save" value="save">Create New Topic</g:buttonSubmit>
    </div>
  </g:form>
</div>
</body>
</html>