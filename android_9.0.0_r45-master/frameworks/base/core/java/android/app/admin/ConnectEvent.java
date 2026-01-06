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
 * limitations under the License.
 */

package android.app.admin;

import android.os.Parcel;
import android.os.Parcelable;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * A class that represents a TCP connect event initiated through the standard network stack.
 *
 * <p>It contains information about the originating app as well as the remote TCP endpoint.
 *
 * <p>Support both IPv4 and IPv6 connections.
 * TCP连接事件表示：代表通过标准网络栈发起的TCP连接事件
 * 信息封装：包含发起连接的应用信息以及远程TCP端点信息
 */
public final class ConnectEvent extends NetworkEvent implements Parcelable {

    /** The destination IP address. */
    private final String mIpAddress;

    /** The destination port number. */
    private final int mPort;

    /** @hide */
    public ConnectEvent(String ipAddress, int port, String packageName, long timestamp) {
        super(packageName, timestamp);
        this.mIpAddress = ipAddress;
        this.mPort = port;
    }

    private ConnectEvent(Parcel in) {
        this.mIpAddress = in.readString();
        this.mPort = in.readInt();
        this.mPackageName = in.readString();
        this.mTimestamp = in.readLong();
        this.mId = in.readLong();
    }

    //IP地址解析：将存储的IP地址字符串转换为 InetAddress 对象
    public InetAddress getInetAddress() {
        try {
            // ipAddress is already an address, not a host name, no DNS resolution will happen.
            return InetAddress.getByName(mIpAddress);
        } catch (UnknownHostException e) {
            // Should never happen as we aren't passing a host name.
            return InetAddress.getLoopbackAddress();
        }
    }

    //返回目标TCP连接的端口号
    public int getPort() {
        return mPort;
    }

    @Override
    public String toString() {
        return String.format("ConnectEvent(%d, %s, %d, %d, %s)", mId, mIpAddress, mPort, mTimestamp,
                mPackageName);
    }

    public static final Parcelable.Creator<ConnectEvent> CREATOR
            = new Parcelable.Creator<ConnectEvent>() {
        @Override
        public ConnectEvent createFromParcel(Parcel in) {
            if (in.readInt() != PARCEL_TOKEN_CONNECT_EVENT) {
                return null;
            }
            return new ConnectEvent(in);
        }

        @Override
        public ConnectEvent[] newArray(int size) {
            return new ConnectEvent[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        // write parcel token first
        out.writeInt(PARCEL_TOKEN_CONNECT_EVENT);
        out.writeString(mIpAddress);
        out.writeInt(mPort);
        out.writeString(mPackageName);
        out.writeLong(mTimestamp);
        out.writeLong(mId);
    }
}
