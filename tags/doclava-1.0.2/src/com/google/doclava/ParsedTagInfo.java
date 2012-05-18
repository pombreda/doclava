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

import java.util.ArrayList;
import java.util.List;

public class ParsedTagInfo extends TagInfo {
  private final ContainerInfo mContainer;
  private String mCommentText;
  private Comment mComment;

  ParsedTagInfo(String name, String kind, String text, ContainerInfo base, SourcePositionInfo sp) {
    super(name, kind, text, SourcePositionInfo.findBeginning(sp, text));
    mContainer = base;
    mCommentText = text;
  }

  public ContainerInfo getContainer() {
    return mContainer;
  }

  @Override public void initVisible(Project project) {
    super.initVisible(project);
    mComment = new Comment(mCommentText, mContainer, position());
    mComment.initVisible(project);
  }

  public List<TagInfo> commentTags() {
    if (mComment == null) {
      throw new IllegalStateException("Expected initVisible() to be called first");
    }
    return mComment.tags();
  }

  protected void setCommentText(String comment) {
    mCommentText = comment;
  }

  public static <T extends ParsedTagInfo> List<TagInfo> joinTags(T[] tags) {
    ArrayList<TagInfo> result = new ArrayList<TagInfo>();
    for (T tag : tags) {
      for (TagInfo tagInfo : tag.commentTags()) {
        result.add(tagInfo);
      }
    }
    return result;
  }
}