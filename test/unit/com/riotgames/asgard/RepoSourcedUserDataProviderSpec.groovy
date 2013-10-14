package com.riotgames.asgard

import com.netflix.asgard.AppRegistration
import com.netflix.asgard.ConfigService
import com.netflix.asgard.Region
import com.netflix.asgard.UserContext
import com.netflix.asgard.mock.Mocks
import com.netflix.asgard.model.AutoScalingGroupBeanOptions
import com.netflix.asgard.model.LaunchContext
import org.yaml.snakeyaml.Yaml

import static RepoSourcedUserDataProvider.*
import spock.lang.Specification

import java.util.regex.Pattern

/**
 *
 */
@SuppressWarnings("GroovyAssignabilityCheck")
class RepoSourcedUserDataProviderSpec extends Specification {

    def grailsApplication
    ConfigService configService
    LaunchContext launchContext

    static String GHE_BASE = 'https://gh.riotgames.com/api/v3/repos/rcloud/configs/contents/'
    static String GHE_SUFFIX = '?access_token=0572f28db13e93433f68394fa0c3ef02b98191a6'

    static String GHP_BASE = 'https://api.github.com/repos/cquinn/configs/contents/'
    static String GHP_SUFFIX = '?access_token=75a3f4c58b6dafb30bed24a21b8edcc0e9892d84'

    static String GH_ACCEPT = 'application/vnd.github.VERSION.raw'

    static String GH_BASE = GHE_BASE
    static String GH_SUFFIX = GHE_SUFFIX

    static String FORMULA_PATH = 'formula/shell.yaml'

    void setup() {

        def repoSourcedUserData = [
            'base' : GHE_BASE,
            'suffix' : GHE_SUFFIX,
            'accept' : GH_ACCEPT,
            'formulaPath' : FORMULA_PATH,
        ]

        grailsApplication = Mocks.grailsApplication()
        grailsApplication.config.cloud.put('repoSourcedUserData', repoSourcedUserData)

        configService = Mock(ConfigService) {
            getAccountName() >> 'rcloud-test'
            getEnvStyle() >> 'test'
            getUserDataVarPrefix() >> 'CLOUD_'
            getRegionalDiscoveryServer(_) >> 'https://eureka.my.com/eureka:8080'
        }

        launchContext = new LaunchContext(userContext: UserContext.auto(Region.US_WEST_2))
        launchContext.application = new AppRegistration(name: 'hello')
        launchContext.autoScalingGroup = new AutoScalingGroupBeanOptions(autoScalingGroupName: 'hello-dev-v001')
    }

    RepoSourcedUserDataProvider makeProvider() {
        RepoSourcedUserDataProvider repoSourcedUserDataProvider =
            new RepoSourcedUserDataProvider(grailsApplication: grailsApplication, configService: configService)
        repoSourcedUserDataProvider.afterPropertiesSet()
        repoSourcedUserDataProvider
    }

    def 'Variables should make variable map'() {
        Variables vars = new Variables(configService, launchContext)

        //println variables
        expect:
        vars.variables != null
        vars.variables.size() == 9
        vars.variables[Variables.ACCOUNT_PAT] == 'rcloud-test'
        vars.variables[Variables.REGION_PAT] == 'us-west-2'
        vars.variables[Variables.ENV_PAT] == 'test'
        vars.variables[Variables.STACK_PAT] == 'dev'
        vars.variables[Variables.APP_PAT] == 'hello'
        vars.variables[Variables.CLUSTER_PAT] == 'hello-dev'
        vars.variables[Variables.GROUP_PAT] == 'hello-dev-v001'
        vars.variables[Variables.EUREKA_PAT] == 'https://eureka.my.com/eureka:8080'
        vars.variables[Variables.VARPREFIX_PAT] == 'CLOUD_'
    }

    def 'Variables should substitute variables'() {
        Variables variables = new Variables(configService, launchContext)
        String tpl = '${account}/${region}/${env}/${stack}/${app}/${cluster}/${group}'
        String sub = variables.substituted(tpl)
        //println "$tpl => $sub"
        expect:
        sub == 'rcloud-test/us-west-2/test/dev/hello/hello-dev/hello-dev-v001'
    }

    //CQ: more tests here for Repo,

    def 'Repo should do something'() {

    }

    def 'Provider should set itself up'() {
        RepoSourcedUserDataProvider repoSourcedUserDataProvider = makeProvider()

        expect:
        repoSourcedUserDataProvider.repo.base == GHE_BASE
        repoSourcedUserDataProvider.repo.suffix == GHE_SUFFIX
        repoSourcedUserDataProvider.repo.accept == GH_ACCEPT
        repoSourcedUserDataProvider.formulaPath == FORMULA_PATH
    }

    def 'Formula should assemble shell user data'() {
        String formulaYaml = '''
format: shell
parts:
  - source: 'base/global.properties'
    kind: shell
    blob: cat dog
'''
        Formula formula = Formula.fromYaml(formulaYaml)
        String ud = formula.assembleUserData()
        //println ud

        expect:
        ud == '''cat dog'''
    }

    def 'Formula should assemble cloudinit user data'() {
        String formulaYaml = '''
format: cloudinit
parts:
  - source: 'base/global.properties'
    kind: shellscript
    blob: cat dog
  - source: 'base/global.properties'
    kind: shellscript
    blob: dig dug
'''
        Formula formula = Formula.fromYaml(formulaYaml)
        String ud = formula.assembleUserData()
        //println ud

        expect:
        ud.contains 'MIME-Version: 1.0'
        ud.contains 'Content-Type: multipart/mixed'
        ud.contains 'boundary='
        ud.contains 'Content-Type: text/x-shellscript'
        ud.contains 'cat dog'
        ud.contains 'dig dug'
    }

    //----------
    // These two tests are not unit tests, but are still very useful.
    //----------

    def 'Repo should retrieve simple file from public github'() {
        Repo repo = new Repo('https://api.github.com/repos/Netflix/asgard/contents/','', GH_ACCEPT)
        String f = repo.retrieveText('grailsw')
        //println f
        expect:
        f != null
        f.length() > 0
    }

    def 'should build complete user data'() {
        RepoSourcedUserDataProvider repoSourcedUserDataProvider = makeProvider()
        String ud = repoSourcedUserDataProvider.buildUserData(launchContext)
        println ud
        expect:
        ud != null
        ud.length() > 0
    }
}
