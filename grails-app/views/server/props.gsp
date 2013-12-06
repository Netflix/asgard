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
  <title>Env Vars and Sys Props</title>
</head>
<body>
  <div class="body">
    <h1>Environment Variables and System Properties</h1>
    <g:if test="${flash.message}">
      <div class="message">${flash.message}</div>
    </g:if>
    <h2>Environment Variables</h2>
    <div class="list">
      <div class="buttons"></div>
      <table class="sortable">
        <thead>
        <tr>
          <th>Key</th>
          <th>Value</th>
        </tr>
        </thead>
        <tbody>
        <g:each var="envVar" in="${environmentVariables}" status="i">
          <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
            <td>${envVar.key}</td>
            <td>${envVar.value}</td>
          </tr>
        </g:each>
        </tbody>
      </table>
    </div>
    <footer/>
  </div>
  <div class="body">
    <h2>System Properties</h2>
    <div class="list">
      <div class="buttons"></div>
      <table class="sortable">
        <thead>
        <tr>
          <th>Key</th>
          <th>Value</th>
        </tr>
        </thead>
        <tbody>
        <g:each var="prop" in="${systemProperties}" status="i">
          <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
            <td>${prop.key}</td>
            <td>${prop.value}</td>
          </tr>
        </g:each>
        </tbody>
      </table>
    </div>
    <footer/>
  </div>
</body>
</html>
