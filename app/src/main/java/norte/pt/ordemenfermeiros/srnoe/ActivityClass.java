package norte.pt.ordemenfermeiros.srnoe;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.parse.Parse;
import com.parse.ParseInstallation;

public class ActivityClass extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putBoolean("INIT",true);
        editor.apply();

        Parse.initialize(this, "vuM4spZvl6kDNqZUb6qTEuf6Y7OVUZ23xoBjzEOh", "TiI82YnBg8pbUmXLD7ww8q7tamjTTEDDrt6aKKz3");
        ParseInstallation.getCurrentInstallation().saveInBackground();
    }


}
