/*
 * Copyright (C) 2007 The Android Open Source Project
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

import android.annotation.Nullable;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;

/**
 * Common base class of common implementation for an {@link Adapter} that can be
 * used in both {@link ListView} (by implementing the specialized
 * {@link ListAdapter} interface) and {@link Spinner} (by implementing the
 * specialized {@link SpinnerAdapter} interface).
 * Android 中一个通用的适配器基类，实现了 ListAdapter 和 SpinnerAdapter 接口，可用于 ListView 和 Spinner 等组件。
 */
public abstract class BaseAdapter implements ListAdapter, SpinnerAdapter {
    private final DataSetObservable mDataSetObservable = new DataSetObservable();//用于管理数据集观察者，实现观察者模式
    private CharSequence[] mAutofillOptions;//存储自动填充选项数据

    public boolean hasStableIds() {
        return false;
    }
    
    public void registerDataSetObserver(DataSetObserver observer) {
        mDataSetObservable.registerObserver(observer);
    }

    public void unregisterDataSetObserver(DataSetObserver observer) {
        mDataSetObservable.unregisterObserver(observer);
    }
    
    /**
     * Notifies the attached observers that the underlying data has been changed
     * and any View reflecting the data set should refresh itself.
     * 通知所有注册的观察者数据集已发生变更
     */
    public void notifyDataSetChanged() {
        mDataSetObservable.notifyChanged();
    }

    /**
     * Notifies the attached observers that the underlying data is no longer valid
     * or available. Once invoked this adapter is no longer valid and should
     * not report further data set changes.
     * 通知所有注册的观察者数据集已失效或不可用
     */
    public void notifyDataSetInvalidated() {
        mDataSetObservable.notifyInvalidated();
    }

    //判断适配器中所有项目是否都处于启用状态
    public boolean areAllItemsEnabled() {
        return true;
    }

    //判断指定位置的项目是否启用
    public boolean isEnabled(int position) {
        return true;
    }

    //获取下拉列表中显示的视图
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getView(position, convertView, parent);
    }

    public int getItemViewType(int position) {
        return 0;
    }

    public int getViewTypeCount() {
        return 1;
    }
    
    public boolean isEmpty() {
        return getCount() == 0;
    }

    //获取自动填充选项数组
    @Override
    public CharSequence[] getAutofillOptions() {
        return mAutofillOptions;
    }

    /**
     * Sets the value returned by {@link #getAutofillOptions()}
     * 设置自动填充选项
     */
    public void setAutofillOptions(@Nullable CharSequence... options) {
        mAutofillOptions = options;
    }
}
