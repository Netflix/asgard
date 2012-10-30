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
  <title>Push AMI to Existing Auto Scaling Group</title>
</head>
<body>
  <div class="body">
    <h1>Push AMI into Auto Scaling Group</h1>
    <g:if test="${flash.message}">
      <div class="message">${flash.message}</div>
    </g:if>
    <g:form method="post" class="validate">
      <input type="hidden" id="name" name="name" value="${name}"/>
      <input type="hidden" id="appName" name="appName" value="${appName}"/>
      <div class="dialog">
        <table>
          <tbody>
          <tr class="prop">
            <td class="name">Application:</td>
            <td><g:linkObject type="application" name="${appName}"/></td>
          </tr>
          <tr class="prop">
            <td class="name">Cluster:</td>
            <td><g:linkObject type="cluster" name="${cluster}"/></td>
          </tr>
          <tr class="prop">
            <td class="name">Auto Scaling Group:</td>
            <td><g:linkObject type="autoScaling" name="${name}"/></td>
          </tr>
          <g:each in="${variables}" var="entry">
            <tr class="prop">
              <td class="name">${entry.key}:</td>
              <td class="value">${entry.value}</td>
            </tr>
          </g:each>
          <g:render template="/launchConfiguration/launchConfigOptions" />
          </tbody>
          <tbody class="numbers">
          <tr class="prop">
            <td colspan="100%"><h2 title="Options that control the process of terminating current instances">Termination Options</h2></td>
          </tr>
          <tr class="prop" title="How many total instances to relaunch">
            <td class="name"><label for="relaunchCount">Total relaunches:</label></td>
            <td class="value"><input type="text" id="relaunchCount" name="relaunchCount" value="${relaunchCount}"/></td>
          </tr>
          <tr class="prop" title="How many should be taken down at the same time">
            <td class="name"><label for="concurrentRelaunches">Concurrent relaunches:</label></td>
            <td class="value"><input type="text" id="concurrentRelaunches" name="concurrentRelaunches" value="${concurrentRelaunches}"/></td>
          </tr>
          <tr class="prop" title="Choose whether to terminate oldest or newest instances first">
            <td class="name"><label id="terminateOrderLabel">Order:</label></td>
            <td class="value"><g:radio id="oldestFirst" name="newestFirst" value="false" checked="checked"/> <label for="oldestFirst" class="choice">Oldest First</label>
              <g:radio id="newestFirst" name="newestFirst" value="true"/> <label for="newestFirst" class="choice">Newest First</label>
            </td>
          </tr>
          <g:if test="${checkHealth}">
            <tr class="prop" title="Check to SKIP waiting for clients to stop using the instance before termination">
              <td class="name"><label for="rudeShutdown">Rude Shutdown:</label></td>
              <td class="value"><g:checkBox id="rudeShutdown" name="rudeShutdown" value="${rudeShutdown}" checked="${rudeShutdown}"/>
                <label for="rudeShutdown">Terminate without waiting for clients to stop using instance</label></td>
            </tr>
          </g:if>
          <g:render template="startupOptions" />
          </tbody>
        </table>
      </div>
      <div class="buttons">
        <g:buttonSubmit class="push" value="Begin Rolling Push" action="startRolling"/>
      </div>
    </g:form>
  </div>
</body>
</html>
