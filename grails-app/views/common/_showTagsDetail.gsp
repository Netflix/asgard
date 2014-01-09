<tr class="prop">
  <td class="name">Tags:</td>
  <td class="value">
    <table class="sortable subitems">
    <thead>
    <tr>
    <th>Tag</th>
    <th>Value</th>
    <th>Propagate</th>
    </tr>
    </thead>
    <tbody>
      <g:each var="tag" in="${tags}">
        <tr class="prop">
          <td><pre>${tag.key}</pre></td>
          <td><pre>${tag.value}</pre></td>
          <td><pre>${tag.propagateAtLaunch}</pre></td>                    
        </tr>
      </g:each>
      </tbody>
    </table>
  </td>
</tr>
