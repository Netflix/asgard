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
<%@ page import="com.netflix.asgard.AwsRdsService" %>
<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  <meta name="layout" content="main"/>
  <title>Create DB Instance</title>
</head>
<body>
  <div class="body">
    <h1>Create DB Instance</h1>
    <g:if test="${flash.message}">
      <div class="message">${flash.message}</div>
    </g:if>
    <g:hasErrors bean="${cmd}">
      <div class="errors">
        <g:renderErrors bean="${cmd}" as="list"/>
      </div>
    </g:hasErrors>
    <g:form action="save" method="post" class="validate">
      <div class="dialog">
        <table>
          <tbody>
          <tr class="prop">
            <td class="name" title="DB Instance Identifier">DB Instance Identifier:</td>
            <td class="value"><g:textField name="dBInstanceIdentifier" value="${params.dBInstanceIdentifier}" class="required"/></td>
          </tr>
          <tr class="prop">
            <td class="name" title="DB Name">DB Name:</td>
            <td class="value"><g:textField name="dBName" value="${params.dBName}"/></td>
          </tr>
          <tr class="prop">
            <td class="name" title="Allocated Storage (in GB)"><label for="allocatedStorage">Capacity (GB):</label></td>
            <td class="value"><g:textField name="allocatedStorage" value="${params.allocatedStorage}" class="required"/></td>
          </tr>
          <tr class="prop">
            <td class="name" title="Port"><label for="port">Port:</label></td>
            <td class="value"><g:textField name="port" value="${params.port ?: AwsRdsService.DEFAULT_PORT}"/></td>
          </tr>
          <tr class="prop">
            <td class="name" title="Backup Retention (in days)"><label for="backupRetentionPeriod">Backup Retention (days):</label></td>
            <td class="value"><g:textField name="backupRetentionPeriod" value="${params.backupRetentionPeriod }"/></td>
          </tr>
          <tr class="prop">
            <td class="name" title="DB Class"><label for="dBInstanceClass">DB Class:</label></td>
            <td class="value"><g:select name="dBInstanceClass" noSelection="['':'']" value="${params.dBInstanceClass }" from="${allDBInstanceClasses}" data-placeholder="-DB class-"/></td>
          </tr>
          <tr class="prop" title="The name of the database engine to be used for this instance.">
            <td class="name"><label for="engine">DB Engine:</label></td>
            <td class="value"><g:select name="engine" value="${params.engine }" from="${allDbInstanceEngines}"/></td>
          </tr>
          <tr class="prop" title="License model information for this DB Instance.">
            <td class="name"><label for="licenseModel">License Model:</label></td>
            <td class="value"><g:select name="licenseModel" value="${params.licenseModel}" from="${allLicenseModels}"/></td>
          </tr>
          <tr class="prop">
            <td class="name" title="Availability Zone"><label for="availabilityZone">Availability Zone:</label></td>
            <td class="value"><g:select name="availabilityZone" noSelection="['':'']" value="${params.availabilityZone }" from="${zoneList.zoneName}" data-placeholder="-Zone-"/></td>
          </tr>
          <tr class="prop">
            <td class="name" title="Master User Name"><label for="masterUsername">Master User Name:</label></td>
            <td class="value"><g:textField name="masterUsername" value="${params.masterUsername }"/></td>
          </tr>
          <tr class="prop">
            <td class="name" title="Master User Password"><label for="masterUserPassword">Master User Password:</label></td>
            <td class="value"><g:textField name="masterUserPassword" value="${params.masterUserPassword }"/></td>
          </tr>
          <tr class="prop">
            <td class="name" title="Daily Backup Window"><label for="preferredBackupWindow">Daily Backup Window:</label></td>
            <td class="value"><g:textField name="preferredBackupWindow" value="${params.preferredBackupWindow }"/></td>
          </tr>
          <tr class="prop">
            <td class="name" title="Weekly Maintenance Window"><label for="preferredMaintenanceWindow">Weekly Maintenance Window:</label></td>
            <td class="value"><g:textField name="preferredMaintenanceWindow" value="${params.preferredMaintenanceWindow }"/></td>
          </tr>
          <tr class="prop">
            <td class="name" title="Multiple AZ"><label for="multiAZ">${params.multiAZ}  Multiple AZ:</label></td>
            <td class="value"><g:checkBox name="multiAZ" value="on" checked="${'on' == params.multiAZ }"/></td>
          </tr>
          <tr class="prop">
            <td class="name">DB Security Groups:</td>
            <td class="value">
              <table>
                <g:each var="g" in="${allDBSecurityGroups.collect{it.dBSecurityGroupName}.sort{it.toLowerCase()}}">
                  <tr class="prop">
                    <td class="name">${g}</td>
                    <td class="value"><g:checkBox name="selectedDBSecurityGroups" value="${g}"  checked="${g in params.selectedDBSecurityGroups}"/></td>
                  </tr>
                </g:each>
              </table>
            </td>
          </tr>
          </tbody>
        </table>
      </div>
      <div class="buttons">
        <g:buttonSubmit class="save" action="save">Create New DB Instance</g:buttonSubmit>
      </div>
    </g:form>
  </div>
</body>
</html>
