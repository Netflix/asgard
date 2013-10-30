<%--

    Copyright 2013 Netflix, Inc.

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
  <title>Instance</title>
</head>

<body>
<div class="body">
  <h1>Links to sub-lists of Instances in ${region.description}</h1>
  <g:if test="${flash.message}">
    <div class="message">${flash.message}</div>
  </g:if>
  <div class="list">
    <div class="buttons"></div>
    <table class="sortable instances">
      <thead>
      <tr>
        <th>Sub-List by Application</th>
      </tr>
      </thead>
      <tbody>
      <tr class="even">
        <td><g:link class="instance" controller="instance" action="list">(All)</g:link></td>
      </tr>
      <tr class="odd">
        <td><g:link class="instance" controller="instance" action="list" id="${noAppId}">(No App)</g:link></td>
      </tr>
      <g:each var="appName" in="${appNames}" status="i">
        <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
          <td><g:link class="instance" controller="instance" action="list" id="${appName}">${appName}</g:link></td>
        </tr>
      </g:each>
      </tbody>
    </table>
  </div>
  <div class="paginateButtons"></div>
</div>
</body>
</html>
