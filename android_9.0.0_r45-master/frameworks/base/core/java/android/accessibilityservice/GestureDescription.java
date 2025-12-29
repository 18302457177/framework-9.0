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

package android.accessibilityservice;

import android.annotation.IntRange;
import android.annotation.NonNull;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.internal.util.Preconditions;

import java.util.ArrayList;
import java.util.List;

/**
 * Accessibility services with the
 * {@link android.R.styleable#AccessibilityService_canPerformGestures} property can dispatch
 * gestures. This class describes those gestures. Gestures are made up of one or more strokes.
 * Gestures are immutable once built.
 * <p>
 * Spatial dimensions throughout are in screen pixels. Time is measured in milliseconds.
 * 描述无障碍服务中执行的手势操作
 */
public final class GestureDescription {
    /** Gestures may contain no more than this many strokes */
    private static final int MAX_STROKE_COUNT = 10;

    /**
     * Upper bound on total gesture duration. Nearly all gestures will be much shorter.
     */
    private static final long MAX_GESTURE_DURATION_MS = 60 * 1000;

    private final List<StrokeDescription> mStrokes = new ArrayList<>();
    private final float[] mTempPos = new float[2];

    /**
     * Get the upper limit for the number of strokes a gesture may contain.
     *
     * @return The maximum number of strokes.
     */
    public static int getMaxStrokeCount() {
        return MAX_STROKE_COUNT;
    }

    /**
     * Get the upper limit on a gesture's duration.
     *
     * @return The maximum duration in milliseconds.
     */
    public static long getMaxGestureDuration() {
        return MAX_GESTURE_DURATION_MS;
    }

    private GestureDescription() {}

    private GestureDescription(List<StrokeDescription> strokes) {
        mStrokes.addAll(strokes);
    }

    /**
     * Get the number of stroke in the gesture.
     *
     * @return the number of strokes in this gesture
     */
    public int getStrokeCount() {
        return mStrokes.size();
    }

    /**
     * Read a stroke from the gesture
     *
     * @param index the index of the stroke
     *
     * @return A description of the stroke.
     */
    public StrokeDescription getStroke(@IntRange(from = 0) int index) {
        return mStrokes.get(index);
    }

    /**
     * Return the smallest key point (where a path starts or ends) that is at least a specified
     * offset
     * @param offset the minimum start time
     * @return The next key time that is at least the offset or -1 if one can't be found
     * 查找时间偏移量至少为指定值的最小关键时间点
     */
    private long getNextKeyPointAtLeast(long offset) {
        long nextKeyPoint = Long.MAX_VALUE;
        for (int i = 0; i < mStrokes.size(); i++) {
            long thisStartTime = mStrokes.get(i).mStartTime;
            if ((thisStartTime < nextKeyPoint) && (thisStartTime >= offset)) {
                nextKeyPoint = thisStartTime;
            }
            long thisEndTime = mStrokes.get(i).mEndTime;
            if ((thisEndTime < nextKeyPoint) && (thisEndTime >= offset)) {
                nextKeyPoint = thisEndTime;
            }
        }
        return (nextKeyPoint == Long.MAX_VALUE) ? -1L : nextKeyPoint;
    }

    /**
     * Get the points that correspond to a particular moment in time.
     * @param time The time of interest
     * @param touchPoints An array to hold the current touch points. Must be preallocated to at
     * least the number of paths in the gesture to prevent going out of bounds
     * @return The number of points found, and thus the number of elements set in each array
     */
    private int getPointsForTime(long time, TouchPoint[] touchPoints) {
        int numPointsFound = 0;
        for (int i = 0; i < mStrokes.size(); i++) {
            StrokeDescription strokeDescription = mStrokes.get(i);
            if (strokeDescription.hasPointForTime(time)) {
                touchPoints[numPointsFound].mStrokeId = strokeDescription.getId();
                touchPoints[numPointsFound].mContinuedStrokeId =
                        strokeDescription.getContinuedStrokeId();
                touchPoints[numPointsFound].mIsStartOfPath =
                        (strokeDescription.getContinuedStrokeId() < 0)
                                && (time == strokeDescription.mStartTime);
                touchPoints[numPointsFound].mIsEndOfPath = !strokeDescription.willContinue()
                        && (time == strokeDescription.mEndTime);
                strokeDescription.getPosForTime(time, mTempPos);
                touchPoints[numPointsFound].mX = Math.round(mTempPos[0]);
                touchPoints[numPointsFound].mY = Math.round(mTempPos[1]);
                numPointsFound++;
            }
        }
        return numPointsFound;
    }

