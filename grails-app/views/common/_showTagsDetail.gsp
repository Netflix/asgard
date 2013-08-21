<tr class="prop">
  <td class="name">Tags:</td>
  <td class="value">
    <table class="sortable subitems">
    <thead>
    <tr>
    <th>Tag</th>
    <th>Value</th>
    <th>Propagate</th>
    <th>Resource</th>
    <th>Type</th>
    </tr>
    </thead>
    <tbody>
      <g:each var="tag" in="${tags}">
        <tr class="prop">
          <td><pre>${tag.key}</pre></td>
          <td><pre>${tag.value}</pre></td>
          <td><pre>${tag.propagateAtLaunch}</pre></td>
          <td><pre>${tag.resourceId}</pre></td>
          <td><pre>${tag.resourceType}</pre></td>
        </tr>
      </g:each>
      </tbody>
    </table>
  </td>
</tr>
