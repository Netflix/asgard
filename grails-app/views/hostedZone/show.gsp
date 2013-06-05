<%--

    Copyright 2013 Netflix, Inc.

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
  <title>${hostedZone.name} Route53 Hosted Zone</title>
</head>
<body>
  <div class="body">
    <h1>Route53 Hosted Zone Details</h1>
    <g:if test="${flash.message}">
      <div class="message">${flash.message}</div>
    </g:if>
    <div class="buttons">
      <g:form>
        <input type="hidden" name="id" value="${hostedZone.id}"/>
        <g:buttonSubmit class="delete" action="delete" value="Delete Hosted Zone" data-warning="${deletionWarning}"/>
      </g:form>
    </div>
    <div>
      <table>
        <tbody>
        <tr class="prop">
          <td class="name">Hosted Zone ID:</td>
          <td class="value">${hostedZone.id}</td>
        </tr>
        <tr class="prop">
          <td class="name">Name:</td>
          <td class="value">${hostedZone.name}</td>
        </tr>
        <tr class="prop">
          <td class="name">Caller Reference:</td>
          <td class="value">${hostedZone.callerReference}</td>
        </tr>
        <tr class="prop">
          <td class="name">Comment:</td>
          <td class="value">${hostedZone.config.comment}</td>
        </tr>
        <tr class="prop">
          <td class="name">Resource Record Set Count:</td>
          <td class="value">${hostedZone.resourceRecordSetCount}</td>
        </tr>
        <tr class="prop">
          <td class="name" colspan="2">Resource Record Sets:</td>
        </tr>
        <tr class="prop">
          <td class="value" colspan="2">
            <g:if test="${resourceRecordSets}">
              <div class="list">
                <div class="buttons">
                  <g:link class="create" action="prepareResourceRecordSet" id="${hostedZone.id}">Create New Resource Record Set</g:link>
                </div>
                <table class="sortable subitems resourceRecordSets">
                  <thead>
                  <tr>
                    <th>Type</th>
                    <th>Name</th>
                    <th>Resource Records</th>
                    <th>TTL</th>
                    <th>Region</th>
                    <th>Alias<br/>Target</th>
                    <th>Set<br/>ID</th>
                    <th>Weight</th>
                    <th>Failover</th>
                    <th>Health<br/>Check<br/>ID</th>
                    <th class="sorttable_nosort"></th>
                  </tr>
                  </thead>
                    <g:each var="resourceRecordSet" in="${resourceRecordSets}" status="i">
                      <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
                        <td>${resourceRecordSet.type}</td>
                        <td class="resourceRecordSetName">${resourceRecordSet.name}</td>
                        <td class="resourceRecords longValues">
                          <ul>
                            <g:each var="resourceRecord" in="${resourceRecordSet.resourceRecords}">
                              <li>${resourceRecord.value}</li>
                            </g:each>
                          </ul>
                        </td>
                        <td>${resourceRecordSet.TTL}</td>
                        <td>${resourceRecordSet.region}</td>
                        <td class="aliasTarget">${resourceRecordSet.aliasTarget?.dNSName}</td>
                        <td class="resourceRecordSetId">${resourceRecordSet.setIdentifier}</td>
                        <td>${resourceRecordSet.weight}</td>
                        <td>${resourceRecordSet.failover}</td>
                        <td>${resourceRecordSet.healthCheckId}</td>
                        <td class="buttons">
                          <g:form>
                            <%--
                              There seems to be no simple way to specify a distinct resource record set.
                              Every field must be specified in order to avoid deleting a similar but incorrect item.
                            --%>
                            <input type="hidden" name="hostedZoneId" value="${hostedZone.id}"/>
                            <input type="hidden" name="resourceRecordSetName" value="${resourceRecordSet.name}"/>
                            <input type="hidden" name="type" value="${resourceRecordSet.type}"/>
                            <input type="hidden" name="setIdentifier" value="${resourceRecordSet.setIdentifier ?: ''}"/>
                            <input type="hidden" name="weight" value="${resourceRecordSet.weight ?: ''}"/>
                            <input type="hidden" name="resourceRecordSetRegion" value="${resourceRecordSet.region ?: ''}"/>
                            <input type="hidden" name="failover" value="${resourceRecordSet.failover ?: ''}"/>
                            <input type="hidden" name="ttl" value="${resourceRecordSet.TTL ?: ''}"/>
                            <input type="hidden" name="resourceRecords" value="${resourceRecordSet.resourceRecords.collect { it.value }.join('\n') ?: ''}"/>
                            <input type="hidden" name="aliasTarget" value="${resourceRecordSet.aliasTarget?.dNSName ?: ''}"/>
                            <input type="hidden" name="healthCheckId" value="${resourceRecordSet.healthCheckId ?: ''}"/>
                            <g:buttonSubmit class="delete" action="removeResourceRecordSet" value="Remove"
                                            data-warning="Really remove resource record set '${resourceRecordSet.name}'?"/>
                          </g:form>
                        </td>
                      </tr>
                    </g:each>
                </table>
                <footer/>
              </div>
            </g:if>
          </td>
        </tr>
        </tbody>
      </table>
    </div>
  </div>
</body>
</html>
