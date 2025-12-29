/*
** Copyright 2009, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

package android.accessibilityservice;

import android.accessibilityservice.IAccessibilityServiceConnection;
import android.graphics.Region;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityWindowInfo;
import android.view.KeyEvent;

/**
 * Top-level interface to an accessibility service component.
 *无障碍服务客户端的顶级接口
 定义系统与无障碍服务之间的通信协议
 * @hide
 */
 oneway interface IAccessibilityServiceClient {

    //初始化服务连接
    void init(in IAccessibilityServiceConnection connection, int connectionId, IBinder windowToken);

    void onAccessibilityEvent(in AccessibilityEvent event, in boolean serviceWantsEvent);

    //中断无障碍反馈
    void onInterrupt();

    void onGesture(int gesture);

    void clearAccessibilityCache();

    void onKeyEvent(in KeyEvent event, int sequence);

    void onMagnificationChanged(in Region region, float scale, float centerX, float centerY);

    void onSoftKeyboardShowModeChanged(int showMode);

    void onPerformGestureResult(int sequence, boolean completedSuccessfully);

    //处理指纹手势捕获状态变化
    void onFingerprintCapturingGesturesChanged(boolean capturing);

    void onFingerprintGesture(int gesture);//处理指纹手势

    void onAccessibilityButtonClicked();

    void onAccessibilityButtonAvailabilityChanged(boolean available);
}
