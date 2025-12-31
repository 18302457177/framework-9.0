/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.animation;

import android.os.SystemClock;
import android.util.ArrayMap;
import android.view.Choreographer;

import java.util.ArrayList;

/**
 * This custom, static handler handles the timing pulse that is shared by all active
 * ValueAnimators. This approach ensures that the setting of animation values will happen on the
 * same thread that animations start on, and that all animations will share the same times for
 * calculating their values, which makes synchronizing animations possible.
 * <p>
 * The handler uses the Choreographer by default for doing periodic callbacks. A custom
 * AnimationFrameCallbackProvider can be set on the handler to provide timing pulse that
 * may be independent of UI frame update. This could be useful in testing.
 * <p>
 * 这个自定义的静态处理程序处理所有活动 ValueAnimator 共享的定时脉冲。
 * 这种方法确保动画数值的设置发生在动画启动的同一线程上，
 * 并且所有动画将共享相同的时间来计算它们的数值，从而使动画同步成为可能。
 * <p>
 * 处理程序默认使用 Choreographer 来执行定期回调。
 * 可以在处理程序上设置自定义的 AnimationFrameCallbackProvider，
 * 以提供可能独立于 UI 帧更新的时序脉冲。这在测试中可能会很有用。
 *
 * @hide
 */

/**
 * Android 动画系统中的核心管理类
 * 主要功能
 * 定时脉冲管理 - 处理所有活动 ValueAnimator 共享的定时脉冲
 * 线程同步 - 确保动画数值设置发生在动画启动的同一线程上
 * 时间同步 - 使所有动画共享相同的时间来计算数值，实现动画同步
 */
public class AnimationHandler {
    /**
     * Internal per-thread collections used to avoid set collisions as animations start and end
     * while being processed.
     * @hide
     */
    private final ArrayMap<AnimationFrameCallback, Long> mDelayedCallbackStartTime =
            new ArrayMap<>();
    private final ArrayList<AnimationFrameCallback> mAnimationCallbacks =
            new ArrayList<>();
    private final ArrayList<AnimationFrameCallback> mCommitCallbacks =
            new ArrayList<>();
    private AnimationFrameCallbackProvider mProvider;

    private final Choreographer.FrameCallback mFrameCallback = new Choreographer.FrameCallback() {
        @Override
        public void doFrame(long frameTimeNanos) {
            doAnimationFrame(getProvider().getFrameTime());
            if (mAnimationCallbacks.size() > 0) {
                getProvider().postFrameCallback(this);
            }
        }
    };

    public final static ThreadLocal<AnimationHandler> sAnimatorHandler = new ThreadLocal<>();
    private boolean mListDirty = false;

    public static AnimationHandler getInstance() {
        if (sAnimatorHandler.get() == null) {
            sAnimatorHandler.set(new AnimationHandler());
        }
        return sAnimatorHandler.get();
    }

    /**
     * By default, the Choreographer is used to provide timing for frame callbacks. A custom
     * provider can be used here to provide different timing pulse.
     */
    public void setProvider(AnimationFrameCallbackProvider provider) {
        if (provider == null) {
            mProvider = new MyFrameCallbackProvider();
        } else {
            mProvider = provider;
        }
    }

    private AnimationFrameCallbackProvider getProvider() {
        if (mProvider == null) {
            mProvider = new MyFrameCallbackProvider();
        }
        return mProvider;
    }

    /**
     * Register to get a callback on the next frame after the delay.
     */
    public void addAnimationFrameCallback(final AnimationFrameCallback callback, long delay) {
        if (mAnimationCallbacks.size() == 0) {
            getProvider().postFrameCallback(mFrameCallback);
        }
        if (!mAnimationCallbacks.contains(callback)) {
            mAnimationCallbacks.add(callback);
        }

        if (delay > 0) {
            mDelayedCallbackStartTime.put(callback, (SystemClock.uptimeMillis() + delay));
        }
    }

