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
<tr class="prop" title="The regular schedule that an action occurs. When Start Time and End Time are specified with, they form the boundaries of the recurring action.">
  <td class="name"><label for="recurrence">Recurrence:</label></td>
  <td class="value"><g:textField name="recurrence" value="${recurrence}"/>&nbsp;<a href="http://en.wikipedia.org/wiki/Cron">What's this?</a></td>
</tr>
<tbody class="numbers">
<tr class="prop" title="The minimum size of the Auto Scaling group.">
  <td class="name">
    <label for="min">Minimum Size:</label>
  </td>
  <td class="value">
    <g:textField name="min" value="${min}"/>
  </td>
</tr>
<tr class="prop" title="The maximum size of the Auto Scaling group.">
  <td class="name">
    <label for="max">Maximum Size:</label>
  </td>
  <td class="value">
    <g:textField name="max" value="${max}"/>
  </td>
</tr>
<tr class="prop" title="The number of instances you prefer to maintain in your Auto Scaling group.">
  <td class="name">
    <label for="desired">Desired Capacity:</label>
  </td>
  <td class="value">
    <g:textField name="desired" value="${desired}"/>
  </td>
</tr>
</tbody>
