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

package android.annotation;

import android.content.Intent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Description of how the annotated broadcast action behaves.
 *
 * @hide
 * 广播行为描述：描述被注解的广播行为的特征和限制
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.SOURCE)
public @interface BroadcastBehavior {
    /**
     * This broadcast will only be delivered to an explicit target.
     *
     * @see Intent#setPackage(String)
     * @see Intent#setComponent(android.content.ComponentName)
     * 广播仅发送到明确目标
     */
    boolean explicitOnly() default false;

    /**
     * This broadcast will only be delivered to registered receivers.
     *
     * @see Intent#FLAG_RECEIVER_REGISTERED_ONLY
     * 广播仅发送到已注册的接收器
     */
    boolean registeredOnly() default false;

    /**
     * This broadcast will include all {@code AndroidManifest.xml} receivers
     * regardless of process state.
     *
     * @see Intent#FLAG_RECEIVER_INCLUDE_BACKGROUND
     * 广播包含所有 AndroidManifest.xml 接收器，不管进程状态
     */
    boolean includeBackground() default false;

    /**
     * This broadcast is protected and can only be sent by the OS.
     * 广播受保护，只能由系统发送
     */
    boolean protectedBroadcast() default false;
}
