/**
* Configure the SAML plugin
**/

import java.util.logging.Logger

import jenkins.model.Jenkins
import hudson.util.Secret
import org.jenkinsci.plugins.saml.SamlSecurityRealm
import org.jenkinsci.plugins.saml.SamlAdvancedConfiguration
import org.jenkinsci.plugins.saml.SamlEncryptionData
import org.jenkinsci.plugins.saml.IdpMetadataConfiguration

@Grapes([
    @Grab(group='org.yaml', module='snakeyaml', version='1.17')
])
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor

Logger logger = Logger.getLogger("")
Jenkins jenkins = Jenkins.getInstance()
Yaml yaml = new Yaml(new SafeConstructor())

String configPath = System.getenv("JENKINS_CONFIG_PATH")
try {
    configText = new File("${configPath}/saml_config.yml").text
} catch (FileNotFoundException e) {
    logger.severe("Cannot find config file path @ ${configPath}/saml_config.yml")
    jenkins.doSafeExit(null)
    System.exit(1)
}

samlConfigs = yaml.load(configText)

IdpMetadataConfiguration idpMetadata = new IdpMetadataConfiguration(samlConfigs.IDP_METADATA_PATH)
int maximumAuthenticationLifetime = samlConfigs.MAX_AUTH_LIFETIME.toInteger()

if (!samlConfigs.ADVANCED_CONFIGURATION.isEmpty()) {
    advancedConfiguration = new SamlAdvancedConfiguration(
        samlConfigs.ADVANCED_CONFIGURATION.FORCE_AUTH,
        samlConfigs.ADVANCED_CONFIGURATION.CONTEXT_CLASS_REF,
        samlConfigs.ADVANCED_CONFIGURATION.ENTITY_ID,
        samlConfigs.ADVANCED_CONFIGURATION.MAXIMUM_SESSION_LIFETIME
    )
} else {
    advancedConfiguration = null
}

if (!samlConfigs.ENCRYPTION_DATA.isEmpty()) {
    encryptionData = new SamlEncryptionData(
        samlConfigs.ENCRYPTION_DATA.KEY_STORE_PATH,
        Secret.fromString(samlConfigs.ENCRYPTION_DATA.KEY_STORE_PASSWORD),
        Secret.fromString(samlConfigs.ENCRYPTION_DATA.PRIVATE_KEY_PASSWORD),
        samlConfigs.ENCRYPTION_DATA.PRIVATE_KEY_ALIAS,
        samlConfigs.ENCRYPTION_DATA.FORCE_SIGN_REDIRECT_BINDING_AUTH_REQUEST
    )
} else {
    encryptionData = null
}

ArrayList<String> conventions = ['none', 'lowercase', 'uppercase']
if (!conventions.contains(samlConfigs.USERNAME_CASE_CONVENTION)) {
    logger.severe("USERNAME_CASE_CONVETION must be one of ${conventions}")
    jenkins.doSafeExit(null)
    System.exit(1)
}

SamlSecurityRealm securityRealm = new SamlSecurityRealm(
    idpMetadata,
    samlConfigs.DISPLAY_NAME_ATTRIBUTE,
    samlConfigs.GROUP_ATTRIBUTE,
    maximumAuthenticationLifetime,
    samlConfigs.USERNAME_ATTRIBUTE,
    samlConfigs.EMAIL_ATTRIBUTE,
    samlConfigs.LOGOUT_URL,
    advancedConfiguration,
    encryptionData,
    samlConfigs.USERNAME_CASE_CONVENTION,
    samlConfigs.BINDING,
    samlConfigs.SAML_CUSTOM_ATTRIBUTES
)

jenkins.setSecurityRealm(securityRealm)
jenkins.save()

logger.info('Successfully Configured the SAML plugin')
