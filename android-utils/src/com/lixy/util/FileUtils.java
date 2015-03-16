package com.lixy.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.webkit.MimeTypeMap;

public class FileUtils {
    /**
     * Recursive copying folder contents
     * 
     * @param source - path to source folder
     * @param target - path to target folder
     * @return true on success
     */
    public static boolean copy(String source, String target) {
        boolean res = false;
        source = source.replaceAll("((\\\\)|/)$", "") + "/";
        target = target.replaceAll("((\\\\)|/)$", "") + "/";
        Log.d("Files.copy()", source + "\r\n" + target);

        File f = new File(source);
        if (f.isDirectory()) {
            new File(target).mkdirs();

            String[] files = f.list();
            for (String file : files) {
                copy(source + "/" + file, target + "/" + file);
            }
            res = true;
        } else {
            saveFileBytes(getFileBytes(source), target);
            res = true;
        }

        return res;
    }

    public static void clearFolder(String path) {
        path = path.replaceAll("((\\\\)|/)$", "") + "/";
        File f = new File(path);
        String[] files = f.list();
        if (files != null) {
            for (String name : files) {
                String file = path + name;
                File sf = new File(file);
                if (sf.isDirectory()) {
                    clearFolder(file);
                    sf.delete();
                } else
                    sf.delete();
            }
        }
    }

    /**
     * Unzip archive to specified folder
     * 
     * @param zipFilePath - path to source zip file
     * @param targetDir - path to target folder for unzipping
     * @return true on success, false on error
     */
    public static boolean unzip(String zipFilePath, String targetDir) {
        boolean res = false;

        try {
            File f = new File(targetDir);
            if (!f.isDirectory()) {
                f.mkdirs();
            }
            ZipInputStream zin = new ZipInputStream(new FileInputStream(zipFilePath));
            try {
                ZipEntry ze = null;
                while ((ze = zin.getNextEntry()) != null) {
                    String path = targetDir + ze.getName();

                    if (ze.isDirectory()) {
                        File unzipFile = new File(path);
                        if (!unzipFile.isDirectory()) {
                            unzipFile.mkdirs();
                        }
                    } else {
                        FileOutputStream fout = new FileOutputStream(path, false);
                        int len = 0;
                        byte[] buffer = new byte[10000];
                        while ((len = zin.read(buffer)) >= 0) {
                            fout.write(buffer, 0, len);
                        }
                        zin.closeEntry();
                        fout.flush();
                        fout.close();
                    }

                    res = true;
                }
            } finally {
                zin.close();
            }
        } catch (Exception e) {
            Log.e("Files.unzip()", e.getMessage() + "");
            e.printStackTrace();
        }

        return res;
    }

    /**
     * Get file content as bytes array
     * 
     * @return byte array on success, NULL on error
     */
    public static byte[] getFileBytes(String path) {
        byte[] content = null;

        File f = new File(path);

        if ((f.exists()) && f.isFile() && f.canRead()) {
            InputStream inputStream = null;
            try {
                int length = (int) f.length();
                inputStream = new FileInputStream(path);
                byte[] buffer = new byte[length];
                int d = inputStream.read(buffer);

                content = buffer;
            } catch (Exception e) {
                Log.e("Files.getFileBytes()", e.getMessage() + "");
            } finally {
                try {
                    inputStream.close();
                } catch (IOException e0) {}
                inputStream = null;
            }
        }

        return content;
    }

    /**
     * Saving bytes array as file. <br/>
     * File will be rewrited or created if it does not exist
     * 
     * @return true on success, false on error
     */
    public static boolean saveFileBytes(byte[] bytes, String path) {
        boolean res = false;

        if ((bytes != null) && (path != null)) {
            FileOutputStream outStream = null;
            try {
                outStream = new FileOutputStream(path);
                outStream.write(bytes);
                outStream.flush();

                if ((new File(path)).isFile()) {
                    res = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (outStream != null) {
                    try {
                        outStream.close();
                    } catch (IOException e) {}
                    outStream = null;
                }
            }
        }

        return res;
    }

    /**
     * Get list of available external storages
     * 
     * @return ArrayList of File objects. <br/>
     *         If there no available cards returns empty ArrayList
     */
    public static ArrayList<File> getExternalCards() {
        ArrayList<File> cards = new ArrayList<File>();

        final String state = Environment.getExternalStorageState();
        Log.d("Files.getExternalCard()", "card state: " + state);

        if (Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            final File peStorage = Environment.getExternalStorageDirectory();

            final String esRootDir;
            if ((esRootDir = peStorage.getParent()) == null) {
                cards.add(peStorage);
            } else {
                final File esRoot = new File(esRootDir);
                final File[] files = esRoot.listFiles();

                for (final File file : files) {
                    if (file.isDirectory() && file.canRead()) {
                        cards.add(file);
                    }
                }

            }
        }
        /*
         * public downloads dir:
         * Environment.getExternalStoragePublicDirectory(Environment
         * .DIRECTORY_DOWNLOADS);
         */

        return cards;
    }

    public static String pathFromUri(Uri uri, Context context) {
        String path = null;
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri, null, null, null, null);
            cursor.moveToFirst();
            String document_id = cursor.getString(0);
            document_id = document_id.substring(document_id.lastIndexOf(":") + 1);
            cursor.close();

            cursor = context.getContentResolver().query(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, MediaStore.Images.Media._ID + " = ? ", new String[] { document_id }, null);
            cursor.moveToFirst();
            path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
        } catch (Exception e) {
            path = uri.toString();
        } finally {
            cursor.close();
        }

