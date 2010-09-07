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
import java.util.List;

public class TagInfo {
  private final String mName;
  private final String mText;
  private final SourcePositionInfo mPosition;
  private String mKind;

  TagInfo(String n, String k, String t, SourcePositionInfo sp) {
    mName = n;
    mText = t;
    mPosition = sp;
    mKind = k;
  }

  public void initVisible(Project project) {}

  String name() {
    return mName;
  }

  String text() {
    return mText;
  }

  String kind() {
    return mKind;
  }

  SourcePositionInfo position() {
    return mPosition;
  }

  void setKind(String kind) {
    mKind = kind;
  }

  public void makeHDF(Data data, String base) {
    data.setValue(base + ".name", name());
    data.setValue(base + ".text", text());
    data.setValue(base + ".kind", kind());
  }

  public static void makeHDF(Data data, String base, List<? extends TagInfo> tags) {
    makeHDF(data, base, tags, null, 0, 0);
  }

  public static void makeHDF(Data data, String base, InheritedTags tags) {
    makeHDF(data, base, tags.tags(), tags.inherited(), 0, 0);
  }

  private static int makeHDF(Data data, String base, List<? extends TagInfo> tags,
      InheritedTags inherited, int j, int depth) {
    int i;
    if (tags.isEmpty() && inherited != null) {
      j = makeHDF(data, base, inherited.tags(), inherited.inherited(), j, depth + 1);
    } else {
      for (i = 0; i < tags.size(); i++, j++) {
        TagInfo t = tags.get(i);
        if (inherited != null && t.name().equals("@inheritDoc")) {
          j = makeHDF(data, base, inherited.tags(), inherited.inherited(), j, depth + 1);
        } else {
          if (t.name().equals("@inheritDoc")) {
            Errors.error(Errors.BAD_INHERITDOC, t.mPosition,
                "@inheritDoc on class/method that is not inherited");
          }
          t.makeHDF(data, base + "." + j);
        }
      }
    }
    return j;
  }
  
  /**
   * Returns true if the given list of tags match. Tags must be ordered
   * equivalently for the lists to be equal.
   */
  static boolean tagsEqual(List<TagInfo> first, List<TagInfo> second) {
    if (first.size() != second.size()) {
      return false;
    }
    for (int i = 0; i < first.size(); i++) {
      if (!first.get(i).mName.equals(second.get(i).mName)) {
        return false;
      }
      if (!first.get(i).mKind.equals(second.get(i).mKind)) {
        return false;
      }
      if (!first.get(i).mText.equals(second.get(i).mText)) {
        return false;
      }
    }
    return true;
  }

  @Override public String toString() {
    return mName + ":" + mText;
  }
}
