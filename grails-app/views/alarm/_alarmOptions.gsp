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
  <td class="name">
    <label for="description">Description:</label>
  </td>
  <td class="value"><textarea cols="40" rows="3" id="description" name="description">${description}</textarea></td>
</tr>
<tr class="prop" title="The Statistic is applied to the Metric.">
  <td class="name">
    <label for="statistic">Statistic:</label>
  </td>
  <td>
    <g:select name="statistic" value="${statistic}" from="${statistics}"/>
  </td>
</tr>
<tr class="prop" title="The Comparison Operator relates the Metric to the Threshold." >
  <td class="name">
    <label for="comparisonOperator">Comparison<br/>Operator:</label>
  </td>
  <td>
    <g:select name="comparisonOperator"
              noSelection="['':'']" value="${comparisonOperator}"
              from="${comparisonOperators}" data-placeholder="Select One..."/>
  </td>
</tr>
<tbody class="numbers">
<tr class="prop">
  <td class="name">
    <label for="threshold">Threshold:</label>
  </td>
  <td class="value">
    <g:textField id="threshold" name="threshold" value="${threshold}"/>
  </td>
</tr>
<tr class="prop">
  <td class="name">
    <label for="period">Period:</label>
  </td>
  <td class="value">
    <g:textField id="period" name="period" value="${period}"/> seconds
  </td>
</tr>
<tr class="prop">
  <td class="name">
    <label for="evaluationPeriods">Evaluation<br/>Periods:</label>
  </td>
  <td class="value">
    <g:textField id="evaluationPeriods" name="evaluationPeriods" value="${evaluationPeriods}"/>
  </td>
</tr>
</tbody>
<tr class="prop">
  <td class="name">
    <label for="topic">Topic:</label>
  </td>
  <td>
    <g:select name="topic" noSelection="${['':'']}" from="${topics}" value="${topic}"/>
  </td>
</tr>
