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
<%@ page import="com.netflix.asgard.model.AutoScalingGroupHealthCheckType" %>
<tr class="prop">
  <td class="name">
    Instance<br/>Bounds:
  </td>
  <td class="value numbers">
    <label for="min">Min:</label> <input type="text" class="number" id="min" name="min" value="${group?.minSize}"/>
    <label for="max">Max:</label> <input type="text" class="number" id="max" name="max" value="${group?.maxSize}"/>
  </td>
</tr>
<tr class="prop">
  <td class="name">
    Desired<br/>Capacity:
  </td>
  <td class="numbers">
    <div class="desiredCapacityContainer ${manualStaticSizingNeeded ? 'showManual' : ''}">
      <div class="manualDesiredCapacity">
        <input type="text" class="number" id="desiredCapacity" name="desiredCapacity" value="${group?.desiredCapacity}" /> instances
      </div>
      <div class="dynamicDesiredCapacity">
        ${group?.desiredCapacity} instance${group?.desiredCapacity == 1 ? '' : 's'}
        <g:tip tipStyle="interactive">
          Controlled automatically.<br/>
          <span class="fakeLink enableManualDesiredCapacityOverride">Click here</span> to change<br/>
          desired capacity manually.<br/>
          (Not recommended)
        </g:tip>
      </div>
    </div>
  </td>
</tr>
<tr class="prop advanced" title="The number of seconds after a scaling activity completes before any further scaling activities can start">
  <td class="name">
    <label for="defaultCooldown">Cooldown:</label>
  </td>
  <td class="value">
    <input type="text" class="number" id="defaultCooldown" name="defaultCooldown" value="${group?.defaultCooldown == null ? '10' : group?.defaultCooldown}"/> seconds
  </td>
</tr>
<tr class="prop advanced" title="The method that the group will use to decide when to replace a problematic instance">
  <td class="name">
    <label for="healthCheckType">ASG Health<br/>Check Type:</label>
  </td>
  <td class="value">
    <select id="healthCheckType" name="healthCheckType">
      <g:each in="${AutoScalingGroupHealthCheckType.values()}" var="type">
        <option ${group?.healthCheckType?.toString() == type.name() ? 'selected' : ''} value="${type.name()}">${type.name()} (${type.description})</option>
      </g:each>
    </select>
  </td>
</tr>
<tr class="prop advanced" title="The number of seconds to wait after instance launch before running the health check">
  <td class="name">
    <label for="healthCheckGracePeriod">ASG Health<br/>Check Grace<br/>Period:</label>
  </td>
  <td class="value">
    <input type="text" class="number" id="healthCheckGracePeriod" name="healthCheckGracePeriod" value="${group?.healthCheckGracePeriod == null ? '600' : group?.healthCheckGracePeriod}"/> seconds
  </td>
</tr>
<tr class="prop advanced" title="The algorithm to use when selecting which instance to terminate">
  <td class="name">
    <label for="terminationPolicy">Termination<br/>Policy:</label>
  </td>
  <td>
    <select id="terminationPolicy" name="terminationPolicy">
      <g:each in="${allTerminationPolicies}" var="policy">
        <option ${terminationPolicy == policy ? 'selected' : ''} value="${policy}">${policy}</option>
      </g:each>
    </select>
  </td>
</tr>
<g:if test="${!subnetPurpose && vpcZoneIdentifier}">
  <td class="name">VPC:</td>
  <td class="warning">The subnet is misconfigured without a purpose.</td>
</g:if>
<g:else>
  <g:render template="/common/vpcSelection" model="[awsAction: 'Launch', awsObject: 'instances']"/>
  <g:render template="/common/zoneSelection" />
</g:else>
<tr class="prop advanced">
  <td class="name">
    AZ Rebalancing:
  </td>
  <td>
    <input type="radio" name="azRebalance" value="enabled" id="azRebalanceEnabled" ${group?.zoneRebalancingSuspended ?  '' : 'checked="checked"'}>
    <label for="azRebalanceEnabled" class="choice">Keep Zones Balanced</label><br/>
    <input type="radio" name="azRebalance" value="disabled" id="azRebalanceDisabled" ${group?.zoneRebalancingSuspended ? 'checked="checked"' : ''}>
    <label for="azRebalanceDisabled" class="choice">Don't Rebalance Zones</label>
  </td>
</tr>
