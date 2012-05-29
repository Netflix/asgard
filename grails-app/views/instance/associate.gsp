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
  <title>Associate Elastic IP with Instance</title>
</head>
<body>
  <div class="body">
    <h1>Associate Elastic IP with Instance</h1>
    <g:if test="${flash.message}">
      <div class="message">${flash.message}</div>
    </g:if>
    <g:hasErrors bean="${instance}">
      <div class="errors">
        <g:renderErrors bean="${instance}" as="list"/>
      </div>
    </g:hasErrors>
    <g:form method="post">
      <input type="hidden" id="instanceId" name="instanceId" value="${instance.instanceId}"/>
      <div class="dialog">
        <g:if test="${eipUsageMessage}">
          <h2>${eipUsageMessage}</h2>
        </g:if>
        <table>
          <tbody>
          <tr class="prop">
            <td class="name">Instance ID:</td>
            <td class="value">${instance.instanceId}</td>
          </tr>
          <tr class="prop">
            <td class="name">
              <label for="publicIp">Public Elastic IP:</label>
            </td>
            <td class="value">
              <select id="publicIp" name="publicIp">
                <g:each var="eip" in="${publicIps}">
                  <option ${instance.publicIpAddress == eip.key ? 'selected="selected"' : ''} value="${eip.key}">${eip.key} : ${eip.value}</option>
                </g:each>
              </select>
            </td>
          </tr>
          </tbody>
        </table>
      </div>
      <div class="buttons">
        <g:buttonSubmit class="save" value="Associate Elastic IP" action="associateDo"/>
      </div>
    </g:form>
  </div>
</body>
</html>
