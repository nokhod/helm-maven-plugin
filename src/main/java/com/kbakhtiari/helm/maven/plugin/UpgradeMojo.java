package com.kbakhtiari.helm.maven.plugin;

import lombok.Data;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import static com.kbakhtiari.helm.maven.plugin.utils.Constants.LogUtils.LOG_TEMPLATE;

@Data
@Mojo(name = "upgrade", defaultPhase = LifecyclePhase.DEPLOY)
public class UpgradeMojo extends AbstractHelmMojo {

  public void execute() throws MojoExecutionException, MojoFailureException {

    for (String inputDirectory : getChartDirectories(getChartDirectory())) {

      getLog().info(String.format(LOG_TEMPLATE, "installing the chart " + inputDirectory));

      callCli(getCommand("upgrade", inputDirectory), "There are test failures");
    }
  }
}
