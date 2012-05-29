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
<%@ page import="com.netflix.asgard.model.AlarmData" %>

<ul class="links">
  <g:each var="dimension" in="${dimensions}">
    <li>
      <g:if test="${dimension.name == AlarmData.DIMENSION_NAME_FOR_ASG}">
        <g:if test="${showAsgText}">${dimension.name}:</g:if>
        <g:linkObject type="autoScaling" name="${dimension.value}"/>
      </g:if>
      <g:else>${dimension.name}: ${dimension.value}</g:else>
    </li>
  </g:each>
</ul>