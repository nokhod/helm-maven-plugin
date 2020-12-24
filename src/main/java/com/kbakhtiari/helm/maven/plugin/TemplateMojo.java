package com.kbakhtiari.helm.maven.plugin;

import lombok.Data;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

@Data
@Mojo(name = "template", defaultPhase = LifecyclePhase.VERIFY)
public class TemplateMojo extends AbstractHelmMojo {

  public void execute() throws MojoExecutionException, MojoFailureException {

    for (String inputDirectory : getChartDirectories(getChartDirectory())) {

      getLog().info("templating the chart " + inputDirectory);

      callCli(getCommand("template", inputDirectory), "There are test failures");
    }
  }
}
