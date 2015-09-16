package com.gmail.ndraiman.wifipasswords.extras;


import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.gmail.ndraiman.wifipasswords.activities.MainActivity;
import com.gmail.ndraiman.wifipasswords.pojo.WifiEntry;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;


/***********************************************************************/
//Copy wpa_supplicant.conf from /data/misc/wifi to sdcard/WifiPasswords

/***********************************************************************/
public class TaskLoadWifiEntries extends AsyncTask<String, Void, ArrayList<WifiEntry>> {


    private static final String LOG_TAG = "TaskLoadWifiEntries";
    private WifiListLoadedListener mListListener;

    public TaskLoadWifiEntries(WifiListLoadedListener listener) {
        mListListener = listener;
    }

    @Override
    protected ArrayList<WifiEntry> doInBackground(String... params) {
        if(!RootCheck.canRunRootCommands()) {
            Log.e(LOG_TAG, "No Root Access");
            cancel(true);
        }

        boolean dirCreated = createDir();
        if (!dirCreated) {
            Log.e(LOG_TAG, "Failed to create app directory");
            return null;
        }
        copyFile();

        return readFile();
    }

    @Override
    protected void onPostExecute(ArrayList<WifiEntry> wifiEntries) {

        MyApplication.getWritableDatabase().deleteAll(false);
        MyApplication.getWritableDatabase().insertWifiEntries(wifiEntries, false);

        if(mListListener != null) {
            L.m("OnPost Execute \n" + wifiEntries.toString());
            mListListener.onWifiListLoaded(wifiEntries);
        }
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        MainActivity.textNoData.setText("No Root Access");
    }



    /**************
     * Helper Methods
     ********************/
    private boolean createDir() {
        Log.e(LOG_TAG, "Creating Dir");
        File folder = new File(Environment.getExternalStorageDirectory() + "/WifiPasswords");
        boolean dirCreated = true;
        if (!folder.exists()) {
            dirCreated = folder.mkdir();
        }
        if (!dirCreated) {
            Log.e(LOG_TAG, "Failed to create directory");
            return false;
        }

        return true;
    }

    private void copyFile() {
        if (!ExecuteAsRootBase.canRunRootCommands()) {
            return;
        }

        Log.e(LOG_TAG, "Copying File");
        try {
            Process suProcess = Runtime.getRuntime().exec("su -c cp /data/misc/wifi/wpa_supplicant.conf /sdcard/WifiPasswords");
            suProcess.waitFor(); //wait for SU command to finish
        } catch (IOException | InterruptedException e) {
            Log.e(LOG_TAG, "copyFile Error: " + e.getClass().getName() + " " + e);
            e.printStackTrace();
        }
    }

    private ArrayList<WifiEntry> readFile() {

        ArrayList<WifiEntry> listWifi = new ArrayList<>();
        try {


            File directory = Environment.getExternalStorageDirectory();
            File file = new File(directory + "/WifiPasswords/wpa_supplicant.conf");

            if (!file.exists()) {
                Log.e(LOG_TAG, "readFile - File not found");
                return null;
            }

            Log.e(LOG_TAG, "Starting to read");

            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            String line = "";
            String title = "";
            String password = "";
            String check = "";

            while ((line = bufferedReader.readLine()) != null) {
                if (line.equals("network={")) {

                    line = bufferedReader.readLine();
                    title = line.substring(7, line.length() - 1);

                    line = bufferedReader.readLine();

                    //Log.i(LOG_TAG, title + " " + line.substring(6, line.length() - 1));
                    //Log.i(LOG_TAG, title + " " + line.substring(1, 4));

                    if ((line.substring(1, 4)).equals("psk")) {
                        password = line.substring(6, line.length() - 1);
                    } else {
                        password = "no password";
                    }

                    Log.e(LOG_TAG, title + " " + password);

                    WifiEntry current = new WifiEntry(title, password);
                    listWifi.add(current);
                }
            }


        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return listWifi;
    }

}
