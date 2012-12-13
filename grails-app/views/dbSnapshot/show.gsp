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
  <title>${snapshot.dBSnapshotIdentifier} DB SnapshotDB Snapshot Details</title>
</head>
<body>
  <div class="body">
    <h1>DB Snapshot Details</h1>
    <g:if test="${flash.message}">
      <div class="message">${flash.message}</div>
    </g:if>
    <div class="buttons">
      <g:form>
        <input type="hidden" name="dBSnapshotIdentifier" value="${snapshot.dBSnapshotIdentifier}"/>
        <g:if test="${snapshot}">
            <g:buttonSubmit class="delete" action="delete" value="Delete DB Snapshot"
                            data-warning="Really delete DB Snapshot '${snapshot.dBSnapshotIdentifier}'?" />
        </g:if>
      </g:form>
    </div>
    <div class="buttons">
      <g:form>
        <input type="hidden" name="name" value="${snapshot.dBSnapshotIdentifier}"/>
        <g:if test="${snapshot}">
          <g:buttonSubmit class="requireLogin restore" action="quickRestore" value="Quick Restore"/>
          <g:textField class="requireLogin" name="dBInstanceIdentifier" size="50" value="NEW-INSTANCE-NAME"/>
        </g:if>
      </g:form>
    </div>
    <div class="dialog">
      <table>
        <tbody>
        <tr class="prop">
          <td class="name" title="DB Instance Identifier">DB Snapshot Identifier:</td>
          <td class="value">${snapshot.dBSnapshotIdentifier}</td>
        </tr>
        <tr class="prop">
          <td class="name" title="DB Instance Identifier">DB Instance Identifier:</td>
          <td class="value">${snapshot.dBInstanceIdentifier}</td>
        </tr>
        <tr class="prop">
          <td class="name" title="Port">Port:</td>
          <td class="value">${snapshot.port}</td>
        </tr>
        <tr class="prop">
          <td class="name" title="Status">Status:</td>
          <td class="value">${snapshot.status}</td>
        </tr>
        <tr class="prop">
          <td class="name" title="Allocated Storage (in GB)">Capacity:</td>
          <td class="value">${snapshot.allocatedStorage} GB</td>
        </tr>
        <tr class="prop">
          <td class="name" title="Availability Zone">Zone:</td>
          <td class="value">${snapshot.availabilityZone}</td>
        </tr>
        <tr class="prop" title="The name of the database engine to be used for this instance.">
          <td class="name">DB Engine:</td>
          <td class="value">${snapshot.engine}</td>
        </tr>
        <tr class="prop" title="The version number of the database engine to use.">
          <td class="name">DB Engine Version:</td>
          <td class="value">${snapshot.engineVersion}</td>
        </tr>
        <tr class="prop" title="License model information for this DB Instance.">
          <td class="name">License Model:</td>
          <td class="value">${snapshot.licenseModel}</td>
        </tr>
        <tr class="prop">
          <td class="name" title="Instance Creation Date">DB Instance Create Date:</td>
          <td class="value">${snapshot.instanceCreateTime}</td>
        </tr>
        <tr class="prop">
          <td class="name" title="Snapshot Creation Date">DB Snapshot Create Date:</td>
          <td class="value">${snapshot.snapshotCreateTime}</td>
        </tr>
        <tr class="prop">
          <td class="name" title="Database Master Username">Master Username:</td>
          <td class="value">${snapshot.masterUsername}</td>
        </tr>
        </tbody>
      </table>
    </div>
  </div>
</body>
</html>
