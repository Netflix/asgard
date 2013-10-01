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
package com.netflix.asgard.retriever

import com.amazonaws.services.ec2.model.DescribeSpotPriceHistoryRequest
import com.amazonaws.services.ec2.model.DescribeSpotPriceHistoryResult
import com.amazonaws.services.ec2.model.SpotPrice
import com.netflix.asgard.AwsEc2Service
import com.netflix.asgard.Region
import spock.lang.Specification

class AwsResultsRetrieverSpec extends Specification {

    def 'should retrieve for all tokens'() {
        AwsEc2Service service = Mock(AwsEc2Service)

        when:
        final retriever = new AwsResultsRetriever<SpotPrice, DescribeSpotPriceHistoryRequest,
                DescribeSpotPriceHistoryResult>(10, 0) {
            DescribeSpotPriceHistoryResult makeRequest(Region region, DescribeSpotPriceHistoryRequest request) {
                service.describeSpotPriceHistory(region, request)
            }
            List<SpotPrice> accessResult(DescribeSpotPriceHistoryResult result) {
                result.spotPriceHistory
            }
            void limitRetrieval(DescribeSpotPriceHistoryRequest request, int remaining) {
                request.withMaxResults(Math.min(10, remaining))
            }
        }
        DescribeSpotPriceHistoryRequest request = new DescribeSpotPriceHistoryRequest(availabilityZone: 'us-east-7')
        List<SpotPrice> actual = retriever.retrieve(Region.US_EAST_1, request)

        then:
        actual == [
                new SpotPrice(spotPrice: '1'),
                new SpotPrice(spotPrice: '2'),
                new SpotPrice(spotPrice: '3'),
                new SpotPrice(spotPrice: '4'),
                new SpotPrice(spotPrice: '5'),
                new SpotPrice(spotPrice: '6'),
                new SpotPrice(spotPrice: '7'),
                new SpotPrice(spotPrice: '8'),
                new SpotPrice(spotPrice: '9'),
        ]

        1 * service.describeSpotPriceHistory(_, new DescribeSpotPriceHistoryRequest(availabilityZone: 'us-east-7',
                maxResults: 10, nextToken: null)) >> {
            new DescribeSpotPriceHistoryResult(nextToken: 'more1', spotPriceHistory: [
                    new SpotPrice(spotPrice: '1'),
                    new SpotPrice(spotPrice: '2'),
                    new SpotPrice(spotPrice: '3'),
            ])
        }
        1 * service.describeSpotPriceHistory(_, new DescribeSpotPriceHistoryRequest(availabilityZone: 'us-east-7',
                maxResults: 7, nextToken: 'more1')) >> {
            new DescribeSpotPriceHistoryResult(nextToken: 'more2', spotPriceHistory: [
                    new SpotPrice(spotPrice: '4'),
                    new SpotPrice(spotPrice: '5'),
                    new SpotPrice(spotPrice: '6'),
            ])
        }
        1 * service.describeSpotPriceHistory(_, new DescribeSpotPriceHistoryRequest(availabilityZone: 'us-east-7',
                maxResults: 4, nextToken: 'more2')) >> {
            new DescribeSpotPriceHistoryResult(nextToken: null, spotPriceHistory: [
                    new SpotPrice(spotPrice: '7'),
                    new SpotPrice(spotPrice: '8'),
                    new SpotPrice(spotPrice: '9'),
            ])
        }
        0 * service.describeSpotPriceHistory(_, _)
    }

    def 'should retrieve only once if no tokens exist'() {
        AwsEc2Service service = Mock(AwsEc2Service)

        when:
        final retriever = new AwsResultsRetriever<SpotPrice, DescribeSpotPriceHistoryRequest,
        DescribeSpotPriceHistoryResult>(10, 0) {
            DescribeSpotPriceHistoryResult makeRequest(Region region, DescribeSpotPriceHistoryRequest request) {
                service.describeSpotPriceHistory(region, request)
            }
            List<SpotPrice> accessResult(DescribeSpotPriceHistoryResult result) {
                result.spotPriceHistory
            }
            void limitRetrieval(DescribeSpotPriceHistoryRequest request, int remaining) {
                request.withMaxResults(Math.min(10, remaining))
            }
        }
        DescribeSpotPriceHistoryRequest request = new DescribeSpotPriceHistoryRequest(availabilityZone: 'us-east-7')
        List<SpotPrice> actual = retriever.retrieve(Region.US_EAST_1, request)

        then:
        actual == [
                new SpotPrice(spotPrice: '1'),
                new SpotPrice(spotPrice: '2'),
                new SpotPrice(spotPrice: '3'),
                new SpotPrice(spotPrice: '4'),
        ]

        1 * service.describeSpotPriceHistory(_, new DescribeSpotPriceHistoryRequest(availabilityZone: 'us-east-7',
                maxResults: 10, nextToken: null)) >> {
            new DescribeSpotPriceHistoryResult(nextToken: null, spotPriceHistory: [
                    new SpotPrice(spotPrice: '1'),
                    new SpotPrice(spotPrice: '2'),
                    new SpotPrice(spotPrice: '3'),
                    new SpotPrice(spotPrice: '4'),
            ])
        }
        0 * service.describeSpotPriceHistory(_, _)
    }

