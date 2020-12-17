package com.kbakhtiari.helm.maven.plugin;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.kbakhtiari.helm.maven.plugin.pojo.ValueOverride;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AbstractHelmWithValueOverrideMojoTest {

  private NoopHelmMojo testMojo = new NoopHelmMojo();

  @Test
  public void noValueOverrideNoValueOption() {
    assertEquals("", testMojo.getValuesOptions(), "without any configuration no values options ");
  }

  @Test
  public void normalValueOverride() {

    ValueOverride override = new ValueOverride();
    override.setOverrides(
        new Gson().toJson(ImmutableMap.<String, String>builder().put("key1", "value1").build()));
    testMojo.setValues(override);

    assertEquals(" --set key1=value1", testMojo.getValuesOptions());
  }

  @Test
  public void multipleNormalValueOverrides() {

    ValueOverride override = new ValueOverride();
    override.setOverrides(
        new Gson()
            .toJson(
                ImmutableMap.<String, String>builder()
                    .put("key1", "value1")
                    .put("key2", "value2")
                    .build()));
    testMojo.setValues(override);

    assertEquals(" --set key1=value1,key2=value2", testMojo.getValuesOptions());
  }

  @Test
  public void stringValueOverrides() {

    ValueOverride override = new ValueOverride();
    override.setStringOverrides(
        new Gson()
            .toJson(
                ImmutableMap.<String, String>builder()
                    .put("key1", "value1")
                    .put("key2", "value2")
                    .build()));
    testMojo.setValues(override);

    assertEquals(" --set-string key1=value1,key2=value2", testMojo.getValuesOptions());
  }

  @Test
  public void fileValueOverrides() {

    ValueOverride override = new ValueOverride();
    override.setFileOverrides(
        new Gson()
            .toJson(
                ImmutableMap.<String, String>builder()
                    .put("key1", "path/to/file1.txt")
                    .put("key2", "D:/absolute/path/to/file2.txt")
                    .build()));
    testMojo.setValues(override);

    assertEquals(
        " --set-file key1=path/to/file1.txt,key2=D:/absolute/path/to/file2.txt",
        testMojo.getValuesOptions());
  }

  @Test
  public void valueYamlOverride() {
    ValueOverride override = new ValueOverride();
    override.setYamlFile("path/to/values.yaml");
    testMojo.setValues(override);

    assertEquals(" --values path/to/values.yaml", testMojo.getValuesOptions());
  }

  @Test
  public void allOverrideUsedTogether() {
    ValueOverride override = new ValueOverride();
    override.setOverrides(
        new Gson()
            .toJson(
                ImmutableMap.<String, String>builder()
                    .put("key1", "value1")
                    .put("key2", "value2")
                    .build()));
    override.setStringOverrides(
        new Gson()
            .toJson(
                ImmutableMap.<String, String>builder()
                    .put("skey1", "svalue1")
                    .put("skey2", "svalue2")
                    .build()));
    override.setFileOverrides(
        new Gson()
            .toJson(
                ImmutableMap.<String, String>builder()
                    .put("fkey1", "path/to/file1.txt")
                    .put("fkey2", "D:/absolute/path/to/file2.txt")
                    .build()));
    override.setYamlFile("path/to/values.yaml");
    testMojo.setValues(override);

    assertEquals(
        " --set key1=value1,key2=value2 --set-string skey1=svalue1,skey2=svalue2 --set-file "
            + "fkey1=path/to/file1.txt,fkey2=D:/absolute/path/to/file2.txt --values path/to/values.yaml",
        testMojo.getValuesOptions());
  }

  private static class NoopHelmMojo extends AbstractHelmMojo {

    @java.lang.Override
    public void execute() {
      /* Noop. */
    }
  }
}
