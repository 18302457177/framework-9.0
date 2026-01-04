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
package android.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Denotes that any overriding methods should invoke this method as well.
 * <p>
 * Example:
 *
 * <pre>
 * <code>
 *  &#64;CallSuper
 *  public abstract void onFocusLost();
 * </code>
 * </pre>
 *
 * @memberDoc If you override this method you <em>must</em> call through to the
 *            superclass implementation.
 *            父类方法调用标记：标记被重写的方法应该同时调用父类方法
 * @hide
 */
@Retention(SOURCE)
@Target({METHOD})
public @interface CallSuper {
}