    // Total duration assumes that the gesture starts at 0; waiting around to start a gesture
    // counts against total duration
    private static long getTotalDuration(List<StrokeDescription> paths) {
        long latestEnd = Long.MIN_VALUE;
        for (int i = 0; i < paths.size(); i++) {
            StrokeDescription path = paths.get(i);
            latestEnd = Math.max(latestEnd, path.mEndTime);
        }
        return Math.max(latestEnd, 0);
    }

    /**
     * Builder for a {@code GestureDescription}
     * 构建 GestureDescription 对象
     * 提供建造者模式来逐步构建手势描述
     */
    public static class Builder {

        private final List<StrokeDescription> mStrokes = new ArrayList<>();

        /**
         * Add a stroke to the gesture description. Up to
         * {@link GestureDescription#getMaxStrokeCount()} paths may be
         * added to a gesture, and the total gesture duration (earliest path start time to latest
         * path end time) may not exceed {@link GestureDescription#getMaxGestureDuration()}.
         *
         * @param strokeDescription the stroke to add.
         *
         * @return this
         */
        public Builder addStroke(@NonNull StrokeDescription strokeDescription) {
            if (mStrokes.size() >= MAX_STROKE_COUNT) {
                throw new IllegalStateException(
                        "Attempting to add too many strokes to a gesture");
            }

            mStrokes.add(strokeDescription);

            if (getTotalDuration(mStrokes) > MAX_GESTURE_DURATION_MS) {
                mStrokes.remove(strokeDescription);
                throw new IllegalStateException(
                        "Gesture would exceed maximum duration with new stroke");
            }
            return this;
        }

        public GestureDescription build() {
            if (mStrokes.size() == 0) {
                throw new IllegalStateException("Gestures must have at least one stroke");
            }
            return new GestureDescription(mStrokes);
        }
    }

    /**
     * Immutable description of stroke that can be part of a gesture.
     * 定义手势中单个笔画的路径、时间和行为特性
     */
    public static class StrokeDescription {
        private static final int INVALID_STROKE_ID = -1;

        static int sIdCounter;

        Path mPath;//笔画的路径
        long mStartTime;
        long mEndTime;
        private float mTimeToLengthConversion;//时间到长度的转换系数
        private PathMeasure mPathMeasure;//路径测量器
        // The tap location is only set for zero-length paths
        float[] mTapLocation;//零长度路径的点击位置
        int mId;
        boolean mContinued;
        int mContinuedStrokeId = INVALID_STROKE_ID;

        /**
         * @param path The path to follow. Must have exactly one contour. The bounds of the path
         * must not be negative. The path must not be empty. If the path has zero length
         * (for example, a single {@code moveTo()}), the stroke is a touch that doesn't move.
         * @param startTime The time, in milliseconds, from the time the gesture starts to the
         * time the stroke should start. Must not be negative.
         * @param duration The duration, in milliseconds, the stroke takes to traverse the path.
         * Must be positive.
         */
        public StrokeDescription(@NonNull Path path,
                @IntRange(from = 0) long startTime,
                @IntRange(from = 0) long duration) {
            this(path, startTime, duration, false);
        }

