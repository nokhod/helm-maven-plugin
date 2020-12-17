package com.kbakhtiari.helm.maven.plugin;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.kbakhtiari.helm.maven.plugin.junit.MojoExtension;
import com.kbakhtiari.helm.maven.plugin.junit.MojoProperty;
import com.kbakhtiari.helm.maven.plugin.junit.SystemPropertyExtension;
import com.kbakhtiari.helm.maven.plugin.pojo.ValueOverride;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@MojoProperty(name = "chartVersion", value = "0.0.1")
@MojoProperty(name = "chartDirectory", value = "junit-helm")
@ExtendWith({SystemPropertyExtension.class, MojoExtension.class})
@MojoProperty(
    name = "helmDownloadUrl",
    value = "https://get.helm.sh/helm-v2.14.3-linux-amd64.tar.gz")
class PackageMojoTest {

  private Path getBaseChartsDirectory() {
    return new File(getClass().getResource("Chart.yaml").getFile()).toPath().getParent();
  }

  @Test
  void happyFlow(PackageMojo mojo) throws MojoExecutionException, URISyntaxException {

    ValueOverride valueOverride = new ValueOverride();
    valueOverride.setOverrides(
        new Gson()
            .toJson(
                ImmutableMap.<String, Object>builder()
                    .put("namespace", "dev")
                    .put(
                        "image",
                        ImmutableMap.builder()
                            .put("tag", "dev-01f547")
                            .put("repository", "myrepo.com")
                            .build())
                    .put(
                        "ingress",
                        ImmutableMap.builder()
                            .put("enabled", "true")
                            .put(
                                "annotations",
                                ImmutableMap.builder()
                                    .put(
                                        "external-dns.alpha.kubernetes.io/target", "kbakhtiari.com")
                                    .put("nginx.ingress.kubernetes.io/rewrite-target", "/$2")
                                    .build())
                            .build())
                    .put(
                        "imageCredentials",
                        ImmutableMap.builder()
                            .put("name", "kbakhtiari-container-registry")
                            .put("registry", "myregistry.com")
                            .put("username", "myusername")
                            .put("password", "mypassword")
                            .put("email", "khodabakhsh.bakhtiari@nl.abnamro.com")
                            .build())
                    .build()));
    doReturn(getBaseChartsDirectory().toString()).when(mojo).getChartDirectory();
    doReturn(valueOverride).when(mojo).getValues();
    doReturn(mock(Path.class)).when(mojo).getHelmExecutablePath();
    doNothing().when(mojo).callCli(contains("helm"), anyString());

    mojo.execute();

    verify(mojo, atLeastOnce()).callCli(contains("helm"), anyString());
  }
}
