<g:if test="${isChaosMonkeyActive}">
  <tr class="prop">
    <td class="name">
      <label>Chaos Monkey:</label>
    </td>
    <td>
      <div>
        <g:radio name="chaosMonkey" id="chaosMonkeyEnabled" value="enabled" checked="${params.chaosMonkey == 'enabled'}"/>
        <label for="chaosMonkeyEnabled" class="choice">Enabled</label>
      </div>
      <div>
        <g:radio name="chaosMonkey" id="chaosMonkeyDisabled" value="disabled" checked="${params.chaosMonkey == 'disabled'}"/>
        <label for="chaosMonkeyDisabled" class="choice">Disabled (Notifications to enable start in 30 days)</label>
      </div>
    </td>
  </tr>
</g:if>
