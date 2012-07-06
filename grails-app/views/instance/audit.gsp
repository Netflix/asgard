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
  <title>${params.id ? params.id + "'s" : 'Orphaned'} instances</title>
</head>
<body>
  <div class="body">
    <h1>Orphaned instances by owner in ${region.description}</h1>
    <g:if test="${flash.message}">
      <div class="message">${flash.message}</div>
    </g:if>
    <g:each in="${ownersToInstanceLists}" var="ownerToInstances">
        <g:render template="instances" model="[instanceList: ownerToInstances.value]">
          <h2>Owner: <g:link class="user" action="audit" params="[id: ownerToInstances.key]" >${ownerToInstances.key}</g:link></h2>
        </g:render>
    </g:each>
  </div>
</body>
</html>
