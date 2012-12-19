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
<tr class="prop advanced">
  <td class="name">
    <label for="selectedSecurityGroups">Security Groups:</label>
  </td>
  <td>
    <g:each var="vpcIdForSecurityGroup" in="${securityGroupsGroupedByVpcId?.keySet()}">
      <div class="securityGroupsSelect vpcId${vpcIdForSecurityGroup ?: ''} ${vpcId == vpcIdForSecurityGroup ? '' : 'concealed'}">
        <g:select name="selectedSecurityGroups" id="selectedSecurityGroupsFor${vpcIdForSecurityGroup}" multiple="multiple" size="5"
                  disabled="${vpcId == vpcIdForSecurityGroup ? 'false' : 'true'}"
                  optionKey="${vpcIdForSecurityGroup ? 'groupId' : 'groupName'}" optionValue="groupName" data-placeholder="Select security groups"
                  from="${securityGroupsGroupedByVpcId[vpcIdForSecurityGroup]}" value="${selectedSecurityGroups}" />
      </div>
    </g:each>
  </td>
</tr>