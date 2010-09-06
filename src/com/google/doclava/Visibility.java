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
import java.util.List;

public final class Visibility {
  public static <T extends Scoped> List<T> filterHidden(Iterable<T> iterable) {
    ImmutableList.Builder<T> result = ImmutableList.builder();
    for (T t : iterable) {
      if (!t.isHidden()) {
        result.add(t);
      }
    }
    return result.build();
  }

  public static ImmutableList<ClassInfo> displayClasses(Iterable<ClassInfo> classInfos) {
    return ImmutableList.copyOf(ClassInfo.ORDER_BY_NAME.sortedCopy(filterHidden(classInfos)));
  }
}
