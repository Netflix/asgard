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
  <title>Asgard is Loading</title>
  <meta name="layout" content="main"/>
  <meta name="hideNav" content="true"/>
</head>
<body>
  <div class="body">
    <g:if test="${flash.message}">
      <div class="message">${flash.message}</div>
    </g:if>
    <h1>Asgard is starting up. This may take a few minutes.</h1>
    <h3>This page will automatically refresh when Asgard is loaded.<img alt="spinner" src="${resource(dir: 'images', file: 'spinner.gif')}"/></h3>
    Time since server startup: <strong><span id="timeSinceStartup">?</span></strong>
    <h2>Caches remaining to load</h2>
    <div id="remainingCaches">?</div>
  </div>
  <script defer type="text/javascript" src="${resource(dir: 'js', file: 'loading.js')}?v=${build}"></script>
</body>
</html>
