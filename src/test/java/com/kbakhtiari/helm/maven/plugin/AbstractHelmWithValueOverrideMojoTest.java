package com.kbakhtiari.helm.maven.plugin;

import com.kbakhtiari.helm.maven.plugin.AbstractHelmMojo;
import com.kbakhtiari.helm.maven.plugin.pojo.ValueOverride;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

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
    override.setOverrides(new LinkedHashMap<>());
    override.getOverrides().put("key1", "value1");
    testMojo.setValues(override);

    assertEquals(" --set key1=value1", testMojo.getValuesOptions());
  }

  @Test
  public void multipleNormalValueOverrides() {
    ValueOverride override = new ValueOverride();
    override.setOverrides(new LinkedHashMap<>());
    override.getOverrides().put("key1", "value1");
    override.getOverrides().put("key2", "value2");
    testMojo.setValues(override);

    assertEquals(" --set key1=value1,key2=value2", testMojo.getValuesOptions());
  }

  @Test
  public void stringValueOverrides() {
    ValueOverride override = new ValueOverride();
    override.setStringOverrides(new LinkedHashMap<>());
    override.getStringOverrides().put("key1", "value1");
    override.getStringOverrides().put("key2", "value2");
    testMojo.setValues(override);

    assertEquals(" --set-string key1=value1,key2=value2", testMojo.getValuesOptions());
  }

  @Test
  public void fileValueOverrides() {
    ValueOverride override = new ValueOverride();
    override.setFileOverrides(new LinkedHashMap<>());
    override.getFileOverrides().put("key1", "path/to/file1.txt");
    override.getFileOverrides().put("key2", "D:/absolute/path/to/file2.txt");
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
    override.setOverrides(new LinkedHashMap<>());
    override.getOverrides().put("key1", "value1");
    override.getOverrides().put("key2", "value2");
    override.setStringOverrides(new LinkedHashMap<>());
    override.getStringOverrides().put("skey1", "svalue1");
    override.getStringOverrides().put("skey2", "svalue2");
    override.setFileOverrides(new LinkedHashMap<>());
    override.getFileOverrides().put("fkey1", "path/to/file1.txt");
    override.getFileOverrides().put("fkey2", "D:/absolute/path/to/file2.txt");
    override.setYamlFile("path/to/values.yaml");
    testMojo.setValues(override);

    assertEquals(
        " --set key1=value1,key2=value2 --set-string skey1=svalue1,skey2=svalue2 --set-file "
            + "fkey1=path/to/file1.txt,fkey2=D:/absolute/path/to/file2.txt --values path/to/values.yaml",
        testMojo.getValuesOptions());
  }

  private static class NoopHelmMojo extends AbstractHelmMojo {

    @Override
    public void execute() {
      /* Noop. */
    }
  }
}
