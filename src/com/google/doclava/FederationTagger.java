// Copyright 2010 Google Inc. All Rights Reserved.

package com.google.doclava;

import com.google.doclava.apicheck.ApiCheck;
import com.google.doclava.apicheck.ApiInfo;
import com.google.doclava.apicheck.ApiParseException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Cross-references documentation among different libraries. A FederationTagger
 * is populated with a list of {@link FederatedSite} objects which are linked
 * against when overlapping content is discovered.
 */
public final class FederationTagger {
  private final List<FederatedSite> federatedSites = new ArrayList<FederatedSite>();
  
  /**
   * Adds a Doclava documentation site for federation. Accepts the base URL of
   * the remote API.
   */
  public void addSite(String name, URL site) {
    try {
      URL xmlURL = new URL(site.toExternalForm() + "/xml/current.xml");
      ApiInfo apiInfo = new ApiCheck().parseApi(xmlURL);
      federatedSites.add(new FederatedSite(name, site, apiInfo));
    } catch (MalformedURLException m) {
      Errors.error(Errors.NO_FEDERATION_DATA, null, "Invalid URL for federation: " + site);
    } catch (ApiParseException e) {
      Errors.error(Errors.NO_FEDERATION_DATA, null, "Could not add site for federation: " + site);
    }
  }
  
  public void tagAll(ClassInfo[] classDocs) {
    Iterator<FederatedSite> federationIter = federatedSites.iterator();
    for (FederatedSite site : federatedSites) {
      applyFederation(site, classDocs);
    }
  }
  
  private void applyFederation(FederatedSite federationSource, ClassInfo[] classDocs) {
    for (ClassInfo classDoc : classDocs) {
      com.google.doclava.apicheck.PackageInfo packageSpec =
        federationSource.apiInfo.getPackages().get(classDoc.containingPackage().name());

      if (packageSpec == null) {
        continue;
      }

      com.google.doclava.apicheck.ClassInfo classSpec =
        packageSpec.allClasses().get(classDoc.name());
      
      if (classSpec == null) {
        continue;
      }
      
      federateMethods(federationSource, classSpec, classDoc);
      federateConstructors(federationSource, classSpec, classDoc);
      federateFields(federationSource, classSpec, classDoc);
      federateClass(federationSource, classDoc);
      federatePackage(federationSource, classDoc.containingPackage());
    }
  }

  private void federateMethods(FederatedSite source,
      com.google.doclava.apicheck.ClassInfo spec, ClassInfo doc) {
    for (MethodInfo method : doc.methods()) {
      for (com.google.doclava.apicheck.ClassInfo superclass : spec.hierarchy()) {
        if (superclass.allMethods().containsKey(method.getHashableName())) {
          method.addFederatedReference(source);
          break;
        }
      }
    }
  }
  
  private void federateConstructors(FederatedSite source,
                               com.google.doclava.apicheck.ClassInfo spec, ClassInfo doc) {
    for (MethodInfo constructor : doc.constructors()) {
      if (spec.allConstructors().containsKey(constructor.getHashableName())) {
        constructor.addFederatedReference(source);
      }
    }
  }
  
  private void federateFields(FederatedSite source,
                               com.google.doclava.apicheck.ClassInfo spec, ClassInfo doc) {
    for (FieldInfo field : doc.fields()) {
      if (spec.allFields().containsKey(field.name())) {
        field.addFederatedReference(source);
      }
    }
  }
  
  private void federateClass(FederatedSite source, ClassInfo doc) {
    doc.addFederatedReference(source);
  }
  
  private void federatePackage(FederatedSite source, PackageInfo pkg) {
    pkg.addFederatedReference(source);
  }
}