        return path;
    }

    public static Uri UriFromPath(String path) {
        Uri uri = null;

        try {
            uri = Uri.fromFile(new File(path));
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        return uri;
    }

    @SuppressLint("DefaultLocale")
    public static String getExtension(String path) {
        String res = "";

        String extension = MimeTypeMap.getFileExtensionFromUrl(path);
        if (extension != null) {
            return extension.toLowerCase();
        } else {
            if (path != null) {
                String[] parts = path.split("\\.");
                int len = parts.length;
                if (len >= 2) {
                    res = (parts[len - 1]).toLowerCase();
                }
            }
        }

        return res;
    }


    /*
     * Images
     */

    public static String BitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        String baseStr = "data:image/png;base64," + Base64.encodeToString(byteArray, Base64.DEFAULT);
        return baseStr;
    }

    /**
     * Convert pixels metric to appropriate dip
     */
    public static int pxToDIP(int px, Context ctx) {
        return (int) (px / Resources.getSystem().getDisplayMetrics().density);
    }

    /**
     * Convert dip to appropriate pixels metric
     */
    public static int dipToPx(int dp, Context ctx) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    public static boolean saveBitmap(byte[] data, String path, int quality) {
        boolean res = false;

        if ((quality > 100) || (quality <= 0)) quality = 100;

        Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(path);
            bmp.compress(Bitmap.CompressFormat.PNG, quality, out);
            out.flush();

            res = true;
        } catch (Exception e) {
            Log.e("saveBitmap", e.getMessage() + "");
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                // e.printStackTrace();
            }
        }

        return res;
    }

    public static boolean isImage(String path) {
        boolean res = false;
        File f = new File(path);
        if (f.isFile()) {
            try {
                String ext = getExtension(path);
                if (ext.equals("jpg") || ext.equals("jpeg") || ext.equals("gif") || ext.equals("bmp") || ext.equals("png")) {
                    res = true;
                }
            } catch (Exception e) {
                Log.e("isImage", e.getMessage() + "");
            }
        }

        return res;
    }


    /**
     * Compress image
     * 
     * @return bitmap for compressed image
     */
    public static Bitmap getPreviewBitmap(byte[] data, int maxWidth, int maxHeight) {
        Bitmap bitm = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try {
            BitmapFactory.decodeByteArray(data, 0, data.length, options);
            int sc = sampleSize(options.outWidth, options.outHeight, maxWidth, maxHeight);
            options.inJustDecodeBounds = false;
            options.inSampleSize = sc;
            bitm = BitmapFactory.decodeByteArray(data, 0, data.length, options);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return bitm;
    }

    public static Bitmap getPreviewBitmap(String path, int maxWidth, int maxHeight) {
        Bitmap bitm = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try {
            BitmapFactory.decodeFile(path, options);
            int sc = sampleSize(options.outWidth, options.outHeight, maxWidth, maxHeight);
            options.inJustDecodeBounds = false;
            options.inSampleSize = sc;
            bitm = BitmapFactory.decodeFile(path, options);
        } catch (Exception e) {
            Log.e("getPreviewBitmap", e.getMessage() + "");
            e.printStackTrace();
        }

        return bitm;
    }


    // Calculate the required degree for image compression:
    private static int sampleSize(int width, int height, int maxWidth, int maxHeight) {
        int sample = 1;
        if ((width > maxWidth) && (height > maxHeight) && (maxWidth > 0) && (maxHeight > 0)) {
            float w = (float) width / maxWidth;
            float h = (float) height / maxHeight;
            sample = (int) Math.ceil((w + h) / 2);
        }

        return sample;
    }
}
