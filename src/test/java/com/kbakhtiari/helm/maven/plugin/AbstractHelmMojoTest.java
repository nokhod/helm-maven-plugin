package com.kbakhtiari.helm.maven.plugin;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.kbakhtiari.helm.maven.plugin.pojo.HelmRepository;
import com.kbakhtiari.helm.maven.plugin.pojo.RepoType;
import com.kbakhtiari.helm.maven.plugin.pojo.ValueOverride;
import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.PasswordAuthentication;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static java.io.File.separator;
import static java.lang.String.format;
import static java.nio.file.Files.write;
import static java.util.Arrays.asList;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class AbstractHelmMojoTest {

  private NoopHelmMojo subjectSpy;
  private Path testPath;
  private Path testHelmExecutablePath;

  private String chartDir;
  private String excludeDir1;
  private String excludeDir2;

  @BeforeEach
  void setUp() throws IOException {

    chartDir = getBaseChartsDirectory().toString();
    excludeDir1 = new StringBuilder(chartDir).append(separator).append("exclude1").toString();
    excludeDir2 = new StringBuilder(chartDir).append(separator).append("exclude2").toString();

    subjectSpy = spy(new NoopHelmMojo());
    testPath = Files.createTempDirectory("test").toAbsolutePath();
    testHelmExecutablePath = testPath.resolve(SystemUtils.IS_OS_WINDOWS ? "helm.exe" : "helm");
  }

  @Test
  void testAppendOverrideMap()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

    final Method appendOverrides =
        AbstractHelmMojo.class.getDeclaredMethod("appendOverrides", Map.class);
    appendOverrides.setAccessible(true);

    final ImmutableMap<Object, Object> argument =
        ImmutableMap.builder()
            .put(
                "ingress",
                ImmutableMap.builder()
                    .put(
                        "hosts",
                        asList(
                            ImmutableMap.builder()
                                .put("host", "kbakhtiari.com")
                                .put("paths", asList("KHODA"))
                                .build()))
                    .build())
            .put("ingress.annotations.external-dns.alpha.kubernetes.io/target", "nginx")
            .build();
    final String invoke = (String) appendOverrides.invoke(subjectSpy, argument);
    assertEquals(
        "ingress.annotations.external-dns.alpha.kubernetes.io/target=nginx,ingress.hosts[0].host=kbakhtiari.com,ingress.hosts[0].paths[0]=KHODA",
        invoke);
  }

  private Path addHelmToTestPath() throws IOException {
    return write(testHelmExecutablePath, new byte[] {});
  }

  private Path getBaseChartsDirectory() {
    return new File(getClass().getResource("Chart.yaml").getFile()).toPath().getParent();
  }

  @AfterEach
  void tearDown() {
    deleteQuietly(testPath.toFile());
  }

  private static class NoopHelmMojo extends AbstractHelmMojo {

    @Override
    public void execute() {
      /* Noop. */
    }
  }

  @Nested
  class GetChartTgzs {

    @Test
    void happyFlow() throws IOException, MojoExecutionException {

      final Path tgzFile = Files.createFile(testPath.resolve("DUMMY_TGZS.tgz"));
      final List<String> chartTgzs = subjectSpy.getChartTgzs(tgzFile.toString());
      final Optional<String> result =
          chartTgzs.stream().filter(tgzs -> tgzs.equals(tgzFile.toString())).findAny();
      assertTrue(result.isPresent(), "the designated tgz file not found");
    }

    @Test
    void checkThrowException() throws IOException, MojoExecutionException {

      assertThrows(
          MojoExecutionException.class,
          () -> {
            subjectSpy.getChartTgzs(
                new StringBuilder(testPath.toString())
                    .append(separator)
                    .append("DUMMY_SUB_DIR")
                    .toString());
          });
    }
  }

  @Nested
  class GetHelmUploadUrl {

    private final HelmRepository helmRepo =
        HelmRepository.builder()
            .name("DUMMY_REPO_NAME")
            .type(RepoType.ARTIFACTORY)
            .url("http://localhost:5000")
            .username("DUMMY_REPO_USERNAME")
            .password("DUMMY_REPO_PASSWORD")
            .build();

    @BeforeEach
    void init() {
      subjectSpy.setUploadRepoStable(helmRepo);
    }

    @Test
    void chartVersionIsNull() {

      subjectSpy.setChartVersion(null);

      assertEquals(helmRepo.getUrl(), subjectSpy.getHelmUploadUrl());
    }

    @Test
    void chartVersionEndsWithSnapshot() {

      subjectSpy.setChartVersion("0.0.1");

      assertEquals(helmRepo.getUrl(), subjectSpy.getHelmUploadUrl());
    }

    @Test
    void chartUploadRepoSnapshotIsNull() {

      subjectSpy.setChartVersion("0.0.1-SNAPSHOT");
      subjectSpy.setUploadRepoSnapshot(null);

      assertEquals(helmRepo.getUrl(), subjectSpy.getHelmUploadUrl());
    }

    @Test
    void chartUploadRepoSnapshotUrlIsNull() {

      subjectSpy.setChartVersion("0.0.1-SNAPSHOT");
      subjectSpy.setUploadRepoSnapshot(HelmRepository.builder().build());

      assertEquals(helmRepo.getUrl(), subjectSpy.getHelmUploadUrl());
    }

    @Test
    void shoulReturnUploadSnapshotRepoUrl() {

      final String repoUrl = "http://localhost:6000";
      subjectSpy.setChartVersion("0.0.1-SNAPSHOT");
      subjectSpy.setUploadRepoSnapshot(HelmRepository.builder().url(repoUrl).build());

      assertEquals(repoUrl, subjectSpy.getHelmUploadUrl());
    }
  }

  @Nested
  class CallCLI {

    @Test
    void happyFlow() throws MojoExecutionException {

      assertDoesNotThrow(
          () -> {
            subjectSpy.callCli("echo hello", "cannot echo hello");
          });
    }

    @Test
    void checkWithVerbose() throws InterruptedException {

      final Log spyLog = spy(subjectSpy.getLog());
      subjectSpy.setLog(spyLog);
      subjectSpy.setVerbose(true);

      assertDoesNotThrow(
          () -> {
            subjectSpy.callCli("echo hello", "cannot echo hello");
          });
      TimeUnit.SECONDS.sleep(3);
      verify(spyLog).info(anyString());
    }

    @Test
    @Disabled
    void checkStdErr() throws InterruptedException {

      final Log spyLog = spy(subjectSpy.getLog());
      doReturn(spyLog).when(subjectSpy).getLog();

      assertDoesNotThrow(
          () -> {
            subjectSpy.callCli("logger -s hello", "cannot echo hello as error");
          });
      TimeUnit.SECONDS.sleep(2);
      verify(spyLog).error(anyString());
    }

    @Test
    void checkBadCommand() throws InterruptedException {

      assertThrows(
          MojoExecutionException.class,
          () -> {
            subjectSpy.callCli("sed -i DUMMY", "cannot echo hello as error");
          });
    }
  }

  @Nested
  class GetChartDirectories {

    @Test
    void getChartDirectoriesReturnChartDirectoriesWithPlainExclusion()
        throws MojoExecutionException {

      Path baseChartsDirectory = getBaseChartsDirectory();

      subjectSpy.setExcludes(new String[] {excludeDir1});

      List<String> chartDirectories =
          subjectSpy.getChartDirectories(baseChartsDirectory.toString());

      assertFalse(
          chartDirectories.contains(excludeDir1),
          format(
              "Charts dirs [%s] should not contain excluded dirs [%s]",
              chartDirectories, excludeDir1));
      assertTrue(
          chartDirectories.contains(excludeDir2),
          format(
              "Charts dirs [%s] should contain not excluded dirs [%s]",
              chartDirectories, excludeDir2));
    }

    @Test
    void getChartDirectoriesReturnChartDirectoriesWithAntPatternsExclusion()
        throws MojoExecutionException {

      Path baseChartsDirectory = getBaseChartsDirectory();

      subjectSpy.setExcludes(new String[] {"**" + separator + "exclude*"});

      List<String> chartDirectories =
          subjectSpy.getChartDirectories(baseChartsDirectory.toString());

      assertFalse(
          chartDirectories.contains(excludeDir1),
          format(
              "Charts dirs [%s] should not contain excluded dirs [%s]",
              chartDirectories, excludeDir1));
      assertFalse(
          chartDirectories.contains(excludeDir2),
          format(
              "Charts dirs [%s] should not contain excluded dirs [%s]",
              chartDirectories, excludeDir2));
    }

    @Test
    void getChartDirectoriesReturnChartDirectories() throws MojoExecutionException {

      List<String> chartDirectories = subjectSpy.getChartDirectories(chartDir);
      List<String> expected = asList(chartDir, excludeDir1, excludeDir2);

      assertTrue(
          chartDirectories.containsAll(expected),
          format(
              "Charts dirs: %s, should contain all expected dirs: %s", chartDirectories, expected));
    }

    @Test
    void getChartDirectoriesThrowsException() {

      assertThrows(
          MojoExecutionException.class,
          () -> {
            subjectSpy.getChartDirectories(chartDir.concat("DUMMY_PATH_SUFFIX"));
          },
          "should have thrown an exception in case of false directory for Chart.yaml");
    }

    @Test
    void getChartDirectoriesPrintsLog() throws MojoExecutionException, IOException {

      ArgumentCaptor<String> logCapture = ArgumentCaptor.forClass(String.class);
      final Log spyLog = spy(subjectSpy.getLog());
      doNothing().when(spyLog).warn(logCapture.capture());
      subjectSpy.setLog(spyLog);

      final String emptyDir =
          new StringBuilder(testPath.toString())
              .append(separator)
              .append("DUMMY_PATH_SUFFIX")
              .toString();
      Files.createDirectory(Paths.get(emptyDir));

      subjectSpy.getChartDirectories(emptyDir);
      verify(spyLog).warn(anyString());
      assertTrue(logCapture.getValue().contains(emptyDir));
    }
  }

  @Nested
  class GetKeyValueTest {

    @Test
    @DisplayName("should return empty string when key is null")
    void checkGetKeyValueNullKey()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

      final Object invoke = getGetKeyValue().invoke(null, null, null);
      assertEquals("[]", invoke.toString(), "should return empty string when key is null");
    }

    @Test
    @DisplayName("should return empty string when key is empty")
    void checkGetKeyValueEmptyKey()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

      final Object invoke = getGetKeyValue().invoke(null, EMPTY, null);
      assertEquals("[]", invoke.toString(), "should return empty string when key is empty");
    }

    @Test
    @DisplayName("should return '[<key>=]' string when value is null")
    void checkGetKeyValueNullValue()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

      final Object invoke = getGetKeyValue().invoke(null, "dummy.key", null);
      assertEquals(
          "[dummy.key=]", invoke.toString(), "should return '[<key>=]' string when value is null");
    }

    @Test
    @DisplayName("should return '[<key>=<value>]' string when value is not empty")
    void checkGetKeyValueStringValue()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

      final Object invoke = getGetKeyValue().invoke(null, "dummy.key", "DUMMY_VALUE");
      assertEquals(
          "[dummy.key=DUMMY_VALUE]",
          invoke.toString(),
          "should return '[<key>=<value>]' string when value is not empty");
    }

    @Test
    @DisplayName("should return '[<key>[<index>]=<value>]' string when value is a list")
    void checkGetKeyValueListValue()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

      final Object invoke =
          getGetKeyValue().invoke(null, "dummy.key", Arrays.asList("DUMMY_VALUE0", "DUMMY_VALUE1"));
      assertEquals(
          "[dummy.key[0]=DUMMY_VALUE0, dummy.key[1]=DUMMY_VALUE1]",
          invoke.toString(),
          "should return '[<key>[<index>]=<value>]' string when value is a list");
    }

    @Test
    @DisplayName("should return '[<key>[<index>].<sub_key>=<value>]' string when value is a map")
    void checkGetKeyValueListOfMapValue()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

      final Object invoke =
          getGetKeyValue()
              .invoke(
                  null,
                  "dummy.key",
                  Arrays.asList(
                      ImmutableMap.builder().put("dummy.sub.key", "DUMMY_SUB_VALUE0").build(),
                      ImmutableMap.builder().put("dummy.sub.key", "DUMMY_SUB_VALUE1").build()));
      assertEquals(
          "[dummy.key[0].dummy.sub.key=DUMMY_SUB_VALUE0, dummy.key[1].dummy.sub.key=DUMMY_SUB_VALUE1]",
          invoke.toString(),
          "should return '[<key>[<index>].<sub_key>=<value>]' string when value is a map");
    }

    @Test
    @DisplayName(
        "should return '[<key>[<index>].<sub_key>[<sub_index>]=<value>]' string when value is a map")
    void checkGetKeyValueListOfMapWithListValue()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

      final Object invoke =
          getGetKeyValue()
              .invoke(
                  null,
                  "dummy.key",
                  Arrays.asList(
                      ImmutableMap.builder()
                          .put("dummy.sub.key", asList("DUMMY_SUB_VALUE0", "DUMMY_SUB_VALUE1"))
                          .build(),
                      ImmutableMap.builder()
                          .put("dummy.sub.key", asList("DUMMY_SUB_VALUE2"))
                          .build()));
      assertEquals(
          "[dummy.key[0].dummy.sub.key[0]=DUMMY_SUB_VALUE0,dummy.key[0].dummy.sub.key[1]=DUMMY_SUB_VALUE1, dummy.key[1].dummy.sub.key[0]=DUMMY_SUB_VALUE2]",
          invoke.toString(),
          "should return '[<key>[<index>].<sub_key>[<sub_index>]=<value>]' string when value is a map");
    }

    @Test
    @DisplayName("should return '[<key>=]' string when value is empty")
    void checkGetKeyValueEmptyValue()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

      final Object invoke = getGetKeyValue().invoke(null, "dummy.key", EMPTY);
      assertEquals(
          "[dummy.key=]", invoke.toString(), "should return '[<key>=]' string when value is empty");
    }

    @Test()
    @DisplayName("should throw an exception when the value is of unknown type")
    void checkThrowExceptionWithUnknownFirstLevelValue() {

      assertThrows(
          Throwable.class,
          () -> {
            getGetKeyValue().invoke(null, "dummy.key", new Byte[] {});
          });
    }

    @Test
    @DisplayName("should throw an exception when the value is of unknown type")
    void checkThrowExceptionWithUnknownDeepLevelValue() {

      assertThrows(
          Throwable.class,
          () -> {
            getGetKeyValue().invoke(null, "dummy.key", asList(new Byte[] {1, 2}));
          });
    }

    private Method getGetKeyValue() throws NoSuchMethodException {
      final Method getKeyValue =
          AbstractHelmMojo.class.getDeclaredMethod("getKeyValue", String.class, Object.class);
      getKeyValue.setAccessible(true);
      return getKeyValue;
    }
  }

  @Nested
  class WhenUseLocalBinaryAndAutoDetectIsEnabled {

    @BeforeEach
    void setUp() {

      subjectSpy.setUseLocalHelmBinary(true);
      subjectSpy.setAutoDetectLocalHelmBinary(true);
      doReturn(new String[] {testPath.toAbsolutePath().toString()})
          .when(subjectSpy)
          .getPathsFromEnvironmentVariables();
    }

    @Test
    @DisplayName(
        "pathsFromEnvironmentVariables shouldn't be null when using local binary is true and autoDetectLocalHelmBinary is false")
    void checkGetPathsFromEnvironmentVariables() {

      final NoopHelmMojo noopHelmMojo = new NoopHelmMojo();
      noopHelmMojo.setAutoDetectLocalHelmBinary(false);
      final String[] pathsFromEnvironmentVariables =
          noopHelmMojo.getPathsFromEnvironmentVariables();
      assertNotNull(
          pathsFromEnvironmentVariables, "pathsFromEnvironmentVariables shouldn't be null");
      assertFalse(
          pathsFromEnvironmentVariables.length == 0,
          "pathsFromEnvironmentVariables shouldn't be empty");
    }

    @Test
    void helmIsAutoDetectedFromPATH() throws MojoExecutionException, IOException {

      final Path expectedPath = addHelmToTestPath();
      assertEquals(expectedPath, subjectSpy.getHelmExecutablePath());
    }

    @Test
    void executionFailsWhenHelmIsNotFoundInPATH() {

      final MojoExecutionException exception =
          assertThrows(MojoExecutionException.class, subjectSpy::getHelmExecutablePath);
      assertTrue(exception.getMessage().contains("not found"));
    }

    @Test
    void helmIsAutoDetectedEvenWhenExecutableDirectoryIsConfigured()
        throws IOException, MojoExecutionException {

      final String explicitExecutableDirectory = "/fish/in/da/sea";
      subjectSpy.setHelmExecutableDirectory(explicitExecutableDirectory);
      final Path expectedPath = addHelmToTestPath();
      assertEquals(expectedPath, subjectSpy.getHelmExecutablePath());
      assertNotEquals(explicitExecutableDirectory, subjectSpy.getHelmExecutablePath());
    }
  }

  @Nested
  class GetAuthentication {

    private final HelmRepository helmRepo =
        HelmRepository.builder()
            .name("DUMMY_REPO_NAME")
            .type(RepoType.ARTIFACTORY)
            .url("http://localhost:5000")
            .username("DUMMY_REPO_USERNAME")
            .password("DUMMY_REPO_PASSWORD")
            .build();

    @Test
    void repoUsernameNotEmptyRepoPasswordEmpty() {

      helmRepo.setPassword(EMPTY);
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            subjectSpy.getAuthentication(helmRepo);
          });
    }

    @Test
    void repoWithUsernameAndPassword() throws MojoExecutionException {

      final PasswordAuthentication result = subjectSpy.getAuthentication(helmRepo);
      assertEquals(helmRepo.getUsername(), result.getUserName());
      assertEquals(helmRepo.getPassword(), new String(result.getPassword()));
    }

    @Test
    void lookUpSettingWithNullServer() throws MojoExecutionException {

      helmRepo.setUsername(EMPTY);
      final Settings settings = mock(Settings.class);
      doReturn(null).when(settings).getServer(anyString());
      subjectSpy.setSettings(settings);

      assertNull(subjectSpy.getAuthentication(helmRepo));
    }

    @Test
    void lookUpSettingWithProperServer() throws MojoExecutionException, SecDispatcherException {

      final String dummyServerUsername = "DUMMY_SERVER_USERNAME";
      final String dummyServerPasswordEncrypted = "DUMMY_SERVER_DECRYPTED_PASSWORD";
      helmRepo.setUsername(EMPTY);
      final Settings settings = mock(Settings.class);
      final Server server = mock(Server.class);
      doReturn(dummyServerUsername).when(server).getUsername();
      doReturn("DUMMY_SERVER_PASSWORD").when(server).getPassword();
      doReturn(server).when(settings).getServer(anyString());
      subjectSpy.setSettings(settings);

      SecDispatcher secDispatcher = mock(SecDispatcher.class);
      doReturn(dummyServerPasswordEncrypted).when(secDispatcher).decrypt(anyString());
      subjectSpy.setSecurityDispatcher(secDispatcher);

      final PasswordAuthentication result = subjectSpy.getAuthentication(helmRepo);
      assertEquals(dummyServerUsername, result.getUserName());
      assertEquals(dummyServerPasswordEncrypted, new String(result.getPassword()));
    }

    @Test
    void lookUpSettingWithImproperServer() throws SecDispatcherException {

      final String dummyServerUsername = "DUMMY_SERVER_USERNAME";
      helmRepo.setUsername(EMPTY);
      final Settings settings = mock(Settings.class);
      final Server server = mock(Server.class);
      doReturn(dummyServerUsername).when(server).getUsername();
      doReturn("DUMMY_SERVER_PASSWORD").when(server).getPassword();
      doReturn(server).when(settings).getServer(anyString());
      subjectSpy.setSettings(settings);

      SecDispatcher secDispatcher = mock(SecDispatcher.class);
      doThrow(SecDispatcherException.class).when(secDispatcher).decrypt(anyString());
      subjectSpy.setSecurityDispatcher(secDispatcher);

      assertThrows(
          MojoExecutionException.class,
          () -> {
            subjectSpy.getAuthentication(helmRepo);
          });
    }

    @Test
    void lookUpSettingWithServerEmptyUsername() {

      helmRepo.setUsername(EMPTY);
      final Settings settings = mock(Settings.class);
      final Server server = mock(Server.class);
      doReturn(EMPTY).when(server).getUsername();
      doReturn(server).when(settings).getServer(anyString());
      subjectSpy.setSettings(settings);

      assertThrows(
          IllegalArgumentException.class,
          () -> {
            subjectSpy.getAuthentication(helmRepo);
          });
    }

    @Test
    void lookUpSettingWithServerEmptyPassword() {

      helmRepo.setUsername(EMPTY);
      final Settings settings = mock(Settings.class);
      final Server server = mock(Server.class);
      doReturn("DUMMY_SERVER_USERNAME").when(server).getUsername();
      doReturn(EMPTY).when(server).getPassword();
      doReturn(server).when(settings).getServer(anyString());
      subjectSpy.setSettings(settings);

      assertThrows(
          IllegalArgumentException.class,
          () -> {
            subjectSpy.getAuthentication(helmRepo);
          });
    }
  }

  @Nested
  class WhenExecutableDirectoryIsSpecifiedAndUseLocalBinaryIsDisabled {

    @BeforeEach
    void setUp() {

      subjectSpy.setUseLocalHelmBinary(false);
      subjectSpy.setHelmExecutableDirectory(testPath.toString());
    }

    @Test
    void helmIsInTheExplicitlyConfiguredDirectory() throws MojoExecutionException, IOException {

      final Path expectedPath = addHelmToTestPath();
      assertEquals(expectedPath, subjectSpy.getHelmExecutablePath());
    }

    @Test
    void executionFailsWhenHelmIsNotFoundInConfiguredDirectory() {

      final MojoExecutionException exception =
          assertThrows(MojoExecutionException.class, subjectSpy::getHelmExecutablePath);
      assertTrue(exception.getMessage().contains("not found"));
    }
  }

  @Nested
  class GetValuesOptions {

    @Test
    public void noValueOverrideNoValueOption() {
      assertEquals(
          EMPTY, subjectSpy.getValuesOptions(), "without any configuration no values options ");
    }

    @Test
    public void normalValueOverride() {

      ValueOverride override = new ValueOverride();
      override.setOverrides(
          new Gson().toJson(ImmutableMap.<String, String>builder().put("key1", "value1").build()));
      subjectSpy.setValues(override);

      assertEquals(" --set key1=value1", subjectSpy.getValuesOptions());
    }

    @Test
    public void multipleNormalValueOverrides() {

      ValueOverride override = new ValueOverride();
      override.setOverrides(
          new Gson()
              .toJson(
                  ImmutableMap.<String, String>builder()
                      .put("key1", "value1")
                      .put("key2", "value2")
                      .build()));
      subjectSpy.setValues(override);

      assertEquals(" --set key1=value1,key2=value2", subjectSpy.getValuesOptions());
    }

    @Test
    public void stringValueOverrides() {

      ValueOverride override = new ValueOverride();
      override.setStringOverrides(
          new Gson()
              .toJson(
                  ImmutableMap.<String, String>builder()
                      .put("key1", "value1")
                      .put("key2", "value2")
                      .build()));
      subjectSpy.setValues(override);

      assertEquals(" --set-string key1=value1,key2=value2", subjectSpy.getValuesOptions());
    }

    @Test
    public void fileValueOverrides() {

      ValueOverride override = new ValueOverride();
      override.setFileOverrides(
          new Gson()
              .toJson(
                  ImmutableMap.<String, String>builder()
                      .put("key1", "path/to/file1.txt")
                      .put("key2", "D:/absolute/path/to/file2.txt")
                      .build()));
      subjectSpy.setValues(override);

      assertEquals(
          " --set-file key1=path/to/file1.txt,key2=D:/absolute/path/to/file2.txt",
          subjectSpy.getValuesOptions());
    }

    @Test
    public void valueYamlOverride() {
      ValueOverride override = new ValueOverride();
      override.setYamlFile("path/to/values.yaml");
      subjectSpy.setValues(override);

      assertEquals(" --values path/to/values.yaml", subjectSpy.getValuesOptions());
    }

    @Test
    public void allOverrideUsedTogether() {
      ValueOverride override = new ValueOverride();
      override.setOverrides(
          new Gson()
              .toJson(
                  ImmutableMap.<String, String>builder()
                      .put("key1", "value1")
                      .put("key2", "value2")
                      .build()));
      override.setStringOverrides(
          new Gson()
              .toJson(
                  ImmutableMap.<String, String>builder()
                      .put("skey1", "svalue1")
                      .put("skey2", "svalue2")
                      .build()));
      override.setFileOverrides(
          new Gson()
              .toJson(
                  ImmutableMap.<String, String>builder()
                      .put("fkey1", "path/to/file1.txt")
                      .put("fkey2", "D:/absolute/path/to/file2.txt")
                      .build()));
      override.setYamlFile("path/to/values.yaml");
      subjectSpy.setValues(override);

      assertEquals(
          new StringBuilder()
              .append(
                  " --set key1=value1,key2=value2 --set-string skey1=svalue1,skey2=svalue2 --set-file ")
              .append(
                  "fkey1=path/to/file1.txt,fkey2=D:/absolute/path/to/file2.txt --values path/to/values.yaml")
              .toString(),
          subjectSpy.getValuesOptions());
    }
  }
}
