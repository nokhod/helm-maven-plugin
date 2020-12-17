package com.kbakhtiari.helm.maven.plugin;

import com.kbakhtiari.helm.maven.plugin.junit.MojoExtension;
import com.kbakhtiari.helm.maven.plugin.junit.MojoProperty;
import com.kbakhtiari.helm.maven.plugin.junit.SystemPropertyExtension;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.Os;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

@EnabledOnOs(OS.LINUX)
@MojoProperty(name = "chartVersion", value = "0.0.1")
@MojoProperty(name = "chartDirectory", value = "junit-helm")
@ExtendWith({SystemPropertyExtension.class, MojoExtension.class})
@MojoProperty(
    name = "helmDownloadUrl",
    value = "https://get.helm.sh/helm-v3.0.0-linux-amd64.tar.gz")
class InitMojoTest {

  @ParameterizedTest
  @ValueSource(strings = {"darwin", "linux", "windows"})
  @DisplayName("Init helm with different download urls.")
  void initMojoHappyPathWhenDownloadHelm(String os, InitMojo mojo) throws Exception {

    // prepare execution
    doNothing().when(mojo).callCli(contains("helm "), anyString());

    // getHelmExecuteablePath is system-depending and has to be mocked for that reason
    // as SystemUtils.IS_OS_WINDOWS will always return false on a *NIX system
    doReturn(Paths.get("dummy/path/to/helm").toAbsolutePath()).when(mojo).getHelmExecutablePath();
    mojo.setHelmDownloadUrl(getOsSpecificDownloadURL(os));

    // run init
    mojo.execute();

    // check helm file
    Path helm =
        Paths.get(mojo.getHelmExecutableDirectory(), "windows".equals(os) ? "helm.exe" : "helm")
            .toAbsolutePath();
    assertTrue(Files.exists(helm), "Helm executable not found at: " + helm);
  }

  @Test
  @DisplayName("Init helm with a automatically detected URL")
  void autoDownloadHelm(InitMojo mojo) throws Exception {

    // prepare execution
    doNothing().when(mojo).callCli(contains("helm "), anyString());
    // getHelmExecuteablePath is system-depending and has to be mocked for that reason
    // as SystemUtils.IS_OS_WINDOWS will always return false on a *NIX system
    doReturn(Paths.get("dummy/path/to/helm").toAbsolutePath()).when(mojo).getHelmExecutablePath();
    mojo.setHelmDownloadUrl(null);
    mojo.setHelmVersion("3.2.0");

    // run init
    mojo.execute();

    // check helm file
    Path helm =
        Paths.get(
                mojo.getHelmExecutableDirectory(),
                "windows".equals(Os.OS_FAMILY) ? "helm.exe" : "helm")
            .toAbsolutePath();
    assertTrue(Files.exists(helm), "Helm executable not found at: " + helm);
  }

  @Test
  void verifyAddingStableByDefault(InitMojo mojo) throws Exception {

    // prepare execution
    ArgumentCaptor<String> helmCommandCaptor = ArgumentCaptor.forClass(String.class);
    doNothing().when(mojo).callCli(helmCommandCaptor.capture(), anyString());
    mojo.setHelmDownloadUrl(getOsSpecificDownloadURL());
    mojo.setAddDefaultRepo(true);

    // run init
    mojo.execute();

    // check captured argument
    String helmDefaultCommand =
        helmCommandCaptor.getAllValues().stream()
            .filter(
                cmd ->
                    cmd.contains(Os.OS_FAMILY == Os.FAMILY_WINDOWS ? "helm.exe repo" : "helm repo"))
            .findAny()
            .orElseThrow(() -> new IllegalArgumentException("Only one helm repo command expected"));

    assertTrue(
        helmDefaultCommand.matches(".*repo\\s+add\\s+stable\\s+https://charts.helm.sh/stable.*"),
        "Adding stable repo by default expected");
  }

  @Test
  void verifyCustomConfigOptions(InitMojo mojo) throws Exception {

    // prepare execution
    ArgumentCaptor<String> helmCommandCaptor = ArgumentCaptor.forClass(String.class);
    doNothing().when(mojo).callCli(helmCommandCaptor.capture(), anyString());
    mojo.setHelmDownloadUrl(getOsSpecificDownloadURL());
    mojo.setRegistryConfig("/path/to/my/registry.json");
    mojo.setRepositoryCache("/path/to/my/repository/cache");
    mojo.setRepositoryConfig("/path/to/my/repositories.yaml");
    mojo.setAddDefaultRepo(true);

    // run init
    mojo.execute();

    // check captured argument
    List<String> helmCommands =
        helmCommandCaptor.getAllValues().stream()
            .filter(cmd -> cmd.contains(Os.OS_FAMILY == Os.FAMILY_WINDOWS ? "helm.exe " : "helm "))
            .collect(Collectors.toList());
    assertEquals(1, helmCommands.size(), "Only helm init command expected");
    String helmDefaultCommand = helmCommands.get(0);
    assertTrue(
        helmDefaultCommand.contains("--registry-config /path/to/my/registry.json"),
        "Option 'registry-config' not set");
    assertTrue(
        helmDefaultCommand.contains("--repository-cache /path/to/my/repository/cache"),
        "Option 'repository-cache' not set");
    assertTrue(
        helmDefaultCommand.contains("--repository-config /path/to/my/repositories.yaml"),
        "Option 'repository-config' not set");
  }

  @Test
  void verifyLocalHelmBinaryUsage(InitMojo mojo) throws MojoExecutionException {
    // Because the download URL is hardcoded to linux, only proceed if the OS is indeed linux.
    assumeTrue(isOSUnix());

    final URL resource = this.getClass().getResource("helm.tar.gz");
    final String helmExecutableDir = new File(resource.getFile()).getParent();
    mojo.callCli(
        "tar -xf "
            + helmExecutableDir
            + File.separator
            // flatten directory structure using --strip to get helm executeable on basedir, see
            // https://www.systutorials.com/docs/linux/man/1-tar/#lbAS
            + "helm.tar.gz --strip=1 --directory="
            + helmExecutableDir,
        "Unable to unpack helm to " + helmExecutableDir);

    // configure mojo
    mojo.setUseLocalHelmBinary(true);
    mojo.setHelmExecutableDirectory(helmExecutableDir);

    // execute
    mojo.execute();
  }

  private boolean isOSUnix() {
    return System.getProperty("os.name").matches(".*n[i|u]x.*");
  }

  private String getOsSpecificDownloadURL() {
    String osForDownload;
    switch (Os.OS_FAMILY) {
      case Os.FAMILY_UNIX:
        osForDownload = "linux";
        break;
      case Os.FAMILY_MAC:
        osForDownload = "darwin";
        break;
      default:
        osForDownload = Os.OS_FAMILY;
    }

    return getOsSpecificDownloadURL(osForDownload);
  }

  private String getOsSpecificDownloadURL(final String os) {
    return "https://get.helm.sh/helm-v3.4.2-"
        + os
        + "-amd64."
        + ("windows".equals(os) ? "zip" : "tar.gz");
  }
}
