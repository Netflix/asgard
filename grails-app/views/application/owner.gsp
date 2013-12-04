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
  <title>Application Owners</title>
</head>
<body>
<div class="body">
  <h1>Application Owners and their cloud usage in ${region}</h1>
  <g:if test="${flash.message}">
    <div class="message">${flash.message}</div>
  </g:if>
  <div class="list">
    <div class="buttons"></div>
    <table class="sortable">
      <thead>
      <tr>
        <th>Name</th>
        <th>Email</th>
        <th>Apps</th>
        <th>ASGs</th>
        <th>Instances</th>
      </tr>
      </thead>
      <tbody>
      <g:each var="theOwner" in="${owners}" status="i">
        <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
          <td><g:link class="user" action="list" id="${theOwner.name}">${theOwner.name}</g:link></td>
          <td>
            <g:each in="${theOwner.emails}" var="email">
              <g:link class="user" action="list" id="${email}">${email}</g:link><br/>
            </g:each>
          </td>
          <td><g:link class="application" controller="application" action="list" id="${theOwner.name}">${theOwner.appNames.size()}</g:link></td>
          <td><g:link class="autoScaling" controller="autoScaling" action="list" id="${theOwner.appNames.join(',')}">${theOwner.autoScalingGroupCount}</g:link></td>
          <td><g:link class="instance" controller="instance" action="list" id="${theOwner.appNames.join(',')}">${theOwner.instanceCount}</g:link></td>
        </tr>
      </g:each>
      </tbody>
    </table>
  </div>
  <footer/>
</div>
</body>
</html>
