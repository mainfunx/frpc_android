package com.car.frpc_android;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class Constants {
    public static final String INI_PATH = "ini";

    public static final String INI_FILE_SUF = ".ini";

    public static String getIniFileParentPath(Context context) {
        return context.getCacheDir().getPath() + File.separator + Constants.INI_PATH;

    }

    public static File getIniFileParent(Context context) {
        String iniFileParentPath = getIniFileParentPath(context);
        File parent = new File(iniFileParentPath);
        if (!parent.exists())
            parent.mkdirs();
        return parent;
    }


    public static void tendToSettings(Context context) {
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
        intent.setData(Uri.fromParts("package", context.getPackageName(), null));
        context.startActivity(intent);
        Toast.makeText(context, R.string.permissionReason, Toast.LENGTH_SHORT).show();
    }

    public static String getStringFromFile(File file) {
        StringBuilder sb = new StringBuilder();
        try {
            FileInputStream inputStream = new FileInputStream(file);
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "gbk");

            BufferedReader reader = new BufferedReader(inputStreamReader);

            String line;

            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            inputStream.close();
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    public static String getStringFromRaw(Context context, int rawName) {
        try {
            InputStreamReader inputReader = new InputStreamReader(context.getResources().openRawResource(rawName));
            BufferedReader bufReader = new BufferedReader(inputReader);
            String line = "";
            StringBuilder result = new StringBuilder();
            while ((line = bufReader.readLine()) != null)
                result.append(line).append("\n");
            return result.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private static String getFileFromAsserts(Context context, String fileName) {

        try {
            ClassLoader classLoader = context.getClass().getClassLoader();
            InputStreamReader isr = new InputStreamReader(classLoader.getResourceAsStream("assets/" + fileName));
            BufferedReader bfr = new BufferedReader(isr);
            String line;
            StringBuilder stringBuilder = new StringBuilder();
            while ((line = bfr.readLine()) != null) {
                stringBuilder.append(line);
            }
            return stringBuilder.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}
