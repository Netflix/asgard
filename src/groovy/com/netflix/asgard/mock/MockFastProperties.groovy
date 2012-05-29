/*
 * Copyright 2012 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.asgard.mock

class MockFastProperties {

    static final String DATA = '''<persistedproperties>
  <properties>
    <property>
      <propertyId>netflix.epic.plugin.limits.maxInstance|akms|test||||</propertyId>
      <key>netflix.epic.plugin.limits.maxInstance</key>
      <value>2000</value>
      <env>test</env>
      <appId>akms</appId>
      <updatedBy>tjones</updatedBy>
      <sourceOfUpdate>nac</sourceOfUpdate>
      <ts>2011-10-19T17:17:44.757Z</ts>
    </property>
    <property>
      <propertyId>greeting.language|helloworld|test||||</propertyId>
      <key>greeting.language</key>
      <value>martian</value>
      <env>test</env>
      <appId>helloworld</appId>
      <countries></countries>
      <updatedBy>wsmith</updatedBy>
      <stack></stack>
      <region></region>
      <sourceOfUpdate>nac</sourceOfUpdate>
      <cmcTicket></cmcTicket>
      <ts>2011-12-22T22:39:41.012Z</ts>
    </property>
  </properties>
</persistedproperties>
'''
}
