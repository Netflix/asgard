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
  <title>Images</title>
</head>
<body>
<div class="body">
  <h1>Images in ${region.description}${packageNames ? ' for ' + packageNames : ''}</h1>
  <g:if test="${flash.message}">
    <div class="message">${flash.message}</div>
  </g:if>
  <g:form method="post">
    <div class="list">
      <div class="buttons"></div>
      <table class="sortable">
        <thead>
        <tr>
          <th>ID</th>
          <th>Name</th>
          <th>Description</th>
          <th>State</th>
          <th>Owner</th>
          <th>Creator</th>
          <th>Creation Time</th>
          <th>Last Used</th>
          <th>Package Version</th>
          <th>Base AMI ID</th>
          <th>Base AMI Date</th>
        </tr>
        </thead>
        <tbody>
        <g:each var="image" in="${images}" status="i">
          <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
            <td><g:linkObject type="image" name="${image.imageId}"/></td>
            <td class="ami">${image.name}</td>
            <td class="ami">${image.description}</td>
            <td>${image.state}</td>
            <td>${accounts[image.ownerId] ?: image.ownerId}</td>
            <td>${image.creator}</td>
            <td>${image.creationTime}</td>
            <td>${image.lastReferencedTime}</td>
            <td class="ami">${image.appVersion}</td>
            <td><g:linkObject type="image" name="${image.baseAmiId}"/></td>
            <td><g:formatDate date="${image.baseAmiDate?.toDate()}" format="yyyy-MM-dd"/></td>
          </tr>
        </g:each>
        </tbody>
      </table>
    </div>
    <div class="paginateButtons">
    </div>
  </g:form>
</div>
</body>
</html>
