package com.kbakhtiari.helm.maven.plugin.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JavaUtils {

  public static <T> T nvl(T a, T b) {

    if (Objects.isNull(a)) {
      return b;
    } else if (a instanceof String && StringUtils.isEmpty((String) a)) {
      return b;
    }
    return a;
  }
}
