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
package android.app;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.Intent;

/**
 * Interface used to control the instantiation of manifest elements.
 *
 * @see #instantiateApplication
 * @see #instantiateActivity
 * @see #instantiateService
 * @see #instantiateReceiver
 * @see #instantiateProvider
 * 组件实例化控制：提供统一接口来控制Android应用中各类组件的实例化过程
 * 依赖注入支持：允许应用覆盖默认的组件创建过程，以便进行依赖注入或类加载器更改
 * 组件生命周期管理：为以下五种主要组件提供实例化钩子：
 * instantiateApplication - 应用程序对象
 * instantiateActivity - 活动组件
 * instantiateService - 服务组件
 * instantiateReceiver - 广播接收器
 * instantiateProvider - 内容提供者
 */
public class AppComponentFactory {

    /**
     * Allows application to override the creation of the application object. This can be used to
     * perform things such as dependency injection or class loader changes to these
     * classes.
     * <p>
     * This method is only intended to provide a hook for instantiation. It does not provide
     * earlier access to the Application object. The returned object will not be initialized
     * as a Context yet and should not be used to interact with other android APIs.
     *
     * @param cl        The default classloader to use for instantiation.
     * @param className The class to be instantiated.
     */
    public @NonNull Application instantiateApplication(@NonNull ClassLoader cl,
            @NonNull String className)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return (Application) cl.loadClass(className).newInstance();
    }

    /**
     * Allows application to override the creation of activities. This can be used to
     * perform things such as dependency injection or class loader changes to these
     * classes.
     * <p>
     * This method is only intended to provide a hook for instantiation. It does not provide
     * earlier access to the Activity object. The returned object will not be initialized
     * as a Context yet and should not be used to interact with other android APIs.
     *
     * @param cl        The default classloader to use for instantiation.
     * @param className The class to be instantiated.
     * @param intent    Intent creating the class.
     */
    public @NonNull Activity instantiateActivity(@NonNull ClassLoader cl, @NonNull String className,
            @Nullable Intent intent)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return (Activity) cl.loadClass(className).newInstance();
    }

    /**
     * Allows application to override the creation of receivers. This can be used to
     * perform things such as dependency injection or class loader changes to these
     * classes.
     *
     * @param cl        The default classloader to use for instantiation.
     * @param className The class to be instantiated.
     * @param intent    Intent creating the class.
     */
    public @NonNull BroadcastReceiver instantiateReceiver(@NonNull ClassLoader cl,
            @NonNull String className, @Nullable Intent intent)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return (BroadcastReceiver) cl.loadClass(className).newInstance();
    }

    /**
     * Allows application to override the creation of services. This can be used to
     * perform things such as dependency injection or class loader changes to these
     * classes.
     * <p>
     * This method is only intended to provide a hook for instantiation. It does not provide
     * earlier access to the Service object. The returned object will not be initialized
     * as a Context yet and should not be used to interact with other android APIs.
     *
     * @param cl        The default classloader to use for instantiation.
     * @param className The class to be instantiated.
     * @param intent    Intent creating the class.
     */
    public @NonNull Service instantiateService(@NonNull ClassLoader cl,
            @NonNull String className, @Nullable Intent intent)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return (Service) cl.loadClass(className).newInstance();
    }

    /**
     * Allows application to override the creation of providers. This can be used to
     * perform things such as dependency injection or class loader changes to these
     * classes.
     * <p>
     * This method is only intended to provide a hook for instantiation. It does not provide
     * earlier access to the ContentProvider object. The returned object will not be initialized
     * with a Context yet and should not be used to interact with other android APIs.
     *
     * @param cl        The default classloader to use for instantiation.
     * @param className The class to be instantiated.
     */
    public @NonNull ContentProvider instantiateProvider(@NonNull ClassLoader cl,
            @NonNull String className)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return (ContentProvider) cl.loadClass(className).newInstance();
    }

    /**
     * @hide
     */
    public static final AppComponentFactory DEFAULT = new AppComponentFactory();
}
