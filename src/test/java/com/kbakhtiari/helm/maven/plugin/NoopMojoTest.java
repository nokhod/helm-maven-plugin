package com.kbakhtiari.helm.maven.plugin;

import com.kbakhtiari.helm.maven.plugin.junit.MojoExtension;
import com.kbakhtiari.helm.maven.plugin.junit.MojoProperty;
import com.kbakhtiari.helm.maven.plugin.junit.SystemPropertyExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static com.kbakhtiari.helm.maven.plugin.utils.Constants.MojoDefaultConstants.HELM_SECURITY;
import static com.kbakhtiari.helm.maven.plugin.utils.Constants.MojoDefaultConstants.HELM_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith({SystemPropertyExtension.class, MojoExtension.class})
@MojoProperty(name = "chartDirectory", value = "some-dummy-directory")
class NoopMojoTest {

  @Test
  @DisplayName("check to see if the default value of skip is false")
  void checkSkipDefault(NoopMojo mojo) {

    assertFalse(mojo.isSkip(), "the default value of skip is not false");
  }

  @Test
  @DisplayName("check to see if the default value of verbose is false")
  void checkVerboseDefault(NoopMojo mojo) {

    assertFalse(mojo.isVerbose(), "the default value of verbose is not false");
  }

  @Test
  @DisplayName("check to see if the default value of chartDirectory is some-dummy-directory")
  void checkChartDirectoryDefault(NoopMojo mojo) {

    assertEquals(
        "some-dummy-directory",
        mojo.getChartDirectory(),
        "the default value of chartDirectory is not some-dummy-directory");
  }

  @Test
  @DisplayName("check the default value of helmExecutableDirectory")
  void checkHelmExecutableDirectoryDefault(NoopMojo mojo) {

    assertTrue(
        mojo.getHelmExecutableDirectory().endsWith("/helm"),
        "the default value of helmExecutableDirectory is not ending with /helm");
  }

  @Test
  @DisplayName("check the default value of outputDirectory")
  void checkOutputDirectoryDefault(NoopMojo mojo) {

    assertTrue(
        mojo.getOutputDirectory().endsWith("/helm/repo"),
        "the default value of outputDirectory is not ending with /helm/repo");
  }

  @Test
  @DisplayName("check to see if the default value of autoDetectLocalHelmBinary is false")
  void checkAutoDetectLocalHelmBinaryDefault(NoopMojo mojo) {

    assertFalse(
        mojo.isAutoDetectLocalHelmBinary(),
        "the default value of autoDetectLocalHelmBinary is not false");
  }

  @Test
  @DisplayName("check to see if the default value of useLocalHelmBinary is false")
  void checkUseLocalHelmBinaryDefault(NoopMojo mojo) {

    assertFalse(
        mojo.isUseLocalHelmBinary(), "the default value of useLocalHelmBinary is not false");
  }

  @Test
  @DisplayName("check to see if the default value of securityDispatcher is default")
  void checkSecurityDispatcherDefault(NoopMojo mojo) {

    assertNotNull(
        mojo.getSecurityDispatcher(), "the default value of securityDispatcher must not be null");
  }

  @Test
  @DisplayName("check to see if the default value of chartVersion is null")
  void checkChartVersionDefault(NoopMojo mojo) {

    assertNull(mojo.getChartVersion(), "the default value of chartVersion must be null");
  }

  @Test
  @DisplayName("check to see if the default value of appVersion is null")
  void checkAppVersionDefault(NoopMojo mojo) {

    assertNull(mojo.getAppVersion(), "the default value of appVersion must be null");
  }

  @Test
  @DisplayName("check to see if the default value of uploadRepoStable is null")
  void checkUploadRepoStableDefault(NoopMojo mojo) {

    assertNull(mojo.getUploadRepoStable(), "the default value of uploadRepoStable must be null");
  }

