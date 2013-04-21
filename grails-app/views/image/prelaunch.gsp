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
  <title>Launch AMI Instance</title>
</head>
<body>
  <div class="body">
    <h1>Launch AMI Instance</h1>
    <g:if test="${flash.message}">
      <div class="message">${flash.message}</div>
    </g:if>
    <g:hasErrors bean="${description}">
      <div class="errors">
        <g:renderErrors bean="${description}" as="list"/>
      </div>
    </g:hasErrors>
    <g:form method="post" class="validate">
      <input type="hidden" id="imageId" name="imageId" value="${imageId}"/>
      <g:each var="n" in="${names}">
        <input type="hidden" id="names" name="names" value="${n}"/>
      </g:each>
      <div class="dialog">
        <table>
          <tbody>
          <tr class="prop">
            <td class="name">Image ID:</td>
            <td>${imageId}</td>
          </tr>
          <g:render template="/launchConfiguration/instanceTypeSelect"/>
          <tr class="prop">
            <td class="name">
              <label for="zone">Availablity Zone:</label>
            </td>
            <td class="value">
              <select id="zone" name="zone">
                <g:each var="z" in="${zoneList}">
                  <g:if test="${zone == z.zoneName}">
                    <option selected value="${z.zoneName}">${z.zoneName}</option>
                  </g:if>
                  <g:else><option value="${z.zoneName}">${z.zoneName}</option></g:else>
                </g:each>
              </select>
            </td>
          </tr>
          <tr>
            <td class="name">Security Groups:</td>
            <td class="value">
              <select multiple="true" id="selectedGroups" name="selectedGroups">
                <g:each var="g" in="${securityGroups}">
                  <option value="${g.groupName}">${g.groupName}</option>
                </g:each>
              </select>
            </td>
          </tr>
          <tr class="prop">
            <td class="name">
              <label for="owner">Owner Username:</label>
            </td>
            <td class="value">
              <input class="required" type="text" id="owner" name="owner" placeholder="jsmith" value="${params.owner?.encodeAsHTML()}"/>
            </td>
          </tr>
          <tr>
            <td class="name">
              <label for="pricing">Pricing:</label>
            </td>
            <td class="value">
              <g:select from="['Spot (cheaper)', 'On Demand (faster start)']" keys="['spot', 'ondemand']" name="pricing" />
            </td>
          </tr>
          </tbody>
        </table>
      </div>
      <div class="buttons">
        <g:buttonSubmit class="push" value="Begin Launch" action="launch"/>
      </div>
    </g:form>
  </div>
</body>
</html>
