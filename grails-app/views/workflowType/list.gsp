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
  <title>Workflow Types</title>
</head>
<body>
<div class="body">
  <g:if test="${flash.message}">
    <div class="message">${flash.message}</div>
  </g:if>
  <h1>Workflow Type</h1>
  <div class="list">
    <div class="buttons"></div>
    <table class="sortable">
      <thead>
      <tr>
        <th>ID</th>
        <th>Name</th>
        <th>Workflow Type</th>
        <th>Version</th>
        <th>Start Timestamp</th>
        <th>Close Timestamp</th>
        <th>Execution Status</th>
        <th>Close Status</th>
        <th>Parent ID</th>
        <th>Parent Name</th>
        <th>Tags</th>
        <th>Canceled</th>
      </tr>
      </thead>
      <tbody>
      <g:each var="workflowTypeInfo" in="${workflowTypeInfos}" status="i">
        <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
          <td><g:linkObject type="execution" name="${workflowTypeInfo.runId}"/></td>
          %{--<td>${workflowTypeInfo.execution.workflowId}</td>--}%
          %{--<td>${workflowTypeInfo.workflowType.name}</td>--}%
          %{--<td>${workflowTypeInfo.workflowType.version}</td>--}%
          %{--<td><g:formatDate date="${workflowTypeInfo.startTimestamp}"/></td>--}%
          %{--<td><g:formatDate date="${workflowTypeInfo.closeTimestamp}"/></td>--}%
          %{--<td>${workflowTypeInfo.executionStatus}</td>--}%
          %{--<td>${workflowTypeInfo.closeStatus}</td>--}%
          %{--<td><g:linkObject type="execution" name="${workflowTypeInfo.parent?.runId}"/></td>--}%
          %{--<td>${workflowTypeInfo.parent?.workflowId}</td>--}%
          %{--<td>${workflowTypeInfo.tags}--}%
            %{--<g:if test="${workflowTypeInfo.tags}">--}%
              %{--<g:each var="tag" in="${workflowTypeInfo.tagList}">--}%
                %{--${tag}<br/>--}%
              %{--</g:each>--}%
            %{--</g:if>--}%
          %{--</td>--}%
          %{--<td>${workflowTypeInfo.cancelRequested}</td>--}%
        </tr>
      </g:each>
      </tbody>
    </table>
  </div>
  <div class="paginateButtons"></div>
</div>
</body>
</html>