  @Test
  @DisplayName("check to see if the default value of uploadRepoSnapshot is null")
  void checkUploadRepoSnapshotDefault(NoopMojo mojo) {

    assertNull(
        mojo.getUploadRepoSnapshot(), "the default value of uploadRepoSnapshot must be null");
  }

  @Test
  @DisplayName("check to see if the default value of helmDownloadUrl is null")
  void checkHelmDownloadUrlDefault(NoopMojo mojo) {

    assertNull(mojo.getHelmDownloadUrl(), "the default value of helmDownloadUrl must be null");
  }

  @Test
  @DisplayName("check to see if the default value of registryConfig is null")
  void checkRegistryConfigDefault(NoopMojo mojo) {

    assertNull(mojo.getRegistryConfig(), "the default value of registryConfig must be null");
  }

  @Test
  @DisplayName("check to see if the default value of registryUrl is null")
  void checkRegistryUrlDefault(NoopMojo mojo) {

    assertNull(mojo.getRegistryUrl(), "the default value of registryUrl must be null");
  }

  @Test
  @DisplayName("check to see if the default value of helmVersion is " + HELM_VERSION)
  void checkHelmVersionDefault(NoopMojo mojo) {

    assertEquals(
        HELM_VERSION,
        mojo.getHelmVersion(),
        "the default value of helmVersion must be " + HELM_VERSION);
  }

  @Test
  @DisplayName("check to see if the default value of helmSecurity is " + HELM_SECURITY)
  void checkHelmSecurityDefault(NoopMojo mojo) {

    assertEquals(
        HELM_SECURITY,
        mojo.getHelmSecurity(),
        "the default value of helmSecurity must be " + HELM_SECURITY);
  }

  @Test
  @DisplayName("check to see if the default value of excludes is null")
  void checkExcludesDefault(NoopMojo mojo) {

    assertNull(mojo.getExcludes(), "the default value of excludes must be null");
  }

  @Test
  @DisplayName("check to see if the default value of registryUsername is null")
  void checkRegistryUsernameDefault(NoopMojo mojo) {

    assertNull(mojo.getRegistryUsername(), "the default value of registryUsername must be null");
  }

  @Test
  @DisplayName("check to see if the default value of registryPassword is null")
  void checkRegistryPasswordDefault(NoopMojo mojo) {

    assertNull(mojo.getRegistryPassword(), "the default value of registryPassword must be null");
  }

  @Test
  @DisplayName("check to see if the default value of repositoryCache is null")
  void checkRepositoryCacheDefault(NoopMojo mojo) {

    assertNull(mojo.getRepositoryCache(), "the default value of repositoryCache must be null");
  }

  @Test
  @DisplayName("check to see if the default value of repositoryConfig is null")
  void checkRepositoryConfigDefault(NoopMojo mojo) {

    assertNull(mojo.getRepositoryConfig(), "the default value of repositoryConfig must be null");
  }

  @Test
  @DisplayName("check to see if the default value of helmExtraRepos is null")
  void checkHelmExtraReposDefault(NoopMojo mojo) {

    assertNull(mojo.getHelmExtraRepos(), "the default value of helmExtraRepos must be null");
  }

  @Test
  @DisplayName("check to see if the default value of releaseName is null")
  void checkReleaseNameDefault(NoopMojo mojo) {

    assertNull(mojo.getReleaseName(), "the default value of releaseName must be null");
  }

  @Test
  @DisplayName("check to see if the default value of namespace is null")
  void checkNamespaceDefault(NoopMojo mojo) {

    assertNull(mojo.getNamespace(), "the default value of namespace must be null");
  }

  @Test
  @DisplayName("check to see if the default value of values is null")
  void checkValuesDefault(NoopMojo mojo) {

    assertNull(mojo.getValues(), "the default value of values must be null");
  }

  @Test
  @DisplayName("check to see if the default value of settings is not null")
  void checkSettingsDefault(NoopMojo mojo) {

    assertNotNull(mojo.getSettings(), "the default value of settings must not be null");
  }
}
