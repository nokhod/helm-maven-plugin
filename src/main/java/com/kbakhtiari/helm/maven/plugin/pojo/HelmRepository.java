package com.kbakhtiari.helm.maven.plugin.pojo;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.maven.plugins.annotations.Parameter;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class HelmRepository {

  /**
   * Name of repository. If no username/password is configured this name is interpreted as server id
   * and used to obtain username/password from server list in <code>settings.xml</code>-
   */
  @Parameter(property = "helm.repo.name", required = true)
  private String name;

  @Parameter(property = "helm.repo.url", required = true)
  private String url;

  /** Username for basic authentication. If present credentials in server list will be ignored. */
  @Parameter(property = "helm.repo.username")
  private String username;

  /** Password for basic authentication. If present credentials in server list will be ignored. */
  @Parameter(property = "helm.repo.password")
  private String password;

  @Parameter(property = "helm.repo.type")
  private RepoType type;
}
