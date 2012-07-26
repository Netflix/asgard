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
  <title>DB Security Groups</title>
</head>
<body>
<div class="body">
  <h1>DB Security Groups in ${region.description}</h1>
  <g:if test="${flash.message}">
    <div class="message">${flash.message}</div>
  </g:if>
  <g:form method="post">
    <div class="list">
      <div class="buttons">
        <g:link class="create" action="create">Create New DB Security Group</g:link>
      </div>
      <table class="sortable">
        <thead>
        <tr>
          <th>Name</th>
          <th>Owner</th>
          <th>Description</th>
          <th>EC2 Permissions</th>
          <th>IP Permissions</th>
        </tr>
        </thead>
        <tbody>
        <g:each var="grp" in="${dbSecurityGroups}" status="i">
          <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
            <td><g:linkObject type="dbSecurity" name="${grp.dBSecurityGroupName}"/></td>
            <td>${accountNames.containsKey(grp.ownerId) ? accountNames[grp.ownerId] : grp.ownerId}</td>
            <td>${grp.dBSecurityGroupDescription}</td>
            <td><g:each var="perm" in="${grp.getEC2SecurityGroups().collect{it.getEC2SecurityGroupName()}.sort()}">
              ${perm}<BR>
            </g:each></td>
            <td><g:each var="perm" in="${grp.getIPRanges().collect{it.getCIDRIP()}.sort()}">
              ${perm}<BR>
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
