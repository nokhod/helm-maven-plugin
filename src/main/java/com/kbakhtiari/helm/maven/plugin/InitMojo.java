package com.kbakhtiari.helm.maven.plugin;

import com.kbakhtiari.helm.maven.plugin.pojo.HelmRepository;
import com.kbakhtiari.helm.maven.plugin.utils.ArchiveEntrySupplier;
import lombok.Data;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.Os;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static com.kbakhtiari.helm.maven.plugin.utils.PredicateUtils.not;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.codehaus.plexus.util.StringUtils.isEmpty;

@Data
@Mojo(name = "init", defaultPhase = LifecyclePhase.INITIALIZE)
public class InitMojo extends AbstractHelmMojo {

  @Parameter(property = "helm.init.skip", defaultValue = "false")
  private boolean skipInit;

  @Parameter(property = "helm.init.add-default-repo", defaultValue = "true")
  private boolean addDefaultRepo;

  public void execute() throws MojoExecutionException {

    if (skip || skipInit) {
      getLog().info("Skip init");
      return;
    }

    getLog().info("Initializing Helm...");

    Path outputDirectory = Paths.get(getOutputDirectory()).toAbsolutePath();
    if (!outputDirectory.toFile().exists()) {
      getLog().info("Creating output directory...");
      try {
        Files.createDirectories(outputDirectory);
      } catch (IOException e) {
        throw new MojoExecutionException(
            "Unable to create output directory at " + outputDirectory, e);
      }
    }

    if (isUseLocalHelmBinary()) {
      verifyLocalHelmBinary();
      getLog().info("Using local HELM binary [" + getHelmExecutablePath() + "]");
    } else {
      downloadAndUnpackHelm();
    }

    if (addDefaultRepo) {
      getLog().info("Adding default repo [stable]");
      callCli(
          getHelmCommand("repo add stable", "https://charts.helm.sh/stable"), "Unable add repo");
    }

    if (getHelmExtraRepos() != null) {
      for (HelmRepository repository : getHelmExtraRepos()) {
        getLog().info("Adding repo [" + repository + "]");
        PasswordAuthentication auth = getAuthentication(repository);
        callCli(
            getCommand(
                new StringBuilder("repo add")
                    .append(SPACE)
                    .append(repository.getName())
                    .append(SPACE)
                    .append(repository.getUrl())
                    .append(SPACE)
                    .append(
                        !Objects.isNull(auth)
                            ? String.format(
                                AUTH_TEMPLATE,
                                auth.getUserName(),
                                String.valueOf(auth.getPassword()))
                            : EMPTY)
                    .toString(),
                ""),
            "Unable add repo");
      }
    }
  }

  private static final String AUTH_TEMPLATE = "--username=%s --password=$s";
  private static final String HELM_DOWNLOAD_URL_TEMPLATE = "https://get.helm.sh/helm-v%s-%s-%s.%s";

  protected void downloadAndUnpackHelm() throws MojoExecutionException {

    Path directory = Paths.get(getHelmExecutableDirectory());
    if (Files.exists(directory.resolve(SystemUtils.IS_OS_WINDOWS ? "helm.exe" : "helm"))) {
      getLog().info("Found helm executable, skip init.");
      return;
    }

    String url = getHelmDownloadUrl();
    if (isEmpty(url)) {
      String os = getOperatingSystem();
      String architecture = getArchitecture();
      String extension = getExtension();
      url =
          String.format(HELM_DOWNLOAD_URL_TEMPLATE, getHelmVersion(), os, architecture, extension);
    }

    getLog().debug("Downloading Helm: " + url);
    try (InputStream dis = new URL(url).openStream();
        InputStream cis = createCompressorInputStream(dis);
        ArchiveInputStream is = createArchiverInputStream(cis)) {

      getLog().debug("creating directory: " + directory);
      Files.createDirectories(directory);

      final Path helmPath =
          Stream.generate(new ArchiveEntrySupplier(is, getLog()))
              .filter(not(Objects::isNull))
              .filter(not(ArchiveEntry::isDirectory))
              .map(ArchiveEntry::getName)
              .filter(name -> name.endsWith("helm.exe") || name.endsWith("helm"))
              .peek(name -> getLog().debug("Use archive entry with name: " + name))
              .map(name -> name.endsWith(".exe") ? "helm.exe" : "helm")
              .map(helm -> directory.resolve(helm))
              .findAny()
              .orElseThrow(
                  () -> new MojoExecutionException("Unable to find helm executable in tar file."));

      try (FileOutputStream output = new FileOutputStream(helmPath.toFile())) {
        IOUtils.copy(is, output);
      }
      addExecPermission(helmPath);
    } catch (IOException e) {
      throw new MojoExecutionException("Unable to download and extract helm executable.", e);
    }
  }