        /**
         * @param path The path to follow. Must have exactly one contour. The bounds of the path
         * must not be negative. The path must not be empty. If the path has zero length
         * (for example, a single {@code moveTo()}), the stroke is a touch that doesn't move.
         * @param startTime The time, in milliseconds, from the time the gesture starts to the
         * time the stroke should start. Must not be negative.
         * @param duration The duration, in milliseconds, the stroke takes to traverse the path.
         * Must be positive.
         * @param willContinue {@code true} if this stroke will be continued by one in the
         * next gesture {@code false} otherwise. Continued strokes keep their pointers down when
         * the gesture completes.
         */
        public StrokeDescription(@NonNull Path path,
                @IntRange(from = 0) long startTime,
                @IntRange(from = 0) long duration,
                boolean willContinue) {
            mContinued = willContinue;
            Preconditions.checkArgument(duration > 0, "Duration must be positive");
            Preconditions.checkArgument(startTime >= 0, "Start time must not be negative");
            Preconditions.checkArgument(!path.isEmpty(), "Path is empty");
            RectF bounds = new RectF();
            path.computeBounds(bounds, false /* unused */);
            Preconditions.checkArgument((bounds.bottom >= 0) && (bounds.top >= 0)
                    && (bounds.right >= 0) && (bounds.left >= 0),
                    "Path bounds must not be negative");
            mPath = new Path(path);
            mPathMeasure = new PathMeasure(path, false);
            if (mPathMeasure.getLength() == 0) {
                // Treat zero-length paths as taps
                Path tempPath = new Path(path);
                tempPath.lineTo(-1, -1);
                mTapLocation = new float[2];
                PathMeasure pathMeasure = new PathMeasure(tempPath, false);
                pathMeasure.getPosTan(0, mTapLocation, null);
            }
            if (mPathMeasure.nextContour()) {
                throw new IllegalArgumentException("Path has more than one contour");
            }
            /*
             * Calling nextContour has moved mPathMeasure off the first contour, which is the only
             * one we care about. Set the path again to go back to the first contour.
             */
            mPathMeasure.setPath(mPath, false);
            mStartTime = startTime;
            mEndTime = startTime + duration;
            mTimeToLengthConversion = getLength() / duration;
            mId = sIdCounter++;
        }

        /**
         * Retrieve a copy of the path for this stroke
         *
         * @return A copy of the path
         */
        public Path getPath() {
            return new Path(mPath);
        }

        /**
         * Get the stroke's start time
         *
         * @return the start time for this stroke.
         */
        public long getStartTime() {
            return mStartTime;
        }

        /**
         * Get the stroke's duration
         *
         * @return the duration for this stroke
         */
        public long getDuration() {
            return mEndTime - mStartTime;
        }

        /**
         * Get the stroke's ID. The ID is used when a stroke is to be continued by another
         * stroke in a future gesture.
         *
         * @return the ID of this stroke
         * @hide
         */
        public int getId() {
            return mId;
        }

        /**
         * Create a new stroke that will continue this one. This is only possible if this stroke
         * will continue.
         *
         * @param path The path for the stroke that continues this one. The starting point of
         *             this path must match the ending point of the stroke it continues.
         * @param startTime The time, in milliseconds, from the time the gesture starts to the
         *                  time this stroke should start. Must not be negative. This time is from
         *                  the start of the new gesture, not the one being continued.
         * @param duration The duration for the new stroke. Must not be negative.
         * @param willContinue {@code true} if this stroke will be continued by one in the
         *             next gesture {@code false} otherwise.
         * @return
         * 创建继续的笔画
         */
        public StrokeDescription continueStroke(Path path, long startTime, long duration,
                boolean willContinue) {
            if (!mContinued) {
                throw new IllegalStateException(
                        "Only strokes marked willContinue can be continued");
            }
            StrokeDescription strokeDescription =
                    new StrokeDescription(path, startTime, duration, willContinue);
            strokeDescription.mContinuedStrokeId = mId;
            return strokeDescription;
        }

