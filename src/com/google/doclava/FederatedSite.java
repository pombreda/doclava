/*
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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