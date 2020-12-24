package com.kbakhtiari.helm.maven.plugin;

import com.kbakhtiari.helm.maven.plugin.pojo.HelmRepository;
import com.kbakhtiari.helm.maven.plugin.pojo.ValueOverride;
import com.kbakhtiari.helm.maven.plugin.utils.PackageUtils;
import lombok.Data;
import lombok.SneakyThrows;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.MatchPatterns;
import org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;

import java.io.File;
import java.io.IOException;
import java.net.PasswordAuthentication;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.kbakhtiari.helm.maven.plugin.utils.Constants.ExceptionMessages.UNKNOWN_VALUE_TYPE_MESSAGE;
import static com.kbakhtiari.helm.maven.plugin.utils.Constants.MojoDefaultConstants.FALSE;
import static com.kbakhtiari.helm.maven.plugin.utils.Constants.MojoDefaultConstants.HELM_SECURITY;
import static com.kbakhtiari.helm.maven.plugin.utils.Constants.MojoDefaultConstants.HELM_VERSION;
import static com.kbakhtiari.helm.maven.plugin.utils.Constants.MojoDefaultConstants.TRUE;
import static com.kbakhtiari.helm.maven.plugin.utils.JavaUtils.nvl;
import static com.kbakhtiari.helm.maven.plugin.utils.PackageUtils.toMap;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@Data
public abstract class AbstractHelmMojo extends AbstractMojo {

  private static final String STRING_KEY_STRING_VALUE_TEMPLATE = "%s=%s";
  private static final String STRING_KEY_LIST_VALUE_TEMPLATE = "%s[%d]=%s";
  private static final String STRING_KEY_LIST_SUB_VALUE_TEMPLATE = "%s[%d].%s";

  @Parameter(property = "helm.skip", defaultValue = FALSE)
  protected boolean skip;

  @Parameter(property = "helm.useLocalHelmBinary", defaultValue = FALSE)
  private boolean useLocalHelmBinary;

  @Parameter(property = "helm.autoDetectLocalHelmBinary", defaultValue = TRUE)
  private boolean autoDetectLocalHelmBinary;

  @Parameter(
      property = "helm.executableDirectory",
      defaultValue = "${project.build.directory}/helm")
  private String helmExecutableDirectory;

  @Parameter(
      property = "helm.outputDirectory",
      defaultValue = "${project.build.directory}/helm/repo")
  private String outputDirectory;

  @Parameter(property = "helm.verbose", defaultValue = FALSE)
  private boolean verbose;

  @Parameter(property = "helm.excludes")
  private String[] excludes;

  @Parameter(property = "helm.chartDirectory", required = true)
  private String chartDirectory;

  @Parameter(property = "helm.chartVersion")
  private String chartVersion;

  @Parameter(property = "helm.appVersion")
  private String appVersion;

  @Parameter(property = "helm.uploadRepo.stable")
  private HelmRepository uploadRepoStable;

  @Parameter(property = "helm.uploadRepo.snapshot")
  private HelmRepository uploadRepoSnapshot;

  @Parameter(property = "helm.downloadUrl")
  private String helmDownloadUrl;

  @Parameter(property = "helm.version", defaultValue = HELM_VERSION)
  private String helmVersion;

  @Parameter(property = "helm.registryConfig")
  private String registryConfig;

  @Parameter(property = "helm.repositoryCache")
  private String repositoryCache;

  @Parameter(property = "helm.repositoryConfig")
  private String repositoryConfig;

  @Parameter(property = "helm.extraRepos")
  private HelmRepository[] helmExtraRepos;

  @Parameter(property = "helm.security", defaultValue = HELM_SECURITY)
  private String helmSecurity;

  @Parameter(property = "helm.releaseName")
  private String releaseName;

  @Parameter(property = "helm.namespace")
  private String namespace;

  @Parameter(property = "helm.values")
  private ValueOverride values;

  @Parameter(defaultValue = "${settings}", readonly = true)
  private Settings settings;

