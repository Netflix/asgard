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
<tr class="prop">
  <td class="name"><label>Auto Scaling Group:</label></td>
  <td class="value"><g:linkObject type="autoScaling" name="${group}"/></td>
</tr>
<tr class="prop" title="The Adjustment Type dictates how the scaling adjustment will be applied.">
  <td class="name">
    <label for="adjustmentType">Adjustment Type:</label>
  </td>
  <td>
    <g:select name="adjustmentType" from="${adjustmentTypes}" value="${adjustmentType}"/>
  </td>
</tr>
<tbody class="numbers">
<tr class="prop">
  <td class="name">
    <label for="adjustment">Adjustment:</label>
  </td>
  <td class="value">
    <g:textField name="adjustment" value="${adjustment}"/>
  </td>
</tr>
<tr class="prop" title="A change in capacity of at least this amount will occur.">
  <td class="name">
    <label for="minAdjustmentStep">Minimum Adjustment:</label>
  </td>
  <td class="value">
    <g:textField name="minAdjustmentStep" value="${minAdjustmentStep}"/>
  </td>
</tr>
<tr class="prop">
  <td class="name">
    <label for="cooldown">Cooldown:</label>
  </td>
  <td class="value">
    <g:textField name="cooldown" value="${cooldown}"/> seconds
  </td>
</tr>
</tbody>
