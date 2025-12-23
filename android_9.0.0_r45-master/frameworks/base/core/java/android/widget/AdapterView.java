/*
 * Copyright (C) 2006 The Android Open Source Project
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

package android.widget;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.database.DataSetObserver;
import android.os.Parcelable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.view.ViewHierarchyEncoder;
import android.view.ViewStructure;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.autofill.AutofillManager;

/**
 * An AdapterView is a view whose children are determined by an {@link Adapter}.
 *
 * <p>
 * See {@link ListView}, {@link GridView}, {@link Spinner} and
 * {@link Gallery} for commonly used subclasses of AdapterView.
 *
 * <div class="special reference">
 * <h3>Developer Guides</h3>
 * <p>For more information about using AdapterView, read the
 * <a href="{@docRoot}guide/topics/ui/binding.html">Binding to Data with AdapterView</a>
 * developer guide.</p></div>
 */

/**
 * AdapterView 是 Android 中一个抽象类，它是所有基于适配器的视图组件的基类。
 * AdapterView 作为连接数据源（通过 Adapter）和 UI 显示的桥梁，负责：
 * 管理数据项与视图的对应关系
 * 处理用户交互事件
 * 维护选中状态
 * 同步数据变化
 * @param <T>
 */
public abstract class AdapterView<T extends Adapter> extends ViewGroup {

    /**
     * The item view type returned by {@link Adapter#getItemViewType(int)} when
     * the adapter does not want the item's view recycled.
     * 这个常量用于在实现 Adapter 接口时，作为 getItemViewType(int position) 方法的返回值，具有特殊含义：
     * 禁止视图回收：当 Adapter 的 getItemViewType(int position) 方法返回 ITEM_VIEW_TYPE_IGNORE 时，表示该位置的视图不应该被 AdapterView 回收和重用
     * 特殊项目标记：通常用于标记那些不应该被常规处理的项目，例如分隔符(separator)或者需要特殊处理的视图
     */
    public static final int ITEM_VIEW_TYPE_IGNORE = -1;

    /**
     * The item view type returned by {@link Adapter#getItemViewType(int)} when
     * the item is a header or footer.
     * 用于标识页眉或页脚项目
     */
    public static final int ITEM_VIEW_TYPE_HEADER_OR_FOOTER = -2;

    /**
     * The position of the first child displayed
     * 用于跟踪当前显示在屏幕上的第一个子视图在适配器数据集中的位置。
     * 当用户滚动列表时，mFirstPosition 会动态更新以反映新的第一个可见项位置
     * 在 getView() 方法中，可用于确定当前正在处理的数据项位置
     * 在布局过程中，用于确定子视图的正确数据绑定
     */
    @ViewDebug.ExportedProperty(category = "scrolling")
    int mFirstPosition = 0;

    /**
     * The offset in pixels from the top of the AdapterView to the top
     * of the view to select during the next layout.
     * 记录 AdapterView 顶部与在下次布局期间要选择的视图顶部之间的像素偏移量
     */
    int mSpecificTop;

    /**
     * Position from which to start looking for mSyncRowId
     * 记录开始查找 mSyncRowId 的起始位置
     * 在数据发生变化后，用于同步视图状态，帮助 AdapterView 找到与之前选中项相对应的新位置
     */
    int mSyncPosition;

    /**
     * Row id to look for when data has changed
     * 存储需要同步的数据行ID
     */
    long mSyncRowId = INVALID_ROW_ID;

    /**
     * Height of the view when mSyncPosition and mSyncRowId where set
     * 记录设置 mSyncPosition 和 mSyncRowId 时视图的高度
     */
    long mSyncHeight;

    /**
     * True if we need to sync to mSyncRowId
     * 标记是否需要执行同步操作
     */
    boolean mNeedSync = false;

    /**
     * Indicates whether to sync based on the selection or position. Possible
     * values are {@link #SYNC_SELECTED_POSITION} or
     * {@link #SYNC_FIRST_POSITION}.
     * 同步模式标识：用于指示是基于选中项还是基于位置进行同步
     */
    int mSyncMode;

    /**
     * Our height after the last layout
     * 记录布局高度：保存上次布局时的视图高度
     */
    private int mLayoutHeight;

    /**
     * Sync based on the selected child
     * 基于选中的子项进行同步
     */
    static final int SYNC_SELECTED_POSITION = 0;

    /**
     * Sync based on the first child displayed
     * 基于第一个显示的子项进行同步
     */
    static final int SYNC_FIRST_POSITION = 1;

    /**
     * Maximum amount of time to spend in {@link #findSyncPosition()}
     * 同步搜索最大持续时间：在 findSyncPosition() 方法中搜索同步位置的最大时间限制
     */
    static final int SYNC_MAX_DURATION_MILLIS = 100;

    /**
     * Indicates that this view is currently being laid out.
     * 布局状态标记：指示 AdapterView 当前是否正在布局中
     */
    boolean mInLayout = false;

    /**
     * The listener that receives notifications when an item is selected.
     */
    OnItemSelectedListener mOnItemSelectedListener;

    /**
     * The listener that receives notifications when an item is clicked.
     */
    OnItemClickListener mOnItemClickListener;

    /**
     * The listener that receives notifications when an item is long clicked.
     */
    OnItemLongClickListener mOnItemLongClickListener;

    /**
     * True if the data has changed since the last layout
     * 数据变更标志：标记适配器数据是否自上次布局以来发生了变化
     */
    boolean mDataChanged;

    /**
     * The position within the adapter's data set of the item to select
     * during the next layout.
     * 在下次布局期间要选择的项目的索引位置
     */
    @ViewDebug.ExportedProperty(category = "list")
    int mNextSelectedPosition = INVALID_POSITION;

    /**
     * The item id of the item to select during the next layout.
     * 在下次布局期间要选择的项目的行ID
     */
    long mNextSelectedRowId = INVALID_ROW_ID;

    /**
     * The position within the adapter's data set of the currently selected item.
     * 当前选中项目的索引位置
     */
    @ViewDebug.ExportedProperty(category = "list")
    int mSelectedPosition = INVALID_POSITION;

    /**
     * The item id of the currently selected item.
     * 当前选中项目的行ID
     */
    long mSelectedRowId = INVALID_ROW_ID;

