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
  <td colspan="2"><h2 title="Options that control the process of waiting for a stable state for each instance">Startup Options</h2></td>
</tr>
<g:if test="${checkHealth}">
  <tr class="prop" title="Wait for instance to register with Eureka and then wait for it to pass its health check">
    <td class="name"><label for="checkHealth">After launch:</label></td>
    <td class="value"><g:checkBox name="checkHealth" value="${checkHealth}" checked="${checkHealth}"/>
      <label for="checkHealth">Wait for Eureka health check pass</label></td>
  </tr>
</g:if>
<tr class="prop waitTime" style="${checkHealth ? 'display:none;' : ''}" title="Additional time to wait after OS boot when not waiting for Eureka health check">
  <td class="name"><label for="afterBootWait">Additional wait time:</label></td>
  <td class="value"><input type="text" class="number" id="afterBootWait" name="afterBootWait" value="${afterBootWait}"/> seconds</td>
</tr>