        /**
         * Check if this stroke is marked to continue in the next gesture.
         *
         * @return {@code true} if the stroke is to be continued.
         */
        public boolean willContinue() {
            return mContinued;
        }

        /**
         * Get the ID of the stroke that this one will continue.
         *
         * @return The ID of the stroke that this stroke continues, or 0 if no such stroke exists.
         * @hide
         */
        public int getContinuedStrokeId() {
            return mContinuedStrokeId;
        }

        float getLength() {
            return mPathMeasure.getLength();
        }

        /* Assumes hasPointForTime returns true */
        boolean getPosForTime(long time, float[] pos) {//获取指定时间的位置
            if (mTapLocation != null) {
                pos[0] = mTapLocation[0];
                pos[1] = mTapLocation[1];
                return true;
            }
            if (time == mEndTime) {
                // Close to the end time, roundoff can be a problem
                return mPathMeasure.getPosTan(getLength(), pos, null);
            }
            float length = mTimeToLengthConversion * ((float) (time - mStartTime));
            return mPathMeasure.getPosTan(length, pos, null);
        }

        //检查时间点是否在笔画时间内
        boolean hasPointForTime(long time) {
            return ((time >= mStartTime) && (time <= mEndTime));
        }
    }

    /**
     * The location of a finger for gesture dispatch
     *记录特定时刻的触摸点信息
     * @hide
     */
    public static class TouchPoint implements Parcelable {
        private static final int FLAG_IS_START_OF_PATH = 0x01;//表示路径起始点标志
        private static final int FLAG_IS_END_OF_PATH = 0x02;//表示路径结束点标志

        public int mStrokeId;//笔画ID
        public int mContinuedStrokeId;//被继续的笔画ID
        public boolean mIsStartOfPath;//是否为路径起始点
        public boolean mIsEndOfPath;//是否为路径结束点
        //触摸点坐标
        public float mX;
        public float mY;

        public TouchPoint() {
        }

        public TouchPoint(TouchPoint pointToCopy) {
            copyFrom(pointToCopy);
        }

        public TouchPoint(Parcel parcel) {
            mStrokeId = parcel.readInt();
            mContinuedStrokeId = parcel.readInt();
            int startEnd = parcel.readInt();
            mIsStartOfPath = (startEnd & FLAG_IS_START_OF_PATH) != 0;
            mIsEndOfPath = (startEnd & FLAG_IS_END_OF_PATH) != 0;
            mX = parcel.readFloat();
            mY = parcel.readFloat();
        }

        public void copyFrom(TouchPoint other) {
            mStrokeId = other.mStrokeId;
            mContinuedStrokeId = other.mContinuedStrokeId;
            mIsStartOfPath = other.mIsStartOfPath;
            mIsEndOfPath = other.mIsEndOfPath;
            mX = other.mX;
            mY = other.mY;
        }

        @Override
        public String toString() {
            return "TouchPoint{"
                    + "mStrokeId=" + mStrokeId
                    + ", mContinuedStrokeId=" + mContinuedStrokeId
                    + ", mIsStartOfPath=" + mIsStartOfPath
                    + ", mIsEndOfPath=" + mIsEndOfPath
                    + ", mX=" + mX
                    + ", mY=" + mY
                    + '}';
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(mStrokeId);
            dest.writeInt(mContinuedStrokeId);
            int startEnd = mIsStartOfPath ? FLAG_IS_START_OF_PATH : 0;
            startEnd |= mIsEndOfPath ? FLAG_IS_END_OF_PATH : 0;
            dest.writeInt(startEnd);
            dest.writeFloat(mX);
            dest.writeFloat(mY);
        }

        public static final Parcelable.Creator<TouchPoint> CREATOR
                = new Parcelable.Creator<TouchPoint>() {
            public TouchPoint createFromParcel(Parcel in) {
                return new TouchPoint(in);
            }

            public TouchPoint[] newArray(int size) {
                return new TouchPoint[size];
            }
        };
    }

