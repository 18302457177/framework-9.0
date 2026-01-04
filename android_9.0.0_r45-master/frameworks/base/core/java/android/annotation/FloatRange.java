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

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Denotes that the annotated element should be a float or double in the given range
 * <p>
 * Example:
 * <pre><code>
 *  &#64;FloatRange(from=0.0,to=1.0)
 *  public float getAlpha() {
 *      ...
 *  }
 * </code></pre>
 *
 * @hide
 * 浮点数范围标记：标记被注解的元素应为指定范围内的浮点数或双精度数
 */
@Retention(SOURCE)
@Target({METHOD,PARAMETER,FIELD,LOCAL_VARIABLE})
public @interface FloatRange {
    /** Smallest value. Whether it is inclusive or not is determined
     * by {@link #fromInclusive} */
    double from() default Double.NEGATIVE_INFINITY;//最小值
    /** Largest value. Whether it is inclusive or not is determined
     * by {@link #toInclusive} */
    double to() default Double.POSITIVE_INFINITY;//最大值

    /** Whether the from value is included in the range */
    boolean fromInclusive() default true;//是否包含最小值

    /** Whether the to value is included in the range */
    boolean toInclusive() default true;//是否包含最大值
}