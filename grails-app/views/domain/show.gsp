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
  <title>${domainName} SimpleDB Domain</title>
</head>
<body>
  <div class="body">
    <h1>SimpleDB Domain Metadata</h1>
    <g:if test="${flash.message}">
      <div class="message">${flash.message}</div>
    </g:if>
    <div class="buttons">
      <g:form action="delete">
        <input type="hidden" name="id" value="${domainName}"/>
        <g:buttonSubmit class="delete" action="delete" value="Delete SimpleDB Domain"
                data-warning="Really delete SimpleDB Domain '${domainName}'?" />
      </g:form>
    </div>
    <div>
      <table>
        <tbody>
        <tr class="prop">
          <td class="name">Domain Name:</td>
          <td class="value">${domainName}</td>
        </tr>
        <tr class="prop" title="The number of all items in the domain">
          <td class="name">Item Count:</td>
          <td class="value">${domainMetadata.itemCount}</td>
        </tr>
        <tr class="prop" title="The total size of all item names in the domain">
          <td class="name">Item Names Size:</td>
          <td class="value">${domainMetadata.itemNamesSize}</td>
        </tr>
        <tr class="prop" title="The number of unique attribute names in the domain">
          <td class="name">Attribute Name Count:</td>
          <td class="value">${domainMetadata.attributeNameCount}</td>
        </tr>
        <tr class="prop" title="The total size of all unique attribute names in the domain">
          <td class="name">Attribute Names Size:</td>
          <td class="value">${domainMetadata.attributeNamesSize}</td>
        </tr>
        <tr class="prop" title="The number of all attribute name/value pairs in the domain">
          <td class="name">Attribute Value Count:</td>
          <td class="value">${domainMetadata.attributeValueCount}</td>
        </tr>
        <tr class="prop" title="The total size of all attribute values in the domain">
          <td class="name">Attribute Values Size:</td>
          <td class="value">${domainMetadata.attributeValuesSize}</td>
        </tr>
        </tbody>
      </table>
    </div>
  </div>
</body>
</html>
