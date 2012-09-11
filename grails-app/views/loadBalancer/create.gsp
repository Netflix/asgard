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
<%@ page import="com.netflix.asgard.Requests" %>
<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  <meta name="layout" content="main"/>
  <title>Create New Load Balancer</title>
</head>
<body>
  <div class="body">
    <h1>Create New Load Balancer</h1>
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
          <tbody valign="top">
            <tr>
              <td colspan="2">
                <h2>Name</h2>
              </td>
            </tr>
            <tr class="prop">
              <td class="name">
                <label for="appName">Application:</label>
              </td>
              <td>
                <g:select title="The application that will run on the load balanced instances" name="appName"
                        noSelection="['':'']" value="${params.appName}" from="${applications}"
                        optionKey="name" optionValue="name" data-placeholder="-Choose application-"/>
              </td>
            </tr>
            <tr class="prop">
              <td class="name">
                <label for="stack">Stack (optional):</label>
              </td>
              <td>
                <div>
                  <g:select title="The name of the stack of applications being logically deployed and managed together" name="stack"
                      noSelection="['':'']" value="${params.appName}" from="${stacks}"
                      optionKey="name" optionValue="name" class="clearableSelect" data-placeholder="-Choose stack-"/>
                </div>
                <div>
                  <g:textField name="newStack" placeholder="New Stack" value="${params.newStack}"/>
                </div>
              </td>
            </tr>
            <tr class="prop">
              <td class="name">
                <label for="detail">Detail (optional):</label>
              </td>
              <td>
                <input type="text" id="detail" name="detail" value="${params.detail ?: "frontend"}"/>
              </td>
            </tr>
            <tr class="divider">
              <td colspan="100%"></td>
            </tr>
          </tbody>
          <tbody valign="top" class="numbers">
            <tr>
              <td colspan="2">
                <h2>Configuration</h2>
              </td>
            </tr>
            <g:render template="/common/vpcSelection" model="[awsAction: 'Create', awsObject: 'ELB']"/>
            <g:render template="/common/zoneSelection" />
            <g:render template="/common/securityGroupSelection" />
            <tr class="prop">
              <td class="name">
                  <label>Listeners:</label>
              </td>
              <td>
                <g:render template="listenerOptions" model="${[index: 1, protocol: params.protocol1 ?: 'HTTP', lbPort: params.lbPort1 ?: '80', instancePort: params.instancePort1 ?: '7001']}" />
              </td>
            </tr>
            <tr class="prop">
              <td class="name">
              </td>
              <td>
                <g:render template="listenerOptions" model="${[index: 2, protocol: params.protocol2, lbPort: params.lbPort2, instancePort: params.instancePort2]}" />
              </td>
            </tr>
            <tr class="prop">
              <td class="name">
                <label>Health Check</label>
              </td>
              <td>
                <label for="target">Target:</label><input type="text" id="target" name="target" value="${params.target ?: "HTTP:7001/healthcheck"}" class="string">
                <label for="interval">Interval:</label><input type="text" id="interval" name="interval" value="${params.interval ?: "10"}">
                <label for="timeout">Timeout:</label><input type="text" id="timeout" name="timeout" value="${params.timeout ?: "5"}">
                <label for="unhealthy">Unhealthy Threshold:</label><input type="text" id="unhealthy" name="unhealthy" value="${params.unhealthy ?: "2"}">
                <label for="healthy">Healthy Threshold:</label><input type="text" id="healthy" name="healthy" value="${params.healthy ?: "10"}">
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div class="buttons">
        <g:buttonSubmit class="save" action="save">Create New Load Balancer</g:buttonSubmit>
      </div>
    </g:form>
  </div>
</body>
</html>
