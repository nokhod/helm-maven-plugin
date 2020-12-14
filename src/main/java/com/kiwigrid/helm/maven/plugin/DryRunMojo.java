package com.kiwigrid.helm.maven.plugin;

import lombok.Data;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;

/**
 * Mojo for simulate a dry run.
 *
 * @author Axel Koehler
 * @since 14.11.17
 */
@Data
@Mojo(name = "dry-run", defaultPhase = LifecyclePhase.TEST)
public class DryRunMojo extends AbstractHelmMojo {

    @Parameter(property = "action", defaultValue = "install")
    private String action;

    @Parameter(property = "helm.dry-run.skip", defaultValue = "false")
    private boolean skipDryRun;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip || skipDryRun) {
            getLog().info("Skip dry run");
            return;
        }
        for (String inputDirectory : getChartDirectories(getChartDirectory())) {
            getLog().info("\n\nPerform dry-run for chart " + inputDirectory + "...");

            final String command = getCommand("upgrade --install", inputDirectory);

            getLog().debug("executing helm command: " + command);

            callCli(new StringBuilder(command).append(" --dry-run ").toString(),
                    "There are test failures");
        }
    }
}
