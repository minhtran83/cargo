/*
 * ========================================================================
 *
 * Codehaus CARGO, copyright 2004-2011 Vincent Massol, 2012-2020 Ali Tokmen.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * ========================================================================
 */
package org.codehaus.cargo.container.jboss;

import java.nio.charset.StandardCharsets;
import org.codehaus.cargo.container.LocalContainer;
import org.codehaus.cargo.container.configuration.ConfigurationCapability;
import org.codehaus.cargo.container.jboss.internal.JBoss71xStandaloneLocalConfigurationCapability;
import org.codehaus.cargo.container.property.GeneralPropertySet;
import org.codehaus.cargo.container.property.User;

/**
 * JBoss 7.1.x standalone local configuration.
 */
public class JBoss71xStandaloneLocalConfiguration extends JBoss7xStandaloneLocalConfiguration
{

    /**
     * JBoss container capability.
     */
    private static final ConfigurationCapability CAPABILITY =
        new JBoss71xStandaloneLocalConfigurationCapability();

    /**
     * {@inheritDoc}
     * @see JBoss7xStandaloneLocalConfiguration#JBoss7xStandaloneLocalConfiguration(String)
     */
    public JBoss71xStandaloneLocalConfiguration(String dir)
    {
        super(dir);

        setProperty(JBossPropertySet.JBOSS_AJP_PORT, "8009");
        setProperty(JBossPropertySet.JBOSS_MANAGEMENT_HTTPS_PORT, "9993");
        setProperty(JBossPropertySet.JBOSS_TRANSACTION_RECOVERY_MANAGER_PORT, "4712");
        setProperty(JBossPropertySet.JBOSS_TRANSACTION_STATUS_MANAGER_PORT, "4713");

        getProperties().remove(GeneralPropertySet.RMI_PORT);
        getProperties().remove(JBossPropertySet.JBOSS_JRMP_PORT);
        getProperties().remove(JBossPropertySet.JBOSS_JMX_PORT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConfigurationCapability getCapability()
    {
        return CAPABILITY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(LocalContainer container)
    {
        super.configure(container);

        // Add token filters for authenticated users
        if (!getUsers().isEmpty())
        {
            StringBuilder usersToken = new StringBuilder(
                "# JBoss application-users.properties file generated by CARGO\n");
            StringBuilder rolesToken = new StringBuilder(
                "# JBoss application-roles.properties file generated by CARGO\n");

            for (User user : getUsers())
            {
                usersToken.append(generateUserPasswordLine(user, "ApplicationRealm"));

                rolesToken.append(user.getName());
                rolesToken.append("=");
                boolean first = true;
                for (String role : user.getRoles())
                {
                    if (first)
                    {
                        first = false;
                    }
                    else
                    {
                        rolesToken.append(",");
                    }
                }
                rolesToken.append('\n');
            }

            getFileHandler().writeTextFile(
                getFileHandler().append(getHome(), "/configuration/application-users.properties"),
                    usersToken.toString(), StandardCharsets.UTF_8);
            getFileHandler().writeTextFile(
                getFileHandler().append(getHome(), "/configuration/application-roles.properties"),
                    rolesToken.toString(), StandardCharsets.UTF_8);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doConfigure(LocalContainer c) throws Exception
    {
        super.doConfigure(c);

        String configurationXmlFile = "configuration/"
            + getPropertyValue(JBossPropertySet.CONFIGURATION) + ".xml";

        addXmlReplacement(
            configurationXmlFile,
            "//server/socket-binding-group/socket-binding[@name='ajp']",
            "port", JBossPropertySet.JBOSS_AJP_PORT);
        removeXmlReplacement(
            configurationXmlFile,
            "//server/management/management-interfaces/native-interface[@interface='management']",
            "port");
        removeXmlReplacement(
            configurationXmlFile,
            "//server/management/management-interfaces/http-interface[@interface='management']",
            "port");
        addXmlReplacement(
            configurationXmlFile,
            "//server/socket-binding-group/socket-binding[@name='management-native']",
            "port", JBossPropertySet.JBOSS_MANAGEMENT_NATIVE_PORT);
        addXmlReplacement(
            configurationXmlFile,
            "//server/socket-binding-group/socket-binding[@name='management-http']",
            "port", JBossPropertySet.JBOSS_MANAGEMENT_HTTP_PORT);
        addXmlReplacement(
            configurationXmlFile,
            "//server/socket-binding-group/socket-binding[@name='management-https']",
            "port", JBossPropertySet.JBOSS_MANAGEMENT_HTTPS_PORT);
        addXmlReplacement(
            configurationXmlFile,
            "//server/socket-binding-group/socket-binding[@name='txn-recovery-environment']",
            "port", JBossPropertySet.JBOSS_TRANSACTION_RECOVERY_MANAGER_PORT);
        addXmlReplacement(
            configurationXmlFile,
            "//server/socket-binding-group/socket-binding[@name='txn-status-manager']",
            "port", JBossPropertySet.JBOSS_TRANSACTION_STATUS_MANAGER_PORT);
        removeXmlReplacement(
            configurationXmlFile,
            "//server/socket-binding-group/socket-binding[@name='jndi']",
            "port");
        removeXmlReplacement(
            configurationXmlFile,
            "//server/socket-binding-group/socket-binding[@name='jmx-connector-registry']",
            "port");
        removeXmlReplacement(
            configurationXmlFile,
            "//server/socket-binding-group/socket-binding[@name='jmx-connector-server']",
            "port");
        removeXmlReplacement(
            configurationXmlFile,
            "//server/profile/subsystem/periodic-rotating-file-handler/level",
            "name");
    }

}
