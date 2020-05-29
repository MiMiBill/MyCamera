package com.cjt2325.cameralibrary.util;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import androidx.annotation.NonNull;

/**
 * =====================================
 * 作    者: 陈嘉桐
 * 版    本：1.1.4
 * 创建日期：2017/4/25
 * 描    述：
 * =====================================
 */
public class CameraParamUtil {
    private static final String TAG = "JCameraView";
    private CameraSizeComparator sizeComparator = new CameraSizeComparator();
    private static CameraParamUtil cameraParamUtil = null;

    private CameraParamUtil() {

    }

    public static CameraParamUtil getInstance() {
        if (cameraParamUtil == null) {
            cameraParamUtil = new CameraParamUtil();
            return cameraParamUtil;
        } else {
            return cameraParamUtil;
        }
    }


    /**
     * 这个方法在魅族M3s上会得到 1920 * 1088 分辨率
     * 该分辨率无法正常录制视频 所以该方法暂时弃置
     *
     * 理想状态下应获取到和 w/h 比例一致的尺寸 即1920*1080
     * 仅在比例无法完全一致时才使用近似比例
     *
     * @param h 长边
     * @param w 短边
     *
     * @deprecated Use getOptimalSize2() instead.
     */
    @Deprecated
    public Camera.Size getOptimalSize(@NonNull List<Camera.Size> sizes, int w, int h, int maxWidth) {
        float ASPECT_TOLERANCE = 0.1F;
        float targetRatio = (float) h / w;
        Camera.Size optimalSize = null;
        float minDiff = Float.MAX_VALUE;

        int targetHeight = h;

        for (Camera.Size size : sizes) {
            float ratio = (float) size.width / size.height;
            // 最大限制宽度使用size.height，因为size中 w > h，而app中竖屏拍照 h > w
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE || size.height > maxWidth) {
                continue;
            }
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Float.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }

        return optimalSize;
    }


    /**
     * 先预设一个最大宽度 排好序轮询得到最接近最大值且最符合指定h/w的尺寸
     * 如果找不到 再走 getPreviewSize 拿一个值
     *
     * @param sizes 支持尺寸
     * @param w 宽
     * @param h 高（长边）
     * @param maxShortEdge 视频短边最大值
     * @return 视频尺寸
     */
    public Camera.Size getOptimalSize2(@NonNull List<Camera.Size> sizes, int w, int h, int maxShortEdge) {
        float ASPECT_TOLERANCE = 0.1F;
        float targetRatio = (float) h / w;
        Camera.Size optimalSize = null;

        float minDiff = Float.MAX_VALUE;
        Collections.sort(sizes, sizeComparator);
        for (int i = 0; i < sizes.size(); i++) {
            Camera.Size size = sizes.get(i);
            if (size.height > maxShortEdge) {
                break;
            }
            float sRatio = (float) size.width / size.height;
            float diff = Math.abs(sRatio - targetRatio);
            if (diff < ASPECT_TOLERANCE && diff <= minDiff) {
                minDiff = diff;
                optimalSize = size;
            }
        }

        if (optimalSize == null) {
            return getPreviewSize(sizes, 900, targetRatio);// th指最小高
        }
        return optimalSize;
    }



    public Camera.Size getPreviewSize(List<Camera.Size> list, int th, float rate) {
        Collections.sort(list, sizeComparator);
        int i = 0;
        for (Camera.Size s : list) {
            if ((s.width > th) && equalRate(s, rate)) {
                Log.i(TAG, "MakeSure Preview :w = " + s.width + " h = " + s.height);
                break;
            }
            i++;
        }
        if (i == list.size()) {
            return getBestSize(list, rate);
        } else {
            return list.get(i);
        }
    }

    public Camera.Size getPictureSize(List<Camera.Size> list, int th, float rate) {
        Collections.sort(list, sizeComparator);
        int i = 0;
        for (Camera.Size s : list) {
            if ((s.width > th) && equalRate(s, rate)) {
                Log.i(TAG, "MakeSure Picture :w = " + s.width + " h = " + s.height);
                break;
            }
            i++;
        }
        if (i == list.size()) {
            return getBestSize(list, rate);
        } else {
            return list.get(i);
        }
    }

    private Camera.Size getBestSize(List<Camera.Size> list, float rate) {
        float previewDisparity = 100;
        int index = 0;
        for (int i = 0; i < list.size(); i++) {
            Camera.Size cur = list.get(i);
            float prop = (float) cur.width / (float) cur.height;
            if (Math.abs(rate - prop) < previewDisparity) {
                previewDisparity = Math.abs(rate - prop);
                index = i;
            }
        }
        return list.get(index);
    }


    private boolean equalRate(Camera.Size s, float rate) {
        float r = (float) (s.width) / (float) (s.height);
        return Math.abs(r - rate) <= 0.2;
    }

    public boolean isSupportedFocusMode(List<String> focusList, String focusMode) {
        for (int i = 0; i < focusList.size(); i++) {
            if (focusMode.equals(focusList.get(i))) {
                Log.i(TAG, "FocusMode supported " + focusMode);
                return true;
            }
        }
        Log.i(TAG, "FocusMode not supported " + focusMode);
        return false;
    }

    public boolean isSupportedPictureFormats(List<Integer> supportedPictureFormats, int jpeg) {
        for (int i = 0; i < supportedPictureFormats.size(); i++) {
            if (jpeg == supportedPictureFormats.get(i)) {
                Log.i(TAG, "Formats supported " + jpeg);
                return true;
            }
        }
        Log.i(TAG, "Formats not supported " + jpeg);
        return false;
    }

    private class CameraSizeComparator implements Comparator<Camera.Size> {
        public int compare(Camera.Size lhs, Camera.Size rhs) {
            if (lhs.width == rhs.width) {
                return 0;
            } else if (lhs.width > rhs.width) {
                return 1;
            } else {
                return -1;
            }
        }

    }

    public int getCameraDisplayOrientation(Context context, int cameraId) {
        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        int rotation = wm.getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;   // compensate the mirror
        } else {
            // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }
}