  @SneakyThrows
  private static <U extends Object> List<String> getKeyValue(String key, U value) {

    if (isEmpty(key)) {
      return Collections.EMPTY_LIST;
    }
    if (isNull(value) || value instanceof String) {
      return asList(format(STRING_KEY_STRING_VALUE_TEMPLATE, key, nvl(value, EMPTY)));
    } else if (value instanceof Collection) {
      List<U> convertedList = (List<U>) value;
      return IntStream.range(0, convertedList.size())
          .mapToObj(
              index -> {
                U item = convertedList.get(index);
                if (item instanceof String) {
                  return format(STRING_KEY_LIST_VALUE_TEMPLATE, key, index, item);
                } else if (item instanceof Map) {
                  final Map<String, U> convertedMap = (Map<String, U>) item;
                  return convertedMap.keySet().stream()
                      .map(subMapKey -> getKeyValue(subMapKey, convertedMap.get(subMapKey)))
                      .flatMap(List::stream)
                      .map(el -> format(STRING_KEY_LIST_SUB_VALUE_TEMPLATE, key, index, el))
                      .collect(joining(","));
                } else {
                  throw new RuntimeException(UNKNOWN_VALUE_TYPE_MESSAGE);
                }
              })
          .collect(toList());
    } else {
      throw new MojoExecutionException(UNKNOWN_VALUE_TYPE_MESSAGE);
    }
  }

  Path getHelmExecutablePath() throws MojoExecutionException {

    String helmExecutable = SystemUtils.IS_OS_WINDOWS ? "helm.exe" : "helm";

    Optional<Path> path =
        isUseLocalHelmBinary() && isAutoDetectLocalHelmBinary()
            ? findInPath(helmExecutable)
            : Optional.of(Paths.get(helmExecutableDirectory, helmExecutable))
                .map(Path::toAbsolutePath)
                .filter(Files::exists);

    return path.orElseThrow(() -> new MojoExecutionException("Helm executable is not found."));
  }

  private Optional<Path> findInPath(final String executable) {

    final String[] paths = getPathsFromEnvironmentVariables();
    return Stream.of(paths)
        .map(Paths::get)
        .map(path -> path.resolve(executable))
        .filter(Files::exists)
        .map(Path::toAbsolutePath)
        .findFirst();
  }

  String[] getPathsFromEnvironmentVariables() {

    return System.getenv("PATH").split(Pattern.quote(File.pathSeparator));
  }

  void callCli(String command, String errorMessage) throws MojoExecutionException {

    getLog().debug("executing command: " + command);

    AtomicInteger exitValue = new AtomicInteger(0);

    try {
      final Process p = Runtime.getRuntime().exec(command);
      new Thread(
              () -> {
                try {
                  String inputLine = IOUtils.toString(p.getInputStream(), StandardCharsets.UTF_8);
                  String errorLine = IOUtils.toString(p.getErrorStream(), StandardCharsets.UTF_8);
                  if (verbose) {
                    getLog().info(inputLine);
                  } else {
                    getLog().debug(inputLine);
                  }
                  if (isNotBlank(errorLine)) {
                    getLog().error(errorLine);
                  }
                } catch (IOException e) {
                  getLog().error(e);
                }
              })
          .start();
      p.waitFor();
      exitValue.set(p.exitValue());
    } catch (Exception e) {
      throw new MojoExecutionException(format("Error processing command [%s]", command), e);
    }

    if (exitValue.get() != 0) {
      throw new MojoExecutionException(errorMessage);
    }
  }

  List<String> getChartDirectories(String path) throws MojoExecutionException {

    List<String> exclusions = new ArrayList<>();

    if (getExcludes() != null) {
      exclusions.addAll(asList(getExcludes()));
    }

    exclusions.addAll(FileUtils.getDefaultExcludesAsList());

    MatchPatterns exclusionPatterns = MatchPatterns.from(exclusions);

    try (Stream<Path> files = Files.walk(Paths.get(path), FileVisitOption.FOLLOW_LINKS)) {
      List<String> chartDirs =
          files
              .filter(p -> p.getFileName().toString().equalsIgnoreCase("chart.yaml"))
              .map(p -> p.getParent().toString())
              .filter(shouldIncludeDirectory(exclusionPatterns))
              .collect(toList());

      if (chartDirs.isEmpty()) {
        getLog().warn("No Charts detected - no Chart.yaml files found below " + path);
      }

      return chartDirs;
    } catch (IOException e) {
      throw new MojoExecutionException("Unable to scan chart directory at " + path, e);
    }
  }

