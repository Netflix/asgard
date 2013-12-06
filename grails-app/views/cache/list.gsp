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
  <title>Caches</title>
</head>
<body>
  <div class="body">
    <h1>Caches</h1>
    <g:if test="${flash.message}">
      <div class="message">${flash.message}</div>
    </g:if>
    <g:if test="${unfilled}">
      <h2>Caches that are not filled</h2>
      <g:each in="${unfilled}" var="cacheName">
        <div class="danger">${cacheName}</div>
      </g:each>
    </g:if>
  </div>
  <div class="body">
    <h2>Global Caches</h2>
    <div class="list">
      <div class="buttons"></div>
      <table class="sortable">
        <thead>
        <tr>
          <th>Name</th>
          <th>Status</th>
          <th>Since Last Fill</th>
          <th>Size</th>
        </tr>
        </thead>
        <tbody>
        <g:each var="g" in="${globalCaches}" status="i">
          <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
            <td>${g.name}</td>
            <td class="${g.filled ? '' : 'danger'}">${g.filled ? 'Filled' : 'Not Filled'}</td>
            <td>${g.timeSinceLastFill}</td>
            <td>${g.size}</td>
          </tr>
        </g:each>
        </tbody>
      </table>
    </div>
    <footer/>
  </div>
  <div class="body">
    <h2>Prices</h2>
    <div class="list">
      <div class="buttons"></div>
      <table class="sortable">
        <thead>
        <tr>
          <th>Name</th>
          <th>Status</th>
        </tr>
        </thead>
        <tbody>
        <g:each var="p" in="${prices}" status="i">
          <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
            <td>${p.name}</td>
            <td class="${p.filled ? '' : 'danger'}">${p.filled ? 'Filled' : 'Not Filled'}</td>
          </tr>
        </g:each>
        </tbody>
      </table>
    </div>
    <footer/>
  </div>
  <div class="body">
    <h2>Multi-Region Caches</h2>
    <div class="list">
      <div class="buttons"></div>
      <table class="sortable">
        <thead>
        <tr>
          <th>Name</th>
          <th>Status</th>
          <th>Regional</th>
        </tr>
        </thead>
        <tbody>
        <g:each var="m" in="${multiRegionCaches}" status="i">
          <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
            <td>${m.name}</td>
            <td class="${m.filled ? '' : 'danger'}">${m.filled ? 'Filled' : 'Not Filled'}</td>
            <td>
              <table>
                <thead>
                <tr>
                  <th>Name</th>
                  <th>Status</th>
                  <th>Since Last Fill</th>
                  <th>Size</th>
                </tr>
                </thead>
                <tbody>
                <g:each in="${m.regionalCaches.values()}" var="cache">
                  <tr>
                    <td>${cache.name}</td>
                    <td class="${cache.filled ? '' : 'danger'}">${cache.filled ? 'Filled' : 'Not Filled'}</td>
                    <td>${cache.timeSinceLastFill}</td>
                    <td>${cache.size}</td>
                  </tr>
                </g:each>
                </tbody>
              </table>
            </td>
          </tr>
        </g:each>
        </tbody>
      </table>
    </div>
    <footer/>
  </div>
</body>
</html>