    def 'should retrieve up to limit'() {
        AwsEc2Service service = Mock(AwsEc2Service)

        when:
        final retriever = new AwsResultsRetriever<SpotPrice, DescribeSpotPriceHistoryRequest,
        DescribeSpotPriceHistoryResult>(5, 0) {
            DescribeSpotPriceHistoryResult makeRequest(Region region, DescribeSpotPriceHistoryRequest request) {
                service.describeSpotPriceHistory(region, request)
            }
            List<SpotPrice> accessResult(DescribeSpotPriceHistoryResult result) {
                result.spotPriceHistory
            }
            void limitRetrieval(DescribeSpotPriceHistoryRequest request, int remaining) {
                request.withMaxResults(Math.min(3, remaining))
            }
        }
        DescribeSpotPriceHistoryRequest request = new DescribeSpotPriceHistoryRequest(availabilityZone: 'us-east-7')
        List<SpotPrice> actual = retriever.retrieve(Region.US_EAST_1, request)

        then:
        actual == [
                new SpotPrice(spotPrice: '1'),
                new SpotPrice(spotPrice: '2'),
                new SpotPrice(spotPrice: '3'),
                new SpotPrice(spotPrice: '4'),
                new SpotPrice(spotPrice: '5'),
        ]

        1 * service.describeSpotPriceHistory(_, new DescribeSpotPriceHistoryRequest(availabilityZone: 'us-east-7',
                maxResults: 3, nextToken: null)) >> {
            new DescribeSpotPriceHistoryResult(nextToken: 'more1', spotPriceHistory: [
                    new SpotPrice(spotPrice: '1'),
                    new SpotPrice(spotPrice: '2'),
                    new SpotPrice(spotPrice: '3'),
            ])
        }
        1 * service.describeSpotPriceHistory(_, new DescribeSpotPriceHistoryRequest(availabilityZone: 'us-east-7',
                maxResults: 2, nextToken: 'more1')) >> {
            new DescribeSpotPriceHistoryResult(nextToken: 'more2', spotPriceHistory: [
                    new SpotPrice(spotPrice: '4'),
                    new SpotPrice(spotPrice: '5'),
            ])
        }
        0 * service.describeSpotPriceHistory(_, _)
    }

    def 'should not enforce limit if limitRetrieval is not implemented'() {
        AwsEc2Service service = Mock(AwsEc2Service)

        when:
        final retriever = new AwsResultsRetriever<SpotPrice, DescribeSpotPriceHistoryRequest,
        DescribeSpotPriceHistoryResult>(5, 0) {
            DescribeSpotPriceHistoryResult makeRequest(Region region, DescribeSpotPriceHistoryRequest request) {
                service.describeSpotPriceHistory(region, request)
            }
            List<SpotPrice> accessResult(DescribeSpotPriceHistoryResult result) {
                result.spotPriceHistory
            }
        }
        DescribeSpotPriceHistoryRequest request = new DescribeSpotPriceHistoryRequest(availabilityZone: 'us-east-7')
        List<SpotPrice> actual = retriever.retrieve(Region.US_EAST_1, request)

        then:
        actual == [
                new SpotPrice(spotPrice: '1'),
                new SpotPrice(spotPrice: '2'),
                new SpotPrice(spotPrice: '3'),
                new SpotPrice(spotPrice: '4'),
                new SpotPrice(spotPrice: '5'),
        ]

        1 * service.describeSpotPriceHistory(_, new DescribeSpotPriceHistoryRequest(availabilityZone: 'us-east-7',
                nextToken: null)) >> {
            new DescribeSpotPriceHistoryResult(nextToken: 'more1', spotPriceHistory: [
                    new SpotPrice(spotPrice: '1'),
                    new SpotPrice(spotPrice: '2'),
                    new SpotPrice(spotPrice: '3'),
            ])
        }
        1 * service.describeSpotPriceHistory(_, new DescribeSpotPriceHistoryRequest(availabilityZone: 'us-east-7',
                nextToken: 'more1')) >> {
            new DescribeSpotPriceHistoryResult(nextToken: 'more2', spotPriceHistory: [
                    new SpotPrice(spotPrice: '4'),
                    new SpotPrice(spotPrice: '5'),
            ])
        }
        0 * service.describeSpotPriceHistory(_, _)
    }

}
