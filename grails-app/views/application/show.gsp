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
  <title>${app.name} Application</title>
</head>
<body>
  <div class="body">
    <h1>Application Details in ${region.description}</h1>
    <g:if test="${flash.message}">
      <div class="message">${flash.message}</div>
    </g:if>
    <div class="buttons">
      <g:form>
        <g:link class="edit" action="edit" params="[id:app.name]">Edit Application</g:link>
        <input type="hidden" name="name" value="${app.name}"/>
        <g:buttonSubmit class="delete" data-warning="Really delete application '${app.name}'?" action="delete" value="Delete Application"/>
        <g:if test="${appSecurityGroup}">
          <input type="hidden" name="securityGroupId" value="${appSecurityGroup.groupId}"/>
          <g:buttonSubmit class="securityEdit" action="security">Edit Application Security Access</g:buttonSubmit>
        </g:if>
        <g:else>
          <g:link class="create" controller="security" action="create" params="[id:app.name]">Create Security Group</g:link>
        </g:else>
      </g:form>
    </div>
    <div class="dialog">
      <table>
        <tbody>
        <tr class="prop">
          <td class="name">Name:</td>
          <td class="value">${app.name}
          <g:if test="${strictName == false}">
            <p class="warning">Warning: Punctuation in name prevents use as frontend service.</p>
          </g:if>
          </td>
        </tr>
        <tr class="prop">
          <td class="name">App Group:</td>
          <td class="value">${app.group}</td>
        </tr>
        <tr class="prop">
          <td class="name">Type:</td>
          <td class="value">${app.type}</td>
        </tr>
        <tr class="prop">
          <td class="name">Description:</td>
          <td class="value">${app.description}</td>
        </tr>
        <tr class="prop">
          <td class="name">Owner:</td>
          <td class="value">${app.owner}</td>
        </tr>
        <tr class="prop">
          <td class="name">Email:</td>
          <td class="value">${app.email}</td>
        </tr>
        <tr class="prop">
          <td class="name">Monitor Bucket Type:</td>
          <td class="value">${app.monitorBucketType.description}</td>
        </tr>
        <g:if test="${isChaosMonkeyActive}">
          <tr class="prop">
            <td class="name">Chaos Monkey:</td>
            <td class="value"><a class="cloudready" href="${chaosMonkeyEditLink}">Edit in Cloudready</a></td>
          </tr>
        </g:if>
        <tr class="prop">
          <td class="name">Create Time:</td>
          <td class="value"><g:formatDate date="${app.createTime}"/></td>
        </tr>
        <tr class="prop">
          <td class="name">Update Time:</td>
          <td class="value"><g:formatDate date="${app.updateTime}"/></td>
        </tr>
        <g:if test="${alertingServiceConfigUrl}">
          <tr class="prop">
            <td class="name">
              <a target="_blank" href="${alertingServiceConfigUrl}">Click here to configure alerts</a>
            </td>
          </tr>
        </g:if>
        <tr class="prop">
          <td><h2>Pattern Matches in ${region.description}</h2></td>
        </tr>
        <tr class="prop">
          <td class="name">Clusters:</td>
          <td>
            <table>
              <g:each var="cluster" in="${clusters}">
                <tr>
                  <td><g:linkObject type="cluster" name="${cluster}"/></td>
                </tr>
              </g:each>
            </table>
          </td>
        </tr>
        <tr class="prop">
          <td class="name">Auto Scaling:</td>
          <td>
            <table>
              <g:each var="g" in="${groups}">
                <tr>
                  <td><g:linkObject type="autoScaling" name="${g.autoScalingGroupName}"/> (${g.instances.size()})</td>
                </tr>
              </g:each>
            </table>
          </td>
        </tr>
        <tr class="prop">
          <td class="name">Load Balancers:</td>
          <td>
            <table>
              <g:each var="b" in="${balancers}">
                <tr>
                  <td><g:linkObject type="loadBalancer" name="${b.loadBalancerName}"/></td>
                </tr>
              </g:each>
            </table>
          </td>
        </tr>
        <tr class="prop">
          <td class="name">Security Groups:</td>
          <td>
            <table>
              <g:each var="s" in="${securities}">
                <tr>
                  <td><g:linkObject type="security" name="${s.groupId}">${s.groupName}</g:linkObject></td>
                </tr>
              </g:each>
            </table>
          </td>
        </tr>
        <tr class="prop">
          <td class="name">Launch Configurations:</td>
          <td>
            <table>
              <g:each var="l" in="${launches}">
                <tr>
                  <td><g:linkObject type="launchConfiguration" name="${l.launchConfigurationName}"/></td>
                </tr>
              </g:each>
            </table>
          </td>
        </tr>
        <tr class="prop">
          <td class="name">Instances:</td>
          <td><g:link controller="instance" action="list" id="${app.name}" class="instance"
                      title="Show running instances of this app">Running Instance List</g:link></td>
        </tr>
        <tr class="prop">
          <td class="name">Images:</td>
          <td><g:link controller="image" action="list" id="${app.name}" class="image"
                      title="Show the machine images for the package with the same name as this">Image List</g:link></td>
        </tr>
        </tbody>
      </table>
    </div>
  </div>
</body>
</html>
