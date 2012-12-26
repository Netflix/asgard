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
  <title>RDS Snapshots</title>
</head>
<body>
  <div class="body">
    <h1>RDS Snapshots in ${region.description}</h1>
    <g:if test="${flash.message}">
      <div class="message">${flash.message}</div>
    </g:if>
    <g:form method="post">
      <div class="list">
        <div class="buttons">
          <g:buttonSubmit class="delete" value="Delete Snapshot(s)" action="delete"
                  data-warning="Really delete snapshot(s)?"/>
        </div>
        <table class="sortable">
          <thead>
          <tr>
            <th>&thinsp;x</th>
            <th>DB Snapshot ID</th>
            <th>DB Instance ID</th>
            <th>Status</th>
            <th>Instance Created</th>
            <th>Snapshot Created</th>
          </tr>
          </thead>
          <tbody>
          <g:each var="s" in="${snapshots}" status="i">
            <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
              <td><g:checkBox class="requireLogin" name="selectedSnapshots" value="${s.dBSnapshotIdentifier}" checked="0"/></td>
              <td><g:linkObject type="dbSnapshot" name="${s.dBSnapshotIdentifier}"/></td>
              <td>${s.dBInstanceIdentifier}</td>
              <td>${s.status}</td>
              <td>${s.instanceCreateTime}</td>
              <td>${s.snapshotCreateTime}</td>
            </tr>
          </g:each>
          </tbody>
        </table>
      </div>
      <div class="paginateButtons">
      </div>
    </g:form>
  </div>
</body>
</html>
