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
  <title>Edit Scaling Policy</title>
</head>

<body>
<div class="body">
  <h1>Edit Scaling Policy</h1>
  <g:if test="${flash.message}">
    <div class="message">${flash.message}</div>
  </g:if>
  <g:hasErrors bean="${cmd}">
    <div class="errors">
      <g:renderErrors bean="${cmd}" as="list"/>
    </div>
  </g:hasErrors>
  <g:form action="update" method="post" class="validate">
    <div class="dialog">
      <table>
        <tbody>
        <tr class="prop">
          <td class="name">Name:</td>
          <td class="value">${scalingPolicy?.policyName}</td>
        </tr>
        <g:render template="policyOptions"/>
        </tbody>
      </table>
    </div>
    <div class="buttons">
      <input type="hidden" name="policyName" value="${scalingPolicy?.policyName}"/>
      <g:buttonSubmit class="save keep" value="update">Update Scaling Policy</g:buttonSubmit>
    </div>
  </g:form>
</div>
</body>
</html>