    /**
     * Register to get a one shot callback for frame commit timing. Frame commit timing is the
     * time *after* traversals are done, as opposed to the animation frame timing, which is
     * before any traversals. This timing can be used to adjust the start time of an animation
     * when expensive traversals create big delta between the animation frame timing and the time
     * that animation is first shown on screen.
     *
     * Note this should only be called when the animation has already registered to receive
     * animation frame callbacks. This callback will be guaranteed to happen *after* the next
     * animation frame callback.
     * 功能概述
     * 单次提交回调注册 - 为帧提交时序注册一次性回调
     * 时序补偿机制 - 用于补偿动画开始时的昂贵遍历操作
     */
    public void addOneShotCommitCallback(final AnimationFrameCallback callback) {
        if (!mCommitCallbacks.contains(callback)) {
            mCommitCallbacks.add(callback);
        }
    }

    /**
     * Removes the given callback from the list, so it will no longer be called for frame related
     * timing.
     */
    public void removeCallback(AnimationFrameCallback callback) {
        mCommitCallbacks.remove(callback);
        mDelayedCallbackStartTime.remove(callback);
        int id = mAnimationCallbacks.indexOf(callback);
        if (id >= 0) {
            mAnimationCallbacks.set(id, null);
            mListDirty = true;
        }
    }

    //功能概述
    //动画帧处理 - 处理所有注册的动画回调，执行基于帧时间的动画更新
    //时间同步 - 确保所有活动动画使用相同的时间基准进行计算
    private void doAnimationFrame(long frameTime) {
        long currentTime = SystemClock.uptimeMillis();
        final int size = mAnimationCallbacks.size();
        for (int i = 0; i < size; i++) {
            final AnimationFrameCallback callback = mAnimationCallbacks.get(i);
            if (callback == null) {
                continue;
            }
            if (isCallbackDue(callback, currentTime)) {
                callback.doAnimationFrame(frameTime);
                if (mCommitCallbacks.contains(callback)) {
                    getProvider().postCommitCallback(new Runnable() {
                        @Override
                        public void run() {
                            commitAnimationFrame(callback, getProvider().getFrameTime());
                        }
                    });
                }
            }
        }
        cleanUpList();
    }

    private void commitAnimationFrame(AnimationFrameCallback callback, long frameTime) {
        if (!mDelayedCallbackStartTime.containsKey(callback) &&
                mCommitCallbacks.contains(callback)) {
            callback.commitAnimationFrame(frameTime);
            mCommitCallbacks.remove(callback);
        }
    }

    /**
     * Remove the callbacks from mDelayedCallbackStartTime once they have passed the initial delay
     * so that they can start getting frame callbacks.
     *
     * @return true if they have passed the initial delay or have no delay, false otherwise.
     * 延迟检查 - 检查动画回调是否已到达执行时间
     * 时间判断 - 判断是否已超过预设的延迟开始时间
     */
    private boolean isCallbackDue(AnimationFrameCallback callback, long currentTime) {
        Long startTime = mDelayedCallbackStartTime.get(callback);
        if (startTime == null) {
            return true;
        }
        if (startTime < currentTime) {
            mDelayedCallbackStartTime.remove(callback);
            return true;
        }
        return false;
    }

    /**
     * Return the number of callbacks that have registered for frame callbacks.
     */
    public static int getAnimationCount() {
        AnimationHandler handler = sAnimatorHandler.get();
        if (handler == null) {
            return 0;
        }
        return handler.getCallbackSize();
    }

    public static void setFrameDelay(long delay) {
        getInstance().getProvider().setFrameDelay(delay);
    }

    public static long getFrameDelay() {
        return getInstance().getProvider().getFrameDelay();
    }

    /*
    自动取消机制 - 基于 ObjectAnimator 的条件自动取消相关动画
    动画冲突处理 - 避免动画冲突，确保动画执行的一致性
*/
    void autoCancelBasedOn(ObjectAnimator objectAnimator) {
        for (int i = mAnimationCallbacks.size() - 1; i >= 0; i--) {
            AnimationFrameCallback cb = mAnimationCallbacks.get(i);
            if (cb == null) {
                continue;
            }
            if (objectAnimator.shouldAutoCancel(cb)) {
                ((Animator) mAnimationCallbacks.get(i)).cancel();
            }
        }
    }

