/*
 * Copyright 2017 The Android Open Source Project
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

package android.app.servertransaction;

import android.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Request for lifecycle state that an activity should reach.
 * @hide
 * 状态管理：定义 Activity 生命周期状态的枚举和管理
 * 目标状态：通过抽象方法 getTargetState() 获取目标生命周期状态
 */
public abstract class ActivityLifecycleItem extends ClientTransactionItem {

    @IntDef(prefix = { "UNDEFINED", "PRE_", "ON_" }, value = {
            UNDEFINED,//未定义状态 (-1)
            PRE_ON_CREATE,//预创建状态 (0)
            ON_CREATE,//创建状态 (1)
            ON_START,//启动状态 (2)
            ON_RESUME,//恢复状态 (3)
            ON_PAUSE,//暂停状态 (4)
            ON_STOP,//停止状态 (5)
            ON_DESTROY,//销毁状态 (6)
            ON_RESTART//重启状态 (7)
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface LifecycleState{}
    public static final int UNDEFINED = -1;
    public static final int PRE_ON_CREATE = 0;
    public static final int ON_CREATE = 1;
    public static final int ON_START = 2;
    public static final int ON_RESUME = 3;
    public static final int ON_PAUSE = 4;
    public static final int ON_STOP = 5;
    public static final int ON_DESTROY = 6;
    public static final int ON_RESTART = 7;

    /** A final lifecycle state that an activity should reach. */
    @LifecycleState
    public abstract int getTargetState();

    /** Called by subclasses to make sure base implementation is cleaned up */
    @Override
    public void recycle() {
    }
}