    /**
     * View to show if there are no items to show.
     * 当适配器中没有数据项时显示的空视图
     */
    private View mEmptyView;

    /**
     * The number of items in the current adapter.
     * 当前适配器中的数据项总数
     */
    @ViewDebug.ExportedProperty(category = "list")
    int mItemCount;

    /**
     * The number of items in the adapter before a data changed event occurred.
     * 数据变更事件发生前适配器中的数据项数量
     */
    int mOldItemCount;

    /**
     * Represents an invalid position. All valid positions are in the range 0 to 1 less than the
     * number of items in the current adapter.
     * 表示无效的位置索引
     */
    public static final int INVALID_POSITION = -1;

    /**
     * Represents an empty or invalid row id
     * 表示无效的行ID
     */
    public static final long INVALID_ROW_ID = Long.MIN_VALUE;

    /**
     * The last selected position we used when notifying
     * 记录上次通知时使用的选中位置
     */
    int mOldSelectedPosition = INVALID_POSITION;

    /**
     * The id of the last selected position we used when notifying
     * 记录上次通知时使用的选中位置的行ID
     */
    long mOldSelectedRowId = INVALID_ROW_ID;

    /**
     * Indicates what focusable state is requested when calling setFocusable().
     * In addition to this, this view has other criteria for actually
     * determining the focusable state (such as whether its empty or the text
     * filter is shown).
     *
     * @see #setFocusable(boolean)
     * @see #checkFocus()
     * 记录期望的焦点状态
     */
    private int mDesiredFocusableState = FOCUSABLE_AUTO;
    //记录期望的触摸模式焦点状态
    private boolean mDesiredFocusableInTouchModeState;

    /** Lazily-constructed runnable for dispatching selection events. */
    //延迟构造的选择事件分发 Runnable
    private SelectionNotifier mSelectionNotifier;

    /** Selection notifier that's waiting for the next layout pass. */
    //等待下一次布局过程的选择通知器
    private SelectionNotifier mPendingSelectionNotifier;

    /**
     * When set to true, calls to requestLayout() will not propagate up the parent hierarchy.
     * This is used to layout the children during a layout pass.
     * 控制布局请求是否向父级传递
     */
    boolean mBlockLayoutRequests = false;

    public AdapterView(Context context) {
        this(context, null);
    }

    public AdapterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AdapterView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    /**
     *
     * @param context 应用程序上下文环境，提供访问应用程序资源、系统服务等的入口，用于创建视图时获取必要的环境信息
     * @param attrs 从布局文件中解析出的 XML 属性集合，包含在布局文件中为该视图定义的自定义属性，用于初始化视图的外观和行为属性
     * @param defStyleAttr 默认的样式属性资源ID，指向主题中定义的默认样式属性，当布局中未指定具体样式时使用的默认样式引用
     * @param defStyleRes 默认的样式资源ID,指向具体的样式资源文件,当 defStyleAttr 未定义时使用的备用样式资源
     */
    public AdapterView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        // If not explicitly specified this view is important for accessibility.
        if (getImportantForAccessibility() == IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        }

