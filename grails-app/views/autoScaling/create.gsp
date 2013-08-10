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
  <title>Create New Auto Scaling Group</title>
</head>
<body>
  <div class="body">
    <h1>Create New Auto Scaling Group</h1>
    <g:if test="${flash.message}">
      <div class="message">${flash.message}</div>
    </g:if>
    <g:hasErrors bean="${cmd}">
      <div class="errors">
        <g:renderErrors bean="${cmd}" as="list"/>
      </div>
    </g:hasErrors>
    <g:form action="save" method="post" class="validate">
      <g:hiddenField name="requestedFromGui" value="true" />
      <g:hiddenField name="appWithClusterOptLevel" value="false" />
      <g:render template="/common/compoundName" />
      <div>
        <table>
          <tbody>
            <tr>
              <td colspan="2">
                <h2>Auto Scaling</h2>
              </td>
            </tr>
            <g:render template="autoScalingOptions" model="[manualStaticSizingNeeded: true]" />
            <g:render template="/loadBalancer/selection"/>
          </tbody>
          <g:render template="/launchConfiguration/launchConfigOptions" />
          <tbody class="clusterChaosMonkeyOptions ${params.appName in appsWithClusterOptLevel ? '' : 'concealed'}">
            <g:render template="/common/chaosMonkeyOptions" />
          <tbody>
        </table>
        <ul id="appsWithClusterOptLevel" class="concealed">
          <g:each var="app" in="${appsWithClusterOptLevel}">
            <li>${app}</li>
          </g:each>
        </ul>
      </div>
      <div class="buttons">
        <g:buttonSubmit class="save" action="save">Create New Auto Scaling Group</g:buttonSubmit>
      </div>
    </g:form>
  </div>
</body>
</html>
