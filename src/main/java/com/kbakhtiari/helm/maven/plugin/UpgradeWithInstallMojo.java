package com.kbakhtiari.helm.maven.plugin;

import lombok.Data;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

@Data
@Mojo(name = "upgrade-with-install", defaultPhase = LifecyclePhase.DEPLOY)
public class UpgradeWithInstallMojo extends AbstractHelmMojo {

  public void execute() throws MojoExecutionException, MojoFailureException {

    for (String inputDirectory : getChartDirectories(getChartDirectory())) {

      getLog().info(String.format(LOG_TEMPLATE, "installing the chart " + inputDirectory));

      callCli(getCommand("upgrade --install", inputDirectory), "There are test failures");
    }
  }
}