  private void addExecPermission(final Path helm) throws IOException {

    Set<String> fileAttributeView = FileSystems.getDefault().supportedFileAttributeViews();

    if (fileAttributeView.contains("posix")) {
      final Set<PosixFilePermission> permissions;
      try {
        permissions = Files.getPosixFilePermissions(helm);
      } catch (UnsupportedOperationException e) {
        getLog().debug("Exec file permission is not set", e);
        return;
      }
      permissions.add(PosixFilePermission.OWNER_EXECUTE);
      Files.setPosixFilePermissions(helm, permissions);

    } else if (fileAttributeView.contains("acl")) {
      String username = System.getProperty("user.name");
      UserPrincipal userPrincipal =
          FileSystems.getDefault().getUserPrincipalLookupService().lookupPrincipalByName(username);
      AclEntry aclEntry =
          AclEntry.newBuilder()
              .setPermissions(AclEntryPermission.EXECUTE)
              .setType(AclEntryType.ALLOW)
              .setPrincipal(userPrincipal)
              .build();

      AclFileAttributeView acl =
          Files.getFileAttributeView(helm, AclFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
      List<AclEntry> aclEntries = acl.getAcl();
      aclEntries.add(aclEntry);
      acl.setAcl(aclEntries);
    }
  }

  private void verifyLocalHelmBinary() throws MojoExecutionException {
    callCli(getHelmExecutablePath() + " version", "Unable to verify local HELM binary");
  }

  private ArchiveInputStream createArchiverInputStream(InputStream is)
      throws MojoExecutionException {

    // Stream must support mark to allow for auto detection of archiver
    if (!is.markSupported()) {
      is = new BufferedInputStream(is);
    }

    try {
      ArchiveStreamFactory archiveStreamFactory = new ArchiveStreamFactory();
      return archiveStreamFactory.createArchiveInputStream(is);

    } catch (ArchiveException e) {
      throw new MojoExecutionException("Unsupported archive type downloaded", e);
    }
  }

  private InputStream createCompressorInputStream(InputStream is) throws MojoExecutionException {

    // Stream must support mark to allow for auto detection of compressor
    if (!is.markSupported()) {
      is = new BufferedInputStream(is);
    }

    // Detect if stream is compressed
    Optional<String> compressorType = Optional.empty();
    try {
      compressorType = Optional.ofNullable(CompressorStreamFactory.detect(is));
    } catch (CompressorException e) {
      getLog().debug("Unknown type of compressed stream", e);
    }

    // If compressed then wrap with compressor stream
    if (compressorType.isPresent()) {
      try {
        CompressorStreamFactory compressorFactory = new CompressorStreamFactory();
        return compressorFactory.createCompressorInputStream(compressorType.get(), is);
      } catch (CompressorException e) {
        throw new MojoExecutionException("Unsupported compressor type: " + compressorType);
      }
    }

    return is;
  }

  private String getArchitecture() {

    String architecture = System.getProperty("os.arch").toLowerCase(Locale.US);

    if (architecture.equals("x86_64") || architecture.equals("amd64")) {
      return "amd64";
    } else if (architecture.equals("x86") || architecture.equals("i386")) {
      return "386";
    } else if (architecture.contains("arm64")) {
      return "arm64";
    } else if (architecture.equals("aarch32") || architecture.startsWith("arm")) {
      return "arm";
    } else if (architecture.contains("ppc64le")
        || (architecture.contains("ppc64")
            && System.getProperty("sun.cpu.endian").equals("little"))) {
      return "ppc64le";
    }

    throw new IllegalStateException("Unsupported architecture: " + architecture);
  }

  private String getExtension() {
    return Os.OS_FAMILY.equals(Os.FAMILY_WINDOWS) ? "zip" : "tar.gz";
  }

  private String getOperatingSystem() {
    switch (Os.OS_FAMILY) {
      case Os.FAMILY_UNIX:
        return "linux";
      case Os.FAMILY_MAC:
        return "darwin";
      case Os.FAMILY_WINDOWS:
        return "windows";
      default:
        throw new IllegalStateException("Unsupported OS: " + Os.OS_FAMILY);
    }
  }
}
