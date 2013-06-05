<%--

    Copyright 2013 Netflix, Inc.

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
  <title>Add Resource Record Set</title>
</head>
<body>
<div class="body">
  <h1>Add Resource Record Set</h1>
  <g:if test="${flash.message}">
    <div class="message">${flash.message}</div>
  </g:if>
  <g:hasErrors bean="${cmd}">
    <div class="errors">
      <g:renderErrors bean="${cmd}" as="list"/>
    </div>
  </g:hasErrors>
  <g:form action="addResourceRecordSet" method="post" class="validate allowEnterKey">
    <div>
      <table>
        <tbody valign="top">
        <tr class="prop">
          <td class="name">Hosted Zone:</td>
          <td class="value"><g:linkObject type="hostedZone" name="${hostedZoneId}"/></td>
        </tr>
        <tr class="prop">
          <td class="name">
            <label for="type">Type:</label>
          </td>
          <td class="value">
            <g:select name="type" from="${types}" value="${params.type}" noSelection="['':'']"/>
          </td>
        </tr>
        <tr class="prop">
          <td class="name">
            <label for="resourceRecordSetName">Resource Record Set Name:</label>
          </td>
          <td class="longInput">
            <g:textField name="resourceRecordSetName" value="${params.resourceRecordSetName}"/>
          </td>
        </tr>
        <tr class="prop">
          <td class="name">
            <label for="resourceRecords">Resource Records:</label>
          </td>
          <td class="longInput">
            <g:textArea name="resourceRecords" value="${params.resourceRecords}"/>
          </td>
        </tr>
        <tr class="prop">
          <td class="name">
            <label for="ttl">Time to live (TTL) in seconds:</label>
          </td>
          <td>
            <g:textField name="ttl" value="${params.ttl}"/>
          </td>
        </tr>
        <tr class="prop">
          <td class="name">
            <label for="resourceRecordSetRegion">Region:</label>
          </td>
          <td class="value">
            <g:select name="resourceRecordSetRegion" from="${resourceRecordSetRegions}" value="${params.resourceRecordSetRegion}" noSelection="['':'']"/>
          </td>
        </tr>
        <tr class="prop">
          <td class="name">
            <label for="weight">Weight:</label>
          </td>
          <td class="value">
            <g:textField name="weight" value="${params.weight}"/>
          </td>
        </tr>
        <tr class="prop">
          <td class="name">
            <label for="failover">Failover:</label>
          </td>
          <td class="value">
            <g:select name="failover" from="${failoverValues}" value="${params.failover}" noSelection="['':'']"/>
          </td>
        </tr>
        <tr class="prop">
          <td class="name">
            <label for="comment">Comment:</label>
          </td>
          <td class="longInput">
            <g:textField name="comment" value="${params.comment}"/>
          </td>
        </tr>
        <tr class="prop">
          <td class="name">
            <label for="setIdentifier">Set Identifier:</label>
          </td>
          <td class="value">
            <g:textField name="setIdentifier" value="${params.setIdentifier}"/>
          </td>
        </tr>
        <tr class="prop">
          <td class="name">
            <label for="aliasTarget">Alias Target:</label>
          </td>
          <td class="longInput">
            <g:textField name="aliasTarget" value="${params.aliasTarget}"/>
          </td>
        </tr>
        <tr class="prop">
          <td class="name">
            <label for="healthCheckId">Health Check Id:</label>
          </td>
          <td class="longInput">
            <g:textField name="healthCheckId" value="${params.healthCheckId}"/>
          </td>
        </tr>
        </tbody>
      </table>
    </div>
    <div class="buttons">
      <input type="hidden" name="hostedZoneId" value="${hostedZoneId}"/>
      <g:buttonSubmit class="save" action="addResourceRecordSet">Add Resource Record Set</g:buttonSubmit>
    </div>
  </g:form>
</div>
</body>
</html>
