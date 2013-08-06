<%@ page import="com.netflix.asgard.deployment.ProceedPreference" %>
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
  <title>Deploy</title>
</head>

<body>
<div class="body">
  <h1>Deploy Next ASG for Cluster '${clusterName}'</h1>
  <g:if test="${flash.message}">
    <div class="message">${flash.message}</div>
  </g:if>
  <g:hasErrors bean="${cmd}">
    <div class="errors">
      <g:renderErrors bean="${cmd}" as="list"/>
    </div>
  </g:hasErrors>
  <g:form action="deploy" method="post" class="validate">
    <div class="dialog">
      <ul class="groupReplacingPush">
      <g:if test="${deploymentWorkflowOptions}">
        <li class="deploymentWorkflowOptions clusterAsgForm">
          <h2>Deployment Options:</h2>
            <table class="deploymentWorkflowOptions">
              <tr class="prop">
                <td class="name">
                  <label for="notificationDestination">Notifications will be sent to:</label>
                </td>
                <td class="value">
                  <g:textField class="required" name="notificationDestination" value="${deploymentWorkflowOptions.notificationDestination}"/>
                </td>
              </tr>
              <tr class="prop numbers">
                <td class="name">
                  <label for="delayDurationMinutes">Delay before deploying:</label>
                </td>
                <td class="value">
                  <g:textField class="required" name="delayDurationMinutes" value="${deploymentWorkflowOptions.delayDurationMinutes}"/>&nbsp;minutes
                </td>
              </tr>
              <tr>
                <td colspan="2"><h3>Canary Step</h3></td>
              </tr>
              <tr>
                <td>Run canary</td>
                <td>
                  <div>
                    <g:radio name="doCanary" id="doCanaryFalse" value="false" checked="${!deploymentWorkflowOptions.doCanary}"/>
                    <label for="doCanaryFalse" class="choice">Skip canary</label>
                  </div>
                  <div>
                    <g:radio name="doCanary" id="doCanaryTrue" value="true" checked="${deploymentWorkflowOptions.doCanary}"/>
                    <label for="doCanaryTrue" class="choice">Canary before full capacity</label>
                  </div>
                </td>
              </tr>
              <tbody class="canaryOptions numbers ${deploymentWorkflowOptions.doCanary ? '' : 'concealed'}">
                <tr class="prop">
                  <td class="name">
                    <label for="canaryCapacity">Instance count:</label>
                  </td>
                  <td class="value">
                    <g:textField class="required" name="canaryCapacity" value="${deploymentWorkflowOptions.canaryCapacity}"/>
                  </td>
                </tr>
                <tr class="prop">
                  <td class="name">
                    <label for="canaryStartUpTimeoutMinutes">Start up timeout:</label>
                  </td>
                  <td class="value">
                    <g:textField class="required" name="canaryStartUpTimeoutMinutes" value="${deploymentWorkflowOptions.canaryStartUpTimeoutMinutes}"/>&nbsp;minutes
                  </td>
                </tr>
                <tr class="prop">
                  <td class="name">
                    <label for="canaryAssessmentDurationMinutes">Assessment duration:</label>
                  </td>
                  <td class="value">
                    <g:textField class="required" name="canaryAssessmentDurationMinutes" value="${deploymentWorkflowOptions.canaryAssessmentDurationMinutes}"/>&nbsp;minutes
                  </td>
                </tr>
                <tr class="prop">
                  <td class="name">
                    Scale to desired capacity after canary assessment:
                  </td>
                  <td>
                    <div>
                      <g:radio name="scaleUp" id="scaleUpYes" value="Yes" checked="${deploymentWorkflowOptions.scaleUp == ProceedPreference.Yes}"/>
                      <label for="scaleUpYes" class="choice">Yes</label>
                    </div>
                    <div>
                      <g:radio name="scaleUp" id="scaleUpNo" value="No" checked="${deploymentWorkflowOptions.scaleUp == ProceedPreference.No}"/>
                      <label for="scaleUpNo" class="choice">No</label>
                    </div>
                    <div>
                      <g:radio name="scaleUp" id="scaleUpAsk" value="Ask" checked="${!deploymentWorkflowOptions.scaleUp || deploymentWorkflowOptions.scaleUp == ProceedPreference.Ask}"/>
                      <label for="scaleUpAsk" class="choice">Ask</label>
                    </div>
                  </td>
                </tr>
              </tbody>
              <tr>
                <td colspan="2"><h3>Full Capacity Step</h3></td>
              </tr>
              <tbody class="numbers">
                <tr class="prop">
                  <td class="name">
                    <label for="desiredCapacityStartUpTimeoutMinutes">Start up timeout:</label>
                  </td>
                  <td class="value">
                    <g:textField class="required" name="desiredCapacityStartUpTimeoutMinutes" value="${deploymentWorkflowOptions.desiredCapacityStartUpTimeoutMinutes}"/>&nbsp;minutes
                  </td>
                </tr>
                <tr class="prop">
                  <td class="name">
                    <label for="desiredCapacityAssessmentDurationMinutes">Assessment duration:</label>
                  </td>
                  <td class="value">
                    <g:textField class="required" name="desiredCapacityAssessmentDurationMinutes" value="${deploymentWorkflowOptions.desiredCapacityAssessmentDurationMinutes}"/>&nbsp;minutes
                  </td>
                </tr>
                <tr>
                  <td colspan="2"><h3>Clean Up Step</h3></td>
                </tr>
                <tr class="prop">
                  <td class="name">
                    Disable previous ASG:
                  </td>
                  <td>
                    <div>
                      <g:radio name="disablePreviousAsg" id="disablePreviousAsgYes" value="Yes" checked="${deploymentWorkflowOptions.disablePreviousAsg == ProceedPreference.Yes}"/>
                      <label for="disablePreviousAsgYes" class="choice">Yes</label>
                    </div>
                    <div>
                      <g:radio name="disablePreviousAsg" id="disablePreviousAsgNo" value="No" checked="${deploymentWorkflowOptions.disablePreviousAsg == ProceedPreference.No}"/>
                      <label for="disablePreviousAsgNo" class="choice">No</label>
                    </div>
                    <div>
                      <g:radio name="disablePreviousAsg" id="disablePreviousAsgAsk" value="Ask" checked="${!deploymentWorkflowOptions.disablePreviousAsg || deploymentWorkflowOptions.disablePreviousAsg == ProceedPreference.Ask}"/>
                      <label for="disablePreviousAsgAsk" class="choice">Ask</label>
                    </div>
                  </td>
                </tr>
              </tbody>
              <tbody class="fullTrafficOptions numbers ${deploymentWorkflowOptions.disablePreviousAsg == ProceedPreference.No ? 'concealed' : ''}">
                <tr class="prop">
                  <td class="name">
                    <label for="fullTrafficAssessmentDurationMinutes">Full traffic assessment duration:</label>
                  </td>
                  <td class="value">
                    <g:textField class="required" name="fullTrafficAssessmentDurationMinutes" value="${deploymentWorkflowOptions.fullTrafficAssessmentDurationMinutes}"/>&nbsp;minutes
                  </td>
                </tr>
                <tr class="prop">
                  <td class="name">
                    Delete previous ASG after full traffic assessment:
                  </td>
                  <td>
                    <div>
                      <g:radio name="deletePreviousAsg" id="deletePreviousAsgYes" value="Yes" checked="${deploymentWorkflowOptions.deletePreviousAsg == ProceedPreference.Yes}"/>
                      <label for="deletePreviousAsgYes" class="choice">Yes</label>
                    </div>
                    <div>
                      <g:radio name="deletePreviousAsg" id="deletePreviousAsgNo" value="No" checked="${deploymentWorkflowOptions.deletePreviousAsg == ProceedPreference.No}"/>
                      <label for="deletePreviousAsgNo" class="choice">No</label>
                    </div>
                    <div>
                      <g:radio name="deletePreviousAsg" id="deletePreviousAsgAsk" value="Ask" checked="${!deploymentWorkflowOptions.deletePreviousAsg || deploymentWorkflowOptions.deletePreviousAsg == ProceedPreference.Ask}"/>
                      <label for="deletePreviousAsgAsk" class="choice">Ask</label>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
      </li>
      </g:if>
      <g:else>
        <g:hiddenField name="createAsgOnly" value="true" />
      </g:else>

      <li class="clusterAsgForm create hideAdvancedItems">
        <g:hiddenField name="name" value="${clusterName}" />
        <g:hiddenField name="noOptionalDefaults" value="true" />
        <h2>Next Group:</h2>
        <span class="toggle fakeLink" id="showAdvancedOptionsToCreateNextGroup">Advanced Options</span>
        <div class="clear"></div>
        <h2>${nextGroupName}</h2>
        <table>
          <tr class="advanced"><td colspan="2"><h2>Auto Scaling</h2></td></tr>
          <g:render template="/autoScaling/autoScalingOptions" />
          <g:render template="/loadBalancer/selection"/>
          <g:render template="/launchConfiguration/launchConfigOptions" />
          <g:if test="${!deploymentWorkflowOptions}">
            <g:render template="/push/startupOptions" />
            <tr class="advanced">
              <td>
                <label for="trafficAllowed">Enable traffic?</label>
              </td>
              <td>
                <input id="trafficAllowed" type="checkbox" name="trafficAllowed" checked="checked" />
                <label for="trafficAllowed">Send client requests to new instances</label>
              </td>
            </tr>
          </g:if>
        </table>
      </li>
    </ul>
    </div>
    <div class="buttons">
      <g:hiddenField name="clusterName" value="${clusterName}"/>
      <g:buttonSubmit class="deploy" value="deploy">Deploy '${nextGroupName}'</g:buttonSubmit>
    </div>
  </g:form>
</div>
</body>
</html>
