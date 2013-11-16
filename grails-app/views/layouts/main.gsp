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
<%@ page import="grails.converters.JSON; com.netflix.asgard.push.GroupResizeOperation" %>
<!DOCTYPE html>
<html>
<head>
  <title><g:layoutTitle default="Asgard ${env}"/></title>
  <meta http-equiv="X-UA-Compatible" content="chrome=1">
  <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
  <link rel="stylesheet" href="${resource(dir: 'css', file: 'main.css')}?v=${build}"/>
  <link rel="stylesheet" href="${resource(dir: 'js/select2-3.2', file: 'select2.css')}?v=${build}"/>
  <!--[if IE]>
    <link rel="stylesheet" href="${resource(dir: 'css', file: 'ie.css')}?v=${build}"/>
  <![endif]-->
  <link rel="shortcut icon" href="${resource(dir: '/', file: 'favicon.ico')}" type="image/x-icon"/>
  <g:layoutHead/>
</head>
<body class="${envStyle} ${occasion.styleClass}">
<g:if test="${ieWithoutChromeFrame}">
<!--[if IE]>
  <script type="text/javascript" src="${resource(dir: 'js', file: 'chromeframeinstall.js')}?v=${build}"></script>
  <div class="messageForIeUsers">
  Please use Chrome, Firefox, or Safari. IE is not supported. If you must use IE then install Google Chrome Frame.
  </div>
