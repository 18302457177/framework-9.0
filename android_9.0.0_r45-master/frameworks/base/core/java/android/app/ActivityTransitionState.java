/*
 * Copyright (C) 2014 The Android Open Source Project
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
package android.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.transition.Transition;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import com.android.internal.view.OneShotPreDrawListener;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * This class contains all persistence-related functionality for Activity Transitions.
 * Activities start exit and enter Activity Transitions through this class.
 * 状态管理：管理 Activity 转场过程中的所有持久化相关功能，包括进入和退出转场的状态信息
 * 共享元素处理：
 * 维护进入转场的共享元素名称列表（mEnteringNames）
 * 管理退出转场的共享元素映射关系（mExitingFrom、mExitingTo）
 * 转场协调器管理：
 * 管理 EnterTransitionCoordinator 和 ExitTransitionCoordinator 实例
 * 处理转场协调器的生命周期和状态
 * 状态保存与恢复：通过 saveState 和 readState 方法在 Activity 生命周期中保存和恢复转场状态
 */
class ActivityTransitionState {

    private static final String ENTERING_SHARED_ELEMENTS = "android:enteringSharedElements";

    private static final String EXITING_MAPPED_FROM = "android:exitingMappedFrom";

    private static final String EXITING_MAPPED_TO = "android:exitingMappedTo";

    /**
     * The shared elements that the calling Activity has said that they transferred to this
     * Activity.
     */
    private ArrayList<String> mEnteringNames;

    /**
     * The names of shared elements that were shared to the called Activity.
     */
    private ArrayList<String> mExitingFrom;

    /**
     * The names of local Views that were shared out, mapped to those elements in mExitingFrom.
     */
    private ArrayList<String> mExitingTo;

    /**
     * The local Views that were shared out, mapped to those elements in mExitingFrom.
     */
    private ArrayList<View> mExitingToView;

    /**
     * The ExitTransitionCoordinator used to start an Activity. Used to make the elements restore
     * Visibility of exited Views.
     */
    private ExitTransitionCoordinator mCalledExitCoordinator;

    /**
     * The ExitTransitionCoordinator used to return to a previous Activity when called with
     * {@link android.app.Activity#finishAfterTransition()}.
     */
    private ExitTransitionCoordinator mReturnExitCoordinator;

    /**
     * We must be able to cancel entering transitions to stop changing the Window to
     * opaque when we exit before making the Window opaque.
     */
    private EnterTransitionCoordinator mEnterTransitionCoordinator;

    /**
     * ActivityOptions used on entering this Activity.
     */
    private ActivityOptions mEnterActivityOptions;

    /**
     * Has an exit transition been started? If so, we don't want to double-exit.
     */
    private boolean mHasExited;

    /**
     * Postpone painting and starting the enter transition until this is false.
     */
    private boolean mIsEnterPostponed;

    /**
     * Potential exit transition coordinators.
     */
    private SparseArray<WeakReference<ExitTransitionCoordinator>> mExitTransitionCoordinators;

    /**
     * Next key for mExitTransitionCoordinator.
     */
    private int mExitTransitionCoordinatorsKey = 1;

    private boolean mIsEnterTriggered;

    public ActivityTransitionState() {
    }

    //管理多个退出转场协调器实例，支持跨任务转场场景，通过弱引用防止内存泄漏，并通过键值机制实现协调器的查找和移除
    public int addExitTransitionCoordinator(ExitTransitionCoordinator exitTransitionCoordinator) {
        if (mExitTransitionCoordinators == null) {
            mExitTransitionCoordinators =
                    new SparseArray<WeakReference<ExitTransitionCoordinator>>();
        }
        WeakReference<ExitTransitionCoordinator> ref = new WeakReference(exitTransitionCoordinator);
        // clean up old references:
        for (int i = mExitTransitionCoordinators.size() - 1; i >= 0; i--) {
            WeakReference<ExitTransitionCoordinator> oldRef
                    = mExitTransitionCoordinators.valueAt(i);
            if (oldRef.get() == null) {
                mExitTransitionCoordinators.removeAt(i);
            }
        }
        int newKey = mExitTransitionCoordinatorsKey++;
        mExitTransitionCoordinators.append(newKey, ref);
        return newKey;
    }

