package com.kbakhtiari.helm.maven.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import static org.apache.commons.lang3.StringUtils.EMPTY;

@Mojo(name = "lint", defaultPhase = LifecyclePhase.TEST)
public class LintMojo extends AbstractHelmMojo {

  @Parameter(property = "helm.lint.skip", defaultValue = "false")
  private boolean skipLint;

  @Parameter(property = "helm.lint.strict", defaultValue = "false")
  private boolean lintStrict;

  public void execute() throws MojoExecutionException {

    if (skip || skipLint) {
      getLog().info("Skip lint");
      return;
    }
    for (String inputDirectory : getChartDirectories(getChartDirectory())) {

      getLog().info("Testing chart " + inputDirectory);
      callCli(
          getCommand("lint", lintStrict ? "--strict" : EMPTY, "There are test failures"),
          "There are test failures");
    }
  }
}
