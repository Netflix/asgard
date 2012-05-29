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
  <title>${query} Global Search Results</title>
</head>
<body>
<div class="body">
  <h1>Global Search Results</h1>
  <h2>Query: ${query}</h2>
  <g:if test="${flash.message}">
    <div class="message">${flash.message}</div>
  </g:if>
  <div class="global">
    <g:each in="${results}" var="regionToMapOfTypesToLists">
      <g:if test="${regionToMapOfTypesToLists.value.size()}">
        <div class="region">
          <h2>${regionToMapOfTypesToLists.key?.description ?: 'Global'}</h2>
          <g:each in="${regionToMapOfTypesToLists.value}" var="typeToList">
            <g:if test="${typeToList.value.size()}">
              <h3>${typeToList.key.displayName}</h3>
              <g:each in="${typeToList.value}" var="entity">
                <g:linkObject name="${typeToList.key.keyer.call(entity)}" type="${typeToList.key.name()}" /><br/>
              </g:each>
            </g:if>
          </g:each>
        </div>
      </g:if>
    </g:each>
  </div>
</div>
</body>
</html>