    //从 Bundle 对象中恢复转场状态信息
    //在 Activity 重建时恢复转场相关的状态信息，确保转场动画能够正确继续
    public void readState(Bundle bundle) {
        if (bundle != null) {
            if (mEnterTransitionCoordinator == null || mEnterTransitionCoordinator.isReturning()) {
                mEnteringNames = bundle.getStringArrayList(ENTERING_SHARED_ELEMENTS);
            }
            if (mEnterTransitionCoordinator == null) {
                mExitingFrom = bundle.getStringArrayList(EXITING_MAPPED_FROM);
                mExitingTo = bundle.getStringArrayList(EXITING_MAPPED_TO);
            }
        }
    }

    public void saveState(Bundle bundle) {
        if (mEnteringNames != null) {
            bundle.putStringArrayList(ENTERING_SHARED_ELEMENTS, mEnteringNames);
        }
        if (mExitingFrom != null) {
            bundle.putStringArrayList(EXITING_MAPPED_FROM, mExitingFrom);
            bundle.putStringArrayList(EXITING_MAPPED_TO, mExitingTo);
        }
    }

    public void setEnterActivityOptions(Activity activity, ActivityOptions options) {
        final Window window = activity.getWindow();
        if (window == null) {
            return;
        }
        // ensure Decor View has been created so that the window features are activated
        window.getDecorView();
        if (window.hasFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
                && options != null && mEnterActivityOptions == null
                && mEnterTransitionCoordinator == null
                && options.getAnimationType() == ActivityOptions.ANIM_SCENE_TRANSITION) {
            mEnterActivityOptions = options;
            mIsEnterTriggered = false;
            if (mEnterActivityOptions.isReturning()) {
                restoreExitedViews();
                int result = mEnterActivityOptions.getResultCode();
                if (result != 0) {
                    Intent intent = mEnterActivityOptions.getResultData();
                    if (intent != null) {
                        intent.setExtrasClassLoader(activity.getClassLoader());
                    }
                    activity.onActivityReenter(result, intent);
                }
            }
        }
    }

    //准备进入转场，初始化 EnterTransitionCoordinator 并启动转场
    public void enterReady(Activity activity) {
        if (mEnterActivityOptions == null || mIsEnterTriggered) {
            return;
        }
        mIsEnterTriggered = true;
        mHasExited = false;
        ArrayList<String> sharedElementNames = mEnterActivityOptions.getSharedElementNames();
        ResultReceiver resultReceiver = mEnterActivityOptions.getResultReceiver();
        if (mEnterActivityOptions.isReturning()) {
            restoreExitedViews();
            activity.getWindow().getDecorView().setVisibility(View.VISIBLE);
        }
        mEnterTransitionCoordinator = new EnterTransitionCoordinator(activity,
                resultReceiver, sharedElementNames, mEnterActivityOptions.isReturning(),
                mEnterActivityOptions.isCrossTask());
        if (mEnterActivityOptions.isCrossTask()) {
            mExitingFrom = new ArrayList<>(mEnterActivityOptions.getSharedElementNames());
            mExitingTo = new ArrayList<>(mEnterActivityOptions.getSharedElementNames());
        }

        if (!mIsEnterPostponed) {
            startEnter();
        }
    }

    //推迟进入转场动画的执行
    public void postponeEnterTransition() {
        mIsEnterPostponed = true;
    }

    public void startPostponedEnterTransition() {
        if (mIsEnterPostponed) {
            mIsEnterPostponed = false;
            if (mEnterTransitionCoordinator != null) {
                startEnter();
            }
        }
    }

    private void startEnter() {
        if (mEnterTransitionCoordinator.isReturning()) {
            if (mExitingToView != null) {
                mEnterTransitionCoordinator.viewInstancesReady(mExitingFrom, mExitingTo,
                        mExitingToView);
            } else {
                mEnterTransitionCoordinator.namedViewsReady(mExitingFrom, mExitingTo);
            }
        } else {
            mEnterTransitionCoordinator.namedViewsReady(null, null);
            mEnteringNames = mEnterTransitionCoordinator.getAllSharedElementNames();
        }

        mExitingFrom = null;
        mExitingTo = null;
        mExitingToView = null;
        mEnterActivityOptions = null;
    }

    public void onStop() {
        restoreExitedViews();
        if (mEnterTransitionCoordinator != null) {
            mEnterTransitionCoordinator.stop();
            mEnterTransitionCoordinator = null;
        }
        if (mReturnExitCoordinator != null) {
            mReturnExitCoordinator.stop();
            mReturnExitCoordinator = null;
        }
    }

