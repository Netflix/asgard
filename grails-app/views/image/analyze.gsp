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
  <div class="list">
    <table class="narrow">
      <caption>${deregisteredAmisToInstanceAsgs.size()} deregistered AMI ids with running instances</caption>
      <thead>
      <tr>
        <th>AMI ID</th>
        <th>Instances</th>
      </tr>
      </thead>
      <tbody>
      <g:each var="imageIdToInstanceAsg" in="${deregisteredAmisToInstanceAsgs.entrySet()}" status="i">
        <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
          <td>${imageIdToInstanceAsg.key}</td>
          <td>
            <g:each in="${imageIdToInstanceAsg.value}" var="instanceAsg">
              <g:linkObject name="${instanceAsg.instance.instanceId}"/>
              ${instanceAsg.instance.ec2Instance.owner} ${instanceAsg.instance.ec2Instance.app}
              <g:linkObject type="autoScaling" name="${instanceAsg.groupName}"/>
              <br/>
            </g:each>
          </td>
        </tr>
      </g:each>
      </tbody>
    </table>
    <g:render template="amis" model="['caption': 'in use with no creation time', 'images': dateless]"/>
    <g:render template="amis" model="['caption': 'AMIs used by running instances but with no discernible Base AMI', 'images': baselessInUse]"/>
    <table class="sortable narrow">
      <caption>${appVersionsToImageLists.size()} appversion values in use, with in-use AMI counts</caption>
      <thead>
      <tr>
        <th>App Version</th>
        <th>AMI Count</th>
        <th>AMIs</th>
      </tr>
      </thead>
      <tbody>
      <g:each var="appVersionToImageList" in="${appVersionsToImageLists.entrySet()}" status="i">
        <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
          <td>${appVersionToImageList.key}</td>
          <td>${appVersionToImageList.value.size()}</td>
          <td>
            <g:each in="${appVersionToImageList.value}" var="image">
              <g:linkObject type="image" name="${image.imageId}"/>
            </g:each>
          </td>
        </tr>
      </g:each>
      </tbody>
    </table>
    <g:render template="amis" model="['caption': 'AMIs used by running instances, sorted by Base AMI Date', 'images': inUseImages]"/>
    </div>
</div>
</body>
</html>
