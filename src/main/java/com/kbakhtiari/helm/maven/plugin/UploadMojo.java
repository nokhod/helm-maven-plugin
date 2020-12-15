package com.kbakhtiari.helm.maven.plugin;

import com.kbakhtiari.helm.maven.plugin.pojo.HelmRepository;
import lombok.Data;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@Data
@Mojo(name = "upload", defaultPhase = LifecyclePhase.DEPLOY)
public class UploadMojo extends AbstractHelmMojo {

  @Parameter(property = "helm.upload.skip", defaultValue = "false")
  private boolean skipUpload;

  public void execute() throws MojoExecutionException {

    if (skip || skipUpload) {
      getLog().info("Skip upload");
      return;
    }

    getLog().info(format(LOG_TEMPLATE, "Uploading to " + getHelmUploadUrl()));
    for (String chartPackageFile : getChartTgzs(getOutputDirectory())) {
      getLog().info(format(LOG_TEMPLATE, "Uploading " + chartPackageFile));
      try {
        uploadSingle(chartPackageFile);
      } catch (IOException e) {
        getLog().error(e.getMessage());
        throw new MojoExecutionException(
            "Error uploading " + chartPackageFile + " to " + getHelmUploadUrl(), e);
      }
    }
  }

  protected void uploadSingle(String file) throws IOException, MojoExecutionException {

    final File fileToUpload = new File(file);
    final HelmRepository uploadRepo = getHelmUploadRepo();

    HttpURLConnection connection;

    if (Objects.isNull(uploadRepo.getType())) {
      throw new IllegalArgumentException(
          "Repository type missing. Check your plugin configuration.");
    }

    switch (uploadRepo.getType()) {
      case ARTIFACTORY:
        connection = getConnectionForUploadToArtifactory(fileToUpload);
        break;
      case CHARTMUSEUM:
        connection = getConnectionForUploadToChartmuseum();
        break;
      case NEXUS:
        connection = getConnectionForUploadToNexus(fileToUpload);
        break;
      default:
        throw new IllegalArgumentException("Unsupported repository type for upload.");
    }

    try (FileInputStream fileInputStream = new FileInputStream(fileToUpload)) {
      IOUtils.copy(fileInputStream, connection.getOutputStream());
    }

    if (connection.getResponseCode() >= 300) {
      String response;
      if (connection.getErrorStream() != null) {
        response = IOUtils.toString(connection.getErrorStream(), Charset.defaultCharset());
      } else if (connection.getInputStream() != null) {
        response = IOUtils.toString(connection.getInputStream(), Charset.defaultCharset());
      } else {
        response = "No details provided";
      }
      throw new RuntimeException(response);
    } else {
      String message = Integer.toString(connection.getResponseCode());
      if (connection.getInputStream() != null) {
        message += " - " + IOUtils.toString(connection.getInputStream(), Charset.defaultCharset());
      }
      getLog().info(message);
    }
    connection.disconnect();
  }

  protected HttpURLConnection getConnectionForUploadToChartmuseum() throws IOException {

    final HttpURLConnection connection =
        (HttpURLConnection) new URL(getHelmUploadUrl()).openConnection();
    connection.setDoOutput(true);
    connection.setRequestMethod("POST");
    connection.setRequestProperty("Content-Type", "application/gzip");

    setBasicAuthHeader(connection);

    return connection;
  }

  private void setBasicAuthHeader(HttpURLConnection connection) {
    HelmRepository helmUploadRepo = getHelmUploadRepo();
    if (isNotEmpty(helmUploadRepo.getUsername()) && isNotEmpty(helmUploadRepo.getPassword())) {
      String encoded =
          Base64.getEncoder()
              .encodeToString(
                  (helmUploadRepo.getUsername() + ":" + helmUploadRepo.getPassword())
                      .getBytes(StandardCharsets.UTF_8));
      connection.setRequestProperty("Authorization", "Basic " + encoded);
    }
  }

  protected HttpURLConnection getConnectionForUploadToArtifactory(File file)
      throws IOException, MojoExecutionException {

    final HttpURLConnection connection = getHttpURLConnection(file);
    connection.setRequestProperty("Content-Type", "application/gzip");
    verifyAndSetAuthentication();
    return connection;
  }

  private HttpURLConnection getHttpURLConnection(File file) throws IOException {
    String uploadUrl = getHelmUploadUrl();
    // Append slash if not already in place
    if (!uploadUrl.endsWith("/")) {
      uploadUrl += "/";
    }
    uploadUrl = uploadUrl + file.getName();

    final HttpURLConnection connection = (HttpURLConnection) new URL(uploadUrl).openConnection();
    connection.setDoOutput(true);
    connection.setRequestMethod("PUT");
    return connection;
  }

  protected HttpURLConnection getConnectionForUploadToNexus(File file)
      throws IOException, MojoExecutionException {

    final HttpURLConnection connection = getHttpURLConnection(file);
    setBasicAuthHeader(connection);
    return connection;
  }

  private void verifyAndSetAuthentication() throws MojoExecutionException {

    PasswordAuthentication authentication = getAuthentication(getHelmUploadRepo());
    if (Objects.isNull(authentication)) {
      throw new IllegalArgumentException(
          "Credentials has to be configured for uploading to Artifactory.");
    }

    Authenticator.setDefault(
        new Authenticator() {
          @Override
          protected PasswordAuthentication getPasswordAuthentication() {
            return authentication;
          }
        });
  }
}
