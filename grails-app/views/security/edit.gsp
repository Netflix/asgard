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
  <title>Edit Security Group</title>
</head>
<body>
  <div class="body">
    <h1>Edit Security Group</h1>
    <g:if test="${flash.message}">
      <div class="message">${flash.message}</div>
    </g:if>
    <g:form method="post">
      <input type="hidden" id="name" name="name" value="${group.groupName}"/>
      <div class="dialog">
        <table>
          <tbody>
          <tr class="prop" title="Changing the name requires a delete and re-create. This may fail if there are references.">
            <td class="name">
              Name:
            </td>
            <td class="value">
              ${group.groupName}
            </td>
          </tr>
          <tr class="prop">
            <td class="name">
              Description:
            </td>
            <td class="value">
              ${group.description}
            </td>
          </tr>
          <tr>
            <td class="name">Ingress Permissions:</td>
            <td class="value">
              <table>
                <tbody>
                <g:each var="g" in="${groups}">
                  <tr>
                    <td><g:checkBox name="selectedGroups" value="${g.key}" checked="${g.value[0]}"
                          /> ${g.key} <input type="text" id="${g.key}" name="${g.key}" value="${g.value[1]}"/></td>
                  </tr>
                </g:each>
                </tbody>
              </table>
            </td>
          </tr>
          </tbody>
        </table>
      </div>
      <g:if test="${editable}">
        <div class="buttons">
          <g:buttonSubmit class="save" value="Update Security Group" action="update"/>
        </div>
      </g:if>
    </g:form>
  </div>
</body>
</html>
