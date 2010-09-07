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

import com.sun.javadoc.ClassDoc;
import java.util.List;

/**
 * A project is a collection of classes to be documented. This class contains
 * the full set of target classes all wired together. A project's Javadoc
 * contents are not necessarily parsed, it's necessary to initialize values
 * for display before dependent APIs can be used.
 */
public interface Project {

  ClassInfo getClassByName(String name);

  ClassInfo getClassReference(ClassDoc classDoc);

  PackageInfo getPackage(String name);

  List<ClassInfo> rootClasses();

  List<ClassInfo> allClasses();

  List<ClassInfo> getClasses(ClassDoc[] classes);

  List<FieldInfo> getAllFields();

  List<MethodInfo> getAllMethods();

  List<TagInfo> getRootTags();
}
