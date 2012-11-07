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
  <title>Create New Queue</title>
</head>
<body>
  <div class="body">
    <h1>Create New Queue</h1>
    <g:if test="${flash.message}">
      <div class="message">${flash.message}</div>
    </g:if>
    <g:hasErrors bean="${cmd}">
      <div class="errors">
        <g:renderErrors bean="${cmd}" as="list"/>
      </div>
    </g:hasErrors>
    <g:form action="save" method="post" class="validate">
      <div>
        <table>
          <tbody>
          <tr>
            <td class="name">
              <label for="name">Queue Name:</label>
            </td>
            <td>
              <input type="text" id="name" name="id" value="${params.id}"/>
            </td>
          </tr>
          <tr>
            <td class="name">
              <label for="visibilityTimeout">Visibility Timeout:</label>
            </td>
            <td>
              <input class="number" type="text" id="visibilityTimeout" name="visibilityTimeout" value="${params.visibilityTimeout ?: 30}"/> seconds
            </td>
          </tr>
          <tr>
            <td class="name"><label for="delay">Delay:</label></td>
            <td>
              <input class="number" type="text" id="delay" name="delay" value="${params.delay ?: 0}"/> seconds
            </td>
          </tr>
          </tbody>
        </table>
      </div>
      <div class="buttons">
        <g:buttonSubmit class="save" value="save">Create New Queue</g:buttonSubmit>
      </div>
    </g:form>
  </div>
</body>
</html>
