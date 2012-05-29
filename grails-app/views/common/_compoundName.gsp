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
<fieldset class="compoundName">
  <div class="guide">
    <h2>Name</h2>
    Optional variables let you customize:
    <ul>
      <li>Environment variables</li>
      <li>Request routing</li>
      <li>Selection of fast property values</li>
    </ul>
  </div>
  <div class="envVars">
    <label>Environment variables for labeled variables</label>
    <pre></pre>
  </div>
  <div class="clear"></div>
  <div class="preview">
    <label>Name Preview</label>
    <div class="nameText">(Name Preview)</div>
  </div>
  <div class="clear"></div>
  <table>
    <tr class="headers">
      <td title="The registered application that should run on the instances"><div class="notation">(Required)</div><label for="appName">Application</label></td>
      <td title="Used to create vertical stacks of dependent services for integration testing"><label for="stack">Stack</label></td>
      <td title="Free-form alphanumeric characters and hyphens to describe any other variables"><label for="detail">Free-form<br/>details</label></td>
      <td><span class="initial">C</span>ountries</td>
      <td><span class="initial">D</span>ev<br/>phase</td>
      <td><span class="initial">H</span>ardware<br/>devices</td>
      <td><span class="initial">P</span>artners</td>
      <td><span class="initial">R</span>evision</td>
    </tr>
    <tr>
      <td><g:select title="The application that will run on the instances" name="appName"
          noSelection="['':'-Application-']" value="${params.appName}" from="${applications}"
          optionKey="name" optionValue="name"/></td>
      <td>
        <div>
          <select id="stack" name="stack" title="The name of the stack of applications being logically deployed and managed together">
            <option value="">-Stack-</option>
            <option disabled>------------</option>
            <g:each var="st" in="${stacks}">
              <option value="${st.name}" ${params.stack == st.name ? "selected" : ""}>${st.name ?: "(no stack)"}</option>
            </g:each>
          </select>
        </div>
        <div>
          <g:textField name="newStack" placeholder="New Stack" value="${params.newStack}"/>
        </div>
      </td>
      <td>
        <g:textField name="detail" value="${params.detail}"/>
      </td>
      <td>
        <g:textField name="countries" value="${params.countries}"/>
        <div><span>Ex:</span> latam</div>
      </td>
      <td>
        <g:textField name="devPhase" value="${params.devPhase}"/>
        <div><span>Ex:</span> stage</div>
      </td>
      <td>
        <g:textField name="hardware" value="${params.hardware}"/>
        <div><span>Ex:</span> phones</div>
      </td>
      <td>
        <g:textField name="partners" value="${params.partners}"/>
        <div><span>Ex:</span> sony</div>
      </td>
      <td>
        <g:textField name="revision" value="${params.revision}" class="short"/>
        <div><span>Ex:</span> 27</div>
      </td>
    </tr>
  </table>
</fieldset>
