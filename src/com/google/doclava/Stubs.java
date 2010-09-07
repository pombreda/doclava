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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Stubs {
  private Set<ClassInfo> notStrippable;

  public void initVisible(HashSet<String> stubPackages, Project project) {
    // figure out which classes we need
    notStrippable = new LinkedHashSet<ClassInfo>();
    // If a class is public or protected, not hidden, and marked as included,
    // then we can't strip it
    for (ClassInfo cl : project.allClasses()) {
      if (cl.checkLevel() && cl.isDefinedLocally()) {
        cantStripThis(cl, notStrippable);
      }
    }

    // complain about anything that looks includable but is not supposed to
    // be written, e.g. hidden things
    for (ClassInfo cl : notStrippable) {
      if (!cl.isHidden()) {
        for (MethodInfo m : cl.allSelfMethods()) {
          if (m.isHidden()) {
            continue;
          }
          if (m.isDeprecated()) {
            // don't bother reporting deprecated methods
            // unless they are public
            Errors.error(Errors.DEPRECATED, m.position(), "Method " + cl.qualifiedName() + "."
                + m.name() + " is deprecated");
          }

          ClassInfo returnClass = m.returnType().asClassInfo();
          if (returnClass != null && returnClass.isHidden()) {
            Errors.error(Errors.UNAVAILABLE_SYMBOL, m.position(), "Method " + cl.qualifiedName()
                + "." + m.name() + " returns unavailable type " + returnClass.name());
          }

          List<ParameterInfo> params = m.parameters();
          for (ParameterInfo p : params) {
            TypeInfo t = p.type();
            if (!t.isPrimitive()) {
              if (t.asClassInfo().isHidden()) {
                Errors.error(Errors.UNAVAILABLE_SYMBOL, m.position(), "Parameter of hidden type "
                    + t.fullName() + " in " + cl.qualifiedName() + "." + m.name() + "()");
              }
            }
          }
        }

        // annotations are handled like methods
        for (MethodInfo m : cl.annotationElements()) {
          if (m.isHidden()) {
            Errors.error(Errors.UNAVAILABLE_SYMBOL, m.position(), "Reference to hidden annotation "
                + m.name());
          }

          ClassInfo returnClass = m.returnType().asClassInfo();
          if (returnClass != null && returnClass.isHidden()) {
            Errors.error(Errors.UNAVAILABLE_SYMBOL, m.position(), "Annotation '" + m.name()
                + "' returns unavailable type " + returnClass.name());
          }

          for (ParameterInfo p : m.parameters()) {
            TypeInfo t = p.type();
            if (!t.isPrimitive()) {
              if (t.asClassInfo().isHidden()) {
                Errors.error(Errors.UNAVAILABLE_SYMBOL, p.position(),
                    "Reference to unavailable annotation class " + t.fullName());
              }
            }
          }
        }
      } else if (cl.isDeprecated()) {
        // not hidden, but deprecated
        Errors.error(Errors.DEPRECATED, cl.position(), "Class " + cl.qualifiedName()
            + " is deprecated");
      }
    }

    if (stubPackages != null) {
      for (ClassInfo cl : notStrippable) {
        if (!stubPackages.contains(cl.containingPackage().name())) {
          notStrippable.remove(cl);
        }
      }
    }
  }

  public Set<ClassInfo> getNotStrippable() {
    return notStrippable;
  }

  public void writeStubsAndXml(String stubsDir, File xmlFile) {

    if (stubsDir == null && xmlFile == null) {
      // nothing to do.
      return;
    }

    PrintStream xmlWriter = null;

    if (xmlFile != null) {
      ClearPage.ensureDirectory(xmlFile);
      try {
        xmlWriter = new PrintStream(xmlFile);
      } catch (FileNotFoundException e) {
        Errors.error(Errors.IO_ERROR, new SourcePositionInfo(xmlFile.getAbsolutePath(), 0, 0),
            "Cannot open file for write.");
      }
    }

    writeStubsAndXml(stubsDir, xmlWriter, notStrippable);
  }

  private void writeStubsAndXml(String stubsDir, PrintStream xmlWriter, Set<ClassInfo> classes) {
    Map<PackageInfo, List<ClassInfo>> packages = new HashMap<PackageInfo, List<ClassInfo>>();
    for (ClassInfo cl : classes) {
      if (!cl.isDocOnly()) {
          // write out the stubs
          if (stubsDir != null) {
            writeClassFile(stubsDir, cl);
          }
          // build class list for xml file
          if (xmlWriter != null && cl.isDefinedLocally()) {
            if (packages.containsKey(cl.containingPackage())) {
              packages.get(cl.containingPackage()).add(cl);
            } else {
              ArrayList<ClassInfo> adding = new ArrayList<ClassInfo>();
              adding.add(cl);
              packages.put(cl.containingPackage(), adding);
            }
          }
      }
    }

    // write out the XML
    if (xmlWriter != null) {
      writeXML(xmlWriter, packages, classes);
      xmlWriter.close();
    }
  }

  private void cantStripThis(ClassInfo cl, Set<ClassInfo> notStrippable) {
    if (!notStrippable.add(cl)) {
      // slight optimization: if it already contains cl, it already contains
      // all of cl's parents
      return;
    }

    // cant strip annotations
    /*
     * if (cl.annotations() != null){ for (AnnotationInstanceInfo ai : cl.annotations()){ if
     * (ai.type() != null){ cantStripThis(ai.type(), notStrippable, "1:" + cl.qualifiedName()); } }
     * }
     */
    // cant strip any public fields or their generics
    if (cl.allSelfFields() != null) {
      for (FieldInfo fInfo : cl.allSelfFields()) {
        if (fInfo.type() != null) {
          if (fInfo.type().asClassInfo() != null) {
            cantStripThis(fInfo.type().asClassInfo(), notStrippable);
          }
          if (fInfo.type().typeArguments() != null) {
            for (TypeInfo tTypeInfo : fInfo.type().typeArguments()) {
              if (tTypeInfo.asClassInfo() != null) {
                cantStripThis(tTypeInfo.asClassInfo(), notStrippable);
              }
            }
          }
        }
      }
    }
    // cant strip any of the type's generics
    if (cl.asTypeInfo() != null) {
      if (cl.asTypeInfo().typeArguments() != null) {
        for (TypeInfo tInfo : cl.asTypeInfo().typeArguments()) {
          if (tInfo.asClassInfo() != null) {
            cantStripThis(tInfo.asClassInfo(), notStrippable);
          }
        }
      }
    }
    // cant strip any of the annotation elements
    // cantStripThis(cl.annotationElements(), notStrippable);
    // take care of methods
    cantStripThis(cl.allSelfMethods(), notStrippable);
    cantStripThis(cl.allConstructors(), notStrippable);
    // blow the outer class open if this is an inner class
    if (cl.containingClass() != null) {
      cantStripThis(cl.containingClass(), notStrippable);
    }
    // blow open super class and interfaces
    ClassInfo supr = cl.realSuperclass();
    if (supr != null) {
      if (supr.isHidden()) {
        // cl is a public class declared as extending a hidden superclass.
        // this is not a desired practice but it's happened, so we deal
        // with it by stripping off the superclass relation for purposes of
        // generating the doc & stub information, and proceeding normally.
        cl.init(cl.asTypeInfo(), cl.realInterfaces(), cl.realInterfaceTypes(), cl.innerClasses(),
            cl.allConstructors(), cl.allSelfMethods(), cl.annotationElements(), cl.allSelfFields(),
            cl.enumConstants(), cl.containingPackage(), cl.containingClass(), null, null,
            cl.annotations(), ImmutableList.<ClassInfo>of());
        Errors.error(Errors.HIDDEN_SUPERCLASS, cl.position(), "Public class " + cl.qualifiedName()
            + " stripped of unavailable superclass " + supr.qualifiedName());
      } else {
        cantStripThis(supr, notStrippable);
      }
    }
  }

  private void cantStripThis(List<MethodInfo> mInfos, Set<ClassInfo> notStrippable) {
    // for each method, blow open the parameters, throws and return types. also blow open their
    // generics
    if (mInfos == null) {
      return;
    }

    for (MethodInfo mInfo : mInfos) {
      if (mInfo.isHidden()) {
        continue;
      }

      if (mInfo.getTypeParameters() != null) {
        for (TypeInfo tInfo : mInfo.getTypeParameters()) {
          if (tInfo.asClassInfo() != null) {
            cantStripThis(tInfo.asClassInfo(), notStrippable);
          }
        }
      }
      if (mInfo.parameters() != null) {
        for (ParameterInfo pInfo : mInfo.parameters()) {
          if (pInfo.type() != null && pInfo.type().asClassInfo() != null) {
            cantStripThis(pInfo.type().asClassInfo(), notStrippable);
            if (pInfo.type().typeArguments() != null) {
              for (TypeInfo tInfoType : pInfo.type().typeArguments()) {
                if (tInfoType.asClassInfo() != null) {
                  ClassInfo tcl = tInfoType.asClassInfo();
                  if (tcl.isHidden()) {
                    Errors.error(Errors.UNAVAILABLE_SYMBOL, mInfo.position(),
                        "Parameter of hidden type " + tInfoType.fullName() + " in "
                            + mInfo.containingClass().qualifiedName() + '.' + mInfo.name()
                            + "()");
                  } else {
                    cantStripThis(tcl, notStrippable);
                  }
                }
              }
            }
          }
        }
      }
      for (ClassInfo thrown : mInfo.thrownExceptions()) {
          cantStripThis(thrown, notStrippable);
      }
      if (mInfo.returnType() != null && mInfo.returnType().asClassInfo() != null) {
        cantStripThis(mInfo.returnType().asClassInfo(), notStrippable);
        if (mInfo.returnType().typeArguments() != null) {
          for (TypeInfo tyInfo : mInfo.returnType().typeArguments()) {
            if (tyInfo.asClassInfo() != null) {
              cantStripThis(tyInfo.asClassInfo(), notStrippable);
            }
          }
        }
      }
    }
  }

  private static String javaFileName(ClassInfo cl) {
    String dir = "";
    PackageInfo pkg = cl.containingPackage();
    if (pkg != null) {
      dir = pkg.name();
      dir = dir.replace('.', '/') + '/';
    }
    return dir + cl.name() + ".java";
  }

  private void writeClassFile(String stubsDir, ClassInfo cl) {
    // inner classes are written by their containing class
    if (cl.containingClass() != null) {
      return;
    }

    // Work around the bogus "Array" class we invent for
    // Arrays.copyOf's Class<? extends T[]> newType parameter. (http://b/2715505)
    if (cl.containingPackage() != null
        && cl.containingPackage().name().equals(PackageInfo.DEFAULT_PACKAGE)) {
      return;
    }

    String filename = stubsDir + '/' + javaFileName(cl);
    File file = new File(filename);
    ClearPage.ensureDirectory(file);

    PrintStream stream = null;
    try {
      stream = new PrintStream(file);
      writeClassFile(stream, cl);
    } catch (FileNotFoundException e) {
      System.err.println("error writing file: " + filename);
    } finally {
      if (stream != null) {
        stream.close();
      }
    }
  }

  private void writeClassFile(PrintStream stream, ClassInfo cl) {
    PackageInfo pkg = cl.containingPackage();
    if (pkg != null) {
      stream.println("package " + pkg.name() + ";");
    }
    writeClass(stream, cl);
  }

  private void writeClass(PrintStream stream, ClassInfo cl) {
    writeAnnotations(stream, cl.annotations());

    stream.print(cl.scope() + " ");
    if (cl.isAbstract() && !cl.isAnnotation() && !cl.isInterface()) {
      stream.print("abstract ");
    }
    if (cl.isStatic()) {
      stream.print("static ");
    }
    if (cl.isFinal() && !cl.isEnum()) {
      stream.print("final ");
    }
    if (false) {
      stream.print("strictfp ");
    }

    HashSet<String> classDeclTypeVars = new HashSet<String>();
    String leafName = cl.asTypeInfo().fullName(classDeclTypeVars);
    int bracket = leafName.indexOf('<');
    if (bracket < 0) bracket = leafName.length() - 1;
    int period = leafName.lastIndexOf('.', bracket);
    if (period < 0) period = -1;
    leafName = leafName.substring(period + 1);

    String kind = cl.kind();
    stream.println(kind + " " + leafName);

    TypeInfo base = cl.superclassType();

    if (!"enum".equals(kind)) {
      if (base != null && !"java.lang.Object".equals(base.qualifiedTypeName())) {
        stream.println("  extends " + base.fullName(classDeclTypeVars));
      }
    }

    List<TypeInfo> interfaces = cl.realInterfaceTypes();
    List<TypeInfo> usedInterfaces = new ArrayList<TypeInfo>();
    for (TypeInfo iface : interfaces) {
      if (notStrippable.contains(iface.asClassInfo()) && !iface.asClassInfo().isDocOnly()) {
        usedInterfaces.add(iface);
      }
    }
    if (usedInterfaces.size() > 0 && !cl.isAnnotation()) {
      // can java annotations extend other ones?
      if (cl.isInterface() || cl.isAnnotation()) {
        stream.print("  extends ");
      } else {
        stream.print("  implements ");
      }
      String comma = "";
      for (TypeInfo iface : usedInterfaces) {
        stream.print(comma + iface.fullName(classDeclTypeVars));
        comma = ", ";
      }
      stream.println();
    }

    stream.println("{");

    for (Iterator<FieldInfo> f = cl.enumConstants().iterator(); f.hasNext();) {
      FieldInfo field = f.next();
      if (!field.constantLiteralValue().equals("null")) {
        stream.println(field.name() + "(" + field.constantLiteralValue()
            + (f.hasNext() ? ")," : ");"));
      } else {
        stream.println(field.name() + "(" + (f.hasNext() ? ")," : ");"));
      }
    }

    for (ClassInfo inner : cl.getRealInnerClasses()) {
      if (notStrippable.contains(inner) && !inner.isDocOnly()) {
        writeClass(stream, inner);
      }
    }


    for (MethodInfo method : cl.getConstructors()) {
      if (!method.isDocOnly()) {
        writeMethod(stream, method, true);
      }
    }

    boolean fieldNeedsInitialization = false;
    boolean staticFieldNeedsInitialization = false;
    for (FieldInfo field : cl.allSelfFields()) {
      if (!field.isDocOnly()) {
        if (!field.isStatic() && field.isFinal() && !fieldIsInitialized(field)) {
          fieldNeedsInitialization = true;
        }
        if (field.isStatic() && field.isFinal() && !fieldIsInitialized(field)) {
          staticFieldNeedsInitialization = true;
        }
      }
    }

    // The compiler includes a default public constructor that calls the super classes
    // default constructor in the case where there are no written constructors.
    // So, if we hide all the constructors, java may put in a constructor
    // that calls a nonexistent super class constructor. So, if there are no constructors,
    // and the super class doesn't have a default constructor, write in a private constructor
    // that works. TODO -- we generate this as protected, but we really should generate
    // it as private unless it also exists in the real code.
    if ((cl.getConstructors().isEmpty() && (!cl.getNonWrittenConstructors().isEmpty() || fieldNeedsInitialization))
        && !cl.isAnnotation() && !cl.isInterface() && !cl.isEnum()) {
      // Errors.error(Errors.HIDDEN_CONSTRUCTOR,
      // cl.position(), "No constructors " +
      // "found and superclass has no parameterless constructor.  A constructor " +
      // "that calls an appropriate superclass constructor " +
      // "was automatically written to stubs.\n");
      stream.println(cl.leafName() + "() { " + superCtorCall(cl, null) + "throw new"
          + " RuntimeException(\"Stub!\"); }");
    }

    for (MethodInfo method : cl.getMethods()) {
      if (cl.isEnum()) {
        if (("values".equals(method.name()) && "()".equals(method.signature()))
            || ("valueOf".equals(method.name()) && "(java.lang.String)".equals(method.signature()))) {
          // skip these two methods on enums, because they're synthetic,
          // although for some reason javadoc doesn't mark them as synthetic,
          // maybe because they still want them documented
          continue;
        }
      }
      if (!method.isDocOnly()) {
        writeMethod(stream, method, false);
      }
    }
    // Write all methods that are hidden, but override abstract methods or interface methods.
    // These can't be hidden.
    for (MethodInfo method : cl.getHiddenMethods()) {
      MethodInfo overriddenMethod =
          method.findRealOverriddenMethod(method, notStrippable);
      if (overriddenMethod != null && !overriddenMethod.isHidden() && !overriddenMethod.isDocOnly()
          && (overriddenMethod.isAbstract() || overriddenMethod.containingClass().isInterface())) {
        cl.addMethod(method);
        writeMethod(stream, method, false);
      }
    }

    for (MethodInfo element : cl.annotationElements()) {
      if (!element.isDocOnly()) {
        writeAnnotationElement(stream, element);
      }
    }

    for (FieldInfo field : cl.allSelfFields()) {
      if (!field.isDocOnly()) {
        writeField(stream, field);
      }
    }

    if (staticFieldNeedsInitialization) {
      stream.print("static { ");
      for (FieldInfo field : cl.allSelfFields()) {
        if (!field.isDocOnly() && field.isStatic() && field.isFinal() && !fieldIsInitialized(field)
            && field.constantValue() == null) {
          stream.print(field.name() + " = " + field.type().defaultValue() + "; ");
        }
      }
      stream.println("}");
    }

    stream.println("}");
  }


  private static void writeMethod(PrintStream stream, MethodInfo method, boolean isConstructor) {
    String comma;

    stream.print(method.scope() + " ");
    if (method.isStatic()) {
      stream.print("static ");
    }
    if (method.isFinal()) {
      stream.print("final ");
    }
    if (method.isAbstract()) {
      stream.print("abstract ");
    }
    if (method.isSynchronized()) {
      stream.print("synchronized ");
    }
    if (method.isNative()) {
      stream.print("native ");
    }
    if (false /* method.isStictFP() */) {
      stream.print("strictfp ");
    }

    stream.print(method.typeArgumentsName(new HashSet<String>()) + " ");

    if (!isConstructor) {
      stream.print(method.returnType().fullName(method.typeVariables()) + " ");
    }
    String n = method.name();
    int pos = n.lastIndexOf('.');
    if (pos >= 0) {
      n = n.substring(pos + 1);
    }
    stream.print(n + "(");
    comma = "";
    int count = 1;
    int size = method.parameters().size();
    for (ParameterInfo param : method.parameters()) {
      stream.print(comma + fullParameterTypeName(method, param.type(), count == size) + " "
          + param.name());
      comma = ", ";
      count++;
    }
    stream.print(")");

    comma = "";
    if (!method.thrownExceptions().isEmpty()) {
      stream.print(" throws ");
      for (ClassInfo thrown : method.thrownExceptions()) {
        stream.print(comma + thrown.qualifiedName());
        comma = ", ";
      }
    }
    if (method.isAbstract() || method.isNative() || method.containingClass().isInterface()) {
      stream.println(";");
    } else {
      stream.print(" { ");
      if (isConstructor) {
        stream.print(superCtorCall(method.containingClass(), method.thrownExceptions()));
      }
      stream.println("throw new RuntimeException(\"Stub!\"); }");
    }
  }

  private static void writeField(PrintStream stream, FieldInfo field) {
    stream.print(field.scope() + " ");
    if (field.isStatic()) {
      stream.print("static ");
    }
    if (field.isFinal()) {
      stream.print("final ");
    }
    if (field.isTransient()) {
      stream.print("transient ");
    }
    if (field.isVolatile()) {
      stream.print("volatile ");
    }

    stream.print(field.type().fullName());
    stream.print(" ");
    stream.print(field.name());

    if (fieldIsInitialized(field)) {
      stream.print(" = " + field.constantLiteralValue());
    }

    stream.println(";");
  }

  private static boolean fieldIsInitialized(FieldInfo field) {
    return (field.isFinal() && field.constantValue() != null)
        || !field.type().dimension().equals("") || field.containingClass().isInterface();
  }

  // Returns 'true' if the method is an @Override of a visible parent
  // method implementation, and thus does not affect the API.
  private boolean methodIsOverride(MethodInfo mi) {
    // Abstract/static/final methods are always listed in the API description
    if (mi.isAbstract() || mi.isStatic() || mi.isFinal()) {
      return false;
    }

    // Find any relevant ancestor declaration and inspect it
    MethodInfo om = mi.findSuperclassImplementation(notStrippable);
    if (om != null) {
      // Visibility mismatch is an API change, so check for it
      if (mi.mIsPrivate == om.mIsPrivate && mi.mIsPublic == om.mIsPublic
          && mi.mIsProtected == om.mIsProtected) {
        // Look only for overrides of an ancestor class implementation,
        // not of e.g. an abstract or interface method declaration
        if (!om.isAbstract()) {
          // If the parent is hidden, we can't rely on it to provide
          // the API
          if (!om.isHidden()) {
            // If the only "override" turns out to be in our own class
            // (which sometimes happens in concrete subclasses of
            // abstract base classes), it's not really an override
            if (!mi.mContainingClass.equals(om.mContainingClass)) {
              return true;
            }
          }
        }
      }
    }
    return false;
  }

  private static boolean canCallMethod(ClassInfo from, MethodInfo m) {
    if (m.isPublic() || m.isProtected()) {
      return true;
    }
    if (m.isPackagePrivate()) {
      String fromPkg = from.containingPackage().name();
      String pkg = m.containingClass().containingPackage().name();
      if (fromPkg.equals(pkg)) {
        return true;
      }
    }
    return false;
  }

  // call a constructor, any constructor on this class's superclass.
  private static String superCtorCall(ClassInfo cl, List<ClassInfo> thrownExceptions) {
    ClassInfo base = cl.realSuperclass();
    if (base == null) {
      return "";
    }
    HashSet<String> exceptionNames = new HashSet<String>();
    if (thrownExceptions != null) {
      for (ClassInfo thrown : thrownExceptions) {
        exceptionNames.add(thrown.name());
      }
    }
    MethodInfo ctor = null;
    // bad exception indicates that the exceptions thrown by the super constructor
    // are incompatible with the constructor we're using for the sub class.
    Boolean badException = false;
    for (MethodInfo m : base.getConstructors()) {
      if (canCallMethod(cl, m)) {
        if (m.thrownExceptions() != null) {
          for (ClassInfo thrown : m.thrownExceptions()) {
            if (!exceptionNames.contains(thrown.name())) {
              badException = true;
            }
          }
        }
        if (badException) {
          badException = false;
          continue;
        }
        // if it has no args, we're done
        if (m.parameters().isEmpty()) {
          return "";
        }
        ctor = m;
      }
    }
    if (ctor != null) {
      String result = "";
      result += "super(";
      List<ParameterInfo> params = ctor.parameters();
      int N = params.size();
      for (int i = 0; i < N; i++) {
        TypeInfo t = params.get(i).type();
        if (t.isPrimitive() && t.dimension().equals("")) {
          String n = t.simpleTypeName();
          if (("byte".equals(n) || "short".equals(n) || "int".equals(n) || "long".equals(n)
              || "float".equals(n) || "double".equals(n))
              && t.dimension().equals("")) {
            result += "0";
          } else if ("char".equals(n)) {
            result += "'\\0'";
          } else if ("boolean".equals(n)) {
            result += "false";
          } else {
            result += "<<unknown-" + n + ">>";
          }
        } else {
          // put null in each super class method. Cast null to the correct type
          // to avoid collisions with other constructors. If the type is generic
          // don't cast it
          result +=
              (!t.isTypeVariable() ? "(" + t.qualifiedTypeName() + t.dimension() + ")" : "")
                  + "null";
        }
        if (i != N - 1) {
          result += ",";
        }
      }
      result += "); ";
      return result;
    } else {
      return "";
    }
  }

  private static void writeAnnotations(PrintStream stream, AnnotationInstanceInfo[] annotations) {
    for (AnnotationInstanceInfo ann : annotations) {
      if (!ann.type().isHidden()) {
        stream.println(ann.toString());
      }
    }
  }

  private static void writeAnnotationElement(PrintStream stream, MethodInfo ann) {
    stream.print(ann.returnType().fullName());
    stream.print(" ");
    stream.print(ann.name());
    stream.print("()");
    AnnotationValueInfo def = ann.defaultAnnotationElementValue();
    if (def != null) {
      stream.print(" default ");
      stream.print(def.valueString());
    }
    stream.println(";");
  }

  private void writeXML(PrintStream xmlWriter, Map<PackageInfo, List<ClassInfo>> allClasses,
      Set<ClassInfo> notStrippable) {
    // extract the set of packages, sort them by name, and write them out in that order
    Set<PackageInfo> allClassKeys = allClasses.keySet();
    PackageInfo[] allPackages = allClassKeys.toArray(new PackageInfo[allClassKeys.size()]);
    Arrays.sort(allPackages, PackageInfo.ORDER_BY_NAME);

    xmlWriter.println("<api>");
    for (PackageInfo pack : allPackages) {
      writePackageXML(xmlWriter, pack, allClasses.get(pack), notStrippable);
    }
    xmlWriter.println("</api>");
  }

  private void writePackageXML(PrintStream xmlWriter, PackageInfo pack, List<ClassInfo> classList,
      Set<ClassInfo> notStrippable) {
    ClassInfo[] classes = classList.toArray(new ClassInfo[classList.size()]);
    Arrays.sort(classes, ClassInfo.ORDER_BY_NAME);
    // Work around the bogus "Array" class we invent for
    // Arrays.copyOf's Class<? extends T[]> newType parameter. (http://b/2715505)
    if (pack.name().equals(PackageInfo.DEFAULT_PACKAGE)) {
      return;
    }
    xmlWriter.println("<package name=\"" + pack.name() + "\"\n"
    // + " source=\"" + pack.position() + "\"\n"
        + ">");
    for (ClassInfo cl : classes) {
      writeClassXML(xmlWriter, cl, notStrippable);
    }
    xmlWriter.println("</package>");


  }

  private void writeClassXML(PrintStream xmlWriter, ClassInfo cl, Set<ClassInfo> notStrippable) {
    String scope = cl.scope();
    String declString = (cl.isInterface()) ? "interface" : "class";
    String deprecatedString = cl.isDeprecated() ? "deprecated" : "not deprecated";
    xmlWriter.println("<" + declString + " name=\"" + cl.name() + "\"");
    if (!cl.isInterface() && !cl.qualifiedName().equals("java.lang.Object")) {
      xmlWriter.println(" extends=\""
          + ((cl.realSuperclass() == null) ? "java.lang.Object" : cl.realSuperclass()
              .qualifiedName()) + "\"");
    }
    xmlWriter.println(" abstract=\"" + cl.isAbstract() + "\"\n" + " static=\"" + cl.isStatic()
        + "\"\n" + " final=\"" + cl.isFinal() + "\"\n" + " deprecated=\"" + deprecatedString
        + "\"\n" + " visibility=\"" + scope + "\"\n"
        // + " source=\"" + cl.position() + "\"\n"
        + ">");

    List<ClassInfo> interfaces = cl.realInterfaces();
    Collections.sort(interfaces, ClassInfo.ORDER_BY_NAME);
    for (ClassInfo iface : interfaces) {
      if (notStrippable.contains(iface)) {
        xmlWriter.println("<implements name=\"" + iface.qualifiedName() + "\">");
        xmlWriter.println("</implements>");
      }
    }

    for (MethodInfo mi : cl.getConstructors()) {
      writeConstructorXML(xmlWriter, mi);
    }

    for (MethodInfo mi : cl.getMethods()) {
      if (!methodIsOverride(mi)) {
        writeMethodXML(xmlWriter, mi);
      }
    }

    for (FieldInfo fi : cl.getFields()) {
      writeFieldXML(xmlWriter, fi);
    }
    xmlWriter.println("</" + declString + ">");

  }

  private static void writeMethodXML(PrintStream xmlWriter, MethodInfo mi) {
    String scope = mi.scope();

    String deprecatedString = mi.isDeprecated() ? "deprecated" : "not deprecated";
    xmlWriter.println("<method name=\""
        + mi.name()
        + "\"\n"
        + ((mi.returnType() != null) ? " return=\""
            + makeXMLcompliant(fullParameterTypeName(mi, mi.returnType(), false)) + "\"\n" : "")
        + " abstract=\"" + mi.isAbstract() + "\"\n" + " native=\"" + mi.isNative() + "\"\n"
        + " synchronized=\"" + mi.isSynchronized() + "\"\n" + " static=\"" + mi.isStatic() + "\"\n"
        + " final=\"" + mi.isFinal() + "\"\n" + " deprecated=\"" + deprecatedString + "\"\n"
        + " visibility=\"" + scope + "\"\n"
        // + " source=\"" + mi.position() + "\"\n"
        + ">");

    // write parameters in declaration order
    int numParameters = mi.parameters().size();
    int count = 0;
    for (ParameterInfo pi : mi.parameters()) {
      count++;
      writeParameterXML(xmlWriter, mi, pi, count == numParameters);
    }

    // but write exceptions in canonicalized order
    List<ClassInfo> exceptions = mi.thrownExceptions();
    Collections.sort(exceptions, ClassInfo.ORDER_BY_NAME);
    for (ClassInfo pi : exceptions) {
      xmlWriter.println("<exception name=\"" + pi.name() + "\" type=\"" + pi.qualifiedName()
          + "\">");
      xmlWriter.println("</exception>");
    }
    xmlWriter.println("</method>");
  }

  private static void writeConstructorXML(PrintStream xmlWriter, MethodInfo mi) {
    String scope = mi.scope();
    String deprecatedString = mi.isDeprecated() ? "deprecated" : "not deprecated";
    xmlWriter.println("<constructor name=\"" + mi.name() + "\"\n" + " type=\""
        + mi.containingClass().qualifiedName() + "\"\n" + " static=\"" + mi.isStatic() + "\"\n"
        + " final=\"" + mi.isFinal() + "\"\n" + " deprecated=\"" + deprecatedString + "\"\n"
        + " visibility=\"" + scope + "\"\n"
        // + " source=\"" + mi.position() + "\"\n"
        + ">");

    int numParameters = mi.parameters().size();
    int count = 0;
    for (ParameterInfo pi : mi.parameters()) {
      count++;
      writeParameterXML(xmlWriter, mi, pi, count == numParameters);
    }

    List<ClassInfo> exceptions = mi.thrownExceptions();
    Collections.sort(exceptions, ClassInfo.ORDER_BY_NAME);
    for (ClassInfo pi : exceptions) {
      xmlWriter.println("<exception name=\"" + pi.name() + "\" type=\"" + pi.qualifiedName()
          + "\">");
      xmlWriter.println("</exception>");
    }
    xmlWriter.println("</constructor>");
  }

  private static void writeParameterXML(PrintStream xmlWriter, MethodInfo method, ParameterInfo pi,
      boolean isLast) {
    xmlWriter.println("<parameter name=\"" + pi.name() + "\" type=\""
        + makeXMLcompliant(fullParameterTypeName(method, pi.type(), isLast)) + "\">");
    xmlWriter.println("</parameter>");
  }

  private static void writeFieldXML(PrintStream xmlWriter, FieldInfo fi) {
    String scope = fi.scope();
    String deprecatedString = fi.isDeprecated() ? "deprecated" : "not deprecated";
    // need to make sure value is valid XML
    String value = makeXMLcompliant(fi.constantLiteralValue());

    String fullTypeName = makeXMLcompliant(fi.type().qualifiedTypeName()) + fi.type().dimension();

    xmlWriter.println("<field name=\"" + fi.name() + "\"\n" + " type=\"" + fullTypeName + "\"\n"
        + " transient=\"" + fi.isTransient() + "\"\n" + " volatile=\"" + fi.isVolatile() + "\"\n"
        + (fieldIsInitialized(fi) ? " value=\"" + value + "\"\n" : "") + " static=\""
        + fi.isStatic() + "\"\n" + " final=\"" + fi.isFinal() + "\"\n" + " deprecated=\""
        + deprecatedString + "\"\n" + " visibility=\"" + scope + "\"\n"
        // + " source=\"" + fi.position() + "\"\n"
        + ">");
    xmlWriter.println("</field>");
  }

  private static String makeXMLcompliant(String s) {
    String returnString = s.replaceAll("&", "&amp;");
    returnString = returnString.replaceAll("<", "&lt;");
    returnString = returnString.replaceAll(">", "&gt;");
    returnString = returnString.replaceAll("\"", "&quot;");
    returnString = returnString.replaceAll("'", "&pos;");
    return returnString;
  }

  private static String fullParameterTypeName(MethodInfo method, TypeInfo type, boolean isLast) {
    String fullTypeName = type.fullName(method.typeVariables());
    if (isLast && method.isVarArgs()) {
      // TODO: note that this does not attempt to handle hypothetical
      // vararg methods whose last parameter is a list of arrays, e.g.
      // "Object[]...".
      fullTypeName = type.fullNameNoDimension(method.typeVariables()) + "...";
    }
    return fullTypeName;
  }
}
