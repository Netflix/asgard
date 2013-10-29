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
  <title>${queue.name} Queue</title>
</head>
<body>
  <div class="body">
    <h1>Queue Details</h1>
    <g:if test="${flash.message}">
      <div class="message">${flash.message}</div>
    </g:if>
    <div class="buttons">
      <g:form action="delete">
        <input type="hidden" name="id" value="${queue.name}"/>
        <g:link class="edit" action="edit" params="[id:queue.name]">Edit Queue</g:link>
        <g:buttonSubmit class="delete" action="delete" value="Delete Queue"
                data-warning="Really delete Queue '${queue.name}'?" />
      </g:form>
    </div>
    <div>
      <table>
        <tbody>
        <tr class="prop">
          <td class="name">Queue Name:</td>
          <td class="value">${queue.name}</td>
        </tr>
        <tr class="prop">
          <td class="name">Queue URL:</td>
          <td class="value">${queue.url}</td>
        </tr>
        <g:each var="attribute" in="${queue.humanReadableAttributes}">
          <tr class="prop">
            <td class="name">${attribute.key}:</td>
            <td class="value"><g:render template="/common/jsonValue" model="['jsonValue': attribute.value]"/></td>
          </tr>
        </g:each>
        </tbody>
      </table>
    </div>
  </div>
</body>
</html>
