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
  <td class="name">Description:</td>
  <td class="value">${alarm.alarmDescription}</td>
</tr>
<tr class="prop">
  <td class="name">Statistic:</td>
  <td class="value">${alarm.statistic}</td>
</tr>
<tr class="prop">
  <td class="name">Namespace:</td>
  <td class="value">${alarm.namespace.encodeAsHTML()}</td>
</tr>
<tr class="prop">
  <td class="name">Metric:</td>
  <td class="value">${alarm.metricName.encodeAsHTML()}</td>
</tr>
<tr class="prop">
  <td class="name">Comparison Operator:</td>
  <td class="value">${alarm.comparisonOperator}</td>
</tr>
<tr class="prop">
  <td class="name">Threshold:</td>
  <td class="value">${alarm.threshold}</td>
</tr>
<tr class="prop">
  <td class="name">Period:</td>
  <td class="value">${alarm.period} seconds</td>
</tr>
<tr class="prop">
  <td class="name">Evaluation Periods:</td>
  <td class="value">${alarm.evaluationPeriods}</td>
</tr>