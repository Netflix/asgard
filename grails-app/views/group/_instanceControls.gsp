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
<table class="clear">
  <tr>
    <td colspan="100%" class="subitems">
      <div class="buttons">
        <span class="count">Total Instances: <span class="instanceCount">${instanceCount}</span></span>
        <g:if test="${discoveryExists}">
          <a class="monitor healthCheckRunLink${runHealthChecks ? " autoLaunch" : ""}" href="javascript:;"
             title="Check the Eureka status and health check result of all instances.">Run All Health Checks</a>
        </g:if>
        <g:buttonSubmit class="stop terminateMany" title="Terminate the selected instances."
                        controller="instance" action="terminate" value="Terminate Instances"/>
      </div>
      <div class="buttons requireLogin">
        <label>Load Balancing:</label>
        <g:buttonSubmit class="removeBalance" title="Remove the selected instances from the group's load balancers."
                        controller="instance" action="deregister" value="Deregister Instances from ELBs"/>
        <g:buttonSubmit class="instanceBalance" title="Add the selected instances to the group's load balancers."
                        controller="instance" action="register" value="Register Instances with group's ELBs" />
      </div>
      <g:if test="${discoveryExists}">
        <div class="buttons requireLogin">
          <label>Eureka:</label>
          <g:buttonSubmit class="outOfService" title="Prevent Eureka from listing the selected instances for use by other applications."
                          controller="instance" action="takeOutOfService" value="Deactivate in Eureka"/>
          <g:buttonSubmit class="inService" title="Allow Eureka to list the selected instances for use by other applications."
                          controller="instance" action="putInService" value="Activate in Eureka"/>
        </div>
      </g:if>
    </td>
  </tr>
</table>
