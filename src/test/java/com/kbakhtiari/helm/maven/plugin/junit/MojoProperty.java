package com.kbakhtiari.helm.maven.plugin.junit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(MojoProperty.MojoProperties.class)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface MojoProperty {

  String name();

  String value();

  @Target({ElementType.METHOD, ElementType.TYPE})
  @Retention(RUNTIME)
  @Documented
  @interface MojoProperties {

    MojoProperty[] value();
  }
}