<![endif]-->
</g:if>
  <div id="spinner" class="spinner" style="display:none;">
    <img src="${resource(dir: 'images', file: 'spinner.gif')}" alt="Spinner"/>
  </div>
  <div class="titlebar">
    <div class="header">
      <a href="${resource(dir: '/')}">
        <img id="occasionIcon" class="logo" title="${occasion.message}" src="${resource(dir: 'images/occasion', file: occasion?.iconFileName)}"/>
        <div class="mainHeader">Asgard</div>
      </a>
      <span>${env}</span>
    </div>
    <g:if test="${!pageProperty(name: 'meta.hideNav')}">
      <div class="region">
        <form action="/" method="GET">
          <g:select title="Switch to a different Amazon region" name="region" class="noSelect2" id="regionSwitcher"
                    value="${region.code}" from="${regions}" optionKey="code" optionValue="description"/><br/>
          <img src="${resource(dir: 'images/worldmap', file: region.mapImageFileName)}" title="${region.description} is your current region"/>
        </form>
      </div>
      <div class="ticket" title="${fullTicketLabel} number for logging change actions.">
        <input type="text" name="ticket" placeholder="${ticketLabel}" id="ticketNumber" title="${fullTicketLabel} number" class="${ticketRequired ? 'required' : 'optional'}"/>
      </div>
      <g:if test="${authenticationEnabled}">
        <shiro:isLoggedIn>
          <div class="authentication">
            Logged in as <shiro:principal/>
            <ul>
              <g:if test="${apiTokenEnabled}">
                <li><g:link controller="apiToken" action="create">Generate API Token</g:link></li>
              </g:if>
              <li><g:link controller="auth" action="signOut" params="${[targetUri: targetUri]}">Logout</g:link></li>
            </ul>
          </div>
        </shiro:isLoggedIn>
        <shiro:isNotLoggedIn>
          <g:link controller="auth" action="login" class="login" params="${[targetUri: targetUri]}">Login</g:link>
        </shiro:isNotLoggedIn>
      </g:if>
      <div class="search" title="Find entities by name">
        <form action="/search" method="GET" class="allowEnterKeySubmit">
          %{--<input type="search" results="10" autosave="asgard${env}globalsearch" name="q" placeholder="Global search by names" value="${params.q}">--}%
        </form>
      </div>
    </g:if>
  </div>
  <g:if test="${!pageProperty(name: 'meta.hideNav')}">
   <ul class="nav">
     <li class="menuButton"><a class="home" href="${resource(dir: '/')}">Home</a></li>
     <li class="menuButton">
       <g:link class="applications" controller="application" action="list">App</g:link>
       <ul>
         <li class="menuButton"><g:link class="applications" controller="application" action="list">Applications</g:link></li>
         <li class="menuButton"><g:link class="stacks" controller="stack" action="list">Stacks</g:link></li>
         <li class="menuButton"><g:link class="users" controller="application" action="owner">Owners</g:link></li>
         <li class="menuButton"><g:link class="securityGroups" controller="security" action="list">Security Groups</g:link></li>
         <g:extLinkToPropertiesConsole />
       </ul>
     </li>
     <li class="menuButton"><g:link class="images" controller="image" action="list">AMI</g:link></li>
     <li class="menuButton">
       <g:link class="cluster" controller="cluster" action="list">Cluster</g:link>
       <ul>
         <li class="menuButton"><g:link class="cluster" controller="cluster" action="list">Clusters</g:link></li>
         <li class="menuButton"><g:link class="autoScaling" controller="autoScaling" action="list">Auto Scaling Groups</g:link></li>
         <li class="menuButton"><g:link class="launchConfigs" controller="launchConfiguration" action="list">Launch Configurations</g:link></li>
         <li class="menuButton"><g:link class="scalingPolicy" controller="scalingPolicy" action="list">Scaling Policies</g:link></li>
         <li class="menuButton"><g:link class="scheduledAction" controller="scheduledAction" action="list">Scheduled Actions</g:link></li>
         <li class="menuButton"><g:link class="alarm" controller="alarm" action="list">CloudWatch Alarms</g:link></li>
       </ul>
     </li>
     <li class="menuButton"><g:link class="loadBalancers" controller="loadBalancer" action="list">ELB</g:link></li>
     <li class="menuButton">
         <g:link class="instances" controller="instance" action="${discoveryExists ? 'apps' : 'list'}">EC2</g:link>
         <ul>
           <li class="menuButton"><g:link class="instances" controller="instance" action="${discoveryExists ? 'apps' : 'list'}">Instances</g:link></li>
           <li class="menuButton"><g:link class="instanceTypes" controller="instanceType" action="list">Instance Types</g:link></li>
           <g:if test="${spotInstancesAreAppropriate}">
             <li class="menuButton"><g:link class="spotInstanceRequest" controller="spotInstanceRequest" action="list">Spot Instance Requests</g:link></li>
           </g:if>
           <li class="menuButton"><g:link class="volumes" controller="volume" action="list">EBS Volumes</g:link></li>
           <li class="menuButton"><g:link class="volumeSnapshot" controller="snapshot" action="list">EBS Snapshots</g:link></li>
         </ul>
     </li>
     <li class="menuButton">
       <g:link class="simpleDb" controller="domain" action="list">SDB</g:link>
     </li>
     <li class="menuButton"><g:link class="topic" controller="topic" action="list">SNS</g:link></li>
     <li class="menuButton"><g:link class="queue" controller="queue" action="list">SQS</g:link></li>
     <li class="menuButton">
       <g:link class="rdsInstances" controller="rdsInstance" action="list">RDS</g:link>
       <ul>
         <li class="menuButton"><g:link class="rdsInstances" controller="rdsInstance" action="list">DB Instances</g:link></li>
         <li class="menuButton"><g:link class="dbSecurity" controller="dbSecurity" action="list">DB Security</g:link></li>
         <li class="menuButton"><g:link class="dbSnapshot" controller="dbSnapshot" action="list">DB Snapshots</g:link></li>
       </ul>
     </li>
     <li class="menuButton"><g:link class="tasks" controller="task" action="list">Task</g:link></li>
   </ul>
  </g:if>
  <div class="clear"></div>
  <g:if test="${bleskDataUrl}">
    <div id="blesk" data-appid="asgard" data-server="${bleskDataUrl}" data-context-env="${env}" data-context-region="${region.code}"></div>
  </g:if>
  <g:layoutBody/>
  <script type="text/javascript">
    window.browserGlobalsFromServer = {
      groupResizeDefaultBatchSize: ${GroupResizeOperation.DEFAULT_BATCH_SIZE},
      params: ${params as JSON},
      region: '${region.code}',
      requireLoginForEdit: ${requireLoginForEdit}
    };
  </script>
  <script type="text/javascript" src="${resource(dir: 'js', file: 'jquery.js')}?v=${build}"></script>
  <script defer type="text/javascript" src="${resource(dir: 'js/select2-3.2', file: 'select2.min.js')}?v=${build}"></script>
  <script defer type="text/javascript" src="${resource(dir: 'js', file: 'custom.js')}?v=${build}"></script>
  <g:if test="${bleskJavaScriptUrl}">
    <script defer type="text/javascript" src="${bleskJavaScriptUrl}?v=${build}"></script>
  </g:if>
  <g:render template="/layouts/occasion"/>
</body>
</html>
