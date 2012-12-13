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
  <title>${dbInstance.dBInstanceIdentifier} DB Instance</title>
</head>
<body>
  <div class="body">
    <h1>DB Instance Details</h1>
    <g:if test="${flash.message}">
      <div class="message">${flash.message}</div>
    </g:if>
    <g:if test="${dbInstance}">
      <div class="buttons">
        <g:form>
          <input type="hidden" name="dBInstanceIdentifier" value="${dbInstance.dBInstanceIdentifier}"/>
            <g:buttonSubmit class="delete" action="delete" value="Delete DB Instance"
                            data-warning="Really delete DB Instance '${dbInstance.dBInstanceIdentifier}'?" />
            <g:link class="edit" action="edit" params="[dBInstanceIdentifier:dbInstance.dBInstanceIdentifier]">Edit DB Instance</g:link>
        </g:form>
      </div>
      <g:if test="${dbInstance?.dBInstanceStatus == 'available'}">
        <div class="buttons">
          <g:form controller="dbSnapshot">
            <input type="hidden" name="dBInstanceIdentifier" value="${dbInstance.dBInstanceIdentifier}"/>
            <g:buttonSubmit class="takeSnapshot requireLogin" action="create" value="Take Snapshot"/>
            <g:textField class="requireLogin" name="snapshotName" size="50" value="${dbInstance.dBInstanceIdentifier}-snapshot"/>
          </g:form>
        </div>
      </g:if>
    </g:if>
    <div class="dialog">
      <table>
        <tbody>
        <tr class="prop">
          <td class="name" title="DB Instance Identifier">DB Instance Identifier:</td>
          <td class="value">${dbInstance.dBInstanceIdentifier}</td>
        </tr>
        <tr class="prop">
          <td class="name" title="Effective hostname from RDS">Hostname:</td>
          <td class="value">${dbInstance?.endpoint?.address}</td>
        </tr>
        <tr class="prop">
          <td class="name" title="Port">Port:</td>
          <td class="value">${dbInstance?.endpoint?.port}</td>
        </tr>
        <tr class="prop">
          <td class="name" title="Allocated Storage (in GB)">Capacity:</td>
          <td class="value">${dbInstance.allocatedStorage} GB</td>
        </tr>
        <tr class="prop">
          <td class="name" title="Availability Zone">Zone:</td>
          <td class="value">${dbInstance.availabilityZone}</td>
        </tr>
        <tr class="prop">
          <td class="name" title="Backup Retention">Backup Retention:</td>
          <td class="value">${dbInstance.backupRetentionPeriod} ${1 == dbInstance.backupRetentionPeriod ? "day" : "days"}</td>
        </tr>
        <tr class="prop">
          <td class="name" title="DB Class">Class:</td>
          <td class="value">${dbInstance.dBInstanceClass}</td>
        </tr>
        <tr class="prop">
          <td class="name" title="Status">Status:</td>
          <td class="value">${dbInstance.dBInstanceStatus}</td>
        </tr>
        <tr class="prop">
          <td class="name" title="Database Name">DB Name:</td>
          <td class="value">${dbInstance.dBName}</td>
        </tr>
        <tr class="prop" title="The name of the database engine to be used for this instance.">
          <td class="name">DB Engine:</td>
          <td class="value">${dbInstance.engine}</td>
        </tr>
        <tr class="prop" title="License model information for this DB Instance.">
          <td class="name">License Model:</td>
          <td class="value">${dbInstance.licenseModel}</td>
        </tr>
        <tr class="prop" title="Indicates the database engine version.">
          <td class="name">DB Engine Version:</td>
          <td class="value">${dbInstance.engineVersion}</td>
        </tr>
        <tr class="prop">
          <td class="name" title="Creation Date">DB Create Date:</td>
          <td class="value"><g:formatDate date="${dbInstance.instanceCreateTime}"/></td>
        </tr>
        <tr class="prop">
          <td class="name" title="Latest Point-In-Time Backup">Restorable To:</td>
          <td class="value"><g:formatDate date="${dbInstance.latestRestorableTime}"/></td>
        </tr>
        <tr class="prop">
          <td class="name" title="Database Master Username">Master Username:</td>
          <td class="value">${dbInstance.masterUsername}</td>
        </tr>
        <tr class="prop">
          <td class="name" title="Deployed to Multiple Availability Zones">Multiple AZ:</td>
          <td class="value">${dbInstance.multiAZ}</td>
        </tr>
        <tr class="prop">
          <td class="name" title="Time range for daily backups">Daily Backup Window:</td>
          <td class="value">${dbInstance.preferredBackupWindow}</td>
        </tr>
        <tr class="prop">
          <td class="name" title="Time range for weekly maintenance">Weekly Maintenance Window:</td>
          <td class="value">${dbInstance.preferredMaintenanceWindow}</td>
        </tr>
        <tr class="prop">
          <td class="name">DB Security Groups:</td>
          <td class="value">
            <table>
              <g:each var="g" in="${dbInstance.dBSecurityGroups}">
                <tr class="prop">
                  <td class="value"><g:linkObject type="dbSecurity" name="${g.dBSecurityGroupName}"/></td>
                </tr>
              </g:each>
            </table>
          </td>
        </tr>
        <tr class="prop">
          <td class="name">DB Snapshots:</td>
          <td class="value">
            <table>
              <g:each var="s" in="${snapshots.sort{it.dBSnapshotIdentifier.toLowerCase()}}">
                <tr class="prop">
                  <td class="value"><g:linkObject type="dbSnapshot" name="${s.dBSnapshotIdentifier}"/></td>
                </tr>
              </g:each>
            </table>
          </td>
        </tr>
        </tbody>
      </table>
    </div>
  </div>
</body>
</html>
