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
<%@ page import="com.netflix.asgard.Requests" %>
<tr class="prop advanced">
  <td class="name">
    <label>Load Balancers<br/>(cannot be added<br/>or removed later):</label>
  </td>
  <td>
    <g:each var="vpcIdForLoadBalancer" in="${loadBalancersGroupedByVpcId?.keySet()}">
      <div class="loadBalancersSelect vpcId${vpcIdForLoadBalancer ?: ''} ${vpcId == vpcIdForLoadBalancer ? '' : 'concealed'}">
        <g:select name="selectedLoadBalancersForVpcId${vpcIdForLoadBalancer ?: ''}" multiple="multiple" size="5"
                  disabled="${vpcId == vpcIdForLoadBalancer ? 'false' : 'true'}"
                  optionKey="loadBalancerName" optionValue="loadBalancerName" data-placeholder="Select load balancers"
                  from="${loadBalancersGroupedByVpcId[vpcIdForLoadBalancer]}" value="${selectedLoadBalancers}" />
      </div>
    </g:each>
    <g:if test="${!loadBalancersGroupedByVpcId}">
      There are no load balancers in this account-region.
    </g:if>
  </td>
</tr>
