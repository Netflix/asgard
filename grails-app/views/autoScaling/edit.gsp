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
<%@ page import="com.netflix.asgard.model.AutoScalingProcessType;" %>
<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  <meta name="layout" content="main"/>
  <title>Edit Auto Scaling Group</title>
</head>
<body>
  <div class="body">
    <h1>Edit Auto Scaling Group</h1>
    <g:if test="${flash.message}">
      <div class="message">${flash.message}</div>
    </g:if>
    <g:hasErrors bean="${group}">
      <div class="errors">
        <g:renderErrors bean="${group}" as="list"/>
      </div>
    </g:hasErrors>
    <g:form method="post">
      <input type="hidden" id="name" name="name" value="${group.autoScalingGroupName}"/>
      <div class="dialog">
        <table>
          <tbody>
          <tr class="prop" title="Changing the name requires a delete and re-create. This will fail if there are instances running.">
            <td class="name">Name:</td>
            <td class="value"><g:linkObject type="autoScaling" name="${group.autoScalingGroupName}"/></td>
          </tr>
          <tr class="prop">
            <td class="name">
              <label for="launchConfiguration">Launch<br/>Configuration:</label>
            </td>
            <td class="value">
              <select id="launchConfiguration" name="launchConfiguration" data-defaultfilter="${group.autoScalingGroupName}">
                <g:each var="launchConfigName" in="${launchConfigurations}">
                  <g:if test="${group.launchConfigurationName == launchConfigName}">
                    <option selected value="${launchConfigName}">${launchConfigName}</option>
                  </g:if>
                  <g:else><option value="${launchConfigName}">${launchConfigName}</option></g:else>
                </g:each>
              </select>
            </td>
          </tr>
          <g:render template="autoScalingOptions"/>
          <tr class="prop advanced">
            <td class="name">
              Launching<br/>instances:
            </td>
            <td>
              <input type="radio" name="launch" value="enabled" id="launchingEnabled" ${launchSuspended ?  '' : 'checked="checked"'}>
              <label for="launchingEnabled" class="choice">Allow Group to Launch Instances</label><br/>
              <input type="radio" name="launch" value="disabled" id="launchingDisabled" ${launchSuspended ? 'checked="checked"' : ''}>
              <label for="launchingDisabled" class="choice">Disable All Instance Launches</label>
            </td>
          </tr>
          <tr class="prop advanced">
            <td class="name">
              Terminating<br/>instances:
            </td>
            <td>
              <input type="radio" name="terminate" value="enabled" id="terminatingEnabled" ${terminateSuspended ?  '' : 'checked="checked"'}>
              <label for="terminatingEnabled" class="choice">Allow Group to Terminate Instances</label><br/>
              <input type="radio" name="terminate" value="disabled" id="terminatingDisabled" ${terminateSuspended ? 'checked="checked"' : ''}>
              <label for="terminatingDisabled" class="choice">Disable All Instance Terminations</label>
            </td>
          </tr>
          <tr class="prop advanced">
            <td class="name">
              Adding<br/>instances<br/>to ELBs:
            </td>
            <td>
              <input type="radio" name="addToLoadBalancer" value="enabled" id="addToLoadBalancerEnabled" ${addToLoadBalancerSuspended ?  '' : 'checked="checked"'}>
              <label for="addToLoadBalancerEnabled" class="choice">Allow Group to Add Instances to ELB</label><br/>
              <input type="radio" name="addToLoadBalancer" value="disabled" id="addToLoadBalancerDisabled" ${addToLoadBalancerSuspended ? 'checked="checked"' : ''}>
              <label for="addToLoadBalancerDisabled" class="choice">Disable Instance Additions to ELB (disables all instances in Discovery)</label>
            </td>
          </tr>
          </tbody>
        </table>
      </div>
      <div class="buttons">
        <g:buttonSubmit class="save" value="Update Auto Scaling Group" action="update"/>
      </div>
    </g:form>
  </div>
</body>
</html>
