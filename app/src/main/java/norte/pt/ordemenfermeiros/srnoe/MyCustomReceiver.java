package norte.pt.ordemenfermeiros.srnoe;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.parse.ParsePushBroadcastReceiver;

public class MyCustomReceiver extends ParsePushBroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (!sharedPrefs.contains("NOTIF") || sharedPrefs.getBoolean("NOTIF", false))
            super.onReceive(context,intent);
    }

}
