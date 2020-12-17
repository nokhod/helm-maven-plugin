package com.kbakhtiari.helm.maven.plugin;

import com.kbakhtiari.helm.maven.plugin.utils.Constants;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import static com.kbakhtiari.helm.maven.plugin.utils.Constants.LogUtils.LOG_TEMPLATE;

@Mojo(name = "dependency-build", defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class DependencyBuildMojo extends AbstractHelmMojo {

  @Parameter(property = "helm.dependency-build.skip", defaultValue = "false")
  private boolean skipDependencyBuild;

  public void execute() throws MojoExecutionException {

    if (skip || skipDependencyBuild) {
      getLog().info("Skip dependency build");
      return;
    }
    for (String inputDirectory : getChartDirectories(getChartDirectory())) {

      getLog().info(String.format(LOG_TEMPLATE, "Build chart dependencies for " + inputDirectory));

      callCli(getCommand("dependency build", inputDirectory), "Failed to resolve dependencies");
    }
  }
}
