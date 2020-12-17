package com.kbakhtiari.helm.maven.plugin.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Constants {

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static class LogUtils {

    public static final String LOG_TEMPLATE =
        new StringBuilder()
            .append(System.lineSeparator())
            .append("\t")
            .append("%s")
            .append(System.lineSeparator())
            .toString();
  }
}
