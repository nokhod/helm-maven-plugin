package com.kbakhtiari.helm.maven.plugin;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static java.nio.file.Files.write;
import static java.util.Arrays.asList;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;

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
    excludeDir1 = chartDir + File.separator + "exclude1";
    excludeDir2 = chartDir + File.separator + "exclude2";

    subjectSpy = Mockito.spy(new NoopHelmMojo());
    testPath = Files.createTempDirectory("test").toAbsolutePath();
    testHelmExecutablePath = testPath.resolve(SystemUtils.IS_OS_WINDOWS ? "helm.exe" : "helm");
  }

  @Test
  void getChartDirectoriesReturnChartDirectories() throws MojoExecutionException {

    List<String> chartDirectories = subjectSpy.getChartDirectories(chartDir);
    List<String> expected = asList(chartDir, excludeDir1, excludeDir2);

    assertTrue(
        chartDirectories.containsAll(expected),
        "Charts dirs: " + chartDirectories + ", should contain all expected dirs: " + expected);
  }

  @Test
  void testAppendOverrideMap()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

    final Method appendOverrides =
        AbstractHelmMojo.class.getDeclaredMethod("appendOverrides", Map.class);
    appendOverrides.setAccessible(true);
    final String invoke =
        (String)
            appendOverrides.invoke(
                new AbstractHelmMojo() {
                  @java.lang.Override
                  public void execute() throws MojoExecutionException, MojoFailureException {}
                },
                ImmutableMap.builder()
                    .put("ingress.annotations.external-dns.alpha.kubernetes.io/target", "nginx")
                    .build());
    assertEquals("ingress.annotations.external-dns.alpha.kubernetes.io/target=nginx", invoke);
  }

  @Test
  void getChartDirectoriesReturnChartDirectoriesWithPlainExclusion() throws MojoExecutionException {
    Path baseChartsDirectory = getBaseChartsDirectory();

    subjectSpy.setExcludes(new String[] {excludeDir1});

    List<String> chartDirectories = subjectSpy.getChartDirectories(baseChartsDirectory.toString());

    assertFalse(
        chartDirectories.contains(excludeDir1),
        "Charts dirs ["
            + chartDirectories
            + "] should not contain excluded dirs ["
            + excludeDir1
            + "]");
    assertTrue(
        chartDirectories.contains(excludeDir2),
        "Charts dirs ["
            + chartDirectories
            + "] should contain not excluded dirs ["
            + excludeDir2
            + "]");
  }

  @Test
  void getChartDirectoriesReturnChartDirectoriesWithAntPatternsExclusion()
      throws MojoExecutionException {
    Path baseChartsDirectory = getBaseChartsDirectory();

    subjectSpy.setExcludes(new String[] {"**" + File.separator + "exclude*"});

    List<String> chartDirectories = subjectSpy.getChartDirectories(baseChartsDirectory.toString());

    assertFalse(
        chartDirectories.contains(excludeDir1),
        "Charts dirs ["
            + chartDirectories
            + "] should not contain excluded dirs ["
            + excludeDir1
            + "]");
    assertFalse(
        chartDirectories.contains(excludeDir2),
        "Charts dirs ["
            + chartDirectories
            + "] should not contain excluded dirs ["
            + excludeDir2
            + "]");
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

    @java.lang.Override
    public void execute() {
      /* Noop. */
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
}
