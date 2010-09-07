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
import com.google.common.collect.Ordering;
import com.sun.javadoc.ClassDoc;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class ClassInfo extends DocInfo implements ContainerInfo, Comparable<ClassInfo>, Scoped {
  public static final Ordering<ClassInfo> ORDER_BY_NAME = new Ordering<ClassInfo>() {
    public int compare(ClassInfo a, ClassInfo b) {
      return a.name().compareTo(b.name());
    }
  };

  /**
   * Constructs a stub representation of a class.
   */
  public ClassInfo(String qualifiedName) {
    super("", SourcePositionInfo.UNKNOWN);
    
    mQualifiedName = qualifiedName;
    int pos = qualifiedName.lastIndexOf('.'); 
    if (pos != -1) {
      mName = qualifiedName.substring(pos + 1);
    } else {
      mName = qualifiedName;
    }
  }

  public ClassInfo(ClassDoc cl, String rawCommentText, SourcePositionInfo position,
      boolean isPublic, boolean isProtected, boolean isPackagePrivate, boolean isPrivate,
      boolean isStatic, boolean isInterface, boolean isAbstract, boolean isOrdinaryClass,
      boolean isException, boolean isError, boolean isEnum, boolean isAnnotation, boolean isFinal,
      boolean isIncluded, String name, String qualifiedName) {
    super(rawCommentText, position);

    mClass = cl;
    mIsPublic = isPublic;
    mIsProtected = isProtected;
    mIsPackagePrivate = isPackagePrivate;
    mIsPrivate = isPrivate;
    mIsStatic = isStatic;
    mIsInterface = isInterface;
    mIsAbstract = isAbstract;
    mIsOrdinaryClass = isOrdinaryClass;
    mIsException = isException;
    mIsError = isError;
    mIsEnum = isEnum;
    mIsAnnotation = isAnnotation;
    mIsFinal = isFinal;
    mIsIncluded = isIncluded;
    mName = name;
    mQualifiedName = qualifiedName;
    mNameParts = name.split("\\.");
  }

  public void init(TypeInfo typeInfo, List<ClassInfo> interfaces, List<TypeInfo> interfaceTypes,
      List<ClassInfo> innerClasses, List<MethodInfo> allConstructors, List<MethodInfo> methods,
      List<MethodInfo> annotationElements, List<FieldInfo> fields, List<FieldInfo> enumConstants,
      PackageInfo containingPackage, ClassInfo containingClass, ClassInfo superclass,
      TypeInfo superclassType, AnnotationInstanceInfo[] annotations, List<ClassInfo> realInnerClasses) {
    mTypeInfo = typeInfo;
    mRealInterfaces.addAll(interfaces);
    mRealInterfaceTypes = interfaceTypes;
    mInnerClasses = innerClasses;
    mAllConstructors = allConstructors;
    mAllSelfMethods = methods;
    mAnnotationElements = annotationElements;
    mAllSelfFields = fields;
    mEnumConstants = enumConstants;
    mContainingPackage = containingPackage;
    mContainingClass = containingClass;
    mRealSuperclass = superclass;
    mRealSuperclassType = superclassType;
    mAnnotations = annotations;
    mRealInnerClasses = realInnerClasses;

    // after providing new methods and new superclass info,clear any cached
    // lists of self + superclass methods, ctors, etc.
    mSuperclassInit = false;
    mConstructors = null;
    mSelfMethods = null;
    mSelfFields = null;
    mSelfAttributes = null;
    mIsDeprecated = false;

    Collections.sort(mEnumConstants, FieldInfo.ORDER_BY_NAME);
    Collections.sort(mInnerClasses, ORDER_BY_NAME);
  }

  public List<ClassInfo> getRealInnerClasses() {
    return mRealInnerClasses;
  }

  public boolean checkLevel() {
    int val = mCheckLevel;
    if (val >= 0) {
      return val != 0;
    } else {
      boolean v =
          Doclava.checkLevel(mIsPublic, mIsProtected, mIsPackagePrivate, mIsPrivate, isHidden());
      mCheckLevel = v ? 1 : 0;
      return v;
    }
  }

  public int compareTo(ClassInfo other) {
    return mQualifiedName.compareTo(other.mQualifiedName);
  }

  @Override
  public ContainerInfo parent() {
    return this;
  }

  public boolean isPublic() {
    return mIsPublic;
  }

  public boolean isProtected() {
    return mIsProtected;
  }

  public boolean isPackagePrivate() {
    return mIsPackagePrivate;
  }

  public boolean isPrivate() {
    return mIsPrivate;
  }

  public boolean isStatic() {
    return mIsStatic;
  }

  public boolean isInterface() {
    return mIsInterface;
  }

  public boolean isAbstract() {
    return mIsAbstract;
  }

  public PackageInfo containingPackage() {
    return mContainingPackage;
  }

  public ClassInfo containingClass() {
    return mContainingClass;
  }

  public boolean isOrdinaryClass() {
    return mIsOrdinaryClass;
  }

  public boolean isException() {
    return mIsException;
  }

  public boolean isError() {
    return mIsError;
  }

  public boolean isEnum() {
    return mIsEnum;
  }

  public boolean isAnnotation() {
    return mIsAnnotation;
  }

  public boolean isFinal() {
    return mIsFinal;
  }

  /**
   * Returns true if the class represented by this object is defined
   * locally, and thus will be included in local documentation.
   */
  public boolean isDefinedLocally() {
    return mIsIncluded;
  }

  /**
   * Adds this type, its supertype, and its interfaces to {@code out}.
   */
  public void addAllTypes(Set<ClassInfo> out) {
    if (!out.add(this)) {
      return;
    }

    if (mRealSuperclass != null) {
      mRealSuperclass.addAllTypes(out);
    }

    if (mSuperclass != null) {
      mSuperclass.addAllTypes(out);
    }

    for (ClassInfo i : mRealInterfaces) {
      i.addAllTypes(out);
    }
  }

  public List<ClassInfo> getInterfaces() {
    checkInitVisibleCalled();
    return mInterfaces;
  }

  public List<ClassInfo> realInterfaces() {
    return mRealInterfaces;
  }

  List<TypeInfo> realInterfaceTypes() {
    return mRealInterfaceTypes;
  }

  public String name() {
    return mName;
  }

  public String[] nameParts() {
    return mNameParts;
  }

  public String leafName() {
    return mNameParts[mNameParts.length - 1];
  }

  public String qualifiedName() {
    return mQualifiedName;
  }

  public List<MethodInfo> allConstructors() {
    return mAllConstructors;
  }

  @Override public void initVisible(Project project) {
    super.initVisible(project);

    // interfaces
    List<ClassInfo> nonHiddenInterfaces = new ArrayList<ClassInfo>();
    for (ClassInfo classInfo : mRealInterfaces) {
      if (classInfo.checkLevel()) {
        nonHiddenInterfaces.add(classInfo);
      }
    }
    Collections.sort(nonHiddenInterfaces);
    mInterfaces = ImmutableList.copyOf(nonHiddenInterfaces);

    // constructors
    // TODO: eliminate this condition; it's necessary for apicheck only
    if (mAllConstructors != null) {
      List<MethodInfo> nonHiddenConstructors = new ArrayList<MethodInfo>();
      List<MethodInfo> nonWrittenConstructors = new ArrayList<MethodInfo>();
      for (MethodInfo m : mAllConstructors) {
        if (m.checkLevel()) {
          nonHiddenConstructors.add(m);
        } else {
          nonWrittenConstructors.add(m);
        }
      }
      Collections.sort(nonHiddenConstructors, MethodInfo.ORDER_BY_NAME_AND_SIGNATURE);
      mConstructors = ImmutableList.copyOf(nonHiddenConstructors);
      Collections.sort(nonWrittenConstructors, MethodInfo.ORDER_BY_NAME_AND_SIGNATURE);
      mNonWrittenConstructors = ImmutableList.copyOf(nonWrittenConstructors);
    }

    // fields and methods, including inherited members
    if (mAllSelfMethods != null) {
      Map<String, MethodInfo> methods = new HashMap<String, MethodInfo>();
      Map<String, MethodInfo> hiddenMethods = new HashMap<String, MethodInfo>();
      Map<String, FieldInfo> fields = new HashMap<String, FieldInfo>();
      gatherMethods(this, methods, hiddenMethods);
      gatherFields(this, fields);
      for (ClassInfo s = mRealSuperclass; s != null; s = s.mRealSuperclass) {
        if (!s.checkLevel()) {
          s.gatherMethods(this, methods, hiddenMethods);
          s.gatherFields(this, fields);
        }
      }
      for (ClassInfo ifc : mRealInterfaces) {
        if (!ifc.checkLevel()) {
          ifc.gatherMethods(this, methods, hiddenMethods);
          ifc.gatherFields(this, fields);
        }
      }

      mSelfMethods = ImmutableList.copyOf(
          MethodInfo.ORDER_BY_NAME_AND_SIGNATURE.sortedCopy(methods.values()));
      mHiddenMethods = ImmutableList.copyOf(
          MethodInfo.ORDER_BY_NAME_AND_SIGNATURE.sortedCopy(hiddenMethods.values()));
      mSelfFields = ImmutableList.copyOf(FieldInfo.ORDER_BY_NAME.sortedCopy(fields.values()));
    }

    // deprecation
    boolean commentDeprecated = comment().isDeprecated();
    boolean annotationDeprecated = false;
    if (annotations() != null) {
      for (AnnotationInstanceInfo annotation : annotations()) {
        if (annotation.type().qualifiedName().equals("java.lang.Deprecated")) {
          annotationDeprecated = true;
          break;
        }
      }
      if (commentDeprecated != annotationDeprecated) {
        Errors.error(Errors.DEPRECATION_MISMATCH, position(), "Class " + qualifiedName()
            + ": @Deprecated annotation and @deprecated comment do not match");
      }
    }
    mIsDeprecated = commentDeprecated | annotationDeprecated;

    if (mAllConstructors != null) { // TODO api check only
      selfAttributes();
    }
  }

  public List<MethodInfo> getConstructors() {
    checkInitVisibleCalled();
    return mConstructors;
  }

  private void checkInitVisibleCalled() {
    if (mInterfaces == null) {
      throw new IllegalStateException(
          "Expected initVisible() to be called first; " + qualifiedName());
    }
  }

  public List<ClassInfo> innerClasses() {
    return mInnerClasses;
  }

  public List<TagInfo> inlineTags() {
    return comment().tags();
  }

  public List<TagInfo> firstSentenceTags() {
    return comment().briefTags();
  }

  public boolean isDeprecated() {
    return mIsDeprecated;
  }

  public List<TagInfo> deprecatedTags() {
    // Should we also do the interfaces?
    return comment().deprecatedTags();
  }

  public List<MethodInfo> annotationElements() {
    return mAnnotationElements;
  }

  public AnnotationInstanceInfo[] annotations() {
    return mAnnotations;
  }

  public List<FieldInfo> allSelfFields() {
    return mAllSelfFields;
  }

  private void gatherMethods(ClassInfo owner, Map<String, MethodInfo> methods,
      Map<String, MethodInfo> hiddenMethods) {
    for (MethodInfo methodInfo : mAllSelfMethods) {
      Map<String, MethodInfo> map = methodInfo.checkLevel() ? methods : hiddenMethods;

      // don't overwrite a method from the subclass
      String hashableName = methodInfo.getHashableName();
      if (!map.containsKey(hashableName)) {
        map.put(hashableName, methodInfo.cloneForClass(owner));
      }
    }
  }

  public void gatherFields(ClassInfo owner, Map<String, FieldInfo> out) {
    for (FieldInfo fieldInfo : mAllSelfFields) {
      if (!fieldInfo.checkLevel()) {
        continue;
      }

      if (!out.containsKey(fieldInfo.name())) {
        out.put(fieldInfo.name(), fieldInfo.cloneForClass(owner));
      }
    }
  }

  public List<FieldInfo> getFields() {
    checkInitVisibleCalled();
    return mSelfFields;
  }

  /**
   * Returns 'check level' methods cloned for this class.
   */
  public ImmutableList<MethodInfo> getMethods() {
    checkInitVisibleCalled();
    return mSelfMethods;
  }

  public List<MethodInfo> allSelfMethods() {
    return mAllSelfMethods;
  }

  public void addMethod(MethodInfo method) {
    mApiCheckMethods.put(method.getHashableName(), method);
    mAllSelfMethods.add(method);
  }
  
  public void setContainingPackage(PackageInfo pkg) {
    mContainingPackage = pkg;
  }

  public List<AttributeInfo> selfAttributes() {
    if (mSelfAttributes == null) {
      TreeMap<FieldInfo, AttributeInfo> attrs = new TreeMap<FieldInfo, AttributeInfo>();

      // the ones in the class comment won't have any methods
      for (AttrTagInfo tag : comment().attrTags()) {
        FieldInfo field = tag.reference();
        if (field != null) {
          AttributeInfo attr = attrs.get(field);
          if (attr == null) {
            attr = new AttributeInfo(this, field);
            attrs.put(field, attr);
          }
          tag.setAttribute(attr);
        }
      }

      // in the methods
      for (MethodInfo m : getMethods()) {
        for (AttrTagInfo tag : m.comment().attrTags()) {
          FieldInfo field = tag.reference();
          if (field != null) {
            AttributeInfo attr = attrs.get(field);
            if (attr == null) {
              attr = new AttributeInfo(this, field);
              attrs.put(field, attr);
            }
            tag.setAttribute(attr);
            attr.methods.add(m);
          }
        }
      }

      // constructors too
      for (MethodInfo m : getConstructors()) {
        for (AttrTagInfo tag : m.comment().attrTags()) {
          FieldInfo field = tag.reference();
          if (field != null) {
            AttributeInfo attr = attrs.get(field);
            if (attr == null) {
              attr = new AttributeInfo(this, field);
              attrs.put(field, attr);
            }
            tag.setAttribute(attr);
            attr.methods.add(m);
          }
        }
      }

      mSelfAttributes = new ArrayList<AttributeInfo>(attrs.values());
      Collections.sort(mSelfAttributes, AttributeInfo.comparator);
    }
    return mSelfAttributes;
  }

  public List<FieldInfo> enumConstants() {
    return mEnumConstants;
  }

  public ClassInfo superclass() {
    if (!mSuperclassInit) {
      if (this.checkLevel()) {
        // rearrange our little inheritance hierarchy, because we need to hide classes that
        // don't pass checkLevel
        ClassInfo superclass = mRealSuperclass;
        while (superclass != null && !superclass.checkLevel()) {
          superclass = superclass.mRealSuperclass;
        }
        mSuperclass = superclass;
      } else {
        mSuperclass = mRealSuperclass;
      }
    }
    return mSuperclass;
  }

  public ClassInfo realSuperclass() {
    return mRealSuperclass;
  }

  /**
   * always the real superclass, not the collapsed one we get through superclass(), also has the
   * type parameter info if it's generic.
   */
  public TypeInfo superclassType() {
    return mRealSuperclassType;
  }

  public TypeInfo asTypeInfo() {
    return mTypeInfo;
  }

  List<TypeInfo> interfaceTypes() {
    List<TypeInfo> types = new ArrayList<TypeInfo>();
    for (ClassInfo classInfo : getInterfaces()) {
      types.add(classInfo.asTypeInfo());
    }
    return types;
  }

  public String relativePath() {
    String s = containingPackage().name();
    s = s.replace('.', '/');
    s += '/';
    s += name();
    s += ".html";
    return s;
  }

  /** Even indirectly */
  public boolean isDerivedFrom(ClassInfo cl) {
    ClassInfo dad = this.superclass();
    if (dad != null) {
      if (dad.equals(cl)) {
        return true;
      } else {
        if (dad.isDerivedFrom(cl)) {
          return true;
        }
      }
    }
    for (ClassInfo iface : getInterfaces()) {
      if (iface.equals(cl)) {
        return true;
      } else {
        if (iface.isDerivedFrom(cl)) {
          return true;
        }
      }
    }
    return false;
  }

  public static void makeLinkListHDF(Data data, String base, List<ClassInfo> classes) {
    int i = 0;
    for (ClassInfo cl : classes) {
      if (cl.checkLevel()) {
        cl.asTypeInfo().makeHDF(data, base + "." + i);
      }
      i++;
    }
  }

  /**
   * Used in lists of this class (packages, nested classes, known subclasses)
   */
  public void makeShortDescrHDF(Data data, String base) {
    mTypeInfo.makeHDF(data, base + ".type");
    data.setValue(base + ".kind", this.kind());
    TagInfo.makeHDF(data, base + ".shortDescr", this.firstSentenceTags());
    TagInfo.makeHDF(data, base + ".deprecated", deprecatedTags());
    data.setValue(base + ".since.key", SinceTagger.keyForName(getSince()));
    data.setValue(base + ".since.name", getSince());
    setFederatedReferences(data, base);
  }

  /**
   * Turns into the main class page
   */
  public void makeHDF(Data data, Iterable<ClassInfo> rootClasses) {
    int i, j, n;
    String name = name();
    String qualified = qualifiedName();

    // class name
    mTypeInfo.makeHDF(data, "class.type");
    mTypeInfo.makeQualifiedHDF(data, "class.qualifiedType");
    data.setValue("class.name", name);
    data.setValue("class.qualified", qualified);
    if (isProtected()) {
      data.setValue("class.scope", "protected");
    } else if (isPublic()) {
      data.setValue("class.scope", "public");
    }
    if (isStatic()) {
      data.setValue("class.static", "static");
    }
    if (isFinal()) {
      data.setValue("class.final", "final");
    }
    if (isAbstract() && !isInterface()) {
      data.setValue("class.abstract", "abstract");
    }

    // class info
    String kind = kind();
    if (kind != null) {
      data.setValue("class.kind", kind);
    }
    data.setValue("class.since.key", SinceTagger.keyForName(getSince()));
    data.setValue("class.since.name", getSince());
    setFederatedReferences(data, "class");

    // the containing package -- note that this can be passed to type_link,
    // but it also contains the list of all of the packages
    containingPackage().makeClassLinkListHDF(data, "class.package");

    // inheritance hierarchy
    List<ClassInfo> superClasses = new ArrayList<ClassInfo>();
    superClasses.add(this);
    ClassInfo supr = superclass();
    while (supr != null) {
      superClasses.add(supr);
      supr = supr.superclass();
    }
    n = superClasses.size();
    for (i = 0; i < n; i++) {
      supr = superClasses.get(n - i - 1);

      supr.asTypeInfo().makeQualifiedHDF(data, "class.inheritance." + i + ".class");
      supr.asTypeInfo().makeHDF(data, "class.inheritance." + i + ".short_class");
      j = 0;
      for (TypeInfo t : supr.interfaceTypes()) {
        t.makeHDF(data, "class.inheritance." + i + ".interfaces." + j);
        j++;
      }
    }

    // class description
    TagInfo.makeHDF(data, "class.descr", inlineTags());
    TagInfo.makeHDF(data, "class.seeAlso", comment().seeTags());
    TagInfo.makeHDF(data, "class.deprecated", deprecatedTags());

    // known subclasses
    TreeMap<String, ClassInfo> direct = new TreeMap<String, ClassInfo>();
    TreeMap<String, ClassInfo> indirect = new TreeMap<String, ClassInfo>();
    for (ClassInfo cl : rootClasses) {
      if (cl.superclass() != null && cl.superclass().equals(this)) {
        direct.put(cl.name(), cl);
      } else if (cl.isDerivedFrom(this)) {
        indirect.put(cl.name(), cl);
      }
    }
    // direct
    i = 0;
    for (ClassInfo cl : direct.values()) {
      if (cl.checkLevel()) {
        cl.makeShortDescrHDF(data, "class.subclasses.direct." + i);
      }
      i++;
    }
    // indirect
    i = 0;
    for (ClassInfo cl : indirect.values()) {
      if (cl.checkLevel()) {
        cl.makeShortDescrHDF(data, "class.subclasses.indirect." + i);
      }
      i++;
    }

    // hide special cases
    if ("java.lang.Object".equals(qualified) || "java.io.Serializable".equals(qualified)) {
      data.setValue("class.subclasses.hidden", "1");
    } else {
      data.setValue("class.subclasses.hidden", "0");
    }

    // nested classes
    i = 0;
    for (ClassInfo inner : innerClasses()) {
      if (inner.checkLevel()) {
        inner.makeShortDescrHDF(data, "class.inners." + i);
      }
      i++;
    }

    // enum constants
    i = 0;
    for (FieldInfo field : enumConstants()) {
      if (field.isConstant()) {
        field.makeHDF(data, "class.enumConstants." + i);
        i++;
      }
    }

    // constants
    i = 0;
    for (FieldInfo field : getFields()) {
      if (field.isConstant()) {
        field.makeHDF(data, "class.constants." + i);
        i++;
      }
    }

    // fields
    i = 0;
    for (FieldInfo field : getFields()) {
      if (!field.isConstant()) {
        field.makeHDF(data, "class.fields." + i);
        i++;
      }
    }

    // public constructors
    i = 0;
    for (MethodInfo ctor : getConstructors()) {
      if (ctor.isPublic()) {
        ctor.makeHDF(data, "class.ctors.public." + i);
        i++;
      }
    }

    // protected constructors
    if (Doclava.checkLevel(Doclava.SHOW_PROTECTED)) {
      i = 0;
      for (MethodInfo ctor : getConstructors()) {
        if (ctor.isProtected()) {
          ctor.makeHDF(data, "class.ctors.protected." + i);
          i++;
        }
      }
    }

    // package private constructors
    if (Doclava.checkLevel(Doclava.SHOW_PACKAGE)) {
      i = 0;
      for (MethodInfo ctor : getConstructors()) {
        if (ctor.isPackagePrivate()) {
          ctor.makeHDF(data, "class.ctors.package." + i);
          i++;
        }
      }
    }

    // private constructors
    if (Doclava.checkLevel(Doclava.SHOW_PRIVATE)) {
      i = 0;
      for (MethodInfo ctor : getConstructors()) {
        if (ctor.isPrivate()) {
          ctor.makeHDF(data, "class.ctors.private." + i);
          i++;
        }
      }
    }

    // public methods
    i = 0;
    for (MethodInfo method : getMethods()) {
      if (method.isPublic()) {
        method.makeHDF(data, "class.methods.public." + i);
        i++;
      }
    }

    // protected methods
    if (Doclava.checkLevel(Doclava.SHOW_PROTECTED)) {
      i = 0;
      for (MethodInfo method : getMethods()) {
        if (method.isProtected()) {
          method.makeHDF(data, "class.methods.protected." + i);
          i++;
        }
      }
    }

    // package private methods
    if (Doclava.checkLevel(Doclava.SHOW_PACKAGE)) {
      i = 0;
      for (MethodInfo method : getMethods()) {
        if (method.isPackagePrivate()) {
          method.makeHDF(data, "class.methods.package." + i);
          i++;
        }
      }
    }

    // private methods
    if (Doclava.checkLevel(Doclava.SHOW_PRIVATE)) {
      i = 0;
      for (MethodInfo method : getMethods()) {
        if (method.isPrivate()) {
          method.makeHDF(data, "class.methods.private." + i);
          i++;
        }
      }
    }

    // xml attributes
    i = 0;
    for (AttributeInfo attr : selfAttributes()) {
      if (attr.checkLevel()) {
        attr.makeHDF(data, "class.attrs." + i);
        i++;
      }
    }

    // inherited methods
    Set<ClassInfo> interfaces = new TreeSet<ClassInfo>();
    addInterfaces(getInterfaces(), interfaces);
    ClassInfo cl = superclass();
    i = 0;
    while (cl != null) {
      addInterfaces(cl.getInterfaces(), interfaces);
      makeInheritedHDF(data, i, cl);
      cl = cl.superclass();
      i++;
    }
    for (ClassInfo iface : interfaces) {
      makeInheritedHDF(data, i, iface);
      i++;
    }
  }

  private static void addInterfaces(List<ClassInfo> ifaces, Set<ClassInfo> out) {
    for (ClassInfo cl : ifaces) {
      out.add(cl);
      addInterfaces(cl.getInterfaces(), out);
    }
  }

  private static void makeInheritedHDF(Data data, int index, ClassInfo cl) {
    int i;

    String base = "class.inherited." + index;
    data.setValue(base + ".qualified", cl.qualifiedName());
    if (cl.checkLevel()) {
      data.setValue(base + ".link", cl.htmlPage());
    }
    String kind = cl.kind();
    if (kind != null) {
      data.setValue(base + ".kind", kind);
    }

    if (cl.isDefinedLocally()) {
      data.setValue(base + ".included", "true");
    } else {
      Doclava.federationTagger.tagAll(ImmutableList.of(cl));
      if (!cl.getFederatedReferences().isEmpty()) {
        FederatedSite site = cl.getFederatedReferences().iterator().next();
        data.setValue(base + ".link", site.linkFor(cl.relativePath()));
        data.setValue(base + ".federated", site.name());
      }
    }

    // xml attributes
    i = 0;
    for (AttributeInfo attr : cl.selfAttributes()) {
      attr.makeHDF(data, base + ".attrs." + i);
      i++;
    }

    // methods
    i = 0;
    for (MethodInfo method : cl.getMethods()) {
      method.makeHDF(data, base + ".methods." + i);
      i++;
    }

    // fields
    i = 0;
    for (FieldInfo field : cl.getFields()) {
      if (!field.isConstant()) {
        field.makeHDF(data, base + ".fields." + i);
        i++;
      }
    }

    // constants
    i = 0;
    for (FieldInfo field : cl.getFields()) {
      if (field.isConstant()) {
        field.makeHDF(data, base + ".constants." + i);
        i++;
      }
    }
  }

  @Override
  public boolean isHidden() {
    int val = mHidden;
    if (val >= 0) {
      return val != 0;
    } else {
      boolean v = isHiddenImpl();
      mHidden = v ? 1 : 0;
      return v;
    }
  }

  public boolean isHiddenImpl() {
    ClassInfo cl = this;
    while (cl != null) {
      PackageInfo pkg = cl.containingPackage();
      if (pkg != null && pkg.isHidden()) {
        return true;
      }
      if (cl.comment().isHidden()) {
        return true;
      }
      cl = cl.containingClass();
    }
    return false;
  }

  private MethodInfo matchMethod(List<MethodInfo> methods, String name, String[] params,
      String[] dimensions, boolean varargs) {
    for (MethodInfo method : methods) {
      if (method.name().equals(name)) {
        if (params == null) {
          return method;
        } else {
          if (method.matchesParams(params, dimensions, varargs)) {
            return method;
          }
        }
      }
    }
    return null;
  }

  public MethodInfo findMethod(String name, String[] params, String[] dimensions, boolean varargs) {
    // first look on our class, and our superclasses

    // for methods
    MethodInfo rv = matchMethod(mAllSelfMethods, name, params, dimensions, varargs);
    if (rv != null) {
      return rv;
    }

    // for constructors
    rv = matchMethod(mAllConstructors, name, params, dimensions, varargs);
    if (rv != null) {
      return rv;
    }

    // then recursively look at our containing class
    ClassInfo containing = containingClass();
    if (containing != null) {
      return containing.findMethod(name, params, dimensions, varargs);
    }

    return null;
  }
  
  /**
   * Returns true if the given method's signature is available in this class,
   * either directly or via inheritance.
   */
  public boolean containsMethod(MethodInfo method) {
    for (MethodInfo m : getMethods()) {
      if (m.getHashableName().equals(method.getHashableName())) {
        return true;
      }
    }
    return false;
  }

  private ClassInfo searchInnerClasses(String[] nameParts, int index) {
    String part = nameParts[index];

    for (ClassInfo in : mInnerClasses) {
      String[] innerParts = in.nameParts();
      if (part.equals(innerParts[innerParts.length - 1])) {
        if (index == nameParts.length - 1) {
          return in;
        } else {
          return in.searchInnerClasses(nameParts, index + 1);
        }
      }
    }
    return null;
  }

  public ClassInfo extendedFindClass(String className) {
    // ClassDoc.findClass has this bug that we're working around here:
    // If you have a class PackageManager with an inner class PackageInfo
    // and you call it with "PackageInfo" it doesn't find it.
    return searchInnerClasses(className.split("\\."), 0);
  }

  public ClassInfo findClass(String className, Project project) {
    return project.getClassReference(mClass.findClass(className));
  }

  public ClassInfo findInnerClass(String className, Project project) {
    // ClassDoc.findClass won't find inner classes. To deal with that,
    // we try what they gave us first, but if that didn't work, then
    // we see if there are any periods in className, and start searching
    // from there.
    String[] nodes = className.split("\\.");
    ClassDoc cl = mClass;
    for (String n : nodes) {
      cl = cl.findClass(n);
      if (cl == null) {
        return null;
      }
    }
    return project.getClassReference(cl);
  }

  public FieldInfo findField(String name) {
    // first look on our class, and our superclasses
    for (FieldInfo f : mAllSelfFields) {
      if (f.name().equals(name)) {
        return f;
      }
    }

    // then look at our enum constants (these are really fields, maybe
    // they should be mixed into fields(). not sure)
    for (FieldInfo f : mEnumConstants) {
      if (f.name().equals(name)) {
        return f;
      }
    }

    // then recursively look at our containing class
    ClassInfo containing = containingClass();
    if (containing != null) {
      return containing.findField(name);
    }

    return null;
  }

  public boolean equals(ClassInfo that) {
    if (that != null) {
      return this.qualifiedName().equals(that.qualifiedName());
    } else {
      return false;
    }
  }

  public List<MethodInfo> getNonWrittenConstructors() {
    checkInitVisibleCalled();
    return mNonWrittenConstructors;
  }

  public String kind() {
    if (isOrdinaryClass()) {
      return "class";
    } else if (isInterface()) {
      return "interface";
    } else if (isEnum()) {
      return "enum";
    } else if (isError()) {
      return "class";
    } else if (isException()) {
      return "class";
    } else if (isAnnotation()) {
      return "@interface";
    }
    return null;
  }
  
  public String scope() {
    if (isPublic()) {
      return "public";
    } else if (isProtected()) {
      return "protected";
    } else if (isPackagePrivate()) {
      return "";
    } else if (isPrivate()) {
      return "private";
    } else {
      throw new RuntimeException("invalid scope for object " + this);
    }
  }

  public List<MethodInfo> getHiddenMethods() {
    checkInitVisibleCalled();
    return mHiddenMethods;
  }

  @Override
  public String toString() {
    return this.qualifiedName();
  }

  private ClassDoc mClass;

  // ctor
  private boolean mIsPublic;
  private boolean mIsProtected;
  private boolean mIsPackagePrivate;
  private boolean mIsPrivate;
  private boolean mIsStatic;
  private boolean mIsInterface;
  private boolean mIsAbstract;
  private boolean mIsOrdinaryClass;
  private boolean mIsException;
  private boolean mIsError;
  private boolean mIsEnum;
  private boolean mIsAnnotation;
  private boolean mIsFinal;
  private boolean mIsIncluded;
  private String mName;
  private String mQualifiedName;
  private TypeInfo mTypeInfo;
  private String[] mNameParts;

  // init
  private List<ClassInfo> mRealInterfaces = new ArrayList<ClassInfo>();
  private List<ClassInfo> mInterfaces;
  private List<TypeInfo> mRealInterfaceTypes;
  private List<ClassInfo> mInnerClasses;
  private List<MethodInfo> mAllConstructors;
  private List<MethodInfo> mAllSelfMethods = new ArrayList<MethodInfo>();
  private List<MethodInfo> mAnnotationElements; // if this class is an annotation
  private List<FieldInfo> mAllSelfFields = new ArrayList<FieldInfo>();
  private List<FieldInfo> mEnumConstants;
  private PackageInfo mContainingPackage;
  private ClassInfo mContainingClass;
  private ClassInfo mRealSuperclass;
  private TypeInfo mRealSuperclassType;
  private ClassInfo mSuperclass;
  private AnnotationInstanceInfo[] mAnnotations;
  private boolean mSuperclassInit;

  // display
  private ImmutableList<MethodInfo> mConstructors;
  private List<ClassInfo> mRealInnerClasses;
  private ImmutableList<MethodInfo> mSelfMethods;
  private ImmutableList<FieldInfo> mSelfFields;
  private List<AttributeInfo> mSelfAttributes;
  private List<MethodInfo> mHiddenMethods;
  private int mHidden = -1;
  private int mCheckLevel = -1;
  private ImmutableList<MethodInfo> mNonWrittenConstructors;
  private boolean mIsDeprecated;
  
  // TODO: Temporary members from apicheck migration.
  private HashMap<String, MethodInfo> mApiCheckMethods = new HashMap<String, MethodInfo>();
  private HashMap<String, FieldInfo> mApiCheckFields = new HashMap<String, FieldInfo>();
  private HashMap<String, ConstructorInfo> mApiCheckConstructors
      = new HashMap<String, ConstructorInfo>();
  
  /**
   * Returns true if {@code cl} implements the interface {@code iface} either by either being that
   * interface, implementing that interface or extending a type that implements the interface.
   */
  private boolean implementsInterface(ClassInfo cl, String iface) {
    if (cl.qualifiedName().equals(iface)) {
      return true;
    }
    for (ClassInfo clImplements : cl.getInterfaces()) {
      if (implementsInterface(clImplements, iface)) {
        return true;
      }
    }
    if (cl.mSuperclass != null && implementsInterface(cl.mSuperclass, iface)) {
      return true;
    }
    return false;
  }


  public void addInterface(ClassInfo iface) {
    mRealInterfaces.add(iface);
  }

  public void addConstructor(ConstructorInfo cInfo) {
    mApiCheckConstructors.put(cInfo.getHashableName(), cInfo);

  }

  public void addField(FieldInfo fInfo) {
    mApiCheckFields.put(fInfo.name(), fInfo);

  }

  public void setSuperClass(ClassInfo superclass) {
    mSuperclass = superclass;
  }

  public Map<String, FieldInfo> allFields() {
    return mApiCheckFields;
  }

  /**
   * Returns all methods defined directly in this class.
   */
  public Map<String, MethodInfo> allMethods() {
    return mApiCheckMethods;
  }

  /**
   * Returns the class hierarchy for this class, starting with this class.
   */
  public Iterable<ClassInfo> hierarchy() {
    List<ClassInfo> result = new ArrayList<ClassInfo>(4);
    for (ClassInfo c = this; c != null; c = c.mSuperclass) {
      result.add(c);
    }
    return result;
  }
  
  public String superclassName() {
    if (mSuperclass == null) {
      if (mQualifiedName.equals("java.lang.Object")) {
        return null;
      }
      throw new IllegalStateException("Superclass not set for " + qualifiedName());
    }
    return mSuperclass.mQualifiedName;
  }
  
  public void setAnnotations(AnnotationInstanceInfo[] annotations) {
    mAnnotations = annotations;
  }
  
  public boolean isConsistent(ClassInfo cl) {
    boolean consistent = true;

    if (isInterface() != cl.isInterface()) {
      Errors.error(Errors.CHANGED_CLASS, cl.position(), "Class " + cl.qualifiedName()
          + " changed class/interface declaration");
      consistent = false;
    }
    for (ClassInfo iface : mRealInterfaces) {
      if (!implementsInterface(cl, iface.mQualifiedName)) {
        Errors.error(Errors.REMOVED_INTERFACE, cl.position(), "Class " + qualifiedName()
            + " no longer implements " + iface);
      }
    }
    for (ClassInfo iface : cl.mRealInterfaces) {
      if (!implementsInterface(this, iface.mQualifiedName)) {
        Errors.error(Errors.ADDED_INTERFACE, cl.position(), "Added interface " + iface
            + " to class " + qualifiedName());
        consistent = false;
      }
    }

    for (MethodInfo mInfo : mApiCheckMethods.values()) {
      if (cl.mApiCheckMethods.containsKey(mInfo.getHashableName())) {
        if (!mInfo.isConsistent(cl.mApiCheckMethods.get(mInfo.getHashableName()))) {
          consistent = false;
        }
      } else {
        /*
         * This class formerly provided this method directly, and now does not. Check our ancestry
         * to see if there's an inherited version that still fulfills the API requirement.
         */
        MethodInfo mi = ClassInfo.overriddenMethod(mInfo, cl);
        if (mi == null) {
          mi = ClassInfo.interfaceMethod(mInfo, cl);
        }
        if (mi == null) {
          Errors.error(Errors.REMOVED_METHOD, mInfo.position(), "Removed public method "
              + mInfo.qualifiedName());
          consistent = false;
        }
      }
    }
    for (MethodInfo mInfo : cl.mApiCheckMethods.values()) {
      if (!mApiCheckMethods.containsKey(mInfo.getHashableName())) {
        /*
         * Similarly to the above, do not fail if this "new" method is really an override of an
         * existing superclass method.
         */
        MethodInfo mi = ClassInfo.overriddenMethod(mInfo, this);
        if (mi == null) {
          Errors.error(Errors.ADDED_METHOD, mInfo.position(), "Added public method "
              + mInfo.qualifiedName());
          consistent = false;
        }
      }
    }

    for (ConstructorInfo mInfo : mApiCheckConstructors.values()) {
      if (cl.mApiCheckConstructors.containsKey(mInfo.getHashableName())) {
        if (!mInfo.isConsistent(cl.mApiCheckConstructors.get(mInfo.getHashableName()))) {
          consistent = false;
        }
      } else {
        Errors.error(Errors.REMOVED_METHOD, mInfo.position(), "Removed public constructor "
            + mInfo.prettySignature());
        consistent = false;
      }
    }
    for (ConstructorInfo mInfo : cl.mApiCheckConstructors.values()) {
      if (!mApiCheckConstructors.containsKey(mInfo.getHashableName())) {
        Errors.error(Errors.ADDED_METHOD, mInfo.position(), "Added public constructor "
            + mInfo.prettySignature());
        consistent = false;
      }
    }

    for (FieldInfo mInfo : mApiCheckFields.values()) {
      if (cl.mApiCheckFields.containsKey(mInfo.name())) {
        if (!mInfo.isConsistent(cl.mApiCheckFields.get(mInfo.name()))) {
          consistent = false;
        }
      } else {
        Errors.error(Errors.REMOVED_FIELD, mInfo.position(), "Removed field "
            + mInfo.qualifiedName());
        consistent = false;
      }
    }
    for (FieldInfo mInfo : cl.mApiCheckFields.values()) {
      if (!mApiCheckFields.containsKey(mInfo.name())) {
        Errors.error(Errors.ADDED_FIELD, mInfo.position(), "Added public field "
            + mInfo.qualifiedName());
        consistent = false;
      }
    }

    if (mIsAbstract != cl.mIsAbstract) {
      consistent = false;
      Errors.error(Errors.CHANGED_ABSTRACT, cl.position(), "Class " + cl.qualifiedName()
          + " changed abstract qualifier");
    }

    if (mIsFinal != cl.mIsFinal) {
      consistent = false;
      Errors.error(Errors.CHANGED_FINAL, cl.position(), "Class " + cl.qualifiedName()
          + " changed final qualifier");
    }

    if (mIsStatic != cl.mIsStatic) {
      consistent = false;
      Errors.error(Errors.CHANGED_STATIC, cl.position(), "Class " + cl.qualifiedName()
          + " changed static qualifier");
    }

    if (!scope().equals(cl.scope())) {
      consistent = false;
      Errors.error(Errors.CHANGED_SCOPE, cl.position(), "Class " + cl.qualifiedName()
          + " scope changed from " + scope() + " to " + cl.scope());
    }

    if (!isDeprecated() == cl.isDeprecated()) {
      consistent = false;
      Errors.error(Errors.CHANGED_DEPRECATED, cl.position(), "Class " + cl.qualifiedName()
          + " has changed deprecation state");
    }

    if (superclassName() != null) {
      if (cl.superclassName() == null || !superclassName().equals(cl.superclassName())) {
        consistent = false;
        Errors.error(Errors.CHANGED_SUPERCLASS, cl.position(), "Class " + qualifiedName()
            + " superclass changed from " + superclassName() + " to " + cl.superclassName());
      }
    } else if (cl.superclassName() != null) {
      consistent = false;
      Errors.error(Errors.CHANGED_SUPERCLASS, cl.position(), "Class " + qualifiedName()
          + " superclass changed from " + "null to " + cl.superclassName());
    }

    return consistent;
  }
  
  // Find a superclass implementation of the given method.
  public static MethodInfo overriddenMethod(MethodInfo candidate, ClassInfo newClassObj) {
    if (newClassObj == null) {
      return null;
    }
    for (MethodInfo mi : newClassObj.mApiCheckMethods.values()) {
      if (mi.matches(candidate)) {
        // found it
        return mi;
      }
    }

    // not found here. recursively search ancestors
    return ClassInfo.overriddenMethod(candidate, newClassObj.mSuperclass);
  }

  // Find a superinterface declaration of the given method.
  public static MethodInfo interfaceMethod(MethodInfo candidate, ClassInfo newClassObj) {
    if (newClassObj == null) {
      return null;
    }
    for (ClassInfo interfaceInfo : newClassObj.getInterfaces()) {
      for (MethodInfo mi : interfaceInfo.mApiCheckMethods.values()) {
        if (mi.matches(candidate)) {
          return mi;
        }
      }
    }
    return ClassInfo.interfaceMethod(candidate, newClassObj.mSuperclass);
  }
  
  public boolean hasConstructor(MethodInfo constructor) {
    String name = constructor.getHashableName();
    for (ConstructorInfo ctor : mApiCheckConstructors.values()) {
      if (name.equals(ctor.getHashableName())) {
        return true;
      }
    }
    return false;
  }
  
  public void setTypeInfo(TypeInfo typeInfo) {
    mTypeInfo = typeInfo;
  }
}
