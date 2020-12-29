package com.kbakhtiari.helm.maven.plugin.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Constants {

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static class MojoDefaultConstants {

    public static final String HELM_VERSION = "3.4.2";
    public static final String TRUE = "true";
    public static final String FALSE = "false";
    public static final String HELM_SECURITY = "~/.m2/settings-security.xml";
  }

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static class ExceptionMessages {

    public static final String UNKNOWN_VALUE_TYPE_MESSAGE = "The specified value's type is unknown";
  }
}
