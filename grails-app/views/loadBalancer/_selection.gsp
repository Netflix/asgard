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
    <label for="selectedLoadBalancers">Load Balancers<br/>(cannot be added<br/>or removed later):</label>
  </td>
  <td>
    <select multiple="true" id="selectedLoadBalancers" name="selectedLoadBalancers" size="10">
      <g:each var="elb" in="${loadBalancers}">
        <option value="${elb.loadBalancerName}" ${Requests.ensureList(selectedLoadBalancers ?: params.selectedLoadBalancers).contains(elb.loadBalancerName) ? "selected" : ""}>${elb.loadBalancerName}</option>
      </g:each>
    </select>
  </td>
</tr>
