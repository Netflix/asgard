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
  <title>Applications</title>
</head>
<body>
<div class="body">
  <h1>Registered Applications${terms ? ' for ' + terms : ''}</h1>
  <g:if test="${flash.message}">
    <div class="message">${flash.message}</div>
  </g:if>

      <g:form method="post">
        <div class="list">
          <div class="buttons">
            <g:link class="create" action="create">Create New Application</g:link>
          </div>
          <table class="sortable">
            <thead>
            <tr>
              <th>Name</th>
              <th>ASGs</th>
              <th>Instances</th>
              <th>Description</th>
              <th>Group</th>
              <th>Email</th>
              <th>Owner</th>
              <th>Create Time</th>
              <th>Update Time</th>
              <th>Tags</th>
            </tr>
            </thead>
            <tbody>
              <g:set var="i" value="0" />
              <g:each in="${applications.groups()}" var="entry">
                <g:set var="group" value="${entry.key}" />
                <g:set var="apps" value="${entry.value}" />
                <g:each in="${apps}" var="app">
                  <g:set var="i" value="${Integer.valueOf(i+1)}" />
                  <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
                    <td class="app"><g:linkObject type="application" name="${app.name}"/></td>
                    <td><g:link class="autoScaling" controller="autoScaling" action="list" id="${app.name}">${groupCountsPerAppName.getCount(app.name)}</g:link></td>
                    <td><g:link class="instance" controller="instance" action="list" id="${app.name}">${instanceCountsPerAppName.getCount(app.name)}</g:link></td>
                    <td class="description">${app.description}</td>
                    <td class="group">
                      <g:link action="list" params="[id:group]">${group}</g:link>
                    </td>
                    <td class="email">${app.email}</td>
                    <td class="owner">${app.owner}</td>
                    <td><g:formatDate date="${app.createTime}"/></td>
                    <td><g:formatDate date="${app.updateTime}"/></td>
                    <td class="tags">
                      <g:each in="${app.tags}" var="tag">
                        <span class="label">
                          <g:link action="list" params="[id: tag]">${tag}</g:link>
                        </span>
                      </g:each>
                    </td>
                  </tr>
                </g:each>
              </g:each>
            </tbody>
          </table>
        </div>
      </g:form>


  <footer/>
</div>
</body>
</html>
