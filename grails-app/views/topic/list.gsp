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
  <title>Topics</title>
</head>
<body>
<div class="body">
  <h1>Topics in ${region.description}</h1>
  <g:if test="${flash.message}">
    <div class="message">${flash.message}</div>
  </g:if>
  <g:form method="post">
    <div class="list">
      <div class="buttons">
        <g:link class="create" action="create">Create New Topic</g:link>
      </div>
      <table class="sortable">
        <thead>
        <tr>
          <th>Topic</th>
        </tr>
        </thead>
        <tbody>
        <g:each var="topic" in="${topics}" status="i">
          <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
            <td><g:linkObject type="topic" name="${topic.name}"/></td>
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
