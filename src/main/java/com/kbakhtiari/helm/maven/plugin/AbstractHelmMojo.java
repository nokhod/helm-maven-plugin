package com.kbakhtiari.helm.maven.plugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import com.kbakhtiari.helm.maven.plugin.pojo.HelmRepository;
import com.kbakhtiari.helm.maven.plugin.pojo.ValueOverride;
import lombok.Data;
import org.apache.commons.collections.MapUtils;
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
import java.io.StringReader;
import java.net.PasswordAuthentication;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@Data
public abstract class AbstractHelmMojo extends AbstractMojo {

  private static final String KEY_VALUE_TEMPLATE = "%s=%s";

  protected static final String LOG_TEMPLATE =
      new StringBuilder()
          .append(System.lineSeparator())
          .append(System.lineSeparator())
          .append("\t\t")
          .append("%s")
          .append(System.lineSeparator())
          .append(System.lineSeparator())
          .toString();

  @Parameter(property = "helm.skip", defaultValue = "false")
  protected boolean skip;

  @Component(role = SecDispatcher.class, hint = "default")
  private SecDispatcher securityDispatcher;

  @Parameter(property = "helm.useLocalHelmBinary", defaultValue = "false")
  private boolean useLocalHelmBinary;

  @Parameter(property = "helm.autoDetectLocalHelmBinary", defaultValue = "true")
  private boolean autoDetectLocalHelmBinary;

  @Parameter(
      property = "helm.executableDirectory",
      defaultValue = "${project.build.directory}/helm")
  private String helmExecutableDirectory;

  @Parameter(
      property = "helm.outputDirectory",
      defaultValue = "${project.build.directory}/helm/repo")
  private String outputDirectory;

  @Parameter(property = "helm.verbose", defaultValue = "false")
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

  @Parameter(property = "helm.version", defaultValue = "3.2.0")
  private String helmVersion;

  @Parameter(property = "helm.registryConfig")
  private String registryConfig;

  @Parameter(property = "helm.repositoryCache")
  private String repositoryCache;

  @Parameter(property = "helm.repositoryConfig")
  private String repositoryConfig;

  @Parameter(property = "helm.extraRepos")
  private HelmRepository[] helmExtraRepos;

  @Parameter(property = "helm.security", defaultValue = "~/.m2/settings-security.xml")
  private String helmSecurity;

  @Parameter(property = "helm.releaseName")
  private String releaseName;

  @Parameter(property = "helm.namespace")
  private String namespace;

  @Parameter(property = "helm.values")
  private ValueOverride values;

  @Parameter(defaultValue = "${settings}", readonly = true)
  private Settings settings;

  private static boolean isJSONValid(String jsonInString) {
    try {
      final Gson gson = new GsonBuilder().create();
      gson.fromJson(jsonInString, Map.class);
      return true;
    } catch (JsonSyntaxException ex) {
      return false;
    }
  }

  private static String getKeyValue(String key, String value) {

    return !isEmpty(key) && !isEmpty(value) ? format(KEY_VALUE_TEMPLATE, key, value) : EMPTY;
  }

  private static String getKeyValue(String key, String mapKey, String mapValue) {

    if (isEmpty(key) && isEmpty(mapValue)) {
      return EMPTY;
    }
    if (key.equalsIgnoreCase("ingress.annotations")) {
      key =
          new StringBuilder("ingress.annotations.")
              .append(mapKey.replaceAll("\\.", "\\\\."))
              .toString();
    } else {
      key = new StringBuilder(key).append("/").append(mapKey).toString();
    }
    return format(KEY_VALUE_TEMPLATE, key, mapValue);
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

    getLog().debug("executing helm command: " + command);

    AtomicInteger exitValue = new AtomicInteger(0);

    getLog().debug(command);

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
                  if (isNotEmpty(errorLine)) {
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
      throw new MojoExecutionException("Error processing command [" + command + "]", e);
    }

    if (exitValue.get() != 0) {
      throw new MojoExecutionException(errorMessage);
    }
  }

  List<String> getChartDirectories(String path) throws MojoExecutionException {

    List<String> exclusions = new ArrayList<>();

    if (getExcludes() != null) {
      exclusions.addAll(Arrays.asList(getExcludes()));
    }

    exclusions.addAll(FileUtils.getDefaultExcludesAsList());

    MatchPatterns exclusionPatterns = MatchPatterns.from(exclusions);

    try (Stream<Path> files = Files.walk(Paths.get(path), FileVisitOption.FOLLOW_LINKS)) {
      List<String> chartDirs =
          files
              .filter(p -> p.getFileName().toString().equalsIgnoreCase("chart.yaml"))
              .map(p -> p.getParent().toString())
              .filter(shouldIncludeDirectory(exclusionPatterns))
              .collect(Collectors.toList());

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
          .filter(p -> p.getFileName().toString().endsWith("tgz"))
          .map(Path::toString)
          .collect(Collectors.toList());
    } catch (IOException e) {
      throw new MojoExecutionException("Unable to scan repo directory at " + path, e);
    }
  }

