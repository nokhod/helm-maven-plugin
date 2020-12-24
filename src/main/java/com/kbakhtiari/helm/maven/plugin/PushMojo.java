package com.kbakhtiari.helm.maven.plugin;

import com.kbakhtiari.helm.maven.plugin.pojo.HelmRepository;
import lombok.Data;
import lombok.SneakyThrows;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import static com.kbakhtiari.helm.maven.plugin.utils.Constants.MojoDefaultConstants.FALSE;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.EMPTY;

@Data
@Mojo(name = "push", defaultPhase = LifecyclePhase.DEPLOY)
public class PushMojo extends AbstractHelmMojo {

  private static final String LOGIN_COMMAND_TEMPLATE = " registry login -u %s -p %s %s ";
  private static final String CHART_SAVE_PUSH_TEMPLATE = " %s %s/%s:%s ";

  @Parameter(property = "helm.push.skip", defaultValue = FALSE)
  private boolean skipPush;

  @SneakyThrows
  public void execute() {

    if (skip || skipPush) {
      getLog().info("Skip push");
      return;
    }
    final HelmRepository registry = getHelmUploadRepo();
    if (Objects.isNull(registry)) {
      getLog().info("there is no helm repo. skipping the upload.");
      return;
    }

    callCli(
        getHelmCommand(
            format(
                LOGIN_COMMAND_TEMPLATE,
                registry.getUsername(),
                registry.getPassword(),
                registry.getUrl()),
            EMPTY),
        "Unable to login to the registry: " + registry.getUrl());

    getLog().info("Uploading to " + registry.getUrl());
    getChartTgzs(getOutputDirectory())
        .forEach(
            chartTgzFile -> {
              getLog().info("Uploading " + chartTgzFile);
              try {
                uploadSingle(Paths.get(chartTgzFile), registry);
              } catch (MojoExecutionException e) {
                throw new RuntimeException(e);
              }
            });
  }

  private void uploadSingle(Path tgz, HelmRepository registry) throws MojoExecutionException {

    callCli(
        getHelmCommand(
            "chart save",
            format(
                CHART_SAVE_PUSH_TEMPLATE,
                tgz.toAbsolutePath().toString(),
                registry.getUrl(),
                getReleaseName(),
                getChartVersion())),
        "Unable to save the chart at " + tgz.toAbsolutePath().toString());

    callCli(
        getHelmCommand(
            "chart push",
            format(
                CHART_SAVE_PUSH_TEMPLATE,
                tgz.toAbsolutePath().toString(),
                registry.getUrl(),
                getReleaseName(),
                getChartVersion())),
        "Unable to push the chart at " + registry.getUrl());
  }
}
