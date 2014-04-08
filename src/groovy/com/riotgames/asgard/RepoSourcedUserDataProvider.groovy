package com.riotgames.asgard

import com.netflix.asgard.ConfigService
import com.netflix.asgard.Relationships
import com.netflix.asgard.model.LaunchContext
import com.netflix.asgard.plugin.AdvancedUserDataProvider
import org.apache.http.*
import org.apache.http.client.methods.*
import org.apache.http.impl.client.*
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.InitializingBean
import org.yaml.snakeyaml.Yaml

import javax.mail.*
import javax.mail.internet.*
import java.nio.charset.Charset
import java.util.regex.Pattern

/**
 * Implementation of AdvancedUserDataProvider that gathers input from Github or other repos using REST APIs and
 * combines the results into a single user-data blob String.
 */
class RepoSourcedUserDataProvider implements AdvancedUserDataProvider, InitializingBean {

    @Autowired
    GrailsApplication grailsApplication

    @Autowired
    ConfigService configService

    Repo repo
    String[] formulaPaths

    /**
     * Initializes a new RepoSourcedUserDataProvider that talks to the grailsApplication to get
     * its configuration
     */
    void afterPropertiesSet() {
        def config = grailsApplication.config.cloud.repoSourcedUserData
        repo = new Repo(config)
        formulaPaths = config.formulaPaths ?: []
    }

    /**
     * A REST-based text resource repo who's URL pattern can be defined with 3 fixed strings, and a dynamic path
     * argument.
     */
    static class Repo {
        final String base
        final String suffix
        final String accept

        Repo(String base, String suffix, String accept) {
            this.base = base
            this.suffix = suffix
            this.accept = accept
        }

        Repo(config) {
            this(config.base ?: '', config.suffix ?: '', config.accept ?: 'application/vnd.github.VERSION.raw')
        }

        /**
         * Retrieves a (UTF8) text file from this repo given a relative resource path and using the configured custom
         * Accept header configured.
         *
         * The latter allow use of Github's Custom Media Types to simplify parsing
         * @see http://developer.github.com/v3/media/
         *
         * @param reop  a Repo spec
         * @param path  a URL path
         * @return  the contents of the text file @path, or null on any error.
         */
        String retrieveText(String path) {
            String url = "${base}${path}${suffix}"

            DefaultHttpClient httpclient = new DefaultHttpClient()
            HttpGet httpGet = new HttpGet(url)
            if (accept) {
                httpGet.addHeader('Accept', accept)
            }

            HttpResponse response = httpclient.execute(httpGet)
            if (response.statusLine.statusCode < 300) {
                HttpEntity he = response.entity
                ByteArrayOutputStream baos = new ByteArrayOutputStream()
                he.writeTo(baos)
                return new String(baos.toByteArray(), Charset.forName('UTF-8'))
            }
            return null
        }
    }

    /**
     * Encapsulation of a pattern=>value map for substituting ${var} variables in text.
     */
    static class Variables {
        final Map<Pattern,String> variables

        /**
         * Precompiled set of variable patterns, all in the full ${} form.
         */
        static final Pattern ACCOUNT_PAT = ~/\$\{account\}/
        static final Pattern REGION_PAT = ~/\$\{region\}/
        static final Pattern ENV_PAT = ~/\$\{env\}/
        static final Pattern STACK_PAT = ~/\$\{stack\}/
        static final Pattern APP_PAT = ~/\$\{app\}/
        static final Pattern CLUSTER_PAT = ~/\$\{cluster\}/
        static final Pattern GROUP_PAT = ~/\$\{group\}/
        static final Pattern EUREKA_PAT = ~/\$\{eureka\}/
        static final Pattern VARPREFIX_PAT = ~/\$\{varPrefix\}/

        /**
         * Populates a map for use by #substituteVariables and that contains a standard set of variable patterns,
         * and values from LaunchContext data. Note that autoScalingGroup and application in launchContext will be null
         * when launching is done for a single instance, and not in an ASG.
         *
         * @param launchContext  the context to extract interesting variables from
         * @return  a map of regex Pattern => String for substituting variables with values.
         */
        Variables(ConfigService configService, LaunchContext launchContext) {
            // NOTE(CQ): maybe use configService.envStyle as the default?
            String envName = splitTail(configService.accountName, '-')
            String groupName = launchContext.autoScalingGroup?.autoScalingGroupName  // null on single instance launch
            String appName = launchContext.application?.name  // null on single instance launch
            variables = [
                (ACCOUNT_PAT) : configService.accountName,
                (REGION_PAT) : launchContext.userContext.region.code,
                (ENV_PAT) : envName,
                (STACK_PAT) : groupName ? Relationships.stackNameFromGroupName(groupName) : '',
                (APP_PAT) : appName?: '',
                (CLUSTER_PAT) : groupName ? Relationships.clusterFromGroupName(groupName) : '',
                (GROUP_PAT) : groupName?: '',
                (EUREKA_PAT) : configService.getRegionalDiscoveryServer(launchContext.userContext.region),
                (VARPREFIX_PAT) : configService.userDataVarPrefix,
            ]
        }

        /**
         * Substitutes variables in a template string and return the populated result.
         *
         * @param template  String containing embedded variables tagged with ${var}
         * @return  the substituted string
         */
        String substituted(String template) {
            String result = template
            if (result) {
                variables.each { varpat,val ->
                    result = result.replaceAll(varpat, val)
                }
            }
            result
        }
    }

    /**
     * Returns the tail part of an input string split using a given delimeter, or the whole input if there are no
     * delimeters therein.
     */
    static String splitTail(String input, String delim) {
        String[] parts = input.split(delim)
        parts && parts.size() > 0 ? parts[parts.size() - 1] : ''
    }

