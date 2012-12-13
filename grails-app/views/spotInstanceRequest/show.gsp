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
  <title>${spotInstanceRequest.spotInstanceRequestId} Spot Request Instance</title>
</head>
<body>
  <div class="body">
    <h1>Spot Instance Request Details</h1>
    <g:if test="${flash.message}">
      <div class="message">${flash.message}</div>
    </g:if>
    <g:if test="${spotInstanceRequest}">
      <g:form>
        <input type="hidden" name="spotInstanceRequestId" value="${spotInstanceRequest.spotInstanceRequestId}"/>
        <div class="buttons">
          <g:buttonSubmit class="stop" action="cancel" value="Cancel Spot Instance Request"
                  data-warning="Really cancel Spot Instance Request: ${spotInstanceRequest.spotInstanceRequestId}?"/>
        </div>
      </g:form>
    </g:if>
    <div>
      <table>
        <tbody>
        <tr>
          <td class="name">Spot Instance Request ID:</td>
          <td class="value">${spotInstanceRequest.spotInstanceRequestId}</td>
        </tr>
        <tr>
          <td class="name">Instance ID:</td>
          <td class="value"><g:linkObject name="${spotInstanceRequest.instanceId}"/></td>
        </tr>
        <g:render template="/launchConfiguration/launchTemplateFields" model="[launchTemplate: spotInstanceRequest.launchSpecification]"/>
        <tr class="prop">
          <td class="name">Spot Price Max:</td>
          <td class="value">${spotInstanceRequest.spotPrice}</td>
        </tr>
        <tr class="prop">
          <td class="name">Type:</td>
          <td class="value">${spotInstanceRequest.type}</td>
        </tr>
        <tr class="prop">
          <td class="name">State:</td>
          <td class="value">${spotInstanceRequest.state}</td>
        </tr>
        <tr class="prop">
          <td class="name">Spot Instance Fault:</td>
          <td class="value">${spotInstanceRequest.fault}</td>
        </tr>
        <tr class="prop">
          <td class="name">Valid From:</td>
          <td class="value"><g:formatDate date="${spotInstanceRequest.validFrom}"/></td>
        </tr>
        <tr class="prop">
          <td class="name">Valid Until:</td>
          <td class="value"><g:formatDate date="${spotInstanceRequest.validUntil}"/></td>
        </tr>
        <tr class="prop">
          <td class="name">Launch Group:</td>
          <td class="value">${spotInstanceRequest.launchGroup}</td>
        </tr>
        <tr class="prop">
          <td class="name">Availability Zone:</td>
          <td class="value"><g:availabilityZone value="${spotInstanceRequest.launchSpecification.placement?.availabilityZone}"/></td>
        </tr>
        <tr class="prop">
          <td class="name">Create Time:</td>
          <td class="value"><g:formatDate date="${spotInstanceRequest.createTime}"/></td>
        </tr>
        <tr class="prop">
          <td class="name">Product Description:</td>
          <td class="value">${spotInstanceRequest.productDescription}</td>
        </tr>
        <g:render template="/common/showTags" model="[entity: spotInstanceRequest]"/>
        </tbody>
      </table>
    </div>
  </div>
</body>
</html>