  private Predicate<String> shouldIncludeDirectory(MatchPatterns exclusionPatterns) {

    return inputDirectory -> {
      boolean matches = exclusionPatterns.matches(inputDirectory, Boolean.FALSE);

      if (matches) {
        getLog().debug("Skip excluded directory " + inputDirectory);
        return Boolean.FALSE;
      }

      return Boolean.TRUE;
    };
  }

  List<String> getChartTgzs(String path) throws MojoExecutionException {

    try (Stream<Path> files = Files.walk(Paths.get(path))) {
      return files
          .filter(p -> FileNameUtils.getExtension(p.toFile().getName()).equals("tgz"))
          .map(Path::toString)
          .collect(toList());
    } catch (IOException e) {
      throw new MojoExecutionException("Unable to scan repo directory at " + path, e);
    }
  }

  String getHelmUploadUrl() {

    return getHelmUploadRepo().getUrl();
  }

  HelmRepository getHelmUploadRepo() {

    if (isNotEmpty(chartVersion)
        && chartVersion.endsWith("-SNAPSHOT")
        && !isNull(uploadRepoSnapshot)
        && isNotEmpty(uploadRepoSnapshot.getUrl())) {
      return uploadRepoSnapshot;
    }
    return uploadRepoStable;
  }

  protected String getValuesOptions() {

    StringBuilder setValuesOptions = new StringBuilder();
    if (values != null) {
      if (isNotEmpty(values.getOverrides())) {
        setValuesOptions.append(" --set ").append(appendOverrides(toMap(values.getOverrides())));
      }
      if (isNotEmpty(values.getStringOverrides())) {
        setValuesOptions
            .append(" --set-string ")
            .append(appendOverrides(toMap(values.getStringOverrides())));
      }
      if (isNotEmpty(values.getFileOverrides())) {
        setValuesOptions
            .append(" --set-file ")
            .append(appendOverrides(toMap(values.getFileOverrides())));
      }
      if (isNotBlank(values.getYamlFile())) {
        setValuesOptions.append(" --values ").append(values.getYamlFile());
      }
    }
    return setValuesOptions.toString();
  }

  private String appendOverrides(Map<String, ?> overrides) {

    final Map<String, ?> flattenOverrides = PackageUtils.flattenOverrides(overrides);
    return flattenOverrides.keySet().stream()
        .map(key -> getKeyValue(key, flattenOverrides.get(key)))
        .flatMap(List::stream)
        .collect(joining(","));
  }

  protected final String getCommand(String action, String inputDirectory)
      throws MojoExecutionException {

    return getCommand(action, EMPTY, inputDirectory);
  }

  protected String getHelmCommand(String action, String args) throws MojoExecutionException {

    return new StringBuilder()
        .append(getHelmExecutablePath())
        .append(format(" %s ", action))
        .append(format(" %s ", args))
        .toString();
  }

  protected final String getCommand(String action, String args, String inputDirectory)
      throws MojoExecutionException {

    return new StringBuilder(getHelmCommand(action, args))
        .append(
            isNotEmpty(getReleaseName()) ? format(" %s ", getReleaseName()) : " --generate-name ")
        .append(inputDirectory)
        .append(
            isNotEmpty(getNamespace())
                ? format(" -n %s ", getNamespace().toLowerCase(Locale.ROOT))
                : EMPTY)
        .append(isVerbose() ? " --debug " : EMPTY)
        .append(
            isNotEmpty(getRegistryConfig())
                ? format(" --registry-config %s ", getRegistryConfig())
                : EMPTY)
        .append(
            isNotEmpty(getRepositoryCache())
                ? format(" --repository-cache %s ", getRepositoryCache())
                : EMPTY)
        .append(
            isNotEmpty(getRepositoryConfig())
                ? format(" --repository-config %s ", getRepositoryConfig())
                : EMPTY)
        .append(getValuesOptions())
        .toString();
  }
}
