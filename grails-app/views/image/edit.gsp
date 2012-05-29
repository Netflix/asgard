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
  <title>Edit Image</title>
</head>
<body>
  <div class="body">
    <h1>Edit Image Attributes</h1>
    <g:if test="${flash.message}">
      <div class="message">${flash.message}</div>
    </g:if>
    <g:hasErrors bean="${image}">
      <div class="errors">
        <g:renderErrors bean="${image}" as="list"/>
      </div>
    </g:hasErrors>
    <g:form method="post">
      <input type="hidden" id="imageId" name="imageId" value="${image.imageId}"/>
      <div class="dialog">
        <table>
          <tbody>
          <tr class="prop">
            <td class="name">ID:</td>
            <td class="value">${image.imageId}</td>
          </tr>
          <%--
          <tr class="prop">
            <td class="name">
              <label for="name">Name:</label>
            </td>
            <td class="value">
              <input type="text" id="name" name="name" value="${image.name}"/>
            </td>
          </tr>
          <tr class="prop">
            <td valign="top" class="name">
              <label for="description">Description:</label>
            </td>
            <td valign="top">
              <input type="text" id="description" name="description" value="${image.description}"/>
            </td>
          </tr>
          --%>
          <tr class="prop">
            <td class="name">Launch Permissions:</td>
            <td class="value">
              <table>
                <tbody>
                <g:each var="a" in="${accounts}">
                  <tr>
                    <td><g:checkBox name="launchPermissions" value="${a.key}" checked="${launchPermissions.contains(a.key)}"/> ${a.value} (${a.key})</td>
                  </tr>
                </g:each>
                </tbody>
              </table>
            </td>
          </tr>

          </tbody>
        </table>
      </div>
      <div class="buttons">
        <g:buttonSubmit class="save" value="Update Image Attributes" action="update"/>
      </div>
    </g:form>
  </div>
</body>
</html>
