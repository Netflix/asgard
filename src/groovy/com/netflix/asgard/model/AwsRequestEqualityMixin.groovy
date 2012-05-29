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
package com.netflix.asgard.model

import com.amazonaws.AmazonWebServiceRequest
import org.apache.commons.lang.builder.EqualsBuilder
import org.apache.commons.lang.builder.HashCodeBuilder
import org.apache.commons.lang.builder.ToStringBuilder

@Category(AmazonWebServiceRequest)
class AwsRequestEqualityMixin {

    public static final Set<String> ignoredFields = [
        'requestClientOptions',  // This field is an implementation detail that causes inequality where it should not.
    ]

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj, ignoredFields);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this, ignoredFields);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}
