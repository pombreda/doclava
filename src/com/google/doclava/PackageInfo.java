/*
 * Copyright (C) 2010 The Android Open Source Project
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

import com.google.clearsilver.jsilver.data.Data;
import com.google.common.collect.ImmutableList;
import com.sun.javadoc.PackageDoc;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public final class PackageInfo extends DocInfo implements ContainerInfo {
  public static final String DEFAULT_PACKAGE = "default package";
  
  public static final Comparator<PackageInfo> ORDER_BY_NAME = new Comparator<PackageInfo>() {
    public int compare(PackageInfo a, PackageInfo b) {
      return a.name().compareTo(b.name());
    }
  };

  public PackageInfo(PackageDoc pkg, String name, SourcePositionInfo position) {
    super(pkg.getRawCommentText(), position);
    mName = name.isEmpty() ? DEFAULT_PACKAGE : name;
    mPackage = pkg;
  }
  
  public PackageInfo(String name) {
    super("", null);
    mName = name;
  }
  
  public PackageInfo(String name, SourcePositionInfo position) {
    super("", position);
    mName = name.isEmpty() ? DEFAULT_PACKAGE : name;
  }

  public boolean isDefinedLocally() {
    return true;
  }

  public String relativePath() {
    String s = mName;
    s = s.replace('.', '/');
    s += "/package-summary.html";
    return s;
  }

  public String fullDescriptionFile() {
    String s = mName;
    s = s.replace('.', '/');
    s += "/package-descr.html";
    return s;
  }
  
  public String fullDescriptionHtmlPage() {
    return htmlPage().replace("/package-summary.html", "/package-descr.html");
  }

  @Override
  public ContainerInfo parent() {
    return null;
  }

  @Override
  public boolean isHidden() {
    return comment().isHidden();
  }

  public boolean checkLevel() {
    // TODO should return false if all classes are hidden but the package isn't.
    // We don't have this so I'm not doing it now.
    return !isHidden();
  }

  public String name() {
    return mName;
  }

  public String qualifiedName() {
    return mName;
  }

  public List<TagInfo> inlineTags() {
    return comment().tags();
  }

  public List<TagInfo> firstSentenceTags() {
    return comment().briefTags();
  }

  public void makeLink(Data data, String base) {
    if (checkLevel()) {
      data.setValue(base + ".link", htmlPage());
    }
    data.setValue(base + ".name", name());
    data.setValue(base + ".since.key", SinceTagger.keyForName(getSince()));
    data.setValue(base + ".since.name", getSince());
  }

  public void makeClassLinkListHDF(Data data, String base) {
    makeLink(data, base);
    ClassInfo.makeLinkListHDF(data, base + ".annotations", getAnnotations());
    ClassInfo.makeLinkListHDF(data, base + ".interfaces", getInterfaces());
    ClassInfo.makeLinkListHDF(data, base + ".classes", ordinaryClasses());
    ClassInfo.makeLinkListHDF(data, base + ".enums", enums());
    ClassInfo.makeLinkListHDF(data, base + ".exceptions", exceptions());
    ClassInfo.makeLinkListHDF(data, base + ".errors", errors());
    data.setValue(base + ".since.key", SinceTagger.keyForName(getSince()));
    data.setValue(base + ".since.name", getSince());
  }

  private void checkInitVisibleCalled() {
    if (mAnnotations == null || mInterfaces == null || mOrdinaryClasses == null || mEnums == null
        || mExceptions == null || mErrors == null) {
      throw new IllegalStateException("Call initVisible() first!");
    }
  }

  public List<ClassInfo> getAnnotations() {
    checkInitVisibleCalled();
    return mAnnotations;
  }
  
  public List<ClassInfo> getInterfaces() {
    checkInitVisibleCalled();
    return mInterfaces;
  }

  public List<ClassInfo> ordinaryClasses() {
    checkInitVisibleCalled();
    return mOrdinaryClasses;
  }

  public List<ClassInfo> enums() {
    checkInitVisibleCalled();
    return mEnums;
  }

  public List<ClassInfo> exceptions() {
    checkInitVisibleCalled();
    return mExceptions;
  }

  public List<ClassInfo> errors() {
    checkInitVisibleCalled();
    return mErrors;
  }

  // in hashed containers, treat the name as the key
  @Override public int hashCode() {
    return mName.hashCode();
  }

  private String mName;
  private PackageDoc mPackage;
  private ImmutableList<ClassInfo> mAnnotations;
  private ImmutableList<ClassInfo> mInterfaces;
  private ImmutableList<ClassInfo> mOrdinaryClasses;
  private ImmutableList<ClassInfo> mEnums;
  private ImmutableList<ClassInfo> mExceptions;
  private ImmutableList<ClassInfo> mErrors;
  
  // TODO: Leftovers from ApiCheck that should be better merged.
  private HashMap<String, ClassInfo> mClasses = new HashMap<String, ClassInfo>();

  public void initVisible(Project project) {
    super.initVisible(project);
    mAnnotations = Visibility.displayClasses(project.convertClasses(mPackage.annotationTypes()));
    mInterfaces = Visibility.displayClasses(project.convertClasses(mPackage.interfaces()));
    mOrdinaryClasses = Visibility.displayClasses(project.convertClasses(mPackage.ordinaryClasses()));
    mEnums = Visibility.displayClasses(project.convertClasses(mPackage.enums()));
    mExceptions = Visibility.displayClasses(project.convertClasses(mPackage.exceptions()));
    mErrors = Visibility.displayClasses(project.convertClasses(mPackage.errors()));
  }

  public void addClass(ClassInfo cl) {
    mClasses.put(cl.name(), cl);
  }

  public HashMap<String, ClassInfo> allClasses() {
    return mClasses;
  }

  public boolean isConsistent(PackageInfo pInfo) {
    boolean consistent = true;
    for (ClassInfo cInfo : mClasses.values()) {
      if (pInfo.mClasses.containsKey(cInfo.name())) {
        if (!cInfo.isConsistent(pInfo.mClasses.get(cInfo.name()))) {
          consistent = false;
        }
      } else {
        Errors.error(Errors.REMOVED_CLASS, cInfo.position(), "Removed public class "
            + cInfo.qualifiedName());
        consistent = false;
      }
    }
    for (ClassInfo cInfo : pInfo.mClasses.values()) {
      if (!mClasses.containsKey(cInfo.name())) {
        Errors.error(Errors.ADDED_CLASS, cInfo.position(), "Added class " + cInfo.name()
            + " to package " + pInfo.name());
        consistent = false;
      }
    }
    return consistent;
  }
}
