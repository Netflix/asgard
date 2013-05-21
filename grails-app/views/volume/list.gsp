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
  <title>EBS Volumes</title>
</head>
<body>
  <div class="body">
    <h1>EBS Volumes in ${region.description}</h1>
    <g:if test="${flash.message}">
      <div class="message">${flash.message}</div>
    </g:if>
    <g:form method="post" class="validate">
      <div class="list">
        <div class="buttons">
          <g:buttonSubmit class="create requireLogin" action="save" value="Create Volume"/><br/>
          <label>Volume Size (in GB):</label><g:textField name="volumeSize" size="12" placeholder="--ENTER SIZE--" class="required requireLogin"/>
          <label>Availability Zone:</label><g:select name="availabilityZone" from="${zoneList.zoneName}" class="requireLogin"/>
        </div>
        <table class="sortable">
          <thead>
          <tr>
            <th>Volume ID</th>
            <th>Status</th>
            <th>Size (GB)</th>
            <th>Zone</th>
            <th>Date Created</th>
            <th>Tags</th>
            <th>Attached To</th>
          </tr>
          </thead>
          <tbody>
          <g:each var="v" in="${volumes}" status="i">
            <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
              <td><g:linkObject type="volume" name="${v.volumeId}"/></td>
              <td>${v.state}</td>
              <td>${v.size}</td>
              <td>${v.availabilityZone}</td>
              <td class="date"><g:formatDate date="${v.createTime}"/></td>
              <td>
                <g:if test="${v.tags}">
                  <g:each var="tag" in="${v.tags}">
                    <span class="tagKey">${tag.key}:</span> ${tag.value}<br/>
                  </g:each>
                </g:if>
              </td>
              <td><g:each var="va" in="${v.attachments.sort{it.instanceId} }">
                <g:linkObject type="instance" name="${va.instanceId}"/><br/>
              </g:each></td>
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
