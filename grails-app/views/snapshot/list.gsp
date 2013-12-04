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
  <title>EBS Snapshots</title>
</head>
<body>
  <div class="body">
    <h1>EBS Snapshots in ${region.description}</h1>
    <g:if test="${flash.message}">
      <div class="message">${flash.message}</div>
    </g:if>
    <g:form method="post">
      <div class="list">
        <div class="buttons"></div>
        <table class="sortable">
          <thead>
          <tr>
            <th>Snapshot ID</th>
            <th>Volume ID</th>
            <th>Status</th>
            <th>Size<br/>(GB)</th>
            <th>Description</th>
            <th>Owner<br/>Alias</th>
            <th>Owner ID</th>
            <th>Progress</th>
            <th>Tags</th>
            <th>Start Time</th>
          </tr>
          </thead>
          <tbody>
          <g:each var="s" in="${snapshots}" status="i">
            <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
              <td><g:linkObject type="snapshot" name="${s.snapshotId}"/></td>
              <td><g:linkObject type="volume" name="${s.volumeId}"/></td>
              <td>${s.state}</td>
              <td>${s.volumeSize}</td>
              <td>${s.description}</td>
              <td>${s.ownerAlias}</td>
              <td>${s.ownerId}</td>
              <td>${s.progress}</td>
              <td>
                <g:if test="${s.tags}">
                  <g:each var="tag" in="${s.tags}">
                    <span class="tagKey">${tag.key}:</span> ${tag.value}<br/>
                  </g:each>
                </g:if>
              </td>
              <td><g:formatDate date="${s.startTime}"/></td>
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
