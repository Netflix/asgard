package com.netflix.asgard

import org.springframework.beans.factory.InitializingBean

class EndpointService implements InitializingBean {
    static transactional = false

    def grailsApplication

    private def providers
    void afterPropertiesSet() {
        providers = [
                'aws': [
                        AmazonAutoScaling: { Region region -> "autoscaling.${region}.amazonaws.com" },
                        AmazonCloudWatch: { Region region -> "monitoring.${region}.amazonaws.com" },
                        AmazonEC2: { Region region -> "ec2.${region}.amazonaws.com" },
                        AmazonElasticLoadBalancing: { Region region -> "elasticloadbalancing.${region}.amazonaws.com" },
                        AmazonRDS: { Region region -> "rds.${region}.amazonaws.com" },
                        // Unconventional S3 endpoints. http://docs.amazonwebservices.com/general/latest/gr/index.html?rande.html
                        AmazonS3: { Region region -> region != Region.US_EAST_1 ? "s3-${region}.amazonaws.com" : "s3.amazonaws.com" },
                        // Unconventional SDB endpoints. http://docs.amazonwebservices.com/general/latest/gr/index.html?rande.html
                        AmazonSimpleDB: { Region region -> region != Region.US_EAST_1 ? "sdb.${region}.amazonaws.com" : "sdb.amazonaws.com" },
                        AmazonSNS: { Region region -> "sns.${region}.amazonaws.com" },
                        AmazonSQS: { Region region -> "sqs.${region}.amazonaws.com" }
                ] as Map<String, Closure>,
                'eucauser': createEucalyptusEndpoints(),//GRZE:HACK: temporary!
                'eucalyptus': createEucalyptusEndpoints()
        ]
    }

    String getEndpoint(Region region, Class serviceInterfaceClass) {
        Set<String> useAws = grailsApplication.config.grails?."${region.provider}"?.useAwsServices ?: []
        if(useAws.contains(serviceInterfaceClass.simpleName)){
            return null;
        } else {
            def endpointSupplier = providers[region.provider]?.get(serviceInterfaceClass.simpleName) ?: null
            endpointSupplier?.call(region)
        }
    }

    private Map<String, Closure> createEucalyptusEndpoints() {
        //GRZE:NOTE: this is a temporary hack related to the region enum fwiw.
        def configHostName = grailsApplication.config.grails?.eucalyptus?.hostName
        [
                AmazonAutoScaling: { Region region, String hostName -> "http://${hostName}:8773/services/AutoScaling" }.rcurry(configHostName),
                AmazonCloudWatch: { Region region, String hostName -> "http://${hostName}:8773/services/CloudWatch" }.rcurry(configHostName),
                AmazonEC2: { Region region, String hostName -> "http://${hostName}:8773/services/Eucalyptus/" }.rcurry(configHostName),
                AmazonElasticLoadBalancing: { Region region, String hostName -> "http://${hostName}:8773/services/LoadBalancing" }.rcurry(configHostName),
                AmazonS3: { Region region, String hostName -> "http://${hostName}:8773/services/Walrus" }.rcurry(configHostName),
        ] as Map<String, Closure>
    }

}
