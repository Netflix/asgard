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
<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  <meta name="layout" content="main"/>
  <title>Icons</title>
</head>
<body>
<div class="body">
  <h1>Tango Open Source Icons</h1>
  <h2><a href="http://tango.freedesktop.org/Tango_Icon_Theme_Guidelines">Guidelines for making new icons</a></h2>
  <g:if test="${flash.message}">
    <div class="message">${flash.message}</div>
  </g:if>
  <g:each var="iconSet" in="${iconSets}">
    <div class="icons">
      ${iconSet.path}<br/>
      <g:each var="icon" in="${iconSet.icons}" status="i">
        <asset:image class="icon" src="${icon.path}" alt="" title="${icon.path}" />
      </g:each>
    </div>
  </g:each>
</body>
</html>
