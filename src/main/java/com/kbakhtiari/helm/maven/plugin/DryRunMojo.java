package com.kbakhtiari.helm.maven.plugin;

import lombok.Data;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Data
@Mojo(name = "dry-run", defaultPhase = LifecyclePhase.TEST)
public class DryRunMojo extends AbstractHelmMojo {

  @Parameter(property = "action", defaultValue = "upgrade --install")
  private String action;

  @Parameter(property = "helm.dry-run.skip", defaultValue = "false")
  private boolean skipDryRun;

  public void execute() throws MojoExecutionException, MojoFailureException {
    if (skip || skipDryRun) {
      getLog().info("Skip dry run");
      return;
    }
    for (String inputDirectory : getChartDirectories(getChartDirectory())) {

      getLog().info(String.format(LOG_TEMPLATE, "Perform dry-run for chart " + inputDirectory));

      callCli(getCommand(action, " --dry-run ", inputDirectory), "There are test failures");
    }
  }
}
