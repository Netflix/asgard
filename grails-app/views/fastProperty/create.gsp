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
  <title>Create New Fast Property</title>
</head>

<body>
<div class="body">
  <h1>Create New Fast Property</h1>
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
      <table class="fastPropertyAttributes ${params.hasAdvancedAttributes ? '': 'hideAdvancedItems'}">
        <tbody>
        <tr class="prop">
          <td class="name">
            <label for="key">Name:</label>
          </td>
          <td class="value">
            <g:textField class="fastPropertyValue required" id="key" name="key" value="${params.key}"/>
          </td>
        </tr>
        <tr class="prop">
          <td class="name">
            <label for="value">Value:</label>
          </td>
          <td class="value">
            <g:textArea cols="30" rows="3" class="fastPropertyValue" id="value" name="value" value="${params.value}"/>
          </td>
        </tr>
        <tr class="prop">
          <td class="name">
            <label for="updatedBy">Updated by:</label>
          </td>
          <td class="value">
            <g:textField class="required" id="updatedBy" name="updatedBy" placeholder="jsmith" value="${updatedBy}"/>
            <span class="toggle fakeLink" id="showAdvancedOptionsToCreateFastProperty">Advanced Options</span>
          </td>
        </tr>
        <g:if test="${fastPropertyInfoUrl}">
          <tr>
            <td colspan="2"><a href="${fastPropertyInfoUrl}">Fast Property Documentation</a></td>
          </tr>
        </g:if>
        <tr class="prop advanced">
          <td class="name">
            <label for="ttl">TTL:</label>
          </td>
          <td class="value">
            <g:textField id="ttl" name="ttl" value="${params.ttl}"/>
            <g:select name="ttlUnit" value="${params.ttlUnit ?: 'Days'}" from="${ttlUnits}" />
          </td>
        </tr>
        <tr class="prop advanced">
          <td class="name">
            <label for="constraints">Constraints:</label>
          </td>
          <td class="value">
            <g:textField id="constraints" name="constraints" value="${params.constraints}"/>
          </td>
        </tr>
        <tr>
          <td colspan="2">
            <h2>Scoping (highest priority first):</h2>
          </td>
        </tr>
        <tr class="prop advanced">
          <td class="name">
            <label for="serverId">Instance ID:</label>
          </td>
          <td class="value">
            <g:textField id="serverId" name="serverId" value="${params.serverId}"/>
          </td>
        </tr>
        <tr class="prop advanced">
          <td class="name">
            <label for="asg">ASG:</label>
          </td>
          <td class="value">
            <g:select name="asg" noSelection="['':'Default (all ASGs)']" value="${params.asg}" from="${asgNames}"
                      class="allowEmptySelect" />
          </td>
        </tr>
        <tr class="prop advanced">
          <td class="name">
            <label for="ami">AMI:</label>
          </td>
          <td class="value">
            <g:select name="ami" noSelection="['':'Default (all AMIs)']" value="${params.ami}" from="${images}"
                      optionKey="imageId" optionValue="imageLocation" class="allowEmptySelect" />
          </td>
        </tr>
        <tr class="prop advanced">
          <td class="name">
            <label for="cluster">Cluster:</label>
          </td>
          <td class="value">
            <g:select name="cluster" noSelection="['':'Default (all clusters)']" value="${params.cluster}" from="${clusterNames}"
                      class="allowEmptySelect" />
          </td>
        </tr>
        <tr class="prop">
          <td valign="top" class="name">
            <label for="appId">Application:</label>
          </td>
          <td>
            <g:select title="The application that this property is used for"
                      name="appId" noSelection="['':'Default (all apps)']" value="${params.appId}" from="${appNames}"
                      class="allowEmptySelect" />
          </td>
        </tr>
        <tr class="prop advanced">
          <td class="name">
            <label for="countries">Countries:</label>
          </td>
          <td class="value">
            <g:textField id="countries" name="countries" value="${params.countries}"/>
          </td>
        </tr>
        <tr class="prop">
          <td class="name">
            <label for="stack">Stack:</label>
          </td>
          <td class="value">
            <g:textField id="stack" name="stack" value="${params.stack}"/>
          </td>
        </tr>
        <tr class="prop advanced">
          <td class="name">
            <label for="zone">Zone:</label>
          </td>
          <td class="value">
            <g:select name="zone" noSelection="['':'Default (all zones)']" value="${params.zone}" from="${zoneNames}"
                      class="allowEmptySelect" />
          </td>
        </tr>
        <tr class="prop">
          <td valign="top" class="name">
            <label for="fastPropertyRegion">Region:</label>
          </td>
          <td>
            <g:select title="The AWS region that this property is used in, or blank for all regions"
                      name="fastPropertyRegion" noSelection="['':'Default (all regions)']"
                      value="${params.fastPropertyRegion}" from="${regionOptions}" class="allowEmptySelect"
                      optionKey="code" optionValue="description"/>
          </td>
        </tr>
        </tbody>
      </table>
    </div>

    <div class="buttons">
      <g:buttonSubmit class="save" value="save">Create New Fast Property</g:buttonSubmit>
    </div>
  </g:form>
</div>
</body>
</html>
