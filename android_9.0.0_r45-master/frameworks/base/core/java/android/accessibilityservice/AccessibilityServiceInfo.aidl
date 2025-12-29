/*
 * Copyright (C) 2009 The Android Open Source Project
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
    Android接口定义语言文件，用于定义跨进程通信（IPC）的接口
 */

package android.accessibilityservice;
/*Parcelable声明
parcelable AccessibilityServiceInfo：
声明 AccessibilityServiceInfo 类型为可序列化
允许在不同进程间传递 AccessibilityServiceInfo 对象
实现跨进程数据传输功能*/
parcelable AccessibilityServiceInfo;
