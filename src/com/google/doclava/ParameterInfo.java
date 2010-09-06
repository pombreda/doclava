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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class ParameterInfo {
  public ParameterInfo(String name, String typeName, TypeInfo type, SourcePositionInfo position) {
    mName = name;
    mTypeName = typeName;
    mType = type;
    mPosition = position;
  }

  TypeInfo type() {
    return mType;
  }

  String name() {
    return mName;
  }

  String typeName() {
    return mTypeName;
  }

  SourcePositionInfo position() {
    return mPosition;
  }
  
  public void makeHDF(Data data, String base, boolean isLastVararg, HashSet<String> typeVariables) {
    data.setValue(base + ".name", this.name());
    type().makeHDF(data, base + ".type", isLastVararg, typeVariables);
  }

  public static void makeHDF(Data data, String base, List<ParameterInfo> params, boolean isVararg,
      HashSet<String> typeVariables) {
    int i = 0;
    for (Iterator<ParameterInfo> p = params.iterator(); p.hasNext();) {
      ParameterInfo parameterInfo = p.next();
      parameterInfo.makeHDF(data, base + "." + i, isVararg && !p.hasNext(), typeVariables);
      i++;
    }
  }
  
  /**
   * Returns true if this parameter's dimension information agrees
   * with the represented callee's dimension information.
   */
  public boolean matchesDimension(String dimension, boolean varargs) {
    // foo(int... a) can be called as foo(int[] a).
    if (varargs) {
      dimension += "[]";
    }
    return mType.dimension().equals(dimension);
  }

  private String mName;
  private String mTypeName;
  private TypeInfo mType;
  private SourcePositionInfo mPosition;
}
