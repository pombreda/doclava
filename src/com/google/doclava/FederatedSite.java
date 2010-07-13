package com.google.doclava;

import com.google.doclava.apicheck.ApiInfo;

import java.net.URL;

/**
 * A remote source of documentation that can be linked against. A site may be
 * linked to when an external library has packages, classes, and members
 * referenced or shared by the codebase for which documentation is being
 * generated.
 */
public final class FederatedSite {
  final String name;
  final URL baseUrl;
  final ApiInfo apiInfo;
  
  FederatedSite(String name, URL baseURL, ApiInfo apiInfo) {
    this.name = name;
    this.baseUrl = baseURL;
    this.apiInfo = apiInfo;
  }
  
  public String linkFor(String htmlPage) {
    return baseUrl + "/" + htmlPage;
  }
}