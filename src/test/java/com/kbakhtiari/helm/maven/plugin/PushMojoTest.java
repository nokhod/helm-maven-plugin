package com.kbakhtiari.helm.maven.plugin;

import com.kbakhtiari.helm.maven.plugin.junit.MojoExtension;
import com.kbakhtiari.helm.maven.plugin.junit.MojoProperty;
import com.kbakhtiari.helm.maven.plugin.junit.SystemPropertyExtension;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@MojoProperty(name = "chartVersion", value = "0.0.1")
@MojoProperty(name = "registryUsername", value = "myuser")
@MojoProperty(name = "registryPassword", value = "mypass")
@MojoProperty(name = "chartDirectory", value = "junit-helm")
@MojoProperty(name = "registryUrl", value = "localhost:5000")
@MojoProperty(name = "releaseName", value = "helm-maven-plugin")
@ExtendWith({SystemPropertyExtension.class, MojoExtension.class})
@MojoProperty(
    name = "helmDownloadUrl",
    value = "https://get.helm.sh/helm-v2.14.3-linux-amd64.tar.gz")
class PushMojoTest {

  private String getOutputDirectory(PushMojo mojo) throws IOException {
    final Path tempDirectory = Files.createTempFile("helm-maven-plugin-", ".tgz");
    new File(tempDirectory.toString()).deleteOnExit();
    return tempDirectory.getParent().toString();
  }

  @Test
  void happyFlow(PushMojo mojo) throws MojoExecutionException, IOException {

    doReturn(getOutputDirectory(mojo)).when(mojo).getOutputDirectory();
    doReturn(mock(Path.class)).when(mojo).getHelmExecutablePath();
    doNothing().when(mojo).callCli(contains("helm"), anyString());
    doReturn("helm").when(mojo).getHelmCommand(anyString(), anyString());

    mojo.execute();

    verify(mojo, atLeastOnce()).callCli(contains("helm"), anyString());
  }
}
