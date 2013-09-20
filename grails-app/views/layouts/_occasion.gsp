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
<%@ page import="com.netflix.asgard.Occasion" %>
<g:if test="${occasion.name() == Occasion.APRILFOOLS.name()}">
  <link rel="stylesheet" href="${resource(dir: 'js/shadowbox-3.0.3', file: 'shadowbox.css')}?v=${build}"/>
  <script defer type="text/javascript" src="${resource(dir: 'js/shadowbox-3.0.3', file: 'shadowbox.js')}?v=${build}"></script>
  <script defer type="text/javascript" src="${resource(dir: 'js', file: 'aprilfools.js')}?v=${build}"></script>
  <g:if test="${autoLaunchFullAprilFoolsJoke}">
    <script defer type="text/javascript" src="${resource(dir: 'js', file: 'aprilfoolslaunch.js')}?v=${build}"></script>
  </g:if>
</g:if>
