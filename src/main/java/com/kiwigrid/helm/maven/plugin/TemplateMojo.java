package com.kiwigrid.helm.maven.plugin;

import lombok.Data;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Mojo for install the chart.
 *
 * @author Khodabakhsh Bakhtiari
 * @since 14.11.17
 */
@Data
@Mojo(name = "template", defaultPhase = LifecyclePhase.VERIFY)
public class TemplateMojo extends AbstractHelmMojo {

    public void execute() throws MojoExecutionException, MojoFailureException {

        for (String inputDirectory : getChartDirectories(getChartDirectory())) {
            getLog().info("\n\ntemplating the chart " + inputDirectory + "...");

            final String command = getCommand("template", inputDirectory);

            getLog().debug("executing helm command: " + command);

            callCli(command, "There are test failures");
        }
    }
}