        mDesiredFocusableState = getFocusable();
        if (mDesiredFocusableState == FOCUSABLE_AUTO) {
            // Starts off without an adapter, so NOT_FOCUSABLE by default.
            super.setFocusable(NOT_FOCUSABLE);
        }
    }

    /**
     * Interface definition for a callback to be invoked when an item in this
     * AdapterView has been clicked.
     * 用于处理 AdapterView 中项目的点击事件。
     */
    public interface OnItemClickListener {

        /**
         * Callback method to be invoked when an item in this AdapterView has
         * been clicked.
         * <p>
         * Implementers can call getItemAtPosition(position) if they need
         * to access the data associated with the selected item.
         *
         * @param parent The AdapterView where the click happened.
         * @param view The view within the AdapterView that was clicked (this
         *            will be a view provided by the adapter)
         * @param position The position of the view in the adapter.
         * @param id The row id of the item that was clicked.
         */
        void onItemClick(AdapterView<?> parent, View view, int position, long id);
    }

    /**
     * Register a callback to be invoked when an item in this AdapterView has
     * been clicked.
     *
     * @param listener The callback that will be invoked.
     */
    public void setOnItemClickListener(@Nullable OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    /**
     * @return The callback to be invoked with an item in this AdapterView has
     *         been clicked, or null if no callback has been set.
     */
    @Nullable
    public final OnItemClickListener getOnItemClickListener() {
        return mOnItemClickListener;
    }

    /**
     * Call the OnItemClickListener, if it is defined. Performs all normal
     * actions associated with clicking: reporting accessibility event, playing
     * a sound, etc.
     *
     * @param view The view within the AdapterView that was clicked.
     * @param position The position of the view in the adapter.
     * @param id The row id of the item that was clicked.
     * @return True if there was an assigned OnItemClickListener that was
     *         called, false otherwise is returned.
     */
    /**
     * 用于触发 AdapterView 中项目的点击事件，执行所有与点击相关的标准操作。
     * @param view 在 AdapterView 中被点击的视图项（由适配器提供的视图）
     * @param position 被点击视图在适配器数据集中的位置索引
     * @param id 被点击项目的行 ID
     * @return
     */
    public boolean performItemClick(View view, int position, long id) {
        final boolean result;
        if (mOnItemClickListener != null) {
            playSoundEffect(SoundEffectConstants.CLICK);
            mOnItemClickListener.onItemClick(this, view, position, id);
            result = true;
        } else {
            result = false;
        }

        if (view != null) {
            view.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);
        }
        return result;
    }

    /**
     * Interface definition for a callback to be invoked when an item in this
     * view has been clicked and held.
     */
    public interface OnItemLongClickListener {
        /**
         * Callback method to be invoked when an item in this view has been
         * clicked and held.
         *
         * Implementers can call getItemAtPosition(position) if they need to access
         * the data associated with the selected item.
         *
         * @param parent The AbsListView where the click happened
         * @param view The view within the AbsListView that was clicked
         * @param position The position of the view in the list
         * @param id The row id of the item that was clicked
         *
         * @return true if the callback consumed the long click, false otherwise
         */
        boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id);
    }


    /**
     * Register a callback to be invoked when an item in this AdapterView has
     * been clicked and held
     *
     * @param listener The callback that will run
     */
    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        if (!isLongClickable()) {
            setLongClickable(true);
        }
        mOnItemLongClickListener = listener;
    }

    /**
     * @return The callback to be invoked with an item in this AdapterView has
     *         been clicked and held, or null if no callback has been set.
     */
    public final OnItemLongClickListener getOnItemLongClickListener() {
        return mOnItemLongClickListener;
    }

    /**
     * Interface definition for a callback to be invoked when
     * an item in this view has been selected.
     * 用于处理 AdapterView 中项目的选择事件。
     */
    public interface OnItemSelectedListener {
        /**
         * <p>Callback method to be invoked when an item in this view has been
         * selected. This callback is invoked only when the newly selected
         * position is different from the previously selected position or if
         * there was no selected item.</p>
         *
         * Implementers can call getItemAtPosition(position) if they need to access the
         * data associated with the selected item.
         *
         * @param parent The AdapterView where the selection happened
         * @param view The view within the AdapterView that was clicked
         * @param position The position of the view in the adapter
         * @param id The row id of the item that is selected
         *           当 AdapterView 中的项目被选中时调用
         *           parent: 发生选择事件的 AdapterView 实例
         * view: 被选中的具体视图项（由 Adapter 提供的视图）
         * position: 被选中项在 Adapter 数据集中的位置索引
         * id: 被选中项的行 ID
         */
        void onItemSelected(AdapterView<?> parent, View view, int position, long id);

        /**
         * Callback method to be invoked when the selection disappears from this
         * view. The selection can disappear for instance when touch is activated
         * or when the adapter becomes empty.
         *
         * @param parent The AdapterView that now contains no selected item.
         *               当 AdapterView 中没有项目被选中时调用
         */
        void onNothingSelected(AdapterView<?> parent);
    }


    /**
     * Register a callback to be invoked when an item in this AdapterView has
     * been selected.
     *
     * @param listener The callback that will run
     */
    public void setOnItemSelectedListener(@Nullable OnItemSelectedListener listener) {
        mOnItemSelectedListener = listener;
    }

    @Nullable
    public final OnItemSelectedListener getOnItemSelectedListener() {
        return mOnItemSelectedListener;
    }

    /**
     * Extra menu information provided to the
     * {@link android.view.View.OnCreateContextMenuListener#onCreateContextMenu(ContextMenu, View, ContextMenuInfo) }
     * callback when a context menu is brought up for this AdapterView.
     * 作用是在 AdapterView 的子项上显示上下文菜单时，提供相关的菜单信息。
     * 主要功能
     * 当用户在 AdapterView（如 ListView、GridView 等）的某个项目上长按时，系统会显示上下文菜单
     * AdapterContextMenuInfo 用于封装与该菜单相关的信息，包括目标视图、位置和行 ID
     */
    public static class AdapterContextMenuInfo implements ContextMenu.ContextMenuInfo {

        public AdapterContextMenuInfo(View targetView, int position, long id) {
            this.targetView = targetView;
            this.position = position;
            this.id = id;
        }

        /**
         * The child view for which the context menu is being displayed. This
         * will be one of the children of this AdapterView.
         * 触发上下文菜单的具体子视图，它是 AdapterView 的子元素之一
         */
        public View targetView;

        /**
         * The position in the adapter for which the context menu is being
         * displayed.
         * 该项目在适配器数据集中的位置索引
         */
        public int position;

        /**
         * The row id of the item for which the context menu is being displayed.
         * 该项目的行 ID（来自适配器的 getItemId() 方法）
         */
        public long id;
    }

    /**
     * Returns the adapter currently associated with this widget.
     *
     * @return The adapter used to provide this view's content.
     */
    public abstract T getAdapter();

    /**
     * Sets the adapter that provides the data and the views to represent the data
     * in this widget.
     *
     * @param adapter The adapter to use to create this view's content.
     */
    public abstract void setAdapter(T adapter);

    /**
     * This method is not supported and throws an UnsupportedOperationException when called.
     *
     * @param child Ignored.
     *
     * @throws UnsupportedOperationException Every time this method is invoked.
     */
    @Override
    public void addView(View child) {
        throw new UnsupportedOperationException("addView(View) is not supported in AdapterView");
    }

    /**
     * This method is not supported and throws an UnsupportedOperationException when called.
     *
     * @param child Ignored.
     * @param index Ignored.
     *
     * @throws UnsupportedOperationException Every time this method is invoked.
     */
    @Override
    public void addView(View child, int index) {
        throw new UnsupportedOperationException("addView(View, int) is not supported in AdapterView");
    }

    /**
     * This method is not supported and throws an UnsupportedOperationException when called.
     *
     * @param child Ignored.
     * @param params Ignored.
     *
     * @throws UnsupportedOperationException Every time this method is invoked.
     */
    @Override
    public void addView(View child, LayoutParams params) {
        throw new UnsupportedOperationException("addView(View, LayoutParams) "
                + "is not supported in AdapterView");
    }

    /**
     * This method is not supported and throws an UnsupportedOperationException when called.
     *
     * @param child Ignored.
     * @param index Ignored.
     * @param params Ignored.
     *
     * @throws UnsupportedOperationException Every time this method is invoked.
     */
    @Override
    public void addView(View child, int index, LayoutParams params) {
        throw new UnsupportedOperationException("addView(View, int, LayoutParams) "
                + "is not supported in AdapterView");
    }

    /**
     * This method is not supported and throws an UnsupportedOperationException when called.
     *
     * @param child Ignored.
     *
     * @throws UnsupportedOperationException Every time this method is invoked.
     */
    @Override
    public void removeView(View child) {
        throw new UnsupportedOperationException("removeView(View) is not supported in AdapterView");
    }

    /**
     * This method is not supported and throws an UnsupportedOperationException when called.
     *
     * @param index Ignored.
     *
     * @throws UnsupportedOperationException Every time this method is invoked.
     */
    @Override
    public void removeViewAt(int index) {
        throw new UnsupportedOperationException("removeViewAt(int) is not supported in AdapterView");
    }

    /**
     * This method is not supported and throws an UnsupportedOperationException when called.
     *
     * @throws UnsupportedOperationException Every time this method is invoked.
     */
    @Override
    public void removeAllViews() {
        throw new UnsupportedOperationException("removeAllViews() is not supported in AdapterView");
    }

    /**
     * 作用
     * 在布局过程中保存当前视图高度，用于后续的同步和定位计算
     * 为 rememberSyncState() 等同步方法提供高度信息
     */
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        mLayoutHeight = getHeight();
    }

    /**
     * Return the position of the currently selected item within the adapter's data set
     *
     * @return int Position (starting at 0), or {@link #INVALID_POSITION} if there is nothing selected.
     * 获取当前在适配器数据集中选中项的位置
     * 从0开始的位置索引，或 INVALID_POSITION（-1）表示没有选中项
     */
    @ViewDebug.CapturedViewProperty
    public int getSelectedItemPosition() {
        return mNextSelectedPosition;
    }

    /**
     * @return The id corresponding to the currently selected item, or {@link #INVALID_ROW_ID}
     * if nothing is selected.
     */
    @ViewDebug.CapturedViewProperty
    public long getSelectedItemId() {
        return mNextSelectedRowId;
    }

    /**
     * @return The view corresponding to the currently selected item, or null
     * if nothing is selected
     * 返回当前在 AdapterView 中选中的视图项
     */
    public abstract View getSelectedView();

    /**
     * @return The data corresponding to the currently selected item, or
     * null if there is nothing selected.
     * 返回当前在 AdapterView 中选中的视图项
     */
    public Object getSelectedItem() {
        T adapter = getAdapter();
        int selection = getSelectedItemPosition();
        if (adapter != null && adapter.getCount() > 0 && selection >= 0) {
            return adapter.getItem(selection);
        } else {
            return null;
        }
    }

    /**
     * @return The number of items owned by the Adapter associated with this
     *         AdapterView. (This is the number of data items, which may be
     *         larger than the number of visible views.)
     *         获取与 AdapterView 关联的适配器中的数据项总数
     */
    @ViewDebug.CapturedViewProperty
    public int getCount() {
        return mItemCount;
    }

    /**
     * Returns the position within the adapter's data set for the view, where
     * view is a an adapter item or a descendant of an adapter item.
     * <p>
     * <strong>Note:</strong> The result of this method only reflects the
     * position of the data bound to <var>view</var> during the most recent
     * layout pass. If the adapter's data set has changed without a subsequent
     * layout pass, the position returned by this method may not match the
     * current position of the data within the adapter.
     *
     * @param view an adapter item, or a descendant of an adapter item. This
     *             must be visible in this AdapterView at the time of the call.
     * @return the position within the adapter's data set of the view, or
     *         {@link #INVALID_POSITION} if the view does not correspond to a
     *         list item (or it is not currently visible)
     * 返回适配器数据集中与指定视图相对应的位置
     */
    public int getPositionForView(View view) {
        View listItem = view;
        try {
            //向上遍历：从输入的 view 开始向上查找父视图，直到找到 AdapterView 本身
            View v;
            while ((v = (View) listItem.getParent()) != null && !v.equals(this)) {
                listItem = v;
            }
        } catch (ClassCastException e) {
            // We made it up to the window without find this list view
            return INVALID_POSITION;
        }
        //在 AdapterView 的子视图中查找对应的视图项
        if (listItem != null) {
            // Search the children for the list item
            final int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                if (getChildAt(i).equals(listItem)) {
                    //返回 mFirstPosition + 子视图索引
                    return mFirstPosition + i;
                }
            }
        }

        // Child not found!
        return INVALID_POSITION;
    }

    /**
     * Returns the position within the adapter's data set for the first item
     * displayed on screen.
     *
     * @return The position within the adapter's data set
     * 获取屏幕上第一个显示的项目在适配器数据集中的位置
     */
    public int getFirstVisiblePosition() {
        return mFirstPosition;
    }

    /**
     * Returns the position within the adapter's data set for the last item
     * displayed on screen.
     *
     * @return The position within the adapter's data set
     */
    public int getLastVisiblePosition() {
        return mFirstPosition + getChildCount() - 1;
    }

    /**
     * Sets the currently selected item. To support accessibility subclasses that
     * override this method must invoke the overridden super method first.
     *
     * @param position Index (starting at 0) of the data item to be selected.
     *                 根据指定的位置索引设置适配器数据集中对应的项目为当前选中项
     */
    public abstract void setSelection(int position);

    /**
     * Sets the view to show if the adapter is empty
     * 设置当适配器为空时显示的特殊视图
     * 作用：
     * 为用户提供数据不可用时的反馈
     * 控制 AdapterView 和空视图的可见性状态
     * 确保空视图具有适当的无障碍支持
     */
    @android.view.RemotableViewMethod
    public void setEmptyView(View emptyView) {
        //将传入的 emptyView 存储到 mEmptyView 字段中
        mEmptyView = emptyView;

        // If not explicitly specified this view is important for accessibility.
        //无障碍支持：如果 emptyView 不为 null 且无障碍重要性为 IMPORTANT_FOR_ACCESSIBILITY_AUTO，则设置为 IMPORTANT_FOR_ACCESSIBILITY_YES
        if (emptyView != null
                && emptyView.getImportantForAccessibility() == IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
            emptyView.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        }

        //更新状态：获取适配器并检查是否为空，然后调用 updateEmptyStatus() 更新显示状态
        final T adapter = getAdapter();
        final boolean empty = ((adapter == null) || adapter.isEmpty());
        updateEmptyStatus(empty);
    }

    /**
     * When the current adapter is empty, the AdapterView can display a special view
     * called the empty view. The empty view is used to provide feedback to the user
     * that no data is available in this AdapterView.
     *
     * @return The view to show if the adapter is empty.
     * 获取当前设置的空视图
     */
    public View getEmptyView() {
        return mEmptyView;
    }

    /**
     * Indicates whether this view is in filter mode. Filter mode can for instance
     * be enabled by a user when typing on the keyboard.
     *
     * @return True if the view is in filter mode, false otherwise.
     * 判断当前 AdapterView 是否处于过滤模式
     */
    boolean isInFilterMode() {
        return false;
    }

    /**
     * 设置焦点状态：重写父类的 setFocusable 方法，根据适配器状态和过滤模式来决定是否可获得焦点
     * @param focusable
     */
    @Override
    public void setFocusable(@Focusable int focusable) {
        //获取适配器状态：检查适配器是否为空或项目数量是否为0
        final T adapter = getAdapter();
        final boolean empty = adapter == null || adapter.getCount() == 0;

        //保存焦点状态：将期望的焦点状态保存到 mDesiredFocusableState
        mDesiredFocusableState = focusable;
        //如果焦点设置为非可聚焦状态，同时设置 mDesiredFocusableInTouchModeState 为 false
        if ((focusable & (FOCUSABLE_AUTO | FOCUSABLE)) == 0) {
            mDesiredFocusableInTouchModeState = false;
        }

        super.setFocusable((!empty || isInFilterMode()) ? focusable : NOT_FOCUSABLE);
    }

    /**
     * 设置触摸模式焦点状态：重写父类的 setFocusableInTouchMode 方法，
     * 根据适配器状态和过滤模式来决定是否在触摸模式下可获得焦点
     * @param focusable
     */
    @Override
    public void setFocusableInTouchMode(boolean focusable) {
        //检查适配器是否为空或项目数量是否为0
        final T adapter = getAdapter();
        final boolean empty = adapter == null || adapter.getCount() == 0;

        //保存焦点模式状态：将期望的触摸模式焦点状态保存到 mDesiredFocusableInTouchModeState
        mDesiredFocusableInTouchModeState = focusable;
        //同步焦点状态：如果设置为可聚焦，则同时将 mDesiredFocusableState 设置为 FOCUSABLE
        if (focusable) {
            mDesiredFocusableState = FOCUSABLE;
        }

        super.setFocusableInTouchMode(focusable && (!empty || isInFilterMode()));
    }

    //检查焦点状态：根据适配器状态和过滤模式重新计算并设置 AdapterView 的焦点状态
    void checkFocus() {
        //获取适配器状态：检查适配器是否为空或项目数量是否为0
        final T adapter = getAdapter();
        final boolean empty = adapter == null || adapter.getCount() == 0;
        //计算可聚焦状态：!empty || isInFilterMode() - 如果适配器不为空或处于过滤模式，则可聚焦
        final boolean focusable = !empty || isInFilterMode();
        // The order in which we set focusable in touch mode/focusable may matter
        // for the client, see View.setFocusableInTouchMode() comments for more
        // details
        super.setFocusableInTouchMode(focusable && mDesiredFocusableInTouchModeState);
        super.setFocusable(focusable ? mDesiredFocusableState : NOT_FOCUSABLE);
        if (mEmptyView != null) {
            updateEmptyStatus((adapter == null) || adapter.isEmpty());
        }
    }

    /**
     * Update the status of the list based on the empty parameter.  If empty is true and
     * we have an empty view, display it.  In all the other cases, make sure that the listview
     * is VISIBLE and that the empty view is GONE (if it's not null).
     */
    /**
     * 更新空状态显示：根据 empty 参数控制 AdapterView 和 mEmptyView 的可见性状态
     */
    private void updateEmptyStatus(boolean empty) {
//        如果处于过滤模式，强制将 empty 设置为 false
        if (isInFilterMode()) {
            empty = false;
        }
        //根据 empty 参数决定显示哪个视图
        if (empty) {
            if (mEmptyView != null) {
                //有空视图：mEmptyView.setVisibility(View.VISIBLE) 和 setVisibility(View.GONE)
                mEmptyView.setVisibility(View.VISIBLE);
                setVisibility(View.GONE);
            } else {
                // If the caller just removed our empty view, make sure the list view is visible
                //无空视图：setVisibility(View.VISIBLE)（确保列表可见）
                setVisibility(View.VISIBLE);
            }

            // We are now GONE, so pending layouts will not be dispatched.
            // Force one here to make sure that the state of the list matches
            // the state of the adapter.
            if (mDataChanged) {
                this.onLayout(false, mLeft, mTop, mRight, mBottom);
            }
        } else {
            //隐藏空视图：mEmptyView.setVisibility(View.GONE)
            //显示列表：setVisibility(View.VISIBLE)
            if (mEmptyView != null) mEmptyView.setVisibility(View.GONE);
            setVisibility(View.VISIBLE);
        }
    }

    /**
     * Gets the data associated with the specified position in the list.
     *
     * @param position Which data to get
     * @return The data associated with the specified position in the list
     * 获取指定位置的数据项：根据给定的位置索引从适配器中获取对应的数据对象
     */
    public Object getItemAtPosition(int position) {
        T adapter = getAdapter();
        return (adapter == null || position < 0) ? null : adapter.getItem(position);
    }

    public long getItemIdAtPosition(int position) {
        T adapter = getAdapter();
        return (adapter == null || position < 0) ? INVALID_ROW_ID : adapter.getItemId(position);
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        throw new RuntimeException("Don't call setOnClickListener for an AdapterView. "
                + "You probably want setOnItemClickListener instead");
    }

    /**
     * Override to prevent freezing of any views created by the adapter.
     * 获取指定位置的数据项：根据给定的位置索引从适配器中获取对应的数据对象
     */
    @Override
    protected void dispatchSaveInstanceState(SparseArray<Parcelable> container) {
        dispatchFreezeSelfOnly(container);
    }

    /**
     * Override to prevent thawing of any views created by the adapter.
     * 恢复实例状态：只恢复 AdapterView 自身的状态
     */
    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
        dispatchThawSelfOnly(container);
    }

    /**
     * 用于监听数据集变化并更新 UI。
     */
    class AdapterDataSetObserver extends DataSetObserver {

        //保存当前状态的 Parcelable 对象
        private Parcelable mInstanceState = null;

        //数据集发生更改时调用
        @Override
        public void onChanged() {
            mDataChanged = true;//更新数据变化标志 mDataChanged
            mOldItemCount = mItemCount;//保存旧的项目数量 mOldItemCount
            mItemCount = getAdapter().getCount();//获取新的项目数量 mItemCount

            // Detect the case where a cursor that was previously invalidated has
            // been repopulated with new data.
            //如果适配器有稳定 ID 且之前数据为空现在有数据，则恢复之前保存的状态
            if (AdapterView.this.getAdapter().hasStableIds() && mInstanceState != null
                    && mOldItemCount == 0 && mItemCount > 0) {
                AdapterView.this.onRestoreInstanceState(mInstanceState);
                mInstanceState = null;
            } else {
                rememberSyncState();//调用 rememberSyncState() 保存同步状态
            }
            checkFocus();//调用 checkFocus() 检查焦点状态
            requestLayout();//调用 requestLayout() 请求重新布局
        }

        //数据集无效化时调用
        @Override
        public void onInvalidated() {
            mDataChanged = true;//设置数据变化标志 mDataChanged

            //如果适配器有稳定 ID，保存当前状态到 mInstanceState
            if (AdapterView.this.getAdapter().hasStableIds()) {
                // Remember the current state for the case where our hosting activity is being
                // stopped and later restarted
                mInstanceState = AdapterView.this.onSaveInstanceState();
            }

            // Data is invalid so we should reset our state
            //状态重置
            mOldItemCount = mItemCount;
            mItemCount = 0;
            mSelectedPosition = INVALID_POSITION;
            mSelectedRowId = INVALID_ROW_ID;
            mNextSelectedPosition = INVALID_POSITION;
            mNextSelectedRowId = INVALID_ROW_ID;
            mNeedSync = false;

            checkFocus();
            requestLayout();
        }

        //清除保存的状态
        public void clearSavedState() {
            mInstanceState = null;
        }
    }

    //窗口分离处理：重写父类的 onDetachedFromWindow 方法，在 AdapterView 从窗口移除时执行清理操作
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(mSelectionNotifier);
    }

    /**
     * 用于处理选择事件的通知。
     * 主要功能
     * 延迟通知：当数据发生变化且需要重新布局时，推迟通知的发送
     * 选择事件分发：在适当时机调用 dispatchOnItemSelected() 方法
     * 处理结果：
     * 满足条件时：将当前实例赋值给 mPendingSelectionNotifier，推迟通知
     * 不满足条件时：直接调用 dispatchOnItemSelected()
     */
    private class SelectionNotifier implements Runnable {
        public void run() {
            //清理状态
            mPendingSelectionNotifier = null;

            //mDataChanged 为 true（数据已更改），getViewRootImpl() 不为 null，isLayoutRequested() 返回 true（需要重新布局）
            if (mDataChanged && getViewRootImpl() != null
                    && getViewRootImpl().isLayoutRequested()) {
                // Data has changed between when this SelectionNotifier was
                // posted and now. Postpone the notification until the next
                // layout is complete and we run checkSelectionChanged().
                if (getAdapter() != null) {
                    mPendingSelectionNotifier = this;
                }
            } else {
                dispatchOnItemSelected();
            }
        }
    }

    //选择状态变更处理：处理 AdapterView 选中项变化的通知逻辑
    void selectionChanged() {
        // We're about to post or run the selection notifier, so we don't need
        // a pending notifier.
        mPendingSelectionNotifier = null;//清除待处理的选择通知器

        //选择通知处理：
        //触发条件：存在 mOnItemSelectedListener 或无障碍功能启用
        if (mOnItemSelectedListener != null
                || AccessibilityManager.getInstance(mContext).isEnabled()) {
            //布局状态判断：在布局过程中，使用 post() 延迟通知
            if (mInLayout || mBlockLayoutRequests) {
                // If we are in a layout traversal, defer notification
                // by posting. This ensures that the view tree is
                // in a consistent state and is able to accommodate
                // new layout or invalidate requests.
                if (mSelectionNotifier == null) {
                    mSelectionNotifier = new SelectionNotifier();
                } else {
                    removeCallbacks(mSelectionNotifier);
                }
                post(mSelectionNotifier);
            } else {
                dispatchOnItemSelected();
            }
        }
        // Always notify AutoFillManager - it will return right away if autofill is disabled.
//        无论是否启用自动填充，都通知 AutofillManager 值发生变化
        final AutofillManager afm = mContext.getSystemService(AutofillManager.class);
        if (afm != null) {
            afm.notifyValueChanged(this);
        }
    }

    //处理 AdapterView 中选中项的通知分发
    private void dispatchOnItemSelected() {
        //触发 OnItemSelectedListener 回调
        fireOnSelected();
        //执行无障碍相关的选择操作
        performAccessibilityActionsOnSelected();
    }

    //根据当前选中状态触发相应的 OnItemSelectedListener 回调
    private void fireOnSelected() {
        //检查是否存在选择监听器
        if (mOnItemSelectedListener == null) {
            return;
        }
        final int selection = getSelectedItemPosition();
        //选中项位置有效时触发 onItemSelected
        if (selection >= 0) {
            View v = getSelectedView();
            mOnItemSelectedListener.onItemSelected(this, v, selection,
                    getAdapter().getItemId(selection));
        } else {
            mOnItemSelectedListener.onNothingSelected(this);
        }
    }

    //处理 AdapterView 中选中项的无障碍相关事件
    private void performAccessibilityActionsOnSelected() {
        //检查无障碍功能是否启用
        if (!AccessibilityManager.getInstance(mContext).isEnabled()) {
            return;
        }
        final int position = getSelectedItemPosition();
        //检查是否存在有效选中项
        if (position >= 0) {
            // we fire selection events here not in View
            //发送选中事件
            sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
        }
    }

    /** @hide */
    //分发无障碍事件填充：处理 AdapterView 中选中视图的无障碍事件填充
    @Override
    public boolean dispatchPopulateAccessibilityEventInternal(AccessibilityEvent event) {
        View selectedView = getSelectedView();
        if (selectedView != null && selectedView.getVisibility() == VISIBLE
                && selectedView.dispatchPopulateAccessibilityEvent(event)) {
            return true;
        }
        return false;
    }

    /** @hide */
    //请求发送无障碍事件内部处理：处理子视图请求发送无障碍事件的内部逻辑
    @Override
    public boolean onRequestSendAccessibilityEventInternal(View child, AccessibilityEvent event) {
        if (super.onRequestSendAccessibilityEventInternal(child, event)) {
            // Add a record for ourselves as well.
            AccessibilityEvent record = AccessibilityEvent.obtain();
            onInitializeAccessibilityEvent(record);
            // Populate with the text of the requesting child.
            child.dispatchPopulateAccessibilityEvent(record);
            event.appendRecord(record);
            return true;
        }
        return false;
    }

    //获取无障碍类名：返回 AdapterView 的类名，用于无障碍服务识别
    @Override
    public CharSequence getAccessibilityClassName() {
        return AdapterView.class.getName();
    }

    /** @hide */
    @Override
    //初始化无障碍节点信息内部处理：为 AdapterView 初始化无障碍节点信息
    public void onInitializeAccessibilityNodeInfoInternal(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfoInternal(info);
        info.setScrollable(isScrollableForAccessibility());
        View selectedView = getSelectedView();
        if (selectedView != null) {
            info.setEnabled(selectedView.isEnabled());
        }
    }

    /** @hide */
    //初始化无障碍事件内部处理：为 AdapterView 初始化无障碍事件信息
    @Override
    public void onInitializeAccessibilityEventInternal(AccessibilityEvent event) {
        super.onInitializeAccessibilityEventInternal(event);
        event.setScrollable(isScrollableForAccessibility());
        View selectedView = getSelectedView();
        if (selectedView != null) {
            event.setEnabled(selectedView.isEnabled());
        }
        event.setCurrentItemIndex(getSelectedItemPosition());
        event.setFromIndex(getFirstVisiblePosition());
        event.setToIndex(getLastVisiblePosition());
        event.setItemCount(getCount());
    }

    //检查 AdapterView 是否支持无障碍滚动功能
    private boolean isScrollableForAccessibility() {
        //获取关联的适配器
        T adapter = getAdapter();
        if (adapter != null) {
            final int itemCount = adapter.getCount();
            //第一个可见项位置大于0（表示可以向上滚动）  最后一个可见项小于总数-1（表示可以向下滚动）
            return itemCount > 0
                    && (getFirstVisiblePosition() > 0 || getLastVisiblePosition() < itemCount - 1);
        }
        return false;
    }

    //动画能力检查：重写父类的 canAnimate() 方法，判断 AdapterView 是否支持动画
    @Override
    protected boolean canAnimate() {
        return super.canAnimate() && mItemCount > 0;
    }

    //处理数据变更：当适配器数据发生变化时，重新计算和设置选中项位置
    void handleDataChanged() {
        final int count = mItemCount;//获取当前数据项数量
        boolean found = false;

        //检查是否有数据项
        if (count > 0) {

            int newPos;

            // Find the row we are supposed to sync to
            //检查是否需要同步到特定位置
            if (mNeedSync) {
                // Update this first, since setNextSelectedPositionInt inspects
                // it
                mNeedSync = false;

                // See if we can find a position in the new data with the same
                // id as the old selection
                //查找匹配同步行ID的位置
                newPos = findSyncPosition();
                if (newPos >= 0) {
                    // Verify that new selection is selectable
                    //查找可选择的位置
                    int selectablePos = lookForSelectablePosition(newPos, true);
                    if (selectablePos == newPos) {
                        // Same row id is selected
                        //设置下一个选中位置
                        setNextSelectedPositionInt(newPos);
                        found = true;
                    }
                }
            }
            if (!found) {
                // Try to use the same position if we can't find matching data
                //获取当前选中位置
                newPos = getSelectedItemPosition();

                // Pin position to the available range
                if (newPos >= count) {
                    newPos = count - 1;
                }
                if (newPos < 0) {
                    newPos = 0;
                }

                // Make sure we select something selectable -- first look down
                //向下查找可选择位置
                int selectablePos = lookForSelectablePosition(newPos, true);
                if (selectablePos < 0) {
                    // Looking down didn't work -- try looking up
                    //向上查找可选择位置
                    selectablePos = lookForSelectablePosition(newPos, false);
                }
                if (selectablePos >= 0) {
                    //设置无效位置
                    setNextSelectedPositionInt(selectablePos);
                    //检查选择状态变化
                    checkSelectionChanged();
                    found = true;
                }
            }
        }
        if (!found) {
            // Nothing is selected
            mSelectedPosition = INVALID_POSITION;
            mSelectedRowId = INVALID_ROW_ID;
            mNextSelectedPosition = INVALID_POSITION;
            mNextSelectedRowId = INVALID_ROW_ID;
            mNeedSync = false;
            checkSelectionChanged();
        }
        //知无障碍状态变化
        notifySubtreeAccessibilityStateChangedIfNeeded();
    }

    /**
     * Called after layout to determine whether the selection position needs to
     * be updated. Also used to fire any pending selection events.
     * 检查选择状态变化：在布局完成后确定是否需要更新选中位置，并触发任何待处理的选择事件
     */
    void checkSelectionChanged() {
        //检查选中位置是否发生变化                                  检查选中行ID是否发生变化
        if ((mSelectedPosition != mOldSelectedPosition) || (mSelectedRowId != mOldSelectedRowId)) {
            //触发选择状态变化处理
            selectionChanged();
            mOldSelectedPosition = mSelectedPosition;
            mOldSelectedRowId = mSelectedRowId;
        }

        // If we have a pending selection notification -- and we won't if we
        // just fired one in selectionChanged() -- run it now.
        //检查是否有待处理的选择通知
        if (mPendingSelectionNotifier != null) {
            //执行待处理的通知
            mPendingSelectionNotifier.run();
        }
    }

    /**
     * Searches the adapter for a position matching mSyncRowId. The search starts at mSyncPosition
     * and then alternates between moving up and moving down until 1) we find the right position, or
     * 2) we run out of time, or 3) we have looked at every position
     *
     * @return Position of the row that matches mSyncRowId, or {@link #INVALID_POSITION} if it can't
     *         be found
     * 查找同步位置：在适配器中搜索匹配 mSyncRowId 的位置，搜索从 mSyncPosition 开始，然后在向上和向下移动之间交替进行
     */
    int findSyncPosition() {
        int count = mItemCount;

        if (count == 0) {
            return INVALID_POSITION;
        }

        long idToMatch = mSyncRowId;
        int seed = mSyncPosition;

        // If there isn't a selection don't hunt for it
        //检查同步行ID是否有效
        if (idToMatch == INVALID_ROW_ID) {
            return INVALID_POSITION;
        }

        // Pin seed to reasonable values
        //限制种子位置在合理范围内
        seed = Math.max(0, seed);
        seed = Math.min(count - 1, seed);

        //设置最大搜索时间
        long endTime = SystemClock.uptimeMillis() + SYNC_MAX_DURATION_MILLIS;

        long rowId;

        // first position scanned so far
        int first = seed;

        // last position scanned so far
        int last = seed;

        // True if we should move down on the next iteration
        //控制下次迭代时移动方向
        boolean next = false;

        // True when we have looked at the first item in the data
        boolean hitFirst;

        // True when we have looked at the last item in the data
        boolean hitLast;

        // Get the item ID locally (instead of getItemIdAtPosition), so
        // we need the adapter
        T adapter = getAdapter();
        if (adapter == null) {
            return INVALID_POSITION;
        }

        ////时间超时
        while (SystemClock.uptimeMillis() <= endTime) {
            rowId = adapter.getItemId(seed);
            //检查是否找到匹配的行ID
            if (rowId == idToMatch) {
                // Found it!
                return seed;
            }

            //跟踪是否已到达数据的开头或结尾
            hitLast = last == count - 1;
            hitFirst = first == 0;

            //已搜索所有位置
            if (hitLast && hitFirst) {
                // Looked at everything
                break;
            }


            if (hitFirst || (next && !hitLast)) {
                // Either we hit the top, or we are trying to move down
                last++;//向不同方向扩展搜索范围
                seed = last;
                // Try going up next time
                next = false;
            } else if (hitLast || (!next && !hitFirst)) {
                // Either we hit the bottom, or we are trying to move up
                first--;//向不同方向扩展搜索范围
                seed = first;
                // Try going down next time
                next = true;
            }

        }

        return INVALID_POSITION;
    }

    /**
     * Find a position that can be selected (i.e., is not a separator).
     *
     * @param position The starting position to look at.
     * @param lookDown Whether to look down for other positions.
     * @return The next selectable position starting at position and then searching either up or
     *         down. Returns {@link #INVALID_POSITION} if nothing can be found.
     * 查找可选择位置：查找一个可以被选中的位置（即不是分隔符的位置）
     */
    int lookForSelectablePosition(int position, boolean lookDown) {
        return position;//从指定位置开始，向上或向下搜索的下一个可选择位置
    }

    /**
     * Utility to keep mSelectedPosition and mSelectedRowId in sync
     * @param position Our current position
     * 设置选中位置：用于同步更新 mSelectedPosition 和 mSelectedRowId 字段
     */
    void setSelectedPositionInt(int position) {
        mSelectedPosition = position;//设置当前选中位置
        mSelectedRowId = getItemIdAtPosition(position);//获取并设置对应位置的数据项ID
    }

    /**
     * Utility to keep mNextSelectedPosition and mNextSelectedRowId in sync
     * @param position Intended value for mSelectedPosition the next time we go
     * through layout
     * 设置下一个选中位置：用于同步更新 mNextSelectedPosition 和 mNextSelectedRowId 字段
     */
    void setNextSelectedPositionInt(int position) {
        mNextSelectedPosition = position;//设置下一个选中位置
        mNextSelectedRowId = getItemIdAtPosition(position);//获取并设置对应位置的数据项ID
        // If we are trying to sync to the selection, update that too
        if (mNeedSync && mSyncMode == SYNC_SELECTED_POSITION && position >= 0) {
            mSyncPosition = position;
            mSyncRowId = mNextSelectedRowId;
        }
    }

    /**
     * Remember enough information to restore the screen state when the data has
     * changed.
     * 记住同步状态：保存足够的信息以便在数据变化后恢复屏幕状态
     */
    void rememberSyncState() {
        //确保有子视图存在
        if (getChildCount() > 0) {
            mNeedSync = true;//标记需要同步
            mSyncHeight = mLayoutHeight;//保存布局高度
            if (mSelectedPosition >= 0) {
                // Sync the selection state
                View v = getChildAt(mSelectedPosition - mFirstPosition);//视图获取
                //状态保存
                mSyncRowId = mNextSelectedRowId;
                mSyncPosition = mNextSelectedPosition;
                if (v != null) {
                    mSpecificTop = v.getTop();//视图顶部位置
                }
                mSyncMode = SYNC_SELECTED_POSITION;
            } else {
                // Sync the based on the offset of the first view
                View v = getChildAt(0);//视图获取
                T adapter = getAdapter();
                if (mFirstPosition >= 0 && mFirstPosition < adapter.getCount()) {
                    mSyncRowId = adapter.getItemId(mFirstPosition);//适配器中对应位置的ID
                } else {
                    mSyncRowId = NO_ID;
                }
                mSyncPosition = mFirstPosition;
                if (v != null) {
                    mSpecificTop = v.getTop();
                }
                mSyncMode = SYNC_FIRST_POSITION;
            }
        }
    }

    /** @hide */
    //编码属性：为 AdapterView 的视图层次结构编码关键属性
    @Override
    protected void encodeProperties(@NonNull ViewHierarchyEncoder encoder) {
        super.encodeProperties(encoder);//首先编码父类属性

        encoder.addProperty("scrolling:firstPosition", mFirstPosition);
        encoder.addProperty("list:nextSelectedPosition", mNextSelectedPosition);
        encoder.addProperty("list:nextSelectedRowId", mNextSelectedRowId);
        encoder.addProperty("list:selectedPosition", mSelectedPosition);
        encoder.addProperty("list:itemCount", mItemCount);
    }

    /**
     * {@inheritDoc}
     *
     * <p>It also sets the autofill options in the structure; when overridden, it should set it as
     * well, either explicitly by calling {@link ViewStructure#setAutofillOptions(CharSequence[])}
     * or implicitly by calling {@code super.onProvideAutofillStructure(structure, flags)}.
     * 提供自动填充结构：为自动填充服务提供 AdapterView 的结构信息
     */
    @Override
    public void onProvideAutofillStructure(ViewStructure structure, int flags) {
        super.onProvideAutofillStructure(structure, flags);

        final Adapter adapter = getAdapter();//获取关联的适配器
        if (adapter == null) return;

        final CharSequence[] options = adapter.getAutofillOptions();//获取适配器的自动填充选项
        if (options != null) {
            structure.setAutofillOptions(options);//设置结构的自动填充选项
        }
    }
}