    private void cleanUpList() {
        if (mListDirty) {
            for (int i = mAnimationCallbacks.size() - 1; i >= 0; i--) {
                if (mAnimationCallbacks.get(i) == null) {
                    mAnimationCallbacks.remove(i);
                }
            }
            mListDirty = false;
        }
    }

    /**
     * 有效回调计数 - 统计 mAnimationCallbacks 列表中非空回调的数量
     * 列表状态检查 - 计算实际有效的动画回调数量
     * @return
     */
    private int getCallbackSize() {
        int count = 0;
        int size = mAnimationCallbacks.size();
        for (int i = size - 1; i >= 0; i--) {
            if (mAnimationCallbacks.get(i) != null) {
                count++;
            }
        }
        return count;
    }

    /**
     * Default provider of timing pulse that uses Choreographer for frame callbacks.
     * 功能概述
     * 默认实现 - 作为 AnimationFrameCallbackProvider 接口的默认实现类
     * 帧同步机制 - 基于 Choreographer 提供与UI帧率同步的定时脉冲
     */
    private class MyFrameCallbackProvider implements AnimationFrameCallbackProvider {

        final Choreographer mChoreographer = Choreographer.getInstance();

        //通过 mChoreographer 发送帧回调
        @Override
        public void postFrameCallback(Choreographer.FrameCallback callback) {
            mChoreographer.postFrameCallback(callback);
        }

        // 发送提交回调到 Choreographer.CALLBACK_COMMIT 队列
        @Override
        public void postCommitCallback(Runnable runnable) {
            mChoreographer.postCallback(Choreographer.CALLBACK_COMMIT, runnable, null);
        }

        //时间获取 - 提供帧时间获取和帧延迟控制功能
        @Override
        public long getFrameTime() {
            return mChoreographer.getFrameTime();
        }

        //延迟控制 - 支持帧延迟设置和获取
        @Override
        public long getFrameDelay() {
            return Choreographer.getFrameDelay();
        }

        @Override
        public void setFrameDelay(long delay) {
            Choreographer.setFrameDelay(delay);
        }
    }

    /**
     * Callbacks that receives notifications for animation timing and frame commit timing.
     * 接收动画时序和帧提交时序通知的回调接口
     */
    interface AnimationFrameCallback {
        /**
         * Run animation based on the frame time.
         * @param frameTime The frame start time, in the {@link SystemClock#uptimeMillis()} time
         *                  base.
         * @return if the animation has finished.
         * 基于帧时间执行动画
         */
        boolean doAnimationFrame(long frameTime);

        /**
         * This notifies the callback of frame commit time. Frame commit time is the time after
         * traversals happen, as opposed to the normal animation frame time that is before
         * traversals. This is used to compensate expensive traversals that happen as the
         * animation starts. When traversals take a long time to complete, the rendering of the
         * initial frame will be delayed (by a long time). But since the startTime of the
         * animation is set before the traversal, by the time of next frame, a lot of time would
         * have passed since startTime was set, the animation will consequently skip a few frames
         * to respect the new frameTime. By having the commit time, we can adjust the start time to
         * when the first frame was drawn (after any expensive traversals) so that no frames
         * will be skipped.
         *
         * @param frameTime The frame time after traversals happen, if any, in the
         *                  {@link SystemClock#uptimeMillis()} time base.
         *                  处理帧提交时间通知
         */
        void commitAnimationFrame(long frameTime);
    }

    /**
     * The intention for having this interface is to increase the testability of ValueAnimator.
     * Specifically, we can have a custom implementation of the interface below and provide
     * timing pulse without using Choreographer. That way we could use any arbitrary interval for
     * our timing pulse in the tests.
     * 可测试性增强 - 为 ValueAnimator 提高可测试性而设计.
     * 时序脉冲抽象 - 提供与 Choreographer 解耦的时序脉冲机制
     *
     * @hide
     */
    public interface AnimationFrameCallbackProvider {
        void postFrameCallback(Choreographer.FrameCallback callback);

        void postCommitCallback(Runnable runnable);

        long getFrameTime();

        long getFrameDelay();

        void setFrameDelay(long delay);
    }
}
