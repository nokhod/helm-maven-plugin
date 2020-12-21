package com.kbakhtiari.helm.maven.plugin;

import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "noop")
public abstract class NoopMojo extends AbstractHelmMojo {}
