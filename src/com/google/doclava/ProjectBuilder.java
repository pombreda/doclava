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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.AnnotationTypeDoc;
import com.sun.javadoc.AnnotationTypeElementDoc;
import com.sun.javadoc.AnnotationValue;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.ExecutableMemberDoc;
import com.sun.javadoc.FieldDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.PackageDoc;
import com.sun.javadoc.ParamTag;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.SeeTag;
import com.sun.javadoc.SourcePosition;
import com.sun.javadoc.Tag;
import com.sun.javadoc.ThrowsTag;
import com.sun.javadoc.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public final class ProjectBuilder {
  private final Map<String, TypeInfo> typesByName = new HashMap<String, TypeInfo>();
  private final Map<PackageDoc, PackageInfo> packages = new HashMap<PackageDoc, PackageInfo>();
  private final Map<ClassDoc, ClassInfo> classes = new HashMap<ClassDoc, ClassInfo>();
  private final Map<ExecutableMemberDoc, MethodInfo> methods = new HashMap<ExecutableMemberDoc, MethodInfo>();
  private final Map<FieldDoc, FieldInfo> fields = new HashMap<FieldDoc, FieldInfo>();
  private final Map<AnnotationDesc, AnnotationInstanceInfo> annotationInstances = new HashMap<AnnotationDesc, AnnotationInstanceInfo>();

  private List<ClassNeedingInit> mClassesNeedingInit = new ArrayList<ClassNeedingInit>();

  private final HashMap<AnnotationValue, AnnotationValueInfo> mAnnotationValues =
      new HashMap<AnnotationValue, AnnotationValueInfo>();
  private HashSet<AnnotationValue> mAnnotationValuesNeedingInit =
      new HashSet<AnnotationValue>();

  public Project build(final RootDoc rootDoc) {
    List<ClassInfo> classInfos = new ArrayList<ClassInfo>();
    for (ClassDoc classDoc : rootDoc.classes()) {
      classInfos.add(obtainClass(classDoc));
    }

    List<ClassInfo> classesNeedingInit2 = new ArrayList<ClassInfo>();
    // fill in the fields that reference other classes
    while (!mClassesNeedingInit.isEmpty()) {
      ClassNeedingInit classNeedingInit = mClassesNeedingInit.remove(
          mClassesNeedingInit.size() - 1);
      initClass(classNeedingInit.c, classNeedingInit.cl);
      classesNeedingInit2.add(classNeedingInit.cl);
    }
    mClassesNeedingInit = null;

    finishAnnotationValueInit();

    final ImmutableList<TagInfo> rootTags = ImmutableList.copyOf(convertTags(rootDoc.inlineTags(), null));
    final ImmutableMap<ClassDoc, ClassInfo> classDocToClass = ImmutableMap.copyOf(classes);
    final ImmutableMap<PackageDoc, PackageInfo> packageDocToPackage = ImmutableMap.copyOf(packages);
    final ImmutableList<ClassInfo> rootClasses = ImmutableList.copyOf(classInfos);
    final ImmutableList<FieldInfo> allFields = ImmutableList.copyOf(fields.values());
    final ImmutableList<MethodInfo> allMethods = ImmutableList.copyOf(methods.values());

    return new Project() {
      public ClassInfo getClassByName(String name) {
        return classDocToClass.get(rootDoc.classNamed(name));
      }

      public ClassInfo getClassReference(ClassDoc classDoc) {
        if (classDoc == null) {
          return null;
        }

        ClassInfo classInfo = classDocToClass.get(classDoc);
        if (classInfo != null) {
          return classInfo;
        }

        classInfo = docToInfo(classDoc);
        classInfo.setTypeInfo(typeToTypeInfo(classDoc, classInfo)); // TODO: proper init
        return classInfo;
      }

      public PackageInfo getPackage(String name) {
        PackageDoc doc = rootDoc.packageNamed(name);
        return packageDocToPackage.get(doc);
      }

      public List<ClassInfo> rootClasses() {
        return rootClasses;
      }

      public List<ClassInfo> allClasses() {
        return new ArrayList<ClassInfo>(classDocToClass.values());
      }

      public List<ClassInfo> getClasses(ClassDoc[] classes) {
        if (classes == null) {
          return null;
        }
        List<ClassInfo> result = new ArrayList<ClassInfo>();
        for (ClassDoc classDoc : classes) {
          result.add(obtainClass(classDoc));
        }
        return result;
      }

      private ClassInfo obtainClass(ClassDoc classDoc) {
        ClassInfo classInfo = classDocToClass.get(classDoc);
        if (classInfo == null) {
          throw new IllegalArgumentException("Not found: " + classDoc);
        }
        return classInfo;
      }

      public List<FieldInfo> getAllFields() {
        return allFields;
      }

      public List<MethodInfo> getAllMethods() {
        return allMethods;
      }

      public List<TagInfo> getRootTags() {
        return rootTags;
      }
    };
  }

  private void initClass(ClassDoc c, ClassInfo cl) {
    MethodDoc[] annotationElements;
    if (c instanceof AnnotationTypeDoc) {
      annotationElements = ((AnnotationTypeDoc) c).elements();
    } else {
      annotationElements = new MethodDoc[0];
    }
    cl.init(obtainType(c), convertClasses(c.interfaces()),
        convertTypes(c.interfaceTypes()), convertClasses(c.innerClasses()),
        convertMethods(c.constructors(false)), convertMethods(c.methods(false)),
        convertMethods(annotationElements), convertFields(c.fields(false)),
        convertFields(c.enumConstants()), obtainPackage(c.containingPackage()),
        obtainClass(c.containingClass()), obtainClass(c.superclass()),
        obtainType(c.superclassType()), convertAnnotationInstances(c.annotations()),
        convertClasses(c.innerClasses(false)));
  }

  private TagInfo convertTag(Tag tag) {
    return new TextTagInfo(tag.name(), tag.kind(), tag.text(),
        convertSourcePosition(tag.position()));
  }

  private ThrowsTagInfo convertThrowsTag(ThrowsTag tag, ContainerInfo base) {
    return new ThrowsTagInfo(tag.name(), tag.text(), tag.kind(),
        obtainClass(tag.exception()), tag.exceptionComment(), base,
        convertSourcePosition(tag.position()));
  }

  private ParamTagInfo convertParamTag(ParamTag tag, ContainerInfo base) {
    return new ParamTagInfo(tag.name(), tag.kind(), tag.text(), tag.isTypeParameter(),
        tag.parameterComment(), tag.parameterName(), base,
        convertSourcePosition(tag.position()));
  }

  private SeeTagInfo convertSeeTag(SeeTag tag, ContainerInfo base) {
    return new SeeTagInfo(tag.name(), tag.kind(), tag.text(), base,
        convertSourcePosition(tag.position()));
  }

  private SourcePositionInfo convertSourcePosition(SourcePosition sp) {
    if (sp == null) {
      return SourcePositionInfo.UNKNOWN;
    }
    return new SourcePositionInfo(sp.file().toString(), sp.line(), sp.column());
  }

  private List<TagInfo> convertTags(Tag[] tags, ContainerInfo base) {
    List<TagInfo> result = new ArrayList<TagInfo>();
    for (Tag tag : tags) {
      if (tag instanceof SeeTag) {
        result.add(convertSeeTag((SeeTag) tag, base));
      } else if (tag instanceof ThrowsTag) {
        result.add(convertThrowsTag((ThrowsTag) tag, base));
      } else if (tag instanceof ParamTag) {
        result.add(convertParamTag((ParamTag) tag, base));
      } else {
        result.add(convertTag(tag));
      }
    }
    return result;
  }

  private List<ClassInfo> convertClasses(ClassDoc[] classes) {
    if (classes == null) {
      return null;
    }
    List<ClassInfo> result = new ArrayList<ClassInfo>();
    for (ClassDoc classDoc : classes) {
      result.add(obtainClass(classDoc));
    }
    return result;
  }

  private ParameterInfo convertParameter(Parameter p, SourcePosition pos) {
    if (p == null) {
      return null;
    }
    return new ParameterInfo(p.name(), p.typeName(), obtainType(p.type()),
        convertSourcePosition(pos));
  }

  private List<ParameterInfo> convertParameters(Parameter[] p, ExecutableMemberDoc m) {
    List<ParameterInfo> result = new ArrayList<ParameterInfo>();
    SourcePosition pos = m.position();
    for (Parameter parameter : p) {
      result.add(convertParameter(parameter, pos));
    }
    return result;
  }

  private List<TypeInfo> convertTypes(Type[] p) {
    if (p == null) {
      return null;
    }
    List<TypeInfo> result = new ArrayList<TypeInfo>();
    for (Type type : p) {
      result.add(obtainType(type));
    }
    return result;
  }

  private class ClassNeedingInit {
    ClassNeedingInit(ClassDoc c, ClassInfo cl) {
      this.c = c;
      this.cl = cl;
    }

    ClassDoc c;
    ClassInfo cl;
  }

  private ClassInfo obtainClass(ClassDoc input) {
    if (input == null) {
      return null;
    }
    
    ClassInfo result = classes.get(input);
    if (result != null) {
      return result;
    }

    if (input.name() == null || input.name().equals("")) {
       return null;
    }
    result = docToInfo(input);
    if (mClassesNeedingInit != null) {
      mClassesNeedingInit.add(new ClassNeedingInit(input, result));
    }
    classes.put(input, result);

    if (mClassesNeedingInit == null) {
      initClass(input, result);
    }

    return result;
  }

  private ClassInfo docToInfo(ClassDoc input) {
    return new ClassInfo(input, input.getRawCommentText(),
        convertSourcePosition(input.position()), input.isPublic(), input.isProtected(),
        input.isPackagePrivate(), input.isPrivate(), input.isStatic(), input.isInterface(),
        input.isAbstract(), input.isOrdinaryClass(), input.isException(), input.isError(),
        input.isEnum(), (input instanceof AnnotationTypeDoc), input.isFinal(), input.isIncluded(),
        input.name(), input.qualifiedName());
  }

  /**
   * Returns the MethodInfos for the given MethodDocs  or ConstructorDocs.
   */
  private <T extends ExecutableMemberDoc> List<MethodInfo> convertMethods(T[] methods) {
    if (methods == null) {
      return null;
    }
    List<MethodInfo> result = new ArrayList<MethodInfo>();
    for (T methodDoc : methods) {
      result.add(obtainMethod(methodDoc));
    }
    return result;
  }

  private MethodInfo obtainMethod(ExecutableMemberDoc o) {
    MethodInfo result = methods.get(o);
    if (result != null) {
      return result;
    }

    if (o == null) {
      return null;
    }

    String kind;
    boolean isAbstract = false;
    MethodInfo overriddenMethod = null;
    TypeInfo returnType = null;
    AnnotationValueInfo annotationElementValue = null;

    if (o instanceof MethodDoc) {
      MethodDoc doc = (MethodDoc) o;
      isAbstract = doc.isAbstract();
      overriddenMethod = obtainMethod(doc.overriddenMethod());
      returnType = obtainType(doc.returnType());
      if (o instanceof AnnotationTypeElementDoc) {
        kind = "annotationElement";
        AnnotationTypeElementDoc a = (AnnotationTypeElementDoc) o;
        annotationElementValue = obtainAnnotationValue(a.defaultValue(), result);
      } else {
        kind = "method";
      }
    } else {
      kind = "constructor";
    }

    ClassInfo containingClass = obtainClass(o.containingClass());
    result = new MethodInfo(o.getRawCommentText(), convertTypes(o.typeParameters()), o.name(),
        o.signature(), containingClass, containingClass, o.isPublic(), o.isProtected(),
        o.isPackagePrivate(), o.isPrivate(), o.isFinal(), o.isStatic(), o.isSynthetic(),
        isAbstract, o.isSynchronized(), o.isNative(), kind, o.flatSignature(), overriddenMethod,
        returnType, convertParameters(o.parameters(), o), convertClasses(o.thrownExceptions()),
        convertSourcePosition(o.position()), convertAnnotationInstances(o.annotations()));
    result.setVarargs(o.isVarArgs());
    result.init(annotationElementValue);

    methods.put(o, result);
    return result;
  }

  private List<FieldInfo> convertFields(FieldDoc[] fields) {
    if (fields == null) {
      return null;
    }
    List<FieldInfo> result = new ArrayList<FieldInfo>();
    for (FieldDoc fieldDoc : fields) {
      result.add(obtainField(fieldDoc));
    }
    return result;
  }

  private FieldInfo obtainField(FieldDoc f) {
    FieldInfo result = fields.get(f);
    if (result != null) {
      return result;
    }

    result = new FieldInfo(f.name(), obtainClass(f.containingClass()),
        obtainClass(f.containingClass()), f.isPublic(), f.isProtected(), f.isPackagePrivate(),
        f.isPrivate(), f.isFinal(), f.isStatic(), f.isTransient(), f.isVolatile(), f.isSynthetic(),
        obtainType(f.type()), f.getRawCommentText(), f.constantValue(),
        convertSourcePosition(f.position()), convertAnnotationInstances(f.annotations()));
    fields.put(f, result);
    return result;
  }

  private PackageInfo obtainPackage(PackageDoc p) {
    PackageInfo result = packages.get(p);
    if (result == null) {
      result = new PackageInfo(p, p.name(), convertSourcePosition(p.position()));
      packages.put(p, result);
    }
    return result;
  }

  private TypeInfo obtainType(Type type) {
    if (type == null) {
      return null;
    }

    String string = typeToString(type);

    TypeInfo typeInfo = typesByName.get(string);
    if (typeInfo != null) {
      return typeInfo;
    }

    typeInfo = typeToTypeInfo(type, obtainClass(type.asClassDoc()));
    typesByName.put(string, typeInfo);

    if (type.asParameterizedType() != null) {
      typeInfo.setTypeArguments(convertTypes(type.asParameterizedType().typeArguments()));
    } else if (type instanceof ClassDoc) {
      typeInfo.setTypeArguments(convertTypes(((ClassDoc) type).typeParameters()));
    } else if (type.asTypeVariable() != null) {
      typeInfo.setBounds(null, convertTypes((type.asTypeVariable().bounds())));
      typeInfo.setIsTypeVariable(true);
    } else if (type.asWildcardType() != null) {
      typeInfo.setIsWildcard(true);
      typeInfo.setBounds(convertTypes(type.asWildcardType().superBounds()),
          convertTypes(type.asWildcardType().extendsBounds()));
    }

    return typeInfo;
  }
  
  private String typeToString(Type t) {
    StringBuilder result = new StringBuilder();
    result.append(t.getClass().getName()).append("/").append(t).append("/");
    if (t.asParameterizedType() != null) {
      result.append(t.asParameterizedType()).append("/");
      if (t.asParameterizedType().typeArguments() != null) {
        for (Type ty : t.asParameterizedType().typeArguments()) {
          result.append(ty).append("/");
        }
      }
    } else {
      result.append("NoParameterizedType//");
    }
    if (t.asTypeVariable() != null) {
      result.append(t.asTypeVariable()).append("/");
      if (t.asTypeVariable().bounds() != null) {
        for (Type ty : t.asTypeVariable().bounds()) {
          result.append(ty).append("/");
        }
      }
    } else {
      result.append("NoTypeVariable//");
    }
    if (t.asWildcardType() != null) {
      result.append(t.asWildcardType()).append("/");
      if (t.asWildcardType().superBounds() != null) {
        for (Type ty : t.asWildcardType().superBounds()) {
          result.append(ty).append("/");
        }
      }
      if (t.asWildcardType().extendsBounds() != null) {
        for (Type ty : t.asWildcardType().extendsBounds()) {
          result.append(ty).append("/");
        }
      }
    } else {
      result.append("NoWildCardType//");
    }

    return result.toString();
  }

  private static TypeInfo typeToTypeInfo(Type t, ClassInfo classInfo) {
    String simpleTypeName = t instanceof ClassDoc
        ? ((ClassDoc) t).name()
        : t.simpleTypeName();
    return new TypeInfo(t.isPrimitive(), t.dimension(), simpleTypeName,
        t.qualifiedTypeName(), classInfo);
  }

  private AnnotationInstanceInfo[] convertAnnotationInstances(AnnotationDesc[] orig) {
    AnnotationInstanceInfo[] out = new AnnotationInstanceInfo[orig.length];
    for (int i = 0; i < orig.length; i++) {
      out[i] = obtainAnnotationInstance(orig[i]);
    }
    return out;
  }

  private AnnotationInstanceInfo obtainAnnotationInstance(AnnotationDesc a) {
    AnnotationInstanceInfo result = annotationInstances.get(a);
    if (result != null) {
      return result;
    }

    ClassInfo annotationType = obtainClass(a.annotationType());
    AnnotationDesc.ElementValuePair[] ev = a.elementValues();
    AnnotationValueInfo[] elementValues = new AnnotationValueInfo[ev.length];
    for (int i = 0; i < ev.length; i++) {
      elementValues[i] = obtainAnnotationValue(ev[i].value(), obtainMethod(ev[i].element()));
    }
    result = new AnnotationInstanceInfo(annotationType, elementValues);
    annotationInstances.put(a, result);
    return result;
  }

  private AnnotationValueInfo obtainAnnotationValue(AnnotationValue o, MethodInfo element) {
    if (o == null) {
      return null;
    }
    AnnotationValueInfo v = mAnnotationValues.get(o);
    if (v != null) return v;
    v = new AnnotationValueInfo(element);
    mAnnotationValues.put(o, v);
    if (mAnnotationValuesNeedingInit != null) {
      mAnnotationValuesNeedingInit.add(o);
    } else {
      initAnnotationValue(o, v);
    }
    return v;
  }

  private void initAnnotationValue(AnnotationValue o, AnnotationValueInfo v) {
    Object orig = o.value();
    Object converted;
    if (orig instanceof Type) {
      // class literal
      converted = obtainType((Type) orig);
    } else if (orig instanceof FieldDoc) {
      // enum constant
      converted = obtainField((FieldDoc) orig);
    } else if (orig instanceof AnnotationDesc) {
      // annotation instance
      converted = obtainAnnotationInstance((AnnotationDesc) orig);
    } else if (orig instanceof AnnotationValue[]) {
      AnnotationValue[] old = (AnnotationValue[]) orig;
      AnnotationValueInfo[] array = new AnnotationValueInfo[old.length];
      for (int i = 0; i < array.length; i++) {
        array[i] = obtainAnnotationValue(old[i], null);
      }
      converted = array;
    } else {
      converted = orig;
    }
    v.init(converted);
  }

  private void finishAnnotationValueInit() {
    while (mAnnotationValuesNeedingInit.size() > 0) {
      HashSet<AnnotationValue> set = mAnnotationValuesNeedingInit;
      mAnnotationValuesNeedingInit = new HashSet<AnnotationValue>();
      for (AnnotationValue o : set) {
        AnnotationValueInfo v = mAnnotationValues.get(o);
        initAnnotationValue(o, v);
      }
    }
    mAnnotationValuesNeedingInit = null;
  }
}
