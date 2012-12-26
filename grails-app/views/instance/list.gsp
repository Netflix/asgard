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
  <title>Instances</title>
</head>
<body>
  <div class="body">
    <h1>Running Instances in ${region.description}${appNames ? ' of ' + appNames : ''}</h1>
    <g:if test="${flash.message}">
      <div class="message">${flash.message}</div>
    </g:if>
    <g:form method="post" class="validate">
      <input type="hidden" name="appNames" value="${params.id}"/>
      <g:render template="instances">
        <div class="buttons">
          <g:buttonSubmit class="stop" value="Terminate Instance(s)" action="terminate"
                          data-warning="Really terminate instance(s)?"/>
          <g:link class="clean" action="audit">Audit Ungrouped Instances</g:link>
        </div>
      </g:render>
      <div class="paginateButtons">
      </div>
    </g:form>
  </div>
</body>
</html>
