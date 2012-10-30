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
<%@ page import="com.netflix.asgard.model.InstancePriceType; com.netflix.asgard.Requests" %>

<tbody valign="top">
  <tr class="advanced">
    <td colspan="2">
      <h2>Launch ${templateType ?: 'Configuration'}</h2>
    </td>
  </tr>
  <tr class="prop">
    <td class="name">
      <label for="imageId">AMI Image ID:</label>
    </td>
    <td>
      <select id="imageId" name="imageId">
        <g:each var="im" in="${images}">
          <option value="${im.imageId}" ${params.imageId == im.imageId || im.imageId == image ? "selected" : ""}>${im.imageLocation} | ${im.imageId}</option>
        </g:each>
      </select>
      <br/>
      <g:if test="${imageListIsShort}">
        <g:link action="${actionName}" params="[id: name, allImages: true]" class="more">Show more AMIs</g:link>
      </g:if>
    </td>
  </tr>
  <tr class="prop">
    <td class="name">
      <label for="instanceType"><g:link controller="instanceType" action="list">Instance Type:</g:link></label>
    </td>
    <td>
      <select id="instanceType" name="instanceType">
        <g:each var="t" in="${instanceTypes}">
          <option value="${t.name}" ${t.name == params.instanceType || t.name == instanceType ? 'selected' : ''}>${t.name} ${t.monthlyLinuxOnDemandPrice ? t.monthlyLinuxOnDemandPrice + '/mo' : ''}</option>
        </g:each>
      </select>
    </td>
  </tr>
  <tr class="prop advanced">
    <td class="name">
      <label for="keyName">SSH Key:</label>
    </td>
    <td>
      <select id="keyName" name="keyName">
        <g:each var="k" in="${keys}">
          <g:if test="${k.keyName == params.keyName || (!params.keyName && k.keyName == defKey)}">
            <option selected value="${k.keyName}">${k.keyName}</option>
          </g:if>
          <g:else>
            <option value="${k.keyName}">${k.keyName}</option>
          </g:else>
        </g:each>
      </select>
    </td>
  </tr>
  <g:render template="/common/securityGroupSelection" />
  <tr class="prop advanced">
    <td class="name">
      <label>Pricing:</label>
    </td>
    <td>
      <div>
        <g:radio name="pricing" id="onDemand" value="${InstancePriceType.ON_DEMAND.name()}" checked="${!pricing || pricing == InstancePriceType.ON_DEMAND.name()}"/>
        <label for="onDemand" class="choice">On-Demand</label>
      </div>
      <div>
        <g:radio name="pricing" id="spot" value="${InstancePriceType.SPOT.name()}" checked="${pricing == InstancePriceType.SPOT.name()}"/>
        <label for="spot" class="choice">Spot</label>
      </div>
    </td>
  </tr>
  <tr class="prop advanced">
    <td class="name">
      <label for="kernelId">Kernel ID:</label>
    </td>
    <td>
      <input type="text" id="kernelId" name="kernelId" value="${params.kernelId}"/>
    </td>
  </tr>
  <tr class="prop advanced">
    <td class="name">
      <label for="ramdiskId">Ram Disk ID:</label>
    </td>
    <td>
      <input type="text" id="ramdiskId" name="ramdiskId" value="${params.ramdiskId}"/>
    </td>
  </tr>
  <tr class="prop advanced">
    <td class="name">
      <label for="iamInstanceProfile">IAM Instance Profile:</label>
    </td>
    <td>
      <input type="text" id="iamInstanceProfile" name="iamInstanceProfile" value="${params.iamInstanceProfile ?: iamInstanceProfile}"/>
    </td>
  </tr>
</tbody>
