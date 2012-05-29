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
<table class="sortable">
  <caption>${images.size()} ${caption}</caption>
  <thead>
  <tr>
    <th>ID</th>
    <th>Instances</th>
    <th>Base AMI Date</th>
    <th>Base AMI ID</th>
    <th>Base AMI Name</th>
    <th>Name</th>
    <th>Location</th>
    <th>Architecture</th>
    <th>Creator</th>
    <th>Creation Time</th>
    <th>Package Version</th>
    <th>Description</th>
  </tr>
  </thead>
  <tbody>
  <g:each var="image" in="${images}" status="i">
    <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
      <td><g:linkObject name="${image.imageId}" /></td>
      <td>
        <g:each var="mergedInstance" in="${imageIdsToInstanceLists[image.imageId]}">
          <g:linkObject name="${mergedInstance.instanceId}"/>
          <g:linkObject type="autoScaling" name="${mergedInstance.autoScalingGroupName}"/>
          <br/>
        </g:each>
      </td>
      <td><g:formatDate date="${image.baseAmiDate?.toDate()}"/></td>
      <td><g:linkObject name="${image.baseAmiId}" /></td>
      <td>${image.baseAmiName}</td>
      <td class="ami">${image.name}</td>
      <td class="ami">${image.imageLocation}</td>
      <td>${image.architecture}</td>
      <td>${image.creator}</td>
      <td>${image.creationTime}</td>
      <td class="ami">${image.appVersion}</td>
      <td class="ami">${image.description}</td>
    </tr>
  </g:each>
  </tbody>
</table>
