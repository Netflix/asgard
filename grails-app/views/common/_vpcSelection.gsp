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
<tr class="prop advanced ${subnetPurposes ? '' : 'concealed'}">
  <td class="name">
    VPC:
  </td>
  <td>
    <div>
      <g:radio name="subnetPurpose" id="subnetRadioNonVpc" value="" data-vpcid="" data-purpose="" checked="${!subnetPurpose}"/>
      <label class="choice">${awsAction} non-VPC ${awsObject}</label>
    </div>
    <g:each var="purpose" in="${subnetPurposes}">
      <div>
        <g:radio name="subnetPurpose" id="subnetRadio${purpose}" value="${purpose}" data-vpcid="${purposeToVpcId[purpose]}"
                 data-purpose="${purpose}" checked="${subnetPurpose == purpose ? 'true' : ''}"/>
        <label class="choice">${awsAction} '${purpose}' VPC ${awsObject}</label>
      </div>
    </g:each>
  </td>
</tr>
