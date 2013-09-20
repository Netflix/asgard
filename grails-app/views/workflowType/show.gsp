<%--

    Copyright 2013 Netflix, Inc.

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
  <title>${workflowType.typeInfo.workflowType.name} Workflow Type</title>
</head>
<body>
<div class="body">
  <h1>Workflow Type Details</h1>
  <g:if test="${flash.message}">
    <div class="message">${flash.message}</div>
  </g:if>
  <div class="buttons"></div>
  <div>
    <table>
      <tbody>
      <tr class="prop">
        <td class="name">Name:</td>
        <td class="value">${workflowType.typeInfo.workflowType.name}</td>
      </tr>
      <tr class="prop">
        <td class="name">Version:</td>
        <td class="value">${workflowType.typeInfo.workflowType.version}</td>
      </tr>
      <tr class="prop">
        <td class="name">Status:</td>
        <td class="value">${workflowType.typeInfo.status}</td>
      </tr>
      <tr class="prop">
        <td class="name">Description:</td>
        <td class="value">${workflowType.typeInfo.description}</td>
      </tr>
      <tr class="prop">
        <td class="name">Creation Date:</td>
        <td class="value">${workflowType.typeInfo.creationDate}</td>
      </tr>
      <tr class="prop">
        <td class="name">Deprecation Date:</td>
        <td class="value">${workflowType.typeInfo.deprecationDate}</td>
      </tr>
      <tr class="prop">
        <td class="name">Default Task Start To Close Timeout:</td>
        <td class="value">${workflowType.configuration.defaultTaskStartToCloseTimeout}</td>
      </tr>
      <tr class="prop">
        <td class="name">Default Execution Start To Close Timeout:</td>
        <td class="value">${workflowType.configuration.defaultExecutionStartToCloseTimeout}</td>
      </tr>
      <tr class="prop">
        <td class="name">Default Task List:</td>
        <td class="value">${workflowType.configuration.defaultTaskList.name}</td>
      </tr>
      <tr class="prop">
        <td class="name">Default Child Policy:</td>
        <td class="value">${workflowType.configuration.defaultChildPolicy}</td>
      </tr>
      </tbody>
    </table>
  </div>
</div>
</body>
</html>