    /**
     * A transformable formula for creating UserData. Defined in YAML, and then transformed and expanded
     * to yield the final representation which can then be assembled into a UserData string.
     */
    static class Formula {
        final formula

        /**
         * Retrieves a formula in YAML from from a repo, and returns a parsed Formula instance iff found and valid,
         * null if not.
         */
        static Formula fromRepo(Repo repo, String path) {
            fromYaml(repo.retrieveText(path))
        }

        /**
         * Returns a parsed Formula instance from text iff it contains a valid formula definition,
         * null if not.
         */
        static Formula fromYaml(String yamlText) {
            if (yamlText) {
                Object yaml = new Yaml().load(yamlText)
                if (yaml?.parts) {
                    return new Formula(yaml)
                }
            }
            null
        }

        private Formula(Object formula) {
            this.formula = formula
        }

        /**
         * Substitutes variables in the the source and target paths.
         */
        void substituteVariables(Variables variables) {
            formula.parts.each { part ->
                part.source = variables.substituted(part.source)
                part.target = variables.substituted(part.target)
                if (part.target.endsWith('/')) {
                    part.target += splitTail(part.source, '/')
                    // reuse the name part of the source if target ends with /
                }
            }
        }

        /**
         * Retrieves source blobs from the repo, and substitute vars as indicated.
         */
        void retrieveBlobs(Repo repo, Variables variables) {
            formula.parts.each { part ->
                part.blob = repo.retrieveText(part.source) // stuff the blob in the part object
                if (part.subst) {
                    part.blob = variables.substituted(part.blob)
                }
            }
        }

        /**
         * Assembles this trasnformed formula into a UserData string in the indicated format.
         */
        String assembleUserData() {
            String userData
            switch (formula.format) {  // the user-data output format: shell, cloudinit
                case 'shell':
                    userData = assembleShellUserData(formula.parts)
                    break
                case 'cloudinit':
                    userData = assembleCloudInitUserData(formula.parts)
                    break
                default:
                    userData = null
            }
            userData
        }

        /**
         * Uses all of the gathered source information to assemble a shellscript format user-data.
         */
        private String assembleShellUserData(List<Object> parts) {
            StringBuilder udf = new StringBuilder()
            parts.each { part ->
                if (part.blob) {
                    switch (part.kind) {
                        case 'copyTo':      // script fragment to write a file
                            udf.append("cat > ${part.target} <<__H3R3__\n")
                            udf.append(part.blob)
                            udf.append("__H3R3__\n")
                            break
                        case 'shell':      // just put the part right in the script
                            udf.append(part.blob)
                            break
                        default:
                            // error?
                            break
                    }
                }
            }
            udf.toString()
        }

        /**
         * Uses all of the gathered source information to assemble a cloud-init multipart-MIME format user-data.
         *
         * @See: http://cloudinit.readthedocs.org/en/latest/topics/format.html
         */
        private String assembleCloudInitUserData(List<Object> parts) {
            def msg = new MimeMessage(new Session(new Properties(), null))
            def multipart = new MimeMultipart()
            parts.each { part ->
                // Mime multipart each part
                if (part.blob) {
                    def bodyPart = new MimeBodyPart()
                    switch (part.kind) {
                        // User-Data Script
                        // Begins with: #! or Content-Type: text/x-shellscript when using a MIME archive.
                        case 'shellscript':
                            bodyPart.setContent(part.blob, 'text/x-shellscript')
                            break

                        // Upstart Job
                        // Begins with: #upstart-job or Content-Type: text/upstart-job when using a MIME archive.
                        case 'upstart-job':
                            bodyPart.setContent(part.blob, 'text/upstart-job')
                            break

                        // Cloud Boothook
                        // Begins with: #cloud-boothook or Content-Type: text/cloud-boothook when using a MIME archive.
                        case 'cloud-boothook':
                            bodyPart.setContent(part.blob, 'text/cloud-boothook')
                            break

                        // Cloud-Config
                        // Begins with: #cloud-config or Content-Type: text/cloud-config when using a MIME archive.
                        case 'cloud-config':
                            bodyPart.setContent(part.blob, 'text/cloud-config')
                            break

                        // NOTE(CQ): how to sub-catagorize these cloud-config sub-parts...?

                        // write_files
                        case 'write-files':
                            String cc = 'write_files:\n  -   content: ' + Base64Codec.encodeAsBase64(part.blob)
                            bodyPart.setContent(cc, 'text/cloud-config')
                            break

                        // ca-certs ?
                        // users ?
                        // groups ?

                        default:
                            break
                    }
                    multipart.addBodyPart(bodyPart)
                }
            }
            msg.content = multipart
            msg.saveChanges()
            ByteArrayOutputStream baos = new ByteArrayOutputStream()
            msg.writeTo(baos, (String[])['Message-Id'])
            baos.toString()

            // NOTE(CQ): could/should gzip+base64 encode here.
        }
    }

    /*
     *
     */
    @Override
    String buildUserData(LaunchContext launchContext) {

        // Build the variable substitution map with LaunchContext-specific values
        Variables variables = new Variables(configService, launchContext)

        // Scans the formulaPaths, substituting vars in each, and retrieves and parses the first one found.
        Formula formula = formulaPaths.findResult { path ->
            Formula.fromRepo(repo, variables.substituted(path))
        }
        if (formula) {
            // Substitute the formula vars and retrieve any referenced blobs
            formula.substituteVariables(variables)
            formula.retrieveBlobs(repo, variables)

            // Put it all together into our final user data
            return formula.assembleUserData()
        } else {
            return ''
        }
    }

}
