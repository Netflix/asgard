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
  <title>${topic.name} Topic</title>
</head>
<body>
  <div class="body">
    <h1>Topic Details</h1>
    <g:if test="${flash.message}">
      <div class="message">${flash.message}</div>
    </g:if>
    <div class="buttons">
      <g:form class="validate">
        <input type="hidden" name="id" value="${topic.arn}"/>
        <g:buttonSubmit class="delete" data-warning="Really delete Topic '${topic.name}'?" action="delete" value="Delete Topic"/>
        <g:link class="create" action="prepareSubscribe" params="[id: topic.name]">Subscribe To Topic</g:link>
        <g:if test="${hasConfirmedSubscriptions}">
          <g:link class="publish" action="preparePublish" params="[id: topic.name]">Publish To Topic</g:link>
        </g:if>
      </g:form>
    </div>
    <div>
      <table>
        <tbody>
        <tr class="prop">
          <td class="name">Topic Name:</td>
          <td class="value">${topic.name}</td>
        </tr>
        <tr class="prop">
          <td class="name">Subscriptions:</td>
          <td class="value">
            <g:if test="${subscriptions}">
              <div class="list">
                <table class="sortable subitems">
                  <thead>
                  <tr>
                    <th>Endpoint</th>
                    <th>Protocol</th>
                    <th></th>
                  </tr>
                  </thead>
                    <g:each var="subscription" in="${subscriptions}" status="i">
                      <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
                        <td><g:snsSubscriptionEndpoint>${subscription.endpoint}</g:snsSubscriptionEndpoint></td>
                        <td>${subscription.protocol}</td>
                        <g:if test="${subscription.isConfirmed()}">
                          <td class="buttons">
                            <g:form>
                              <input type="hidden" name="topic" value="${topic.name}"/>
                              <input type="hidden" name="subscriptionArn" value="${subscription.arn}"/>
                              <g:buttonSubmit class="delete" data-warning="Really unsubscribe '${subscription.endpoint}'?" action="unsubscribe" value="Unsubscribe"/>
                            </g:form>
                          </td>
                        </g:if>
                        <g:else>
                          <td>Unconfirmed</td>
                        </g:else>
                      </tr>
                    </g:each>
                </table>
              </div>
            </g:if>
          </td>
        </tr>
        <g:each var="attribute" in="${topic.attributes}">
          <tr class="prop">
            <td class="name">${com.netflix.asgard.Meta.splitCamelCase(attribute.key)}:</td>
            <td class="value"><g:render template="/common/jsonValue" model="['jsonValue': attribute.value]"/></td>
          </tr>
        </g:each>
        </tbody>
      </table>
    </div>
  </div>
</body>
</html>
