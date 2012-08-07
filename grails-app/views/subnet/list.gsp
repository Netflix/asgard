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
  <title>Subnets</title>
</head>

<body>
<div class="body">
  <h1>Subnets in ${region.description}</h1>
  <g:if test="${flash.message}">
    <div class="message">${flash.message}</div>
  </g:if>
  <g:form method="post">
    <div class="list">
      <table class="sortable">
        <thead>
        <tr>
          <th>Subnet ID</th>
          <th>VPC ID</th>
          <th>State</th>
          <th>Availability Zone</th>
          <th>Available</th>
          <th>CIDR</th>
          <th>Purpose</th>
          <th>Target</th>
        </tr>
        </thead>
        <tbody>
        <g:each var="subnet" in="${subnets}" status="i">
          <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
            <td>${subnet.subnetId}</td>
            <td>${subnet.vpcId}</td>
            <td>${subnet.state}</td>
            <td><g:availabilityZone value="${subnet.availabilityZone}"/></td>
            <td>${subnet.availableIpAddressCount}</td>
            <td>${subnet.cidrBlock}</td>
            <td>${subnet.purpose}</td>
            <td>${subnet.target}</td>
          </tr>
        </g:each>
        </tbody>
      </table>
    </div>
  </g:form>
</div>
</body>
</html>
