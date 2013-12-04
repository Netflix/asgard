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
  <title>Auto Scaling Groups with Missing AMIs</title>
</head>
<body>
<div class="body">
  <h1>Auto Scaling Groups with Missing AMIs in ${region.description}${appNames ? ' for ' + appNames : ''}</h1>
  <g:if test="${flash.message}">
    <div class="message">${flash.message}</div>
  </g:if>
  <g:form method="post">
    <div class="buttons">
    </div>
    <div class="list">
      <table class="sortable">
        <thead>
        <tr>
          <th>Group Name</th>
          <th>App</th>
          <th>Email</th>
          <th>Launch Configuration</th>
          <th class="tiny">Min</th>
          <th class="tiny">Max</th>
          <th class="tiny">Des</th>
          <th>Instances</th>
          <th>Created Time</th>
        </tr>
        </thead>
        <tbody>
        <g:each in="${autoScalingGroups}" status="i" var="group">
          <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
            <td class="autoScaling"><g:linkObject type="autoScaling" name="${group.autoScalingGroupName}"/></td>
            <td class="app"><g:linkObject type="application" name="${groupNamesToApps[group.autoScalingGroupName]?.name}"/></td>
            <td>${groupNamesToApps[group.autoScalingGroupName]?.email}</td>
            <td class="launchConfig"><g:linkObject type="launchConfiguration" name="${group.launchConfigurationName}"/></td>
            <td>${group.minSize}</td>
            <td>${group.maxSize}</td>
            <td>${group.desiredCapacity}</td>
            <td class="countAndList hideAdvancedItems">
              <span class="toggle fakeLink">${group.instances.size()}</span>
              <div class="advancedItems tiny">
                <g:each var="ins" in="${group.instances}">
                  <g:linkObject name="${ins.instanceId}"/><br/>
                </g:each>
              </div>
            </td>
            <td><g:formatDate date="${group.createdTime}"/></td>
          </tr>
        </g:each>
        </tbody>
      </table>
    </div>
    <footer/>
  </g:form>
</div>
</body>
</html>
