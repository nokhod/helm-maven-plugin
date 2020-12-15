package com.kbakhtiari.helm.maven.plugin;

import com.kbakhtiari.helm.maven.plugin.junit.MojoExtension;
import com.kbakhtiari.helm.maven.plugin.junit.MojoProperty;
import com.kbakhtiari.helm.maven.plugin.junit.SystemPropertyExtension;
import com.kbakhtiari.helm.maven.plugin.pojo.HelmRepository;
import com.kbakhtiari.helm.maven.plugin.pojo.RepoType;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.settings.Server;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@Disabled
@ExtendWith({SystemPropertyExtension.class, MojoExtension.class})
@MojoProperty(
    name = "helmDownloadUrl",
    value = "https://get.helm.sh/helm-v2.14.3-linux-amd64.tar.gz")
@MojoProperty(name = "chartDirectory", value = "junit-helm")
@MojoProperty(name = "chartVersion", value = "0.0.1")
public class UploadMojoTest {

  @Test
  public void uploadToArtifactoryRequiresCredentials(UploadMojo mojo)
      throws MojoExecutionException {
    final HelmRepository helmRepo = new HelmRepository();
    helmRepo.setType(RepoType.ARTIFACTORY);
    helmRepo.setName("my-artifactory");
    helmRepo.setUrl("https://somwhere.com/repo");
    mojo.setUploadRepoStable(helmRepo);

    URL resource = this.getClass().getResource("app-0.1.0.tgz");
    final List<String> tgzs = new ArrayList<>();
    tgzs.add(resource.getFile());

    doReturn(helmRepo).when(mojo).getHelmUploadRepo();
    doReturn(tgzs).when(mojo).getChartTgzs(anyString());

    assertThrows(IllegalArgumentException.class, mojo::execute, "Missing credentials must fail.");
  }

  @Test
  public void uploadToArtifactoryWithRepositoryCredentials(UploadMojo mojo)
      throws IOException, MojoExecutionException {
    final HelmRepository helmRepo = new HelmRepository();
    helmRepo.setType(RepoType.ARTIFACTORY);
    helmRepo.setName("my-artifactory");
    helmRepo.setUrl("https://somwhere.com/repo");
    helmRepo.setUsername("foo");
    helmRepo.setPassword("bar");
    mojo.setUploadRepoStable(helmRepo);

    final URL resource = this.getClass().getResource("app-0.1.0.tgz");
    final File fileToUpload = new File(resource.getFile());
    final List<String> tgzs = new ArrayList<>();
    tgzs.add(resource.getFile());

    doReturn(helmRepo).when(mojo).getHelmUploadRepo();
    doReturn(tgzs).when(mojo).getChartTgzs(anyString());

    assertNotNull(mojo.getConnectionForUploadToArtifactory(fileToUpload));
  }

  @Test
  public void uploadToArtifactoryWithPlainCredentialsFromSettings(UploadMojo mojo)
      throws IOException, MojoExecutionException {
    final Server server = new Server();
    server.setId("my-artifactory");
    server.setUsername("foo");
    server.setPassword("bar");
    final List<Server> servers = new ArrayList<>();
    servers.add(server);
    mojo.getSettings().setServers(servers);

    final HelmRepository helmRepo = new HelmRepository();
    helmRepo.setType(RepoType.ARTIFACTORY);
    helmRepo.setName("my-artifactory");
    helmRepo.setUrl("https://somwhere.com/repo");
    mojo.setUploadRepoStable(helmRepo);

    final URL resource = this.getClass().getResource("app-0.1.0.tgz");
    final File fileToUpload = new File(resource.getFile());
    final List<String> tgzs = new ArrayList<>();
    tgzs.add(resource.getFile());

    doReturn(helmRepo).when(mojo).getHelmUploadRepo();
    doReturn(tgzs).when(mojo).getChartTgzs(anyString());

    assertNotNull(mojo.getConnectionForUploadToArtifactory(fileToUpload));
  }

  @Test
  public void uploadToArtifactoryWithEncryptedCredentialsFromSettings(UploadMojo mojo)
      throws IOException, MojoExecutionException {

    final Server server = new Server();
    server.setId("artifactory");
    server.setUsername("foo");
    server.setPassword("{GGhJc6qP+v0Hg2l+dei1MQFZt/55PzyFXY0MUMxcQdQ=}");
    mojo.getSettings().setServers(asList(server));

    final HelmRepository helmRepo = new HelmRepository();
    helmRepo.setType(RepoType.ARTIFACTORY);
    helmRepo.setName("artifactory");
    helmRepo.setUrl("https://somwhere.com/repo");
    mojo.setUploadRepoStable(helmRepo);

    final URL resource = this.getClass().getResource("app-0.1.0.tgz");
    final File fileToUpload = new File(resource.getFile());

    doReturn(this.getClass().getResource("settings-security.xml").getFile())
        .when(mojo)
        .getHelmSecurity();
    doReturn(helmRepo).when(mojo).getHelmUploadRepo();
    doReturn(asList(resource.getFile())).when(mojo).getChartTgzs(anyString());

    assertNotNull(mojo.getConnectionForUploadToArtifactory(fileToUpload));

    final PasswordAuthentication pwd =
        Authenticator.requestPasswordAuthentication(
            InetAddress.getLocalHost(), 443, "https", "", "basicauth");
    assertEquals("foo", pwd.getUserName());
    assertEquals("bar", String.valueOf(pwd.getPassword()));
  }

