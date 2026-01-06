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
 */

package android.app.backup;

import android.content.Context;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.File;

/**
 * Like FileBackupHelper, but takes absolute paths for the files instead of
 * subpaths of getFilesDir()
 *
 * @hide
 * 文件备份助手：用于备份和恢复指定绝对路径的文件
 */
public class AbsoluteFileBackupHelper extends FileBackupHelperBase implements BackupHelper {
    private static final String TAG = "AbsoluteFileBackupHelper";
    private static final boolean DEBUG = false;

    Context mContext;
    String[] mFiles;//需要备份/恢复的文件路径数组

    /**
     * Construct a helper for backing up / restoring the files at the given absolute locations
     * within the file system.
     *
     * @param context
     * @param files
     */
    public AbsoluteFileBackupHelper(Context context, String... files) {
        super(context);

        mContext = context;
        mFiles = files;
    }

    /**
     * Based on oldState, determine which of the files from the application's data directory
     * need to be backed up, write them to the data stream, and fill in newState with the
     * state as it exists now.
     * 执行备份操作
     * 验证恢复数据的键是否在文件列表中
     * 将数据写入对应文件
     */
    public void performBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
            ParcelFileDescriptor newState) {
        // use the file paths as the keys, too
        performBackup_checked(oldState, data, newState, mFiles, mFiles);
    }

    /**
     * Restore one absolute file entity from the restore stream
     * 执行恢复操作
     * 验证恢复数据的键是否在文件列表中
     * 将数据写入对应文件
     */
    public void restoreEntity(BackupDataInputStream data) {
        if (DEBUG) Log.d(TAG, "got entity '" + data.getKey() + "' size=" + data.size());
        String key = data.getKey();
        if (isKeyInList(key, mFiles)) {
            File f = new File(key);
            writeFile(f, data);
        }
    }
}

