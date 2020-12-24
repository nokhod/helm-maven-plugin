package com.kbakhtiari.helm.maven.plugin;

import lombok.Data;
import lombok.SneakyThrows;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.Map;

import static com.kbakhtiari.helm.maven.plugin.utils.PackageUtils.overrideValuesFile;
import static com.kbakhtiari.helm.maven.plugin.utils.PackageUtils.toMap;
import static java.lang.String.format;
import static org.codehaus.plexus.util.StringUtils.isNotEmpty;

@Data
@Mojo(name = "package", defaultPhase = LifecyclePhase.PACKAGE)
public class PackageMojo extends AbstractHelmMojo {

  @Parameter(property = "helm.package.skip", defaultValue = "false")
  private boolean skipPackage;

  @SneakyThrows
  public void execute() {

    if (skip || skipPackage) {
      getLog().info("Skip package");
      return;
    }

    for (String inputDirectory : getChartDirectories(getChartDirectory())) {

      getLog().info("Packaging chart " + inputDirectory);

      // final Map normalizeMapKeys = flattenOverrides(toMap(getValues().getOverrides()));
      final Map overridesMap = toMap(getValues().getOverrides());
      getLog().debug("the normalized map is: " + overridesMap);
      overrideValuesFile(inputDirectory, overridesMap, getLog());

      StringBuilder args =
          new StringBuilder(format("%s -d %s", inputDirectory, getOutputDirectory()));

      if (isNotEmpty(getChartVersion())) {
        getLog().info(format("Setting chart version to %s", getChartVersion()));
        args.append(" --version ").append(getChartVersion());
      }

      if (isNotEmpty(getAppVersion())) {
        getLog().info(format("Setting App version to %s", getAppVersion()));
        args.append(" --app-version ").append(getAppVersion());
      }
      callCli(
          getHelmCommand("package", args.toString()),
          "Unable to package chart at " + inputDirectory);
    }
  }
}
