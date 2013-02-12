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
<tr class="prop">
  <td class="name">Image:</td>
  <td class="value">
    <g:if test="${launchTemplate.imageId}">
      <g:linkObject type="image" name="${launchTemplate.imageId}"/>${image ? ' | ' + image.architecture + ' | ' + image.imageLocation : ''}
    </g:if>
  </td>
</tr>
<tr class="prop">
  <td class="name">SSH Key:</td>
  <td class="value">${launchTemplate.keyName}</td>
</tr>
<tr class="prop">
  <td class="name">Security Groups:</td>
  <td class="value">[<g:each var="g" in="${launchTemplate.securityGroups}" status="j"><g:if test="${j>0}">, </g:if><g:linkObject type="security" name="${g}"/></g:each>]</td>
</tr>
%{-- Some Amazon APIs expose the user data, others hide it. Don't show blank user data if doing so might be confusing. --}%
<g:if test="${launchTemplate.userData}">
  <tr class="prop">
    <td class="name">User Data:</td>
    <td class="value">
      <g:textArea class="resizable" name="userData" value="${launchTemplate.userData}" rows="2" cols="100" readonly="true"/>
    </td>
  </tr>
</g:if>
<tr class="prop">
  <td class="name"><g:link controller="instanceType" action="list">Instance Type:</g:link></td>
  <td class="value">${launchTemplate.instanceType}</td>
</tr>
<g:if test="${launchTemplate.kernelId}">
  <tr class="prop">
    <td class="name">Kernel ID:</td>
    <td class="value">${launchTemplate.kernelId}</td>
  </tr>
</g:if>
<g:if test="${launchTemplate.ramdiskId}">
  <tr class="prop">
    <td class="name">Ramdisk ID:</td>
    <td class="value">${launchTemplate.ramdiskId}</td>
  </tr>
</g:if>
<g:if test="${launchTemplate.blockDeviceMappings}">
  <tr class="prop">
    <td class="name">Block Device Mappings:</td>
    <td class="value">
      <g:each var="blockDeviceMapping" in="${launchTemplate.blockDeviceMappings?.sort { it.deviceName }}">
        <div>${blockDeviceMapping}</div>
      </g:each>
    </td>
  </tr>
</g:if>
<g:if test="${launchTemplate.iamInstanceProfile}">
  <tr class="prop">
    <td class="name">IAM Instance Profile:</td>
    <td class="value">${launchTemplate.iamInstanceProfile}</td>
  </tr>
</g:if>
