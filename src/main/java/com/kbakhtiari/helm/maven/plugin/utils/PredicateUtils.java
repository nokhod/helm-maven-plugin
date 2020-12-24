package com.kbakhtiari.helm.maven.plugin.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.function.Predicate;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PredicateUtils {

  public static <T> Predicate<T> not(Predicate<T> t) {
    return t.negate();
  }
}