  String getHelmUploadUrl() {

    String uploadUrl = uploadRepoStable.getUrl();
    if (chartVersion != null
        && chartVersion.endsWith("-SNAPSHOT")
        && uploadRepoSnapshot != null
        && isNotEmpty(uploadRepoSnapshot.getUrl())) {
      uploadUrl = uploadRepoSnapshot.getUrl();
    }

    return uploadUrl;
  }

  HelmRepository getHelmUploadRepo() {

    if (chartVersion != null
        && chartVersion.endsWith("-SNAPSHOT")
        && uploadRepoSnapshot != null
        && isNotEmpty(uploadRepoSnapshot.getUrl())) {
      return uploadRepoSnapshot;
    }
    return uploadRepoStable;
  }

  PasswordAuthentication getAuthentication(HelmRepository repository)
      throws MojoExecutionException {

    String id = repository.getName();

    if (isNotEmpty(repository.getUsername())) {
      if (isEmpty(repository.getPassword())) {
        throw new IllegalArgumentException(
            "Repo " + id + " has a username but no password defined.");
      }
      getLog().debug("Repo " + id + " has credentials defined, skip searching in server list.");
      return new PasswordAuthentication(
          repository.getUsername(), repository.getPassword().toCharArray());
    }

    Server server = settings.getServer(id);
    if (server == null) {
      getLog()
          .info(
              "No credentials found for " + id + " in configuration or settings.xml server list.");
      return null;
    }

    getLog().debug("Use credentials from server list for " + id + ".");
    if (isEmpty(server.getUsername()) || isEmpty(server.getPassword())) {
      throw new IllegalArgumentException(
          "Repo " + id + " was found in server list but has no username/password.");
    }

    try {
      return new PasswordAuthentication(
          server.getUsername(), getSecDispatcher().decrypt(server.getPassword()).toCharArray());
    } catch (SecDispatcherException e) {
      throw new MojoExecutionException(e.getMessage());
    }
  }

  protected SecDispatcher getSecDispatcher() {

    if (securityDispatcher instanceof DefaultSecDispatcher) {
      ((DefaultSecDispatcher) securityDispatcher).setConfigurationFile(getHelmSecurity());
    }
    return securityDispatcher;
  }

  protected String getValuesOptions() {

    StringBuilder setValuesOptions = new StringBuilder();
    if (values != null) {
      if (MapUtils.isNotEmpty(values.getOverrides())) {
        setValuesOptions.append(" --set ").append(appendOverrideMap(values.getOverrides()));
      }
      if (MapUtils.isNotEmpty(values.getStringOverrides())) {
        setValuesOptions
            .append(" --set-string ")
            .append(appendOverrideMap(values.getStringOverrides()));
      }
      if (MapUtils.isNotEmpty(values.getFileOverrides())) {
        setValuesOptions
            .append(" --set-file ")
            .append(appendOverrideMap(values.getFileOverrides()));
      }
      if (isNotBlank(values.getYamlFile())) {
        setValuesOptions.append(" --values ").append(values.getYamlFile());
      }
    }
    return setValuesOptions.toString();
  }

  private String appendOverrideMap(Map<String, String> overrides) {

    return overrides.keySet().stream()
        .map(key -> convertValue(key, overrides.get(key)))
        .collect(Collectors.joining(","));
  }

  private String convertValue(final String key, final String value) {

    getLog().info(format("adding key: %s and value: %s to overriding values", key, value));
    if (isJSONValid(value)) {
      final Gson gson = new GsonBuilder().create();
      JsonReader reader = new JsonReader(new StringReader(value));
      reader.setLenient(true);
      Map<String, String> map = gson.fromJson(reader, Map.class);
      return map.keySet().stream()
          .map(mapKey -> getKeyValue(key, mapKey, map.get(mapKey)))
          .collect(Collectors.joining(","));
    } else {
      return getKeyValue(key, value);
    }
  }

  protected final String getCommand(String action, String inputDirectory)
      throws MojoExecutionException {

    return getCommand(action, EMPTY, inputDirectory);
  }

  protected final String getHelmCommand(String action, String args) throws MojoExecutionException {

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
                ? format(" --registry-config=%s ", getRegistryConfig())
                : EMPTY)
        .append(
            isNotEmpty(getRepositoryCache())
                ? format(" --repository-cache=%s ", getRepositoryCache())
                : EMPTY)
        .append(
            isNotEmpty(getRepositoryConfig())
                ? format(" --repository-config=%s ", getRepositoryConfig())
                : EMPTY)
        .append(getValuesOptions())
        .toString();
  }
}
