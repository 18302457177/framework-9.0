/*
 * Copyright (C) 2016 The Android Open Source Project
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
 * limitations under the License
 */

package android.app;

import android.os.IRemoteCallback;
import android.os.RemoteException;

/**
 * @hide
 * 用户切换观察者的基类
 */
public class UserSwitchObserver extends IUserSwitchObserver.Stub {
    //用户切换时的回调
    @Override
    public void onUserSwitching(int newUserId, IRemoteCallback reply) throws RemoteException {
        if (reply != null) {
            reply.sendResult(null);
        }
    }

    //用户切换完成时的回调
    @Override
    public void onUserSwitchComplete(int newUserId) throws RemoteException {}

    //前台配置文件切换时的回调
    @Override
    public void onForegroundProfileSwitch(int newProfileId) throws RemoteException {}

    //锁定引导完成时的回调
    @Override
    public void onLockedBootComplete(int newUserId) throws RemoteException {}
}