    public void onResume(Activity activity, boolean isTopOfTask) {
        // After orientation change, the onResume can come in before the top Activity has
        // left, so if the Activity is not top, wait a second for the top Activity to exit.
        if (isTopOfTask || mEnterTransitionCoordinator == null) {
            restoreExitedViews();
            restoreReenteringViews();
        } else {
            activity.mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mEnterTransitionCoordinator == null ||
                            mEnterTransitionCoordinator.isWaitingForRemoteExit()) {
                        restoreExitedViews();
                        restoreReenteringViews();
                    }
                }
            }, 1000);
        }
    }

    public void clear() {
        mEnteringNames = null;
        mExitingFrom = null;
        mExitingTo = null;
        mExitingToView = null;
        mCalledExitCoordinator = null;
        mEnterTransitionCoordinator = null;
        mEnterActivityOptions = null;
        mExitTransitionCoordinators = null;
    }

    private void restoreExitedViews() {
        if (mCalledExitCoordinator != null) {
            mCalledExitCoordinator.resetViews();
            mCalledExitCoordinator = null;
        }
    }

    //恢复重新进入的视图状态，用于处理返回转场场景
    private void restoreReenteringViews() {
        if (mEnterTransitionCoordinator != null && mEnterTransitionCoordinator.isReturning() &&
                !mEnterTransitionCoordinator.isCrossTask()) {
            mEnterTransitionCoordinator.forceViewsToAppear();
            mExitingFrom = null;
            mExitingTo = null;
            mExitingToView = null;
        }
    }

    public boolean startExitBackTransition(final Activity activity) {
        if (mEnteringNames == null || mCalledExitCoordinator != null) {
            return false;
        } else {
            if (!mHasExited) {
                mHasExited = true;
                Transition enterViewsTransition = null;
                ViewGroup decor = null;
                boolean delayExitBack = false;
                if (mEnterTransitionCoordinator != null) {
                    enterViewsTransition = mEnterTransitionCoordinator.getEnterViewsTransition();
                    decor = mEnterTransitionCoordinator.getDecor();
                    delayExitBack = mEnterTransitionCoordinator.cancelEnter();
                    mEnterTransitionCoordinator = null;
                    if (enterViewsTransition != null && decor != null) {
                        enterViewsTransition.pause(decor);
                    }
                }

                mReturnExitCoordinator = new ExitTransitionCoordinator(activity,
                        activity.getWindow(), activity.mEnterTransitionListener, mEnteringNames,
                        null, null, true);
                if (enterViewsTransition != null && decor != null) {
                    enterViewsTransition.resume(decor);
                }
                if (delayExitBack && decor != null) {
                    final ViewGroup finalDecor = decor;
                    OneShotPreDrawListener.add(decor, () -> {
                        if (mReturnExitCoordinator != null) {
                            mReturnExitCoordinator.startExit(activity.mResultCode,
                                    activity.mResultData);
                        }
                    });
                } else {
                    mReturnExitCoordinator.startExit(activity.mResultCode, activity.mResultData);
                }
            }
            return true;
        }
    }

    public boolean isTransitionRunning() {
        // Note that *only* enter *or* exit will be running at any given time
        if (mEnterTransitionCoordinator != null) {
            if (mEnterTransitionCoordinator.isTransitionRunning()) {
                return true;
            }
        }
        if (mCalledExitCoordinator != null) {
            if (mCalledExitCoordinator.isTransitionRunning()) {
                return true;
            }
        }
        if (mReturnExitCoordinator != null) {
            if (mReturnExitCoordinator.isTransitionRunning()) {
                return true;
            }
        }
        return false;
    }

    public void startExitOutTransition(Activity activity, Bundle options) {
        mEnterTransitionCoordinator = null;
        if (!activity.getWindow().hasFeature(Window.FEATURE_ACTIVITY_TRANSITIONS) ||
                mExitTransitionCoordinators == null) {
            return;
        }
        ActivityOptions activityOptions = new ActivityOptions(options);
        if (activityOptions.getAnimationType() == ActivityOptions.ANIM_SCENE_TRANSITION) {
            int key = activityOptions.getExitCoordinatorKey();
            int index = mExitTransitionCoordinators.indexOfKey(key);
            if (index >= 0) {
                mCalledExitCoordinator = mExitTransitionCoordinators.valueAt(index).get();
                mExitTransitionCoordinators.removeAt(index);
                if (mCalledExitCoordinator != null) {
                    mExitingFrom = mCalledExitCoordinator.getAcceptedNames();
                    mExitingTo = mCalledExitCoordinator.getMappedNames();
                    mExitingToView = mCalledExitCoordinator.copyMappedViews();
                    mCalledExitCoordinator.startExit();
                }
            }
        }
    }
}
