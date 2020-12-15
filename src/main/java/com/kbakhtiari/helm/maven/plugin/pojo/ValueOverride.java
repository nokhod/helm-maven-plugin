package com.kbakhtiari.helm.maven.plugin.pojo;

import lombok.Data;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.Map;


@Data
public class ValueOverride {

  /** Values that will be passed with the option --set of helm command line. */
  @Parameter(property = "helm.values.overrides")
  private Map<String, String> overrides;

  /**
   * Values that will be passed with the option --set-string to the helm command line.
   *
   * <p>This option forces the values to be transformed and manipulated as strings by Go template.
   */
  @Parameter(property = "helm.values.stringOverrides")
  private Map<String, String> stringOverrides;

  /**
   * Values that will be passed with the option --set-file to the helm command line.
   *
   * <p>Values here point to files that contain the content you want to inject. Very useful to use
   * with en entire script you want to insert optionally somewhere for instance.
   */
  @Parameter(property = "helm.values.fileOverrides")
  private Map<String, String> fileOverrides;

  /**
   * Value YAML file that will be passed with option --values or -f of the helm command line.
   *
   * <p>It can be seen as creating a temporary extending chart with its dedicated values.yaml.
   */
  @Parameter(property = "helm.values.yamlFile")
  private String yamlFile;
}
