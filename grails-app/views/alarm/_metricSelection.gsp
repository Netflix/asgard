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
  <td class="name">Metric:</td>
  <td class="value inlineInputs customOrExistingInput">
    <div>
      <input id="useExistingMetric" type="checkBox" ${ useExistingMetric ? 'checked="checked"' : '' }/>
      <label for="useExistingMetric">Use Existing Metric</label>
    </div>
    <div class="${ !useExistingMetric ? 'concealed' : '' }">
      <g:select name="existingMetric" noSelection="['':'']"
                value="${existingMetric?.encodeAsHTML()}"
                from="${metrics}" optionKey="${ { it.encodeAsHTML() } }" optionValue="displayText"
                disabled="${!useExistingMetric}" data-placeholder="Select One..."/>
    </div>
    <div class="${ useExistingMetric ? 'concealed' : '' }">
      <label for="namespace">Namespace:</label>
      <input type="text" id="namespace" name="namespace" value="${namespace?.encodeAsHTML()}" ${ useExistingMetric ? 'disabled="disabled"' : '' }/>
      <label for="metric">Metric Name:</label>
      <input type="text" id="metric" name="metric" value="${metric?.encodeAsHTML()}" ${ useExistingMetric ? 'disabled="disabled"' : '' }/>
    </div>
  </td>
</tr>
