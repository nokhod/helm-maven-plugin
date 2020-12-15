package com.kbakhtiari.helm.maven.plugin;

import com.kbakhtiari.helm.maven.plugin.LintMojo;
import com.kbakhtiari.helm.maven.plugin.junit.MojoExtension;
import com.kbakhtiari.helm.maven.plugin.junit.MojoProperty;
import com.kbakhtiari.helm.maven.plugin.junit.SystemPropertyExtension;
import com.kbakhtiari.helm.maven.plugin.pojo.ValueOverride;
import org.codehaus.plexus.util.Os;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

@MojoProperty(name = "chartVersion", value = "0.0.1")
@MojoProperty(name = "chartDirectory", value = "junit-chart")
@ExtendWith({SystemPropertyExtension.class, MojoExtension.class})
@MojoProperty(
    name = "helmDownloadUrl",
    value = "https://get.helm.sh/helm-v2.14.3-linux-amd64.tar.gz")
public class LintMojoTest {

  @Test
  public void valuesFile(LintMojo mojo) throws Exception {

    ValueOverride override = new ValueOverride();
    override.setYamlFile("overrideValues.yaml");
    mojo.setValues(override);
    mojo.setChartDirectory(
        Paths.get(getClass().getResource("Chart.yaml").toURI()).getParent().toString());

    ArgumentCaptor<String> helmCommandCaptor = ArgumentCaptor.forClass(String.class);
    doNothing().when(mojo).callCli(helmCommandCaptor.capture(), anyString());
    doReturn(Paths.get("helm" + (Os.OS_FAMILY == Os.FAMILY_WINDOWS ? ".exe" : "")))
        .when(mojo)
        .getHelmExecutablePath();

    mojo.execute();

    assertTrue(helmCommandCaptor.getValue().contains("--values overrideValues.yaml"));
  }
}
