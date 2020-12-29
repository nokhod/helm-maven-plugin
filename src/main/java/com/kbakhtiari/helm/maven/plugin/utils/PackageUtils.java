package com.kbakhtiari.helm.maven.plugin.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.gson.Gson;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.EMPTY;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PackageUtils {

  private static Map mergeMaps(Map map1, Map map2) {

    map2.forEach(
        (key, value) ->
            map1.merge(
                key,
                value,
                (v1, v2) -> {
                  if (v1 instanceof Map && v2 instanceof Map) {
                    return mergeMaps((Map) v1, (Map) v2);
                  }
                  /** command line values coming with --set are preceding other values * */
                  return v2;
                }));
    return map1;
  }

  private static <U> Map<String, U> removeSpecialDuplicates(Map<String, U> map) {

    Map<String, U> treeMap = new TreeMap<>(map);
    map.forEach(
        (key, value) -> {
          /** command line values coming with --set are preceding other values * */
          if (key.startsWith("^") && key.endsWith("^")) {
            String normalKey = key.replace("^", EMPTY);
            treeMap.put(normalKey, value);
            treeMap.remove(key);
          }
          if (value instanceof Map && !Objects.isNull(treeMap.get(key))) {
            treeMap.put(key, (U) removeSpecialDuplicates((Map) value));
          }
        });
    return treeMap;
  }

  public static <U> Map<String, U> flattenOverrides(Map<String, U> overrides) {

    final Map<String, U> mutableMap = new TreeMap<>();
    overrides.forEach(
        (key, value) -> {
          if (value instanceof Map) {
            Map<String, U> flatMap = flattenOverrides((Map) value);
            String finalKey = key;
            flatMap.forEach(
                (flatMapKey, flatMapValue) ->
                    mutableMap.put(
                        new StringBuilder(finalKey).append(".").append(flatMapKey).toString(),
                        flatMapValue));
          } else {
            if (key.startsWith("^") && key.endsWith("^")) {
              key = key.replace("^", EMPTY).replaceAll("\\.", "\\\\.");
            }
            mutableMap.put(key, value);
          }
        });
    return mutableMap;
  }

  public static Map toMap(final String values) {

    return new Gson().fromJson(values, Map.class);
  }

  public static void overrideValuesFile(
      String inputDirectory, Map<String, ?> overrides, Log logger) {

    logger.info(format("rewriting the values.yaml with override values " + inputDirectory));
    try {
      final Path path =
          Paths.get(
              new StringBuilder(inputDirectory)
                  .append(File.separator)
                  .append("values.yaml")
                  .toString());
      if (path.toFile().exists()) {
        logger.info("values file found. will combine it with override values");
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        Map valuesFromFile = mapper.readValue(path.toFile(), Map.class);
        final Map mergedMaps = mergeMaps(valuesFromFile, overrides);
        final Map normalizedMap = removeSpecialDuplicates(mergedMaps);
        mapper.writeValue(path.toFile(), normalizedMap);
      }
    } catch (IOException e) {
      logger.error(
          "error happened while reading/writing to values.yaml with the message: "
              + e.getMessage());
    }
  }
}
