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
  <title>Edit DB Security Group</title>
</head>
<body>
  <div class="body">
    <h1>Edit DB Security Group</h1>
    <g:if test="${flash.message}">
      <div class="message">${flash.message}</div>
    </g:if>
    <g:form method="post">
      <input type="hidden" id="name" name="name" value="${group.dBSecurityGroupName}"/>
      <div class="dialog">
        <table>
          <tbody>
          <tr class="prop" title="Changing the name requires a delete and re-create. This may fail if there are references.">
            <td class="name">
              <label for="newName">Name:</label>
            </td>
            <td class="value">
              <input type="text" id="newName" name="newName" value="${group.dBSecurityGroupName}"/>
            </td>
          </tr>
          <tr class="prop">
            <td class="name">
              <label for="description">Description:</label>
            </td>
            <td class="value">
              <input type="text" id="description" name="description" value="${group.dBSecurityGroupDescription}"/>
            </td>
          </tr>
          <tr>
            <td class="name">EC2 Security Group Ingress Permissions:</td>
            <td class="value">
              <table>
                <tbody>
                <g:each var="g" in="${allEC2Groups.sort{it.toLowerCase()}}">
                  <tr>
                    <td><g:checkBox name="selectedGroups" value="${g}" checked="${g in selectedEC2Groups}"/> ${g}</td>
                  </tr>
                </g:each>
                </tbody>
              </table>
            </td>
          </tr>
          <tr>
            <td class="name">IP Range Ingress Permissions:</td>
            <td class="value">
              <g:textArea name="ipRanges" value="${group.getIPRanges().collect{it.getCIDRIP()}.join('\n')}" rows="5" cols="40"/>
            </td>
          </tr>
          </tbody>
        </table>
      </div>
      <div class="buttons">
        <g:buttonSubmit class="save" value="Update DB Security Group" action="update"/>
      </div>
    </g:form>
  </div>
</body>
</html>
