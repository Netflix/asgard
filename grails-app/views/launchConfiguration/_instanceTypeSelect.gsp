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
<tr class="prop ${rowClass}">
  <td class="name">
    <label for="instanceType"><g:link controller="instanceType" action="list">Instance Type:</g:link></label>
  </td>
  <td class="value">
    <select id="instanceType" name="instanceType">
      <g:each var="t" in="${instanceTypes}">
        <option value="${t.name}" ${t.name == params.instanceType || t.name == instanceType ? 'selected' : ''}>${t.name} ${t.monthlyLinuxOnDemandPrice ? t.monthlyLinuxOnDemandPrice + '/mo' : ''}</option>
      </g:each>
    </select>
  </td>
</tr>