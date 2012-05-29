package com.netflix.asgard

import com.netflix.asgard.plugin.UserDataProvider
import javax.xml.bind.DatatypeConverter
import org.springframework.beans.factory.annotation.Autowired

class DefaultUserDataProvider implements UserDataProvider {

    @Autowired
    ConfigService configService

    @Autowired
    ApplicationService applicationService

    String buildUserDataForVariables(UserContext userContext, String appName, String autoScalingGroupName,
            String launchConfigName) {
        Names names = Relationships.dissectCompoundName(autoScalingGroupName)
        String result = exportVar('ENVIRONMENT', configService.accountName) +
            exportVar('MONITOR_BUCKET', applicationService.getMonitorBucket(userContext, appName, names.cluster)) +
            exportVar('APP', appName) +
            exportVar('STACK', names.stack) +
            exportVar('CLUSTER', names.cluster) +
            exportVar('AUTO_SCALE_GROUP', autoScalingGroupName) +
            exportVar('LAUNCH_CONFIG', launchConfigName) +
            exportVar('EC2_REGION', userContext.region.code, false)
        List<String> additionalEnvVars = names.labeledEnvironmentVariables(configService.userDataVarPrefix)
        result += additionalEnvVars ? additionalEnvVars.join('\n') : ''
        DatatypeConverter.printBase64Binary(result.bytes)
    }

    private String exportVar(String name, String val, boolean includePrefix = true) {
        "export ${includePrefix ? configService.userDataVarPrefix : ''}${name}=${val}\n"
    }
}
