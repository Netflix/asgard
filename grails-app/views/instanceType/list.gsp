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
  <title>Instance Types</title>
</head>
<body>
  <div class="body">
    <h1>Instance types and hourly prices in ${region.description}</h1>
    <g:if test="${flash.message}">
      <div class="message">${flash.message}</div>
    </g:if>
    <div class="list">
      <div class="buttons"></div>
      <table class="sortable instanceType">
        <tr>
          <th>Name</th>
          <th>Description</th>
          <th class="sorttable_nosort">Mem</th>
          <th class="sorttable_nosort">Storage</th>
          <th class="sorttable_nosort">CPU</th>
          <th>Arch</th>
          <th>IO Perf</th>
          <th>Linux<br/>On<br/>Dem</th>
          <th>Linux<br/>Res</th>
          <th>Linux<br/>Spot</th>
          <th>Win<br/>On<br/>Dem</th>
          <th>Win<br/>Res</th>
          <th>Win<br/>Spot</th>
        </tr>
        <g:each in="${instanceTypes}" var="instanceType">
          <tr>
            <td>${instanceType.name}</td>
            <td class="description">${instanceType.hardwareProfile?.description}</td>
            <td>${instanceType.hardwareProfile?.memory}</td>
            <td>${instanceType.hardwareProfile?.storage}</td>
            <td class="cpu">${instanceType.hardwareProfile?.cpuSummary}<br/>${instanceType.hardwareProfile?.cpuDetail}</td>
            <td class="architecture">${instanceType.hardwareProfile?.architecture}</td>
            <td class="ioPerformance">${instanceType.hardwareProfile?.ioPerformance}</td>
            <td>${instanceType.linuxOnDemandPrice ? '$' + instanceType.linuxOnDemandPrice : ''}</td>
            <td>${instanceType.linuxReservedPrice ? '$' + instanceType.linuxReservedPrice : ''}</td>
            <td>${instanceType.linuxSpotPrice ? '$' + instanceType.linuxSpotPrice : ''}</td>
            <td>${instanceType.windowsOnDemandPrice ? '$' + instanceType.windowsOnDemandPrice : ''}</td>
            <td>${instanceType.windowsReservedPrice ? '$' + instanceType.windowsReservedPrice : ''}</td>
            <td>${instanceType.windowsSpotPrice ? '$' + instanceType.windowsSpotPrice : ''}</td>
          </tr>
        </g:each>
      </table>
    </div>
  </div>
</body>
</html>
