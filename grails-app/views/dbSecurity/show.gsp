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
  <title>${group.dBSecurityGroupName} DB Security Group</title>
</head>
<body>
  <div class="body">
    <h1>DB Security Group Details</h1>
    <g:if test="${flash.message}">
      <div class="message">${flash.message}</div>
    </g:if>
    <div class="buttons">
      <g:form>
        <input type="hidden" name="name" value="${group.dBSecurityGroupName}"/>
        <g:link class="edit" action="edit" params="[name:group.dBSecurityGroupName]">Edit DB Security Group</g:link>
        <g:buttonSubmit class="delete"
                data-warning="Really delete DB Security Group '${group.dBSecurityGroupName}'?"
                action="delete" value="Delete DB Security Group"/>
      </g:form>
    </div>
    <div class="dialog">
      <table>
        <tbody>
        <tr class="prop">
          <td class="name">Name:</td>
          <td class="value">${group.dBSecurityGroupName}</td>
        </tr>
        <tr class="prop">
          <td class="name">Owner ID:</td>
          <td class="value">${accountNames.containsKey(group.ownerId) ? accountNames[group.ownerId] : '?'} (${group.ownerId})</td>
        </tr>
        <tr class="prop">
          <td class="name">Description:</td>
          <td class="value">${group.dBSecurityGroupDescription}</td>
        </tr>
        <tr class="prop">
          <td class="name">EC2 Security Group Ingress Permissions:</td>
          <td class="value">
            <g:each var="perm" in="${group.getEC2SecurityGroups().collect{it.getEC2SecurityGroupName()}.sort{it.toLowerCase()} }" status="i">
              ${perm}<br/>
            </g:each>
          </td>
        </tr>
        <tr class="prop">
          <td class="name">IP Range Ingress Permissions:</td>
          <td class="value">
            <g:each var="perm" in="${group.getIPRanges().collect{it.getCIDRIP()}?.sort()}" status="i">
              ${perm}<br/>
            </g:each>
          </td>
        </tr>
        </tbody>
      </table>
    </div>
  </div>
</body>
</html>
