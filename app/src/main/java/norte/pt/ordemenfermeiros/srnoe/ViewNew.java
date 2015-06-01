package norte.pt.ordemenfermeiros.srnoe;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;


public class ViewNew extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_new);

        Intent intent = getIntent();
        final String URL = intent.getStringExtra("url");

        WebView newWV = (WebView) findViewById(R.id.newWV);
        newWV.getSettings().setBuiltInZoomControls(true);
        newWV.getSettings().setSupportZoom(true);

        final String code = intent.getStringExtra("content");

        if (code != null)
            newWV.loadData(code , "text/html;charset=UTF-8", null);
        else
            newWV.loadData("Erro a carregar a notícia." , "text/html;charset=UTF-8", null);

        TextView titleTV = (TextView)findViewById(R.id.titleTV);
        final String TITLE = intent.getStringExtra("title");
        titleTV.setText(TITLE);
        titleTV.setMovementMethod(new ScrollingMovementMethod());

        ImageButton shareIB = (ImageButton)findViewById(R.id.shareIB);
        shareIB.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent emailIntent = new Intent();
                    emailIntent.setAction(Intent.ACTION_SEND);

                    emailIntent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(URL));
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, TITLE);
                    emailIntent.setType("message/rfc822");

                    PackageManager pm = getPackageManager();
                    Intent sendIntent = new Intent(Intent.ACTION_SEND);
                    sendIntent.setType("text/plain");

                    Intent openInChooser = Intent.createChooser(emailIntent, "Partilhar por:");

                    List<ResolveInfo> resInfo = pm.queryIntentActivities(sendIntent, 0);
                    List<LabeledIntent> intentList = new ArrayList<>();
                    for (int i = 0; i < resInfo.size(); i++) {
                        ResolveInfo ri = resInfo.get(i);
                        String packageName = ri.activityInfo.packageName;
                        if(packageName.contains("android.email")) {
                            emailIntent.setPackage(packageName);
                        } else if(packageName.contains("twitter") || packageName.contains("facebook") || packageName.contains("talk") || packageName.contains("mms") || packageName.contains("android.gm")) {
                            Intent intent = new Intent();
                            intent.setComponent(new ComponentName(packageName, ri.activityInfo.name));
                            intent.setAction(Intent.ACTION_SEND);
                            intent.setType("text/plain");
                            if(packageName.contains("talk")) {
                                intent.putExtra(Intent.EXTRA_TEXT, URL);
                            } else if(packageName.contains("twitter")) {
                                intent.putExtra(Intent.EXTRA_TEXT, URL);
                            } else if(packageName.contains("facebook")) {
                                intent.putExtra(Intent.EXTRA_TEXT, URL);
                            } else if(packageName.contains("mms")) {
                                intent.putExtra(Intent.EXTRA_TEXT, URL);
                            } else if(packageName.contains("android.gm")) {
                                intent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(URL));
                                intent.putExtra(Intent.EXTRA_SUBJECT,TITLE);
                                intent.setType("message/rfc822");
                            }

                            intentList.add(new LabeledIntent(intent, packageName, ri.loadLabel(pm), ri.icon));
                        }
                    }

                    LabeledIntent[] extraIntents = intentList.toArray( new LabeledIntent[ intentList.size() ]);

                    openInChooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, extraIntents);
                    startActivity(openInChooser);
                }
            });

        ImageButton browserIB = (ImageButton)findViewById(R.id.browserIB);
        browserIB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(URL));
                startActivity(browserIntent);
            }
        });

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final ImageButton favIB = (ImageButton)findViewById(R.id.favIB);
        if (sharedPrefs.getBoolean("FAV" + URL, false))
            favIB.setBackgroundResource(R.mipmap.ic_action_important_y);

        favIB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                if (sharedPrefs.getBoolean("FAV" + URL, false)) {
                    favIB.setBackgroundResource(R.mipmap.ic_action_important);
                    sharedPrefs.edit().putBoolean("FAV" + URL, false).apply();
                    Toast.makeText(getApplicationContext(), "Removido dos favoritos.", Toast.LENGTH_SHORT).show();
                }
                else {
                    favIB.setBackgroundResource(R.mipmap.ic_action_important_y);
                    sharedPrefs.edit().putBoolean("FAV" + URL, true).apply();
                    Toast.makeText(getApplicationContext(), "Adicionado aos favoritos!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
