/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.content.pm.IPackageManager;

/**
 * Special private access for certain globals related to a process.
 * @hide
 * 进程全局访问：提供对进程相关某些全局对象的特殊私有访问接口
 * 应用实例获取：通过 getInitialApplication() 方法获取进程中创建的第一个 Application 对象
 * 包名获取：通过 getInitialPackage() 方法返回加载到进程中的第一个 .apk 的包名
 * 包管理器访问：通过 getPackageManager() 方法获取包管理器的原始接口 IPackageManager
 * 核心设置获取：通过 getIntCoreSetting() 方法获取整数类型的核心设置值
 */
public class AppGlobals {
    /**
     * Return the first Application object made in the process.
     * NOTE: Only works on the main thread.
     */
    public static Application getInitialApplication() {
        return ActivityThread.currentApplication();
    }
    
    /**
     * Return the package name of the first .apk loaded into the process.
     * NOTE: Only works on the main thread.
     */
    public static String getInitialPackage() {
        return ActivityThread.currentPackageName();
    }

    /**
     * Return the raw interface to the package manager.
     * @return The package manager.
     */
    public static IPackageManager getPackageManager() {
        return ActivityThread.getPackageManager();
    }

    /**
     * Gets the value of an integer core setting.
     *
     * @param key The setting key.
     * @param defaultValue The setting default value.
     * @return The core settings.
     */
    public static int getIntCoreSetting(String key, int defaultValue) {
        ActivityThread currentActivityThread = ActivityThread.currentActivityThread();
        if (currentActivityThread != null) {
            return currentActivityThread.getIntCoreSetting(key, defaultValue);
        } else {
            return defaultValue;
        }
    }
}
