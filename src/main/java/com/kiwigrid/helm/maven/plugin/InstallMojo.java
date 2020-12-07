package com.kiwigrid.helm.maven.plugin;

import lombok.Data;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.util.Locale;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * Mojo for install the chart.
 *
 * @author Khodabakhsh Bakhtiari
 * @since 14.11.17
 */
@Data
@Mojo(name = "install", defaultPhase = LifecyclePhase.DEPLOY)
public class InstallMojo extends AbstractHelmMojo {

    public void execute() throws MojoExecutionException, MojoFailureException {

        for (String inputDirectory : getChartDirectories(getChartDirectory())) {
            getLog().info("\n\ninstalling the chart " + inputDirectory + "...");

            final String command = new StringBuilder()
                    .append(getHelmExecutablePath())
                    .append(" install ")
                    .append(isNotEmpty(getReleaseName()) ? format(" %s ",getReleaseName()) : " --generate-name ")
                    .append(inputDirectory)
                    .append(isNotEmpty(getNamespace()) ? format(" -n %s ", getNamespace().toLowerCase(Locale.ROOT)) : EMPTY)
                    .append(isVerbose() ? " --debug " : EMPTY)
                    .append(isNotEmpty(getRegistryConfig()) ? format(" --registry-config=%s ", getRegistryConfig()) : EMPTY)
                    .append(isNotEmpty(getRepositoryCache()) ? format(" --repository-cache=%s ", getRepositoryCache()) : EMPTY)
                    .append(isNotEmpty(getRepositoryConfig()) ? format(" --repository-config=%s ", getRepositoryConfig()) : EMPTY)
                    .append(getValuesOptions()).toString();

            getLog().debug("executing helm command: " + command);

            callCli(command, "There are test failures", isVerbose());
        }
    }
}
