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
<label for="protocol${index}">Protocol:</label>
<g:select name="protocol${index}" value="${protocol}" from="${protocols}" noSelection="['':'']" />
<label for="lbPort${index}">Load Balancer Port:</label><g:textField name="lbPort${index}" value="${lbPort}"/>
<label for="instancePort${index}">Instance Port:</label><g:textField name="instancePort${index}" value="${instancePort}"/>