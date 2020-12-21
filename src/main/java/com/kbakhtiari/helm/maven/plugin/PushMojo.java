package com.kbakhtiari.helm.maven.plugin;

import lombok.SneakyThrows;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static com.kbakhtiari.helm.maven.plugin.utils.Constants.LogUtils.LOG_TEMPLATE;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;

@Mojo(name = "push", defaultPhase = LifecyclePhase.DEPLOY)
public class PushMojo extends AbstractHelmMojo {

  @Parameter(property = "helm.push.skip", defaultValue = "false")
  private boolean skipPush;

  @SneakyThrows
  public void execute() {

    if (skip || skipPush) {
      getLog().info("Skip push");
      return;
    }
    final Path path = Paths.get(getOutputDirectory()).toAbsolutePath();
    if (path.toFile().exists()) {

      final List<Path> tgzs =
          Files.walk(path, 1, FileVisitOption.FOLLOW_LINKS)
              .filter(file -> FileNameUtils.getExtension(file.toFile().getName()).equals("tgz"))
              .collect(Collectors.toList());
      getLog().debug(format(LOG_TEMPLATE, "found the tgz files as: " + tgzs));

      callCli(
          getHelmCommand(
              format(
                  "registry login -u %s -p %s %s",
                  getRegistryUsername(), getRegistryPassword(), getRegistryUrl()),
              EMPTY),
          "Unable to login to the registry: " + getRegistryUrl());

      for (Path tgz : tgzs) {

        callCli(
            getHelmCommand(
                "chart save",
                new StringBuilder()
                    .append(tgz.toAbsolutePath().toString())
                    .append(SPACE)
                    .append(getRegistryUrl())
                    .append("/")
                    .append(getReleaseName())
                    .append(":")
                    .append(getChartVersion())
                    .toString()),
            "Unable to save the chart at " + tgz.toAbsolutePath().toString());

        callCli(
            getHelmCommand(
                "chart push",
                new StringBuilder()
                    .append(getRegistryUrl())
                    .append("/")
                    .append(getReleaseName())
                    .append(":")
                    .append(getChartVersion())
                    .toString()),
            "Unable to push the chart at " + getRegistryUrl());
      }
    } else {
      throw new MojoExecutionException("the output directory does not exist");
    }
  }
}
