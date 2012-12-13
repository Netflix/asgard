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
  <title>${volume.volumeId} Volume</title>
</head>
<body>
  <div class="body">
    <h1>Volume Details</h1>
    <g:if test="${flash.message}">
      <div class="message">${flash.message}</div>
    </g:if>
    <g:if test="${volume}">
      <g:form controller="volume">
        <input type="hidden" name="volumeId" value="${volume.volumeId}"/>
        <div class="buttons">
          <g:buttonSubmit class="delete" action="delete" value="Delete Volume"
                          data-warning="Really delete Volume '${volume.volumeId}'?" />
        </div>
      </g:form>
      <g:form controller="snapshot">
        <input type="hidden" name="volumeId" value="${volume.volumeId}"/>
        <div class="buttons">
          <g:buttonSubmit class="takeSnapshot requireLogin" action="create" value="Take Snapshot"/>
          <g:textField name="description" size="50" value="--ENTER SNAPSHOT DESCRIPTION HERE--" class="requireLogin"/>
        </div>
      </g:form>
    </g:if>
    <div class="dialog">
      <table>
        <tbody>
        <tr class="prop">
          <td class="name" title="Volume ID">Volume ID:</td>
          <td class="value">${volume.volumeId}</td>
        </tr>
        <tr class="prop">
          <td class="name" title="Availability Zone">Zone:</td>
          <td class="value">${volume.availabilityZone}</td>
        </tr>
        <tr class="prop">
          <td class="name" title="Size">Size:</td>
          <td class="value">${volume.size} GB</td>
        </tr>
        <tr class="prop">
          <td class="name" title="Status">Status:</td>
          <td class="value">${volume.state}</td>
        </tr>
        <tr class="prop">
          <td class="name" title="Snapshot ID (if created from snapshot)">Snapshot ID:</td>
          <td class="value"><g:linkObject type="snapshot" name="${volume.snapshotId}"/></td>
        </tr>
        <tr class="prop">
          <td class="name" title="Creation Time">Created:</td>
          <td class="value"><g:formatDate date="${volume.createTime}"/></td>
        </tr>
        <g:render template="/common/showTags" model="[entity: volume]"/>
        <tr class="prop">
          <td class="name" title="Attachments">Attachments:</td>
          <td class="value">
            <table>
              <g:each var="va" in="${volume.attachments.sort{it.instanceId}}">
                <tr class="prop">
                  <td class="name" title="Instance">Instance:</td>
                  <td class="value">${va.instanceId}</td>
                </tr>
                <tr class="prop">
                  <td class="name" title="Device">Device:</td>
                  <td class="value">${va.device}</td>
                </tr>
                <tr class="prop">
                  <td class="name" title="Status">Status:</td>
                  <td class="value">${va.state}</td>
                </tr>
                <tr class="prop">
                  <td class="name" title="Attach Time">Attached:</td>
                  <td class="value"><g:formatDate date="${va.attachTime}"/></td>
                </tr>
                <tr class="prop">
                  <td class="name" title="Delete on Termination?">Delete on Terminate:</td>
                  <td class="value">${va.deleteOnTermination}</td>
                </tr>
                <tr class="prop">
                  <td class="name" title="Detach">
                    <div class="buttons">
                      <g:form>
                        <input type="hidden" name="volumeId" value="${volume.volumeId}"/>
                        <input type="hidden" name="instanceId" value="${va.instanceId}"/>
                        <input type="hidden" name="device" value="${va.device}"/>
                        <g:buttonSubmit class="delete" onclick="return confirm('Really Detach ${volume.volumeId} from ${va.instanceId} ?');"
                          action="detach" value="Detach Volume"/>
                      </g:form>
                    </div>
                  </td>
                </tr>
              </g:each>
            </table>
          </td>
        </tr>
        </tbody>
      </table>
    </div>
  </div>
</body>
</html>
