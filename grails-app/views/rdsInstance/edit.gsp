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
  <title>Edit DB Instance</title>
</head>
<body>
  <div class="body">
    <h1>Edit DB Instance</h1>
    <g:if test="${flash.message}">
      <div class="message">${flash.message}</div>
    </g:if>
    <g:hasErrors bean="${cmd }">
      <g:renderErrors bean="${cmd}" as="list"/>
    </g:hasErrors>
    <g:form method="post">
    <div class="dialog">
      <table>
        <tbody>
        <tr class="prop">
          <td class="name" title="DB Instance Identifier">DB Instance Identifier:</td>
          <td class="value">${dbInstance.dBInstanceIdentifier}</td>
        </tr>
        <tr class="prop">
          <td class="name" title="Allocated Storage (in GB)"><label for="allocatedStorage">Capacity (GB):</label></td>
          <td class="value"><g:textField name="allocatedStorage" value="${dbInstance.allocatedStorage}"/></td>
        </tr>
        <tr class="prop">
          <td class="name" title="Backup Retention (in days)"><label for="backupRetentionPeriod">Backup Retention (days):</label></td>
          <td class="value"><g:textField name="backupRetentionPeriod" value="${dbInstance.backupRetentionPeriod}"/></td>
        </tr>
        <tr class="prop">
          <td class="name" title="DB Class"><label for="dBInstanceClass">DB Class:</label></td>
          <td class="value"><g:select name="dBInstanceClass" noSelection="['':'-Choose your database class-']" value="${dbInstance.dBInstanceClass }" from="${allDBInstanceClasses}"/></td>
        </tr>
        <tr class="prop">
          <td class="name" title="Master User Password"><label for="masterUserPassword">Master User Password:</label></td>
          <td class="value"><g:textField name="masterUserPassword" value=""/></td>
        </tr>
        <tr class="prop">
          <td class="name" title="Daily Backup Window"><label for="preferredBackupWindow">Daily Backup Window:</label></td>
          <td class="value"><g:textField name="preferredBackupWindow" value="${dbInstance.preferredBackupWindow}"/></td>
        </tr>
        <tr class="prop">
          <td class="name" title="Weekly Maintenance Window"><label for="preferredMaintenanceWindow">Weekly Maintenance Window:</label></td>
          <td class="value"><g:textField name="preferredMaintenanceWindow" value="${dbInstance.preferredMaintenanceWindow}"/></td>
        </tr>
        <tr class="prop">
          <td class="name" title="Multiple AZ"><label for="multiAZ">Multiple AZ:</label></td>
          <td class="value"><g:checkBox name="multiAZ" value="${dbInstance.multiAZ}"/></td>
        </tr>
        
        <tr class="prop">
          <td class="name">DB Security Groups:</td>
          <td class="value">
            <table>
              <g:each var="g" in="${allDBSecurityGroups.collect{it.dBSecurityGroupName}.sort{it.toLowerCase()}}">
                <tr class="prop">
                  <td class="name">${g}</td>  
                  <td class="value"><g:checkBox name="selectedDBSecurityGroups" value="${g}" checked="${g in instanceDBSecurityGroups}"/></td>
                </tr>
              </g:each>
            </table>
          </td>
        </tr>
       
        </tbody>
      </table>
    </div>

    <div class="buttons">
        <input type="hidden" name="dBInstanceIdentifier" value="${dbInstance.dBInstanceIdentifier}"/>
        <g:if test="${dbInstance}">
          <g:buttonSubmit class="save" onclick="return confirm('Really update ${dbInstance.dBInstanceIdentifier}?');"
                  action="update" value="Update DB Instance"/>
        </g:if>
      
    </div>
    </g:form>

  </div>
</body>
</html>
