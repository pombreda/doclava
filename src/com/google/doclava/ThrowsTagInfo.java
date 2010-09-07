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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ThrowsTagInfo extends ParsedTagInfo {
  private static final Pattern PATTERN = Pattern.compile("(\\S+)\\s+(.*)", Pattern.DOTALL);
  private ClassInfo mException;

  public ThrowsTagInfo(String name, String kind, String text, ContainerInfo base,
      SourcePositionInfo sp) {
    super(name, kind, text, base, sp);
  }

  @Override public void initVisible(Project project) {
    super.initVisible(project);

    String text = text();
    ContainerInfo base = getContainer();
    Matcher m = PATTERN.matcher(text);
    if (m.matches()) {
      setCommentText(m.group(2));
      String className = m.group(1);
      if (base instanceof ClassInfo) {
        mException = ((ClassInfo) base).findClass(className, project);
      }
      if (mException == null) {
        mException = project.getClassByName(className);
      }
    }
  }

  public ThrowsTagInfo(String name, String kind, String text, ClassInfo exception,
      String exceptionComment, ContainerInfo base, SourcePositionInfo sp) {
    super(name, kind, text, base, sp);
    mException = exception;
    setCommentText(exceptionComment);
  }

  public ClassInfo exception() {
    return mException;
  }

  public TypeInfo exceptionType() {
    return mException != null ? mException.asTypeInfo() : null;
  }

  public static void makeHDF(Data data, String base, List<ThrowsTagInfo> tags) {
    int i = 0;
    for (ThrowsTagInfo info : tags) {
      TagInfo.makeHDF(data, base + '.' + i + ".comment", info.commentTags());
      if (info.exceptionType() != null) {
        info.exceptionType().makeHDF(data, base + "." + i + ".type");
      }
      i++;
    }
  }
}
