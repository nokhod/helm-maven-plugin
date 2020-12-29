package com.kbakhtiari.helm.maven.plugin.junit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface SystemProperty {

  String name();

  String value() default "";

  boolean unset() default false;

  @Documented
  @Retention(RUNTIME)
  @Target({ElementType.METHOD, ElementType.TYPE})
  @interface SystemProperties {

    SystemProperty[] value();
  }
}