  @Test
  public void verifyHttpConnectionForArtifactoryUpload(UploadMojo uploadMojo)
      throws IOException, MojoExecutionException {
    final URL resource = this.getClass().getResource("app-0.1.0.tgz");
    final File fileToUpload = new File(resource.getFile());

    final HelmRepository helmRepo = new HelmRepository();
    helmRepo.setType(RepoType.ARTIFACTORY);
    helmRepo.setName("my-artifactory");
    helmRepo.setUrl("https://somwhere.com/repo");
    helmRepo.setUsername("foo");
    helmRepo.setPassword("bar");
    uploadMojo.setUploadRepoStable(helmRepo);

    // Call
    HttpURLConnection httpURLConnection =
        uploadMojo.getConnectionForUploadToArtifactory(fileToUpload);

    // Verify
    assertEquals("PUT", httpURLConnection.getRequestMethod());
    String expectedUploadUrl = helmRepo.getUrl() + "/" + fileToUpload.getName();
    assertEquals(expectedUploadUrl, httpURLConnection.getURL().toString());

    String contentTypeHeader = httpURLConnection.getRequestProperty("Content-Type");
    assertNotNull(contentTypeHeader);
    assertEquals("application/gzip", contentTypeHeader);
  }

  @Test
  public void verifyHttpConnectionForChartmuseumUpload(UploadMojo uploadMojo) throws IOException {
    final HelmRepository helmRepo = new HelmRepository();
    helmRepo.setType(RepoType.CHARTMUSEUM);
    helmRepo.setName("my-chartmuseum");
    helmRepo.setUrl("https://somwhere.com/repo");
    uploadMojo.setUploadRepoStable(helmRepo);

    // Call
    HttpURLConnection httpURLConnection = uploadMojo.getConnectionForUploadToChartmuseum();

    // Verify
    assertEquals("POST", httpURLConnection.getRequestMethod());
    assertEquals(helmRepo.getUrl(), httpURLConnection.getURL().toString());

    String contentTypeHeader = httpURLConnection.getRequestProperty("Content-Type");
    assertNotNull(contentTypeHeader);
    assertEquals("application/gzip", contentTypeHeader);
  }

  @Test
  public void verifyUploadToArtifactory(UploadMojo uploadMojo)
      throws MojoExecutionException, IOException {
    final URL resource = this.getClass().getResource("app-0.1.0.tgz");
    final File fileToUpload = new File(resource.getFile());
    final List<String> tgzs = new ArrayList<>();
    tgzs.add(resource.getFile());

    final HelmRepository helmRepo = new HelmRepository();
    helmRepo.setType(RepoType.ARTIFACTORY);
    helmRepo.setName("my-artifactory");
    helmRepo.setUrl("https://somwhere.com/repo");
    helmRepo.setUsername("foo");
    helmRepo.setPassword("bar");
    uploadMojo.setUploadRepoStable(helmRepo);

    doReturn(helmRepo).when(uploadMojo).getHelmUploadRepo();
    doReturn(tgzs).when(uploadMojo).getChartTgzs(anyString());

    HttpURLConnection urlConnectionMock = Mockito.mock(HttpURLConnection.class);
    doReturn(new NullOutputStream()).when(urlConnectionMock).getOutputStream();
    doReturn(new ByteArrayInputStream("ok".getBytes(StandardCharsets.UTF_8)))
        .when(urlConnectionMock)
        .getInputStream();
    doNothing().when(urlConnectionMock).connect();
    doReturn(urlConnectionMock).when(uploadMojo).getConnectionForUploadToArtifactory(fileToUpload);

    // call Mojo
    uploadMojo.execute();

    verify(uploadMojo).getConnectionForUploadToArtifactory(fileToUpload);
  }

  @Test
  public void repositoryTypeRequired(UploadMojo uploadMojo) throws MojoExecutionException {
    final HelmRepository helmRepo = new HelmRepository();
    helmRepo.setName("unknown-repo");
    helmRepo.setUrl("https://somwhere.com/repo");
    uploadMojo.setUploadRepoStable(helmRepo);

    URL resource = this.getClass().getResource("app-0.1.0.tgz");
    final List<String> tgzs = new ArrayList<>();
    tgzs.add(resource.getFile());

    doReturn(helmRepo).when(uploadMojo).getHelmUploadRepo();
    doReturn(tgzs).when(uploadMojo).getChartTgzs(anyString());

    assertThrows(
        IllegalArgumentException.class, uploadMojo::execute, "Missing repo type must fail.");
  }

  @Test
  public void verfifyNullErrorStreamOnFailedUpload(UploadMojo uploadMojo)
      throws IOException, MojoExecutionException {
    final HelmRepository helmRepo = new HelmRepository();
    helmRepo.setType(RepoType.CHARTMUSEUM);
    helmRepo.setName("my-chartmuseum");
    helmRepo.setUrl("https://somwhere.com/repo");
    uploadMojo.setUploadRepoStable(helmRepo);

    URL testChart = this.getClass().getResource("app-0.1.0.tgz");

    final HttpURLConnection urlConnectionMock = Mockito.mock(HttpURLConnection.class);
    doReturn(new NullOutputStream()).when(urlConnectionMock).getOutputStream();
    doReturn(301).when(urlConnectionMock).getResponseCode();
    doReturn(null).when(urlConnectionMock).getErrorStream();
    doReturn(null).when(urlConnectionMock).getInputStream();
    doNothing().when(urlConnectionMock).connect();
    doReturn(urlConnectionMock).when(uploadMojo).getConnectionForUploadToChartmuseum();

    try {
      uploadMojo.uploadSingle(testChart.getFile());
    } catch (Exception e) {
      assertNotNull(e.getMessage(), "Exception must provide a message");
      return;
    }
    fail("BadUploadException expected on failed upload");
  }

  /** Writes to nowhere */
  public class NullOutputStream extends OutputStream {
    @Override
    public void write(int b) throws IOException {}
  }
}
