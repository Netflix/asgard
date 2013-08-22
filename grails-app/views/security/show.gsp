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
  <title>${group.groupName} Security Group</title>
</head>
<body>
  <div class="body">
    <h1>Security Group Details</h1>
    <g:if test="${flash.message}">
      <div class="message">${flash.message}</div>
    </g:if>
    <g:if test="${editable}">
      <div class="buttons">
        <g:form>
          <input type="hidden" name="name" value="${group.groupName}"/>
          <input type="hidden" name="id" value="${group.groupId}"/>
          <g:link class="edit" action="edit" params="[id: group.groupId]">Edit Security Group</g:link>
          <g:buttonSubmit class="delete" action="delete" value="Delete Security Group"
                          data-warning="Really delete Security Group '${group.groupName}'?" />
        </g:form>
      </div>
    </g:if>
    <div class="dialog">
      <table>
        <tbody>
        <tr class="prop">
          <td class="name">Name:</td>
          <td class="value">${group.groupName}</td>
        </tr><tr class="prop">
            <td class="name">Group ID:</td>
            <td class="value">${group.groupId}</td>
        </tr>
        <tr class="prop">
            <td class="name">VPC ID:</td>
            <td class="value">${group.vpcId}</td>
        </tr>
        <tr class="prop">
          <td class="name">Owner ID:</td>
          <td class="value">${accountNames.containsKey(group.ownerId) ? accountNames[group.ownerId] : '?'} (${group.ownerId})</td>
        </tr>
        <tr class="prop">
          <td class="name">Description:</td>
          <td class="value">${group.description}</td>
        </tr>
        <tr class="prop">
          <td class="name">Ingress Permissions:</td>
          <td class="value">
            <table>
              <tbody>
              <g:each var="perm" in="${group.ipPermissions}" status="i">
                <tr class="prop">
                  <td class="value">
                    ${perm.ipProtocol} ${perm.fromPort}-${perm.toPort}
                    [<g:each var="pair" in="${perm.userIdGroupPairs}" status="j">
                      <g:if test="${j>0}">, </g:if>
                      <g:if test="${pair.userId == group.ownerId}">
                        <g:linkObject type="security" name="${pair.groupId}">${pair.groupName}</g:linkObject>
                      </g:if>
                      <g:else>
                        <div class="security">${pair.groupName} (${accountNames[pair.userId] ?: pair.userId} ${pair.groupId})</div>
                      </g:else>
                    </g:each>]
                    ${perm.ipRanges}
                  </td>
                </tr>
              </g:each>
              </tbody>
            </table>
          </td>
        </tr>
        <g:render template="/common/showTags" model="[entity: group]"/>
        <tr class="prop">
          <td><h2>Pattern Matches</h2></td>
        </tr>
        <tr class="prop">
          <td class="name">Application:</td>
          <td class="value"><g:linkObject type="application" name="${app?.name}"/></td>
        </tr>
        <tr class="prop">
          <td class="name">Launch Configurations:</td>
          <td class="value">
            <g:each var="launchConfig" in="${launchConfigs}" status="i">
              <g:linkObject type="launchConfiguration" name="${launchConfig.launchConfigurationName}"/>
            </g:each>
          </td>
        </tr>
        <tr class="prop">
          <td class="name">Instances:</td>
          <td class="value">
            <g:each var="instance" in="${instances}" status="i">
              <g:linkObject type="instance" name="${instance.instanceId}"/>
            </g:each>
          </td>
        </tr>
        <tr class="prop">
          <td class="name">Load Balancers:</td>
          <td class="value">
            <g:each var="elb" in="${elbs}" status="i">
              <g:linkObject type="loadBalancer" name="${elb.loadBalancerName}"/>
            </g:each>
          </td>
        </tr>
        </tbody>
      </table>
    </div>
  </div>
</body>
</html>
