package com.kbakhtiari.helm.maven.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import static java.lang.String.format;
import static org.codehaus.plexus.util.StringUtils.isNotEmpty;

@Mojo(name = "package", defaultPhase = LifecyclePhase.PACKAGE)
public class PackageMojo extends AbstractHelmMojo {

  @Parameter(property = "helm.package.skip", defaultValue = "false")
  private boolean skipPackage;

  public void execute() throws MojoExecutionException {

    if (skip || skipPackage) {
      getLog().info("Skip package");
      return;
    }

    for (String inputDirectory : getChartDirectories(getChartDirectory())) {

      getLog().info(format(LOG_TEMPLATE, "Packaging chart " + inputDirectory));

      StringBuilder args = new StringBuilder(format("-d %s", getOutputDirectory()));

      if (isNotEmpty(getChartVersion())) {
        getLog()
            .info(format(LOG_TEMPLATE, format("Setting chart version to %s", getChartVersion())));
        args.append(" --version ").append(getChartVersion());
      }

      if (isNotEmpty(getAppVersion())) {
        getLog().info(format("Setting App version to %s", getAppVersion()));
        args.append(" --app-version ").append(getAppVersion());
      }
      callCli(
          getCommand("pack", args.toString(), inputDirectory),
          "Unable to package chart at " + inputDirectory);
    }
  }
}
