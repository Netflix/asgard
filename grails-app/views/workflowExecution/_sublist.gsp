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
      <th>Run ID</th>
      <th>Workflow Type</th>
      <th>Version</th>
      <th>Start Timestamp</th>
      <th>Close Timestamp</th>
      <th>Close Status</th>
      <th>Canceled</th>
      <th>Tags</th>
    </tr>
    </thead>
    <tbody>
    <g:each var="executionInfo" in="${executions}" status="i">
      <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
        <td><g:linkObject type="workflowExecution" params="[runId: executionInfo.execution.runId, workflowId: executionInfo.execution.workflowId]" name="${executionInfo.execution.runId[0..6]}..."/></td>
        <td>${executionInfo.workflowType.name}</td>
        <td>${executionInfo.workflowType.version}</td>
        <td><g:formatDate date="${executionInfo.startTimestamp}"/></td>
        <td><g:formatDate date="${executionInfo.closeTimestamp}"/></td>
        <td>${executionInfo.closeStatus}</td>
        <td>${executionInfo.cancelRequested}</td>
        <td>${executionInfo.tagList}</td>
      </tr>
    </g:each>
    </tbody>
  </table>
</div>
<div class="paginateButtons"></div>
