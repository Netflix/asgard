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
  <title>${fastProperty.key}</title>
</head>
<body>
  <div class="body">
    <h1>Fast Property Details in ${region.description}</h1>
    <g:if test="${flash.message}">
      <div class="message">${flash.message}</div>
    </g:if>
    <div class="buttons">
      <g:form class="validate">
        <input type="hidden" name="id" value="${fastProperty.id}"/>
        <input type="hidden" name="fastPropertyRegion" value="${fastProperty.region}"/>
        <g:link class="edit" action="edit" id="${fastProperty.id}">Edit Fast Property</g:link>
        <g:buttonSubmit class="delete" data-warning="Really delete Fast Property '${fastProperty.id}'?" action="delete" value="Delete Fast Property"/>
        <label for="username">Username: </label><input type="text" id="username" placeholder="jsmith" name="updatedBy" required="true" value="${fastProperty.updatedBy}" />
      </g:form>
    </div>
    <div class="dialog">
      <table>
        <tbody>
        <tr class="prop">
          <td class="name">Name:</td>
          <td class="value">${fastProperty.key}</td>
        </tr>
        <tr class="prop">
          <td class="name">Value:</td>
          <td class="value bigValue">${fastProperty.value?.encodeAsHTML()}</td>
        </tr>
        <tr class="prop">
          <td class="name">Updated By:</td>
          <td class="value">${fastProperty.updatedBy?.encodeAsHTML()}</td>
        </tr>
        <g:render template="fastPropertyAttributes" />
        </tbody>
      </table>
    </div>
  </div>
</body>
</html>
