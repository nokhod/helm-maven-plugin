# What is helm maven plugin?

This is a Maven plugin for testing, packaging and uploading HELM charts.

"HELM is a tool for managing Kubernetes charts. Charts are packages of pre-configured Kubernetes resources."

Visit https://docs.helm.sh for detailed information.

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.kiwigrid/helm-maven-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.kiwigrid/helm-maven-plugin)

[![Build Status](https://travis-ci.org/kiwigrid/helm-maven-plugin.svg?branch=master)](https://travis-ci.org/kiwigrid/helm-maven-plugin)

# Why?

Currently, (December 2020) there is no simple Maven plugin handling complex configurations to package existing HELM
charts. This project was initially forked from [kiwigrid](https://github.com/kiwigrid/helm-maven-plugin) brilliant
effort.

# How?

By default, the plugin automatically downloads Helm at the specified version. You can also manually specify the download
URL. Next to that it is possible to specify a local Helm binary. In all cases Helm will be executed in the background.

Add following dependency to your pom.xml:

```xml

<dependency>
    <groupId>com.kbakhtiari</groupId>
    <artifactId>helm-maven-plugin</artifactId>
    <version>0.0.1</version>
</dependency>
```

## Configuration Examples

### Helm URL Auto Detection

The default setting is to construct the Helm download URL based upon the detected OS and architecture:

```xml

<build>
    <plugins>
        <plugin>
            <groupId>com.kbakhtiari</groupId>
            <artifactId>helm-maven-plugin</artifactId>
            <version>0.0.1</version>
            <configuration>
                <chartDirectory>${project.basedir}</chartDirectory>
                <chartVersion>${project.version}</chartVersion>
                <helmVersion>3.4.2</helmVersion>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### Usage with Downloaded Binary

```xml

<build>
    <plugins>
        ...
        <plugin>
            <groupId>com.kbakhtiari</groupId>
            <artifactId>helm-maven-plugin</artifactId>
            <version>0.0.1</version>
            <configuration>
                <chartDirectory>${project.basedir}</chartDirectory>
                <chartVersion>${project.version}</chartVersion>
                <!-- This is the related section when using binary download -->
                <helmDownloadUrl>https://get.helm.sh/helm-v3.4.2-linux-amd64.tar.gz</helmDownloadUrl>
            </configuration>
        </plugin>
        ...
    </plugins>
</build>
```

### Usage with Local Binary

When `useLocalHelmBinary` is enabled, the plugin by default will search for the `helm` executable in `PATH`:

```xml

<build>
    <plugins>
        ...
        <plugin>
            <groupId>com.kbakhtiari</groupId>
            <artifactId>helm-maven-plugin</artifactId>
            <version>0.0.1</version>
            <configuration>
                <chartDirectory>${project.basedir}</chartDirectory>
                <chartVersion>${project.version}</chartVersion>
                <!-- This is the related section to use local binary with auto-detection enabled. -->
                <useLocalHelmBinary>true</useLocalHelmBinary>
            </configuration>
        </plugin>
        ...
    </plugins>
</build>
```

The following is an example configuration that explicitly sets the directory in which to look for the `helm` executable,
and disables the auto-detection feature:

```xml

<build>
    <plugins>
        ...
        <plugin>
            <groupId>com.kbakhtiari</groupId>
            <artifactId>helm-maven-plugin</artifactId>
            <version>0.0.1</version>
            <configuration>
                <chartDirectory>${project.basedir}</chartDirectory>
                <chartVersion>${project.version}</chartVersion>
                <!-- This is the related section to use local binary with auto-detection disabled. -->
                <useLocalHelmBinary>true</useLocalHelmBinary>
                <autoDetectLocalHelmBinary>false</autoDetectLocalHelmBinary>
                <helmExecutableDirectory>/usr/local/bin</helmExecutableDirectory>
            </configuration>
        </plugin>
        ...
    </plugins>
</build>
```

### More Complex Example

```xml

<build>
    <plugins>
        ...
        <plugin>
            <groupId>com.kbakhtiari</groupId>
            <artifactId>helm-maven-plugin</artifactId>
            <version>0.0.1</version>
            <configuration>
                <chartDirectory>${project.basedir}</chartDirectory>
                <chartVersion>${project.version}</chartVersion>
                <uploadRepoStable>
                    <name>stable-repo</name>
                    <url>https://repo.example.com/artifactory/helm-stable</url>
                    <username>foo</username>
                    <password>bar</password>
                </uploadRepoStable>
                <uploadRepoSnapshot>
                    <name>snapshot-repo</name>
                    <url>https://my.chart.museum/api/charts</url>
                </uploadRepoSnapshot>
                <helmDownloadUrl>https://get.helm.sh/helm-v3.4.2-linux-amd64.tar.gz</helmDownloadUrl>
                <helmHomeDirectory>${project.basedir}/target/helm/home</helmHomeDirectory>
                <registryConfig>~/.config/helm/registry.json</registryConfig>
                <repositoryCache>~/.cache/helm/repository</repositoryCache>
                <repositoryConfig>~/.config/helm/repositories.yaml</repositoryConfig>
                <!-- Lint with strict mode -->
                <lintStrict>true</lintStrict>
                <!-- Disable adding of default repo stable https://charts.helm.sh/stable -->
                <addDefaultRepo>false</addDefaultRepo>
                <!-- Exclude a directory to avoid processing -->
                <excludes>
                    <exclude>${project.basedir}/excluded</exclude>
                    <exclude>${project.basedir}/**/excluded*</exclude>
                </excludes>
                <!-- Add an additional repo -->
                <helmExtraRepos>
                    <helmRepo>
                        <name>kbakhtiari</name>
                        <url>https://gitlab.com/ikbengeweldig/helm-maven-plugin</url>
                    </helmRepo>
                </helmExtraRepos>
                <!-- extra value settings for the lint command -->
                <values>
                    <overrides>
                        {
                        "component1":{
                        "install": {
                        "path": "/opt/component1"
                        }
                        }
                        }
                    </overrides>
                    <yamlFile>${project.basedir}/src/test/resources/myOverrides.yaml</yamlFile>
                </values>
            </configuration>
        </plugin>
        ...
    </plugins>
</build>
```

# Features

- Package Helm charts from standard folder structure
- Test Helm charts (Helm lint)
- Recursive chart detection (subcharts)
- Helm does not need to be installed
- Upload to chart registries

# Usage

## Goals

- `helm:init` initializes Helm by downloading a specific version
- `helm:dependency-build` resolves the chart dependencies
- `helm:package` packages the given charts (chart.tar.gz)
- `helm:lint` tests the given charts
- `helm:dry-run` simulates an install
- `helm:push` pushes the packaged chart to the registry
- `helm:template` locally render templates
- `helm:upgrade` upgrade a release

## Configuration

Parameter | Type | User Property | Required | Description
--- | --- | --- | --- | ---
`<chartDirectory>` | string | helm.chartDirectory | true | root directory of your charts
`<chartVersion>` | string | helm.chartVersion | true | Version of the charts. The version have to be in the [SEMVER-Format](https://semver.org/), required by helm.
`<appVersion>` | string | helm.appVersion | false | The version of the app. This needn't be SemVer.
`<helmDownloadUrl>` | string | helm.downloadUrl | false | URL to download helm. Leave empty to autodetect URL based upon OS and architecture.
`<helmVersion>` | string | helm.version | false | Version of helm to download. Defaults to 3.4.2
`<excludes>` | list of strings | helm.excludes | false | list of chart directories to exclude
`<useLocalHelmBinary>` | boolean | helm.useLocalHelmBinary | false | Controls whether a local binary should be used instead of downloading it. If set to `true` path has to be set with property `executableDirectory`
`<autoDetectLocalHelmBinary>` | boolean | helm.autoDetectLocalHelmBinary | true | Controls whether the local binary should be auto-detected from `PATH` environment variable. If set to `false` the binary in `<helmExecutableDirectory>` is used. This property has no effect unless `<useLocalHelmBinary>` is set to `true`.
`<helmExecutableDirectory>` | string | helm.executableDirectory | false | directory of your helm installation (default: `${project.build.directory}/helm`)
`<outputDirectory>` | string | helm.outputDirectory | false | chart output directory (default: `${project.build.directory}/helm/repo`)
`<registryConfig>` | string | helm.registryConfig | false | path to the registry config file
`<repositoryCache>` | string | helm.repositoryCache | false | path to the file containing cached repository indexes
`<repositoryConfig>` | string | helm.repositoryConfig | false | path to the file containing repository names and URLs
`<helmExtraRepos>` | list of [HelmRepository](./src/main/java/com/kiwigrid/helm/maven/plugin/HelmRepository.java) | helm.extraRepos | false | adds extra repositories while init
`<uploadRepoStable>`| [HelmRepository](./src/main/java/com/kiwigrid/helm/maven/plugin/HelmRepository.java) | helm.uploadRepo.stable | true | Upload repository for stable charts
`<uploadRepoSnapshot>`| [HelmRepository](./src/main/java/com/kiwigrid/helm/maven/plugin/HelmRepository.java) | helm.uploadRepo.snapshot | false | Upload repository for snapshot charts (determined by version postfix 'SNAPSHOT')
`<lintStrict>` | boolean | helm.lint.strict | false | run lint command with strict option (fail on lint warnings)
`<addDefaultRepo>` | boolean | helm.init.add-default-repo | true | If true, stable repo (https://charts.helm.sh/stable) will be added
`<skip>` | boolean | helm.skip | false | skip plugin execution
`<skipInit>` | boolean | helm.init.skip | false | skip init goal
`<skipLint>` | boolean | helm.lint.skip | false | skip lint goal
`<skipDryRun>` | boolean | helm.dry-run.skip | false | skip dry-run goal
`<skipDependencyBuild>` | boolean | helm.dependency-build.skip | false | skip dependency-build goal
`<skipPackage>` | boolean | helm.package.skip | false | skip package goal
`<skipUpload>` | boolean | helm.upload.skip | false | skip upload goal
`<values>` | [ValueOverride](./src/main/java/com/kiwigrid/helm/maven/plugin/ValueOverride.java) | helm.values | false | override some values for linting with helm.values.overrides (--set option), helm.values.stringOverrides (--set-string option), helm.values.fileOverrides (--set-file option) and last but not least helm.values.yamlFile (--values option)

## Packaging with the Helm Lifecycle

To keep your pom files small you can use 'helm' packaging. This binds `helm:init` to the initialize phase, `helm:lint`
to the test phase, `helm:package` to the package phase and `helm:upload` to the deploy phase.

```xml

<pom>
    <artifactId>my-helm-charts</artifactId>
    <version>0.0.1</version>
    <packaging>helm</packaging>
    ...
    <build>
        <plugins>
            <plugin>
                <groupId>com.kbakhtiari</groupId>
                <artifactId>helm-maven-plugin</artifactId>
                <!-- Mandatory when you use a custom lifecycle -->
                <extensions>true</extensions>
                <configuration>
                    ...
                </configuration>
            </plugin>
        </plugins>
        ....
    </build>
</pom>
```