    /**
     * A step along a gesture. Contains all of the touch points at a particular time
     *将手势分解为时间序列上的离散步骤
     * @hide
     */
    public static class GestureStep implements Parcelable {
        public long timeSinceGestureStart;  //从手势开始的时间
        public int numTouchPoints;//触摸点数量
        public TouchPoint[] touchPoints;//触摸点数组

        public GestureStep(long timeSinceGestureStart, int numTouchPoints,
                TouchPoint[] touchPointsToCopy) {
            this.timeSinceGestureStart = timeSinceGestureStart;
            this.numTouchPoints = numTouchPoints;
            this.touchPoints = new TouchPoint[numTouchPoints];
            for (int i = 0; i < numTouchPoints; i++) {
                this.touchPoints[i] = new TouchPoint(touchPointsToCopy[i]);
            }
        }

        public GestureStep(Parcel parcel) {
            timeSinceGestureStart = parcel.readLong();
            Parcelable[] parcelables =
                    parcel.readParcelableArray(TouchPoint.class.getClassLoader());
            numTouchPoints = (parcelables == null) ? 0 : parcelables.length;
            touchPoints = new TouchPoint[numTouchPoints];
            for (int i = 0; i < numTouchPoints; i++) {
                touchPoints[i] = (TouchPoint) parcelables[i];
            }
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeLong(timeSinceGestureStart);
            dest.writeParcelableArray(touchPoints, flags);
        }

        public static final Parcelable.Creator<GestureStep> CREATOR
                = new Parcelable.Creator<GestureStep>() {
            public GestureStep createFromParcel(Parcel in) {
                return new GestureStep(in);
            }

            public GestureStep[] newArray(int size) {
                return new GestureStep[size];
            }
        };
    }

    /**
     * Class to convert a GestureDescription to a series of GestureSteps.
     *生成用于执行的手势步骤序列
     * @hide
     */
    public static class MotionEventGenerator {
        /* Lazily-created scratch memory for processing touches */
        private static TouchPoint[] sCurrentTouchPoints;

        public static List<GestureStep> getGestureStepsFromGestureDescription(
                GestureDescription description, int sampleTimeMs) {
            final List<GestureStep> gestureSteps = new ArrayList<>();

            // Point data at each time we generate an event for
            final TouchPoint[] currentTouchPoints =
                    getCurrentTouchPoints(description.getStrokeCount());
            int currentTouchPointSize = 0;
            /* Loop through each time slice where there are touch points */
            long timeSinceGestureStart = 0;
            long nextKeyPointTime = description.getNextKeyPointAtLeast(timeSinceGestureStart);
            while (nextKeyPointTime >= 0) {
                timeSinceGestureStart = (currentTouchPointSize == 0) ? nextKeyPointTime
                        : Math.min(nextKeyPointTime, timeSinceGestureStart + sampleTimeMs);
                currentTouchPointSize = description.getPointsForTime(timeSinceGestureStart,
                        currentTouchPoints);
                gestureSteps.add(new GestureStep(timeSinceGestureStart, currentTouchPointSize,
                        currentTouchPoints));

                /* Move to next time slice */
                nextKeyPointTime = description.getNextKeyPointAtLeast(timeSinceGestureStart + 1);
            }
            return gestureSteps;
        }

        //提供缓存的触摸点数组，避免重复创建
        private static TouchPoint[] getCurrentTouchPoints(int requiredCapacity) {
            if ((sCurrentTouchPoints == null) || (sCurrentTouchPoints.length < requiredCapacity)) {
                sCurrentTouchPoints = new TouchPoint[requiredCapacity];
                for (int i = 0; i < requiredCapacity; i++) {
                    sCurrentTouchPoints[i] = new TouchPoint();
                }
            }
            return sCurrentTouchPoints;
        }
    }
}
