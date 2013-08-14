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
  <title>Security Groups</title>
</head>
<body>
<div class="body">
  <h1>Security Groups in ${region.description}${appNames ? ' for ' + appNames : ''}</h1>
  <g:if test="${flash.message}">
    <div class="message">${flash.message}</div>
  </g:if>
  <g:form method="post">
    <div class="list">
      <div class="buttons">
        <g:link class="create" action="create">Create New Security Group</g:link>
      </div>
      <table class="sortable">
        <thead>
        <tr>
          <th>Name</th>
          <th>Description</th>
          <th>VPC</th>
          <th>Ingress Permissions</th>
        </tr>
        </thead>
        <tbody>
        <g:each var="grp" in="${securityGroups}" status="i">
          <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
            <td><g:linkObject type="security" name="${grp.groupId}">${grp.groupName}</g:linkObject></td>
            <td>${grp.description}</td>
            <td>${grp.vpcId}</td>
            <td><g:each var="perm" in="${grp.ipPermissions}">
              ${perm.ipProtocol} ${perm.fromPort}-${perm.toPort} [
              <g:each var="pair" in="${perm.userIdGroupPairs}" status="j"><g:if test="${j>0}">, </g:if>
                <g:linkObject type="security" name="${pair.groupId}">${pair.groupName}</g:linkObject>
              </g:each>
              ] ${perm.ipRanges ?: ''}
              <br>
            </g:each></td>
          </tr>
        </g:each>
        </tbody>
      </table>
    </div>
    <div class="paginateButtons">
    </div>
  </g:form>
</div>
</body>
</html>
