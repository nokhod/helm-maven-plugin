package com.kbakhtiari.helm.maven.plugin;

import com.kbakhtiari.helm.maven.plugin.junit.MojoExtension;
import com.kbakhtiari.helm.maven.plugin.junit.MojoProperty;
import com.kbakhtiari.helm.maven.plugin.junit.SystemPropertyExtension;
import com.kbakhtiari.helm.maven.plugin.pojo.HelmRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import static java.util.Arrays.asList;
import static java.util.Collections.EMPTY_LIST;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@MojoProperty(name = "chartDirectory", value = "junit-helm")
@ExtendWith({SystemPropertyExtension.class, MojoExtension.class})
class PushMojoTest {

  private static final HelmRepository REGISTRY =
      HelmRepository.builder()
          .name("gitlab")
          .url("registry.gitlab.example.com")
          .username("DUMMY_USERNAME")
          .password("DUMMY_PASSWORD")
          .build();

  @Test
  void checkRegistryIsNull(PushMojo pushMojo) throws MojoExecutionException {

    doReturn(null).when(pushMojo).getHelmUploadRepo();

    pushMojo.execute();

    verify(pushMojo).getHelmUploadRepo();
    verify(pushMojo, never()).callCli(anyString(), anyString());
  }

  @Test
  void checkRegistryLogin(PushMojo pushMojo) throws MojoExecutionException {

    doReturn(REGISTRY).when(pushMojo).getHelmUploadRepo();
    doNothing().when(pushMojo).callCli(anyString(), anyString());
    doReturn(EMPTY).when(pushMojo).getHelmCommand(anyString(), anyString());
    doReturn(EMPTY_LIST).when(pushMojo).getChartTgzs(anyString());

    pushMojo.execute();

    verify(pushMojo).getHelmUploadRepo();

    ArgumentCaptor<String> captorCommand = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> captorArgument = ArgumentCaptor.forClass(String.class);

    verify(pushMojo).getHelmCommand(captorCommand.capture(), captorArgument.capture());
    assertTrue(
        captorCommand
            .getValue()
            .contains(
                "registry login -u DUMMY_USERNAME -p DUMMY_PASSWORD registry.gitlab.example.com"));
    assertTrue(captorArgument.getValue().equals(EMPTY));
  }

  @Test
  void checkUploadSingle(PushMojo pushMojo) throws MojoExecutionException {

    doReturn(REGISTRY).when(pushMojo).getHelmUploadRepo();
    doNothing().when(pushMojo).callCli(anyString(), anyString());
    doReturn(EMPTY).when(pushMojo).getHelmCommand(anyString(), anyString());
    doReturn(asList("DUMMY/PATH")).when(pushMojo).getChartTgzs(anyString());
    doReturn("DUMMY_RELEASE_NAME").when(pushMojo).getReleaseName();
    doReturn("0.0.1-SNAPSHOT").when(pushMojo).getChartVersion();

    pushMojo.execute();

    verify(pushMojo).getHelmUploadRepo();

    ArgumentCaptor<String> captorCommand = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> captorArgument = ArgumentCaptor.forClass(String.class);

    verify(pushMojo, atLeast(3)).getHelmCommand(captorCommand.capture(), captorArgument.capture());
    assertEquals(
        1,
        captorCommand.getAllValues().stream()
            .filter(
                value ->
                    value.contains(
                        "registry login -u DUMMY_USERNAME -p DUMMY_PASSWORD registry.gitlab.example.com"))
            .count());
    assertEquals(
        1,
        captorCommand.getAllValues().stream()
            .filter(value -> value.contains("chart save"))
            .count());
    assertEquals(
        1,
        captorCommand.getAllValues().stream()
            .filter(value -> value.contains("chart push"))
            .count());
    assertEquals(
        1,
        captorArgument.getAllValues().stream()
            .filter(
                value ->
                    value.contains(
                        "DUMMY/PATH registry.gitlab.example.com/DUMMY_RELEASE_NAME:0.0.1-SNAPSHOT"))
            .count());
    assertEquals(
        2,
        captorArgument.getAllValues().stream()
            .filter(
                value ->
                    value.contains("registry.gitlab.example.com/DUMMY_RELEASE_NAME:0.0.1-SNAPSHOT"))
            .count());
  }

  @Nested
  @MojoProperty(name = "chartDirectory", value = "junit-helm")
  @ExtendWith({SystemPropertyExtension.class, MojoExtension.class})
  class SkipTest {

    @Test
    void checkDoNothingWhenSkipIsTrue(PushMojo pushMojo) throws MojoExecutionException {

      pushMojo.setSkip(true);
      pushMojo = spy(pushMojo);

      pushMojo.execute();

      verify(pushMojo, never()).callCli(anyString(), anyString());
    }

    @Test
    void checkDefaultValueOfSkip(PushMojo pushMojo) throws MojoExecutionException {

      assertFalse(pushMojo.isSkipPush());
    }

    @Test
    void checkDoNothingWhenSkipPushIsTrue(PushMojo pushMojo) throws MojoExecutionException {

      pushMojo.setSkipPush(true);
      pushMojo = spy(pushMojo);

      pushMojo.execute();

      verify(pushMojo, never()).callCli(anyString(), anyString());
    }
  }
}
