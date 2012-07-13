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
<h1>${classification} Workflow Executions</h1>
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
    <g:each var="executionInfo" in="${executions}" status="i">
      <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
        <td><g:linkObject type="execution" name="${executionInfo.execution.runId}"/></td>
        <td>${executionInfo.execution.workflowId}</td>
        <td>${executionInfo.workflowType.name}</td>
        <td>${executionInfo.workflowType.version}</td>
        <td><g:formatDate date="${executionInfo.startTimestamp}"/></td>
        <td><g:formatDate date="${executionInfo.closeTimestamp}"/></td>
        <td>${executionInfo.executionStatus}</td>
        <td>${executionInfo.closeStatus}</td>
        <td><g:linkObject type="execution" name="${executionInfo.parent?.runId}"/></td>
        <td>${executionInfo.parent?.workflowId}</td>
        <td>${executionInfo.tags}
          <g:if test="${executionInfo.tags}">
            <g:each var="tag" in="${executionInfo.tagList}">
              ${tag}<br/>
            </g:each>
          </g:if>
        </td>
        <td>${executionInfo.cancelRequested}</td>
      </tr>
    </g:each>
    </tbody>
  </table>
</div>
<div class="paginateButtons"></div>
