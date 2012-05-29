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
  <title>Edit Fast Property</title>
</head>
<body>
  <div class="body">
    <h1>Edit Fast Property</h1>
    <g:if test="${flash.message}">
      <div class="message">${flash.message}</div>
    </g:if>
    <g:form method="post" class="validate">
      <input type="hidden" name="id" value="${fastProperty.id}"/>
      <div class="dialog">
        <table>
          <tbody>
          <tr class="prop">
            <td class="name">Application:</td>
            <td class="value">${fastProperty.appId ?: 'Default (all apps)'}</td>
          </tr>
          <tr class="prop">
            <td class="name">Environment:</td>
            <td class="value">${fastProperty.env}</td>
          </tr>
          <tr class="prop">
            <td class="name">Key:</td>
            <td class="value">${fastProperty.key}</td>
          </tr>
          <tr class="prop">
            <td class="name"><label for="value">Value:</label></td>
            <td class="value"><g:textArea cols="30" rows="3" class="fastPropertyValue" id="value" name="value" value="${fastProperty.value}"/></td>
          </tr>
          <tr class="prop">
            <td class="name"><label for="updatedBy">Updated By:</label></td>
            <td class="value"><g:textField id="updatedBy" name="updatedBy" value="${fastProperty.updatedBy}" class="required"/></td>
          </tr>
        <tr class="prop">
          <td class="name">Region:</td>
          <td class="value">${fastProperty.region}</td>
        </tr>
        <tr class="prop">
          <td class="name">Stack:</td>
          <td class="value">${fastProperty.stack?.encodeAsHTML()}</td>
        </tr>
        <tr class="prop">
          <td class="name">Countries:</td>
          <td class="value">${fastProperty.countries?.encodeAsHTML()}</td>
        </tr>
        <tr class="prop">
          <td class="name">${ticketLabel.encodeAsHTML()}:</td>
          <td class="value">${fastProperty.cmcTicket?.encodeAsHTML()}</td>
        </tr>
        <tr class="prop">
          <td class="name">ID:</td>
          <td class="value">${fastProperty.id}</td>
        </tr>
        </table>
      </div>
      <div class="buttons">
        <g:buttonSubmit class="save" onclick="return confirm('Change value of '${key}'?');" value="Update Fast Property" action="update"/>
      </div>
    </g:form>
  </div>
</body>
</html>
