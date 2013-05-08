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
<g:if test="${fastPropertyInfoUrl}">
  <tr>
    <td colspan="2"><a href="${fastPropertyInfoUrl}">Fast Property Documentation</a></td>
  </tr>
</g:if>
<tr class="prop">
  <td class="name">Timestamp:</td>
  <td class="value">${fastProperty.timestamp}</td>
</tr>
<g:if test="${fastProperty.ttl}">
  <tr class="prop">
    <td class="name">Expires:</td>
    <td class="value">${fastProperty.expires}</td>
  </tr>
</g:if>
<g:if test="${fastProperty.constraints}">
  <tr class="prop">
    <td class="name">Constraints:</td>
    <td class="value">${fastProperty.constraints}</td>
  </tr>
</g:if>
<g:if test="${fastProperty.cmcTicket}">
  <tr class="prop">
    <td class="name">${ticketLabel.encodeAsHTML()}:</td>
    <td class="value">${fastProperty.cmcTicket?.encodeAsHTML()}</td>
  </tr>
</g:if>
<tr class="prop">
  <td class="name">Source:</td>
  <td class="value">${fastProperty.sourceOfUpdate?.encodeAsHTML()}</td>
</tr>
<tr class="prop">
  <td class="name">ID:</td>
  <td class="value">${fastProperty.id}</td>
</tr>
<tr>
  <td colspan="2">
    <h2>Scoping (highest priority first):</h2>
  </td>
<g:if test="${fastProperty.serverId}">
  <tr class="prop">
    <td class="name">Instance ID:</td>
    <td class="value"><g:linkObject type="instance" name="${fastProperty.serverId}">${fastProperty.serverId}</g:linkObject></td>
  </tr>
</g:if>
<g:if test="${fastProperty.asg}">
  <tr class="prop">
    <td class="name">ASG:</td>
    <td class="value"><g:linkObject type="autoScaling" name="${fastProperty.asg}">${fastProperty.asg}</g:linkObject></td>
  </tr>
</g:if>
<g:if test="${fastProperty.ami}">
  <tr class="prop">
    <td class="name">AMI:</td>
    <td class="value"><g:linkObject type="image" name="${fastProperty.ami}">${fastProperty.ami}</g:linkObject></td>
  </tr>
</g:if>
<g:if test="${fastProperty.cluster}">
  <tr class="prop">
    <td class="name">Cluster:</td>
    <td class="value"><g:linkObject type="cluster" name="${fastProperty.cluster}">${fastProperty.cluster}</g:linkObject></td>
  </tr>
</g:if>
<g:if test="${fastProperty.appId}">
  <tr class="prop">
    <td class="name">Application:</td>
    <td class="value"><g:linkObject type="application" name="${fastProperty.appId?.toLowerCase()}">${fastProperty.appId}</g:linkObject></td>
  </tr>
</g:if>
<tr class="prop">
  <td class="name">Env:</td>
  <td class="value">${fastProperty.env}</td>
</tr>
<g:if test="${fastProperty.countries}">
  <tr class="prop">
    <td class="name">Countries:</td>
    <td class="value">${fastProperty.countries?.encodeAsHTML()}</td>
  </tr>
</g:if>
<g:if test="${fastProperty.stack}">
  <tr class="prop">
    <td class="name">Stack:</td>
    <td class="value"><g:linkObject type="stack" name="${fastProperty.stack?.encodeAsHTML()}">${fastProperty.stack?.encodeAsHTML()}</g:linkObject></td>
  </tr>
</g:if>
<g:if test="${fastProperty.zone}">
  <tr class="prop">
    <td class="name">Zone:</td>
    <td class="value"><g:availabilityZone value="${fastProperty.zone}"/></td>
  </tr>
</g:if>
<g:if test="${fastProperty.region}">
  <tr class="prop">
    <td class="name">Region:</td>
    <td class="value">${fastProperty.region}</td>
  </tr>
</g:if>
