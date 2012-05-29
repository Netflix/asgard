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
  <title>Create New Subscription</title>
</head>
<body>
<div class="body">
  <h1>Create New Subscription</h1>
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
            <label>Topic:</label>
          </td>
          <td class="value"><g:linkObject type="topic" name="${topic}"/></td>
        </tr>
        <tr class="prop" title="The place where messages will be sent to.">
          <td class="name">
            <label for="endpoint">Endpoint:</label>
          </td>
          <td class="value">
            <g:textField name="endpoint" value="${endpoint}"/>
          </td>
        </tr>
        <tr class="prop" title="How messages will be sent to the endpoint.">
          <td class="name">
            <label for="protocol">Protocol:</label>
          </td>
          <td>
            <g:select name="protocol" value="${protocol}" from="${protocols}" optionKey="value" optionValue="value"/>
          </td>
        </tr>
        </tbody>
      </table>
    </div>
    <div class="buttons">
      <input type="hidden" name="topic" value="${topic}"/>
      <g:buttonSubmit class="save" value="subscribe">Create New Subscription</g:buttonSubmit>
    </div>
  </g:form>
</div>
</body>
</html>