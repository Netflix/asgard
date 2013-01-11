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
  <title>${snapshot.snapshotId} Snapshot</title>
</head>
<body>
  <div class="body">
    <h1>Snapshot Details</h1>
    <g:if test="${flash.message}">
      <div class="message">${flash.message}</div>
    </g:if>
    <g:hasErrors bean="${snapshotCommand}">
      <g:renderErrors bean="${snapshotCommand}" as="list"/>
    </g:hasErrors>
    <g:if test="${snapshot}">
      <g:form>
        <input type="hidden" name="snapshotId" value="${snapshot.snapshotId}"/>
        <div class="buttons">
          <g:if test="${accountName == env}">
            <g:buttonSubmit class="delete" action="delete" value="Delete Snapshot"
                            data-warning="Really delete Snapshot '${snapshot.snapshotId}'?" />
          </g:if>
          <g:else>
            <g:buttonSubmit disabled="true" class="delete" action="ignore"
                            value="This snapshot can only be deleted in ${accountName}"/>
          </g:else>
        </div>
      </g:form>
      <g:form class="validate">
        <div class="buttons">
          <g:buttonSubmit class="create requireLogin" action="restore" value="Create Volume From Snapshot"/><br/>
          Volume Size (in GB): <g:textField name="volumeSize" size="15" placeholder="--ENTER SIZE--" class="required requireLogin" />
          Availability Zone: <g:select name="zone" from="${zoneList.zoneName}" class="requireLogin" />
        </div>
      </g:form>
    </g:if>
    <div class="dialog">
      <table>
        <tbody>
        <tr class="prop">
          <td class="name" title="Snapshot ID">Snapshot ID:</td>
          <td class="value">${snapshot.snapshotId}</td>
        </tr>
        <tr class="prop">
          <td class="name" title="Volume ID">Volume ID:</td>
          <td class="value"><g:linkObject type="volume" name="${snapshot.volumeId}"/></td>
        </tr>
        <tr class="prop">
          <td class="name" title="Status">Status:</td>
          <td class="value">${snapshot.state}</td>
        </tr>
        <tr class="prop">
          <td class="name" title="Size">Size:</td>
          <td class="value">${snapshot.volumeSize} GB</td>
        </tr>
        <tr class="prop">
          <td class="name" title="Description">Description:</td>
          <td class="value">${snapshot.description}</td>
        </tr>
        <tr class="prop">
          <td class="name" title="Owner">Owner:</td>
          <td class="value">${accountName} (${snapshot.ownerId}) ${snapshot.ownerAlias}</td>
        </tr>
        <tr class="prop">
          <td class="name" title="Progress">Progress:</td>
          <td class="value">${snapshot.progress}</td>
        </tr>
        <tr class="prop">
          <td class="name" title="Start Time">Start Time:</td>
          <td class="value"><g:formatDate date="${snapshot.startTime}"/></td>
        </tr>
        <g:render template="/common/showTags" model="[entity: snapshot]"/>
        </tbody>
      </table>
    </div>
  </div>
</body>
</html>
