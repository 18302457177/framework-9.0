/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.app.slice;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.StringDef;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.internal.util.ArrayUtils;
import com.android.internal.util.Preconditions;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * A slice is a piece of app content and actions that can be surfaced outside of the app.
 *
 * <p>They are constructed using {@link Builder} in a tree structure
 * that provides the OS some information about how the content should be displayed.
 * 树形结构：使用 Builder 以树形结构构建
 * 显示信息：为操作系统提供关于内容显示方式的信息
 */
public final class Slice implements Parcelable {

    /**
     * @hide
     */
    @StringDef(prefix = { "HINT_" }, value = {
            HINT_TITLE,//表示此内容是切片中其他内容的标题
            HINT_LIST,//表示此内容下的所有子项都应该被视为列表项
            HINT_LIST_ITEM,//表示此项是列表的一部分
            HINT_LARGE,//表示此内容重要且应尽可能放大显示
            HINT_ACTIONS,//表示切片包含可分组到UI控制区域的多个操作
            HINT_SELECTED,//表示此项（及其子项）是当前选择项
            HINT_NO_TINT,//表示此内容不应被着色
            HINT_SHORTCUT,//表示此内容仅在切片作为快捷方式显示时才显示
            HINT_TOGGLE,//表示此内容有关联的开关操作
            HINT_HORIZONTAL,//表示列表项水平排列效果更好
            HINT_PARTIAL,//表示此切片不完整，加载完成后会发送更新
            HINT_SEE_MORE,//表示此项目用于指示切片有关联的更多内容
            HINT_KEYWORDS,//表示此子切片内容表示与父切片相关的关键词列表
            HINT_ERROR,//表示此切片表示错误
            HINT_TTL,//表示此项目代表内容的生存时间
            HINT_LAST_UPDATED,//表示此项目代表内容创建或最后更新时间
            HINT_PERMISSION_REQUEST,//表示此切片表示显示切片的权限请求
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SliceHint {}
    /**
     * @hide
     */
    @StringDef(prefix = { "SUBTYPE_" }, value = {
            SUBTYPE_COLOR,//表示颜色信息
            SUBTYPE_CONTENT_DESCRIPTION,//表示内容描述
            SUBTYPE_MAX,//表示最大值
            SUBTYPE_MESSAGE,//表示通信消息
            SUBTYPE_PRIORITY,//表示优先级
            SUBTYPE_RANGE,//表示范围信息
            SUBTYPE_SOURCE,//表示消息来源
            SUBTYPE_TOGGLE,//表示开关状态
            SUBTYPE_VALUE,//表示当前值
            SUBTYPE_LAYOUT_DIRECTION,//表示布局方向
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SliceSubtype {}

    /**
     * Hint that this content is a title of other content in the slice. This can also indicate that
     * the content should be used in the shortcut representation of the slice (icon, label, action),
     * normally this should be indicated by adding the hint on the action containing that content.
     *
     * @see SliceItem#FORMAT_ACTION
     */
    public static final String HINT_TITLE       = "title";
    /**
     * Hint that all sub-items/sub-slices within this content should be considered
     * to have {@link #HINT_LIST_ITEM}.
     */
    public static final String HINT_LIST        = "list";
    /**
     * Hint that this item is part of a list and should be formatted as if is part
     * of a list.
     */
    public static final String HINT_LIST_ITEM   = "list_item";
    /**
     * Hint that this content is important and should be larger when displayed if
     * possible.
     */
    public static final String HINT_LARGE       = "large";
    /**
     * Hint that this slice contains a number of actions that can be grouped together
     * in a sort of controls area of the UI.
     */
    public static final String HINT_ACTIONS     = "actions";
    /**
     * Hint indicating that this item (and its sub-items) are the current selection.
     */
    public static final String HINT_SELECTED    = "selected";
    /**
     * Hint to indicate that this content should not be tinted.
     */
    public static final String HINT_NO_TINT     = "no_tint";
    /**
     * Hint to indicate that this content should only be displayed if the slice is presented
     * as a shortcut.
     */
    public static final String HINT_SHORTCUT = "shortcut";
    /**
     * Hint indicating this content should be shown instead of the normal content when the slice
     * is in small format.
     */
    public static final String HINT_SUMMARY = "summary";
    /**
     * Hint to indicate that this content has a toggle action associated with it. To indicate that
     * the toggle is on, use {@link #HINT_SELECTED}. When the toggle state changes, the intent
     * associated with it will be sent along with an extra {@link #EXTRA_TOGGLE_STATE} which can be
     * retrieved to see the new state of the toggle.
     * @hide
     */
    public static final String HINT_TOGGLE = "toggle";
    /**
     * Hint that list items within this slice or subslice would appear better
     * if organized horizontally.
     */
    public static final String HINT_HORIZONTAL = "horizontal";
    /**
     * Hint to indicate that this slice is incomplete and an update will be sent once
     * loading is complete. Slices which contain HINT_PARTIAL will not be cached by the
     * OS and should not be cached by apps.
     */
    public static final String HINT_PARTIAL     = "partial";
    /**
     * A hint representing that this item should be used to indicate that there's more
     * content associated with this slice.
     */
    public static final String HINT_SEE_MORE = "see_more";
    /**
     * @see Builder#setCallerNeeded
     * @hide
     */
    public static final String HINT_CALLER_NEEDED = "caller_needed";
    /**
     * A hint to indicate that the contents of this subslice represent a list of keywords
     * related to the parent slice.
     * Expected to be on an item of format {@link SliceItem#FORMAT_SLICE}.
     */
    public static final String HINT_KEYWORDS = "keywords";
    /**
     * A hint to indicate that this slice represents an error.
     */
    public static final String HINT_ERROR = "error";
    /**
     * Hint indicating an item representing a time-to-live for the content.
     */
    public static final String HINT_TTL = "ttl";
    /**
     * Hint indicating an item representing when the content was created or last updated.
     */
    public static final String HINT_LAST_UPDATED = "last_updated";
    /**
     * A hint to indicate that this slice represents a permission request for showing
     * slices.
     */
    public static final String HINT_PERMISSION_REQUEST = "permission_request";
    /**
     * Subtype to indicate that this item indicates the layout direction for content
     * in the slice.
     * Expected to be an item of format {@link SliceItem#FORMAT_INT}.
     */
    public static final String SUBTYPE_LAYOUT_DIRECTION = "layout_direction";
    /**
     * Key to retrieve an extra added to an intent when a control is changed.
     */
    public static final String EXTRA_TOGGLE_STATE = "android.app.slice.extra.TOGGLE_STATE";
    /**
     * Key to retrieve an extra added to an intent when the value of a slider is changed.
     * @deprecated remove once support lib is update to use EXTRA_RANGE_VALUE instead
     * @removed
     */
    @Deprecated
    public static final String EXTRA_SLIDER_VALUE = "android.app.slice.extra.SLIDER_VALUE";
    /**
     * Key to retrieve an extra added to an intent when the value of an input range is changed.
     */
    public static final String EXTRA_RANGE_VALUE = "android.app.slice.extra.RANGE_VALUE";
    /**
     * Subtype to indicate that this is a message as part of a communication
     * sequence in this slice.
     * Expected to be on an item of format {@link SliceItem#FORMAT_SLICE}.
     */
    public static final String SUBTYPE_MESSAGE = "message";
    /**
     * Subtype to tag the source (i.e. sender) of a {@link #SUBTYPE_MESSAGE}.
     * Expected to be on an item of format {@link SliceItem#FORMAT_TEXT},
     * {@link SliceItem#FORMAT_IMAGE} or an {@link SliceItem#FORMAT_SLICE} containing them.
     */
    public static final String SUBTYPE_SOURCE = "source";
    /**
     * Subtype to tag an item as representing a color.
     * Expected to be on an item of format {@link SliceItem#FORMAT_INT}.
     */
    public static final String SUBTYPE_COLOR = "color";
    /**
     * Subtype to tag an item as representing a slider.
     * @deprecated remove once support lib is update to use SUBTYPE_RANGE instead
     * @removed
     */
    @Deprecated
    public static final String SUBTYPE_SLIDER = "slider";
    /**
     * Subtype to tag an item as representing a range.
     * Expected to be on an item of format {@link SliceItem#FORMAT_SLICE} containing
     * a {@link #SUBTYPE_VALUE} and possibly a {@link #SUBTYPE_MAX}.
     */
    public static final String SUBTYPE_RANGE = "range";
    /**
     * Subtype to tag an item as representing the max int value for a {@link #SUBTYPE_RANGE}.
     * Expected to be on an item of format {@link SliceItem#FORMAT_INT}.
     */
    public static final String SUBTYPE_MAX = "max";
    /**
     * Subtype to tag an item as representing the current int value for a {@link #SUBTYPE_RANGE}.
     * Expected to be on an item of format {@link SliceItem#FORMAT_INT}.
     */
    public static final String SUBTYPE_VALUE = "value";
    /**
     * Subtype to indicate that this content has a toggle action associated with it. To indicate
     * that the toggle is on, use {@link #HINT_SELECTED}. When the toggle state changes, the
     * intent associated with it will be sent along with an extra {@link #EXTRA_TOGGLE_STATE}
     * which can be retrieved to see the new state of the toggle.
     */
    public static final String SUBTYPE_TOGGLE = "toggle";
    /**
     * Subtype to tag an item representing priority.
     * Expected to be on an item of format {@link SliceItem#FORMAT_INT}.
     */
    public static final String SUBTYPE_PRIORITY = "priority";
    /**
     * Subtype to tag an item to use as a content description.
     * Expected to be on an item of format {@link SliceItem#FORMAT_TEXT}.
     */
    public static final String SUBTYPE_CONTENT_DESCRIPTION = "content_description";
    /**
     * Subtype to tag an item as representing a time in milliseconds since midnight,
     * January 1, 1970 UTC.
     */
    public static final String SUBTYPE_MILLIS = "millis";

    private final SliceItem[] mItems;
    private final @SliceHint String[] mHints;
    private SliceSpec mSpec;
    private Uri mUri;

    Slice(ArrayList<SliceItem> items, @SliceHint String[] hints, Uri uri, SliceSpec spec) {
        mHints = hints;
        mItems = items.toArray(new SliceItem[items.size()]);
        mUri = uri;
        mSpec = spec;
    }

    protected Slice(Parcel in) {
        mHints = in.readStringArray();
        int n = in.readInt();
        mItems = new SliceItem[n];
        for (int i = 0; i < n; i++) {
            mItems[i] = SliceItem.CREATOR.createFromParcel(in);
        }
        mUri = Uri.CREATOR.createFromParcel(in);
        mSpec = in.readTypedObject(SliceSpec.CREATOR);
    }

    /**
     * @return The spec for this slice
     */
    public @Nullable SliceSpec getSpec() {
        return mSpec;
    }

    /**
     * @return The Uri that this Slice represents.
     */
    public Uri getUri() {
        return mUri;
    }

    /**
     * @return All child {@link SliceItem}s that this Slice contains.
     */
    public List<SliceItem> getItems() {
        return Arrays.asList(mItems);
    }

    /**
     * @return All hints associated with this Slice.
     */
    public @SliceHint List<String> getHints() {
        return Arrays.asList(mHints);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringArray(mHints);
        dest.writeInt(mItems.length);
        for (int i = 0; i < mItems.length; i++) {
            mItems[i].writeToParcel(dest, flags);
        }
        mUri.writeToParcel(dest, 0);
        dest.writeTypedObject(mSpec, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * @hide
     */
    public boolean hasHint(@SliceHint String hint) {
        return ArrayUtils.contains(mHints, hint);
    }

    /**
     * Returns whether the caller for this slice matters.
     * @see Builder#setCallerNeeded
     */
    public boolean isCallerNeeded() {
        return hasHint(HINT_CALLER_NEEDED);
    }

    /**
     * A Builder used to construct {@link Slice}s
     */
    public static class Builder {

        private final Uri mUri;//切片的标识符
        private ArrayList<SliceItem> mItems = new ArrayList<>();//切片项列表
        private @SliceHint ArrayList<String> mHints = new ArrayList<>();//切片提示列表
        private SliceSpec mSpec;//切片规范

        /**
         * @deprecated TO BE REMOVED
         * @removed
         */
        @Deprecated
        public Builder(@NonNull Uri uri) {
            mUri = uri;
        }

        /**
         * Create a builder which will construct a {@link Slice} for the given Uri.
         * @param uri Uri to tag for this slice.
         * @param spec the spec for this slice.
         */
        public Builder(@NonNull Uri uri, SliceSpec spec) {
            mUri = uri;
            mSpec = spec;
        }

        /**
         * Create a builder for a {@link Slice} that is a sub-slice of the slice
         * being constructed by the provided builder.
         * @param parent The builder constructing the parent slice
         */
        public Builder(@NonNull Slice.Builder parent) {
            mUri = parent.mUri.buildUpon().appendPath("_gen")
                    .appendPath(String.valueOf(mItems.size())).build();
        }

        /**
         * Tells the system whether for this slice the return value of
         * {@link SliceProvider#onBindSlice(Uri, java.util.Set)} may be different depending on
         * {@link SliceProvider#getCallingPackage()} and should not be cached for multiple
         * apps.
         * 设置是否需要调用方信息，避免缓存
         */
        public Builder setCallerNeeded(boolean callerNeeded) {
            if (callerNeeded) {
                mHints.add(HINT_CALLER_NEEDED);
            } else {
                mHints.remove(HINT_CALLER_NEEDED);
            }
            return this;
        }

        /**
         * Add hints to the Slice being constructed
         * 添加切片提示
         */
        public Builder addHints(@SliceHint List<String> hints) {
            mHints.addAll(hints);
            return this;
        }

        /**
         * @deprecated TO BE REMOVED
         * @removed
         */
        public Builder setSpec(SliceSpec spec) {
            mSpec = spec;
            return this;
        }

        /**
         * Add a sub-slice to the slice being constructed
         * @param subType Optional template-specific type information
         * @see SliceItem#getSubType()
         * 添加子切片
         */
        public Builder addSubSlice(@NonNull Slice slice, @Nullable @SliceSubtype String subType) {
            Preconditions.checkNotNull(slice);
            mItems.add(new SliceItem(slice, SliceItem.FORMAT_SLICE, subType,
                    slice.getHints().toArray(new String[slice.getHints().size()])));
            return this;
        }

        /**
         * Add an action to the slice being constructed
         * @param subType Optional template-specific type information
         * @see SliceItem#getSubType()
         * 添加操作
         */
        public Slice.Builder addAction(@NonNull PendingIntent action, @NonNull Slice s,
                @Nullable @SliceSubtype String subType) {
            Preconditions.checkNotNull(action);
            Preconditions.checkNotNull(s);
            List<String> hints = s.getHints();
            s.mSpec = null;
            mItems.add(new SliceItem(action, s, SliceItem.FORMAT_ACTION, subType, hints.toArray(
                    new String[hints.size()])));
            return this;
        }

        /**
         * Add text to the slice being constructed
         * @param subType Optional template-specific type information
         * @see SliceItem#getSubType()
         * 添加文本内容
         */
        public Builder addText(CharSequence text, @Nullable @SliceSubtype String subType,
                @SliceHint List<String> hints) {
            mItems.add(new SliceItem(text, SliceItem.FORMAT_TEXT, subType, hints));
            return this;
        }

        /**
         * Add an image to the slice being constructed
         * @param subType Optional template-specific type information
         * @see SliceItem#getSubType()
         * 添加图标
         */
        public Builder addIcon(Icon icon, @Nullable @SliceSubtype String subType,
                @SliceHint List<String> hints) {
            Preconditions.checkNotNull(icon);
            mItems.add(new SliceItem(icon, SliceItem.FORMAT_IMAGE, subType, hints));
            return this;
        }

        /**
         * Add remote input to the slice being constructed
         * @param subType Optional template-specific type information
         * @see SliceItem#getSubType()
         * 添加远程输入
         */
        public Slice.Builder addRemoteInput(RemoteInput remoteInput,
                @Nullable @SliceSubtype String subType,
                @SliceHint List<String> hints) {
            Preconditions.checkNotNull(remoteInput);
            mItems.add(new SliceItem(remoteInput, SliceItem.FORMAT_REMOTE_INPUT,
                    subType, hints));
            return this;
        }

        /**
         * Add an integer to the slice being constructed
         * @param subType Optional template-specific type information
         * @see SliceItem#getSubType()
         * 添加整数值
         */
        public Builder addInt(int value, @Nullable @SliceSubtype String subType,
                @SliceHint List<String> hints) {
            mItems.add(new SliceItem(value, SliceItem.FORMAT_INT, subType, hints));
            return this;
        }

        /**
         * @deprecated TO BE REMOVED.
         * @removed
         *
         */
        @Deprecated
        public Slice.Builder addTimestamp(long time, @Nullable @SliceSubtype String subType,
                @SliceHint List<String> hints) {
            return addLong(time, subType, hints);
        }

        /**
         * Add a long to the slice being constructed
         * @param subType Optional template-specific type information
         * @see SliceItem#getSubType()
         * 添加长整数值
         */
        public Slice.Builder addLong(long value, @Nullable @SliceSubtype String subType,
                @SliceHint List<String> hints) {
            mItems.add(new SliceItem(value, SliceItem.FORMAT_LONG, subType,
                    hints.toArray(new String[hints.size()])));
            return this;
        }

        /**
         * Add a bundle to the slice being constructed.
         * <p>Expected to be used for support library extension, should not be used for general
         * development
         * @param subType Optional template-specific type information
         * @see SliceItem#getSubType()
         * 添加 Bundle 数据
         */
        public Slice.Builder addBundle(Bundle bundle, @Nullable @SliceSubtype String subType,
                @SliceHint List<String> hints) {
            Preconditions.checkNotNull(bundle);
            mItems.add(new SliceItem(bundle, SliceItem.FORMAT_BUNDLE, subType,
                    hints));
            return this;
        }

        /**
         * Construct the slice.
         */
        public Slice build() {
            return new Slice(mItems, mHints.toArray(new String[mHints.size()]), mUri, mSpec);
        }
    }

    public static final Creator<Slice> CREATOR = new Creator<Slice>() {
        @Override
        public Slice createFromParcel(Parcel in) {
            return new Slice(in);
        }

        @Override
        public Slice[] newArray(int size) {
            return new Slice[size];
        }
    };

    /**
     * @hide
     * @return A string representation of this slice.
     */
    public String toString() {
        return toString("");
    }

    private String toString(String indent) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mItems.length; i++) {
            sb.append(indent);
            if (Objects.equals(mItems[i].getFormat(), SliceItem.FORMAT_SLICE)) {
                sb.append("slice:\n");
                sb.append(mItems[i].getSlice().toString(indent + "   "));
            } else if (Objects.equals(mItems[i].getFormat(), SliceItem.FORMAT_TEXT)) {
                sb.append("text: ");
                sb.append(mItems[i].getText());
                sb.append("\n");
            } else {
                sb.append(mItems[i].getFormat());
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}
