package com.kbakhtiari.helm.maven.plugin;

import lombok.Data;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import static com.kbakhtiari.helm.maven.plugin.utils.Constants.MojoDefaultConstants.FALSE;
import static com.kbakhtiari.helm.maven.plugin.utils.Constants.MojoDefaultConstants.TRUE;
import static org.apache.commons.lang3.StringUtils.EMPTY;

@Data
@Mojo(name = "upgrade", defaultPhase = LifecyclePhase.DEPLOY)
public class UpgradeMojo extends AbstractHelmMojo {

  @Parameter(property = "helm.upgrade.skip", defaultValue = FALSE)
  private boolean skipUpgrade;

  @Parameter(property = "helm.upgrade.upgradeWithInstall", defaultValue = TRUE)
  private boolean upgradeWithInstall;

  public void execute() throws MojoExecutionException, MojoFailureException {

    for (String inputDirectory : getChartDirectories(getChartDirectory())) {

      getLog()
          .info(
              new StringBuilder()
                  .append("installing the chart ")
                  .append(upgradeWithInstall ? "with install " : EMPTY)
                  .append(inputDirectory)
                  .toString());

      callCli(
          getCommand("upgrade " + (upgradeWithInstall ? " --install" : EMPTY), inputDirectory),
          "Error happened during upgrading the chart");
    }
  }
}
