package norte.pt.ordemenfermeiros.srnoe;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.parse.ParseAnalytics;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends ActionBarActivity {

    private ArrayList<New> news;
    private ProgressDialog pd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.mipmap.ic_launcher);
        getSupportActionBar().setTitle("S. R. Norte");

        news = new ArrayList<>();

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        ParseAnalytics.trackAppOpenedInBackground(getIntent());

        String[] opts = {"Favoritos", "Mensagens", "Notícias", "Posts", "Ordem dos Enfermeiros"};
        getSupportActionBar().setSubtitle(opts[sharedPrefs.getInt("FILTER", 4)]);


        if (treatNotifications()) {
            finish();
            System.exit(0);
        }

        pd = new ProgressDialog(this);

        if (!sharedPrefs.getBoolean("INIT", false)) { //Is it not the first time onCreate is called? (ignore the "false")
            news = getFromSP();
            if (news != null) {
                int which = sharedPrefs.getInt("FILTER", 4);
                if (which != 4)
                    filterBy(which);
                else
                    displayNews(news);
            }
        }
        else
            new FetchNews().execute("http://176.111.109.0/");
    }

    private boolean treatNotifications() { //Takes care of the parse notifications
        Bundle mBundle = getIntent().getExtras();
        if (mBundle != null) {
            String mData = mBundle.getString("com.parse.Data");
            if (mData != null) {
                try {
                    JSONObject notif = new JSONObject(mData);
                    if (notif.has("id")) {
                        Intent intent = getOpenFacebookIntent(getBaseContext(), notif.getString("id"));
                        startActivity(intent);
                        return true;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    private static Intent getOpenFacebookIntent(Context context, String postid) { //Opens facebook on post page
        try {
            context.getPackageManager().getPackageInfo("com.facebook.katana", 0);
            return new Intent(Intent.ACTION_VIEW, Uri.parse("fb://post/" + postid));
        } catch (Exception e) {
            return new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/" + postid));
        }
    }

    private ArrayList<New> getFromSP() { //Gets the data from SharedPreferences in case there's no internet
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Gson gson = new Gson();
        String json = sharedPrefs.getString("NEWS", null);
        Type type = new TypeToken<ArrayList<New>>() {}.getType();
        return gson.fromJson(json, type);
    }

    private void displayNews(ArrayList<New> nws) { //displays the content
        NewsAdapter adapter = new NewsAdapter(MainActivity.this, nws, MainActivity.this);
        final ListView list = (ListView) findViewById(R.id.list);
        list.setAdapter(adapter);
    }

    private Date toDate(String date, String dateFormat) throws ParseException {//converts String to Date
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat, Locale.getDefault());
        return sdf.parse(date);
    }

    private class FetchNews extends AsyncTask<String, Integer, Boolean> { //Gets the data from server

        final String FBURL = "http://www.bestappsolutions.pt/srnoe/fbposts.txt"; //facebook
        final String NURL = "http://www.bestappsolutions.pt/srnoe/news.txt"; //site
        final String BURL = "http://www.bestappsolutions.pt/srnoe/blogs.txt"; //blog
        SharedPreferences sharedPrefs;

        // Runs in UI before background thread is called
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pd.setTitle("SRN OE");
            pd.setMessage("A carregar notícias...");
            pd.show();
            sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        }

        // This is run in a background thread
        @Override
        protected Boolean doInBackground(String... params) {

            OkHttpClient client = new OkHttpClient();
            boolean res = false;
            try {
                if (isConnectedToServer(params[0])) {
                    res = true;

                    Request request = new Request.Builder()
                            .url(FBURL)
                            .build();

                    Response response = client.newCall(request).execute();
                    String JSONString = response.body().string();

                    String id, message, descpt, date, img, url;
                    id = message = descpt = date = url = null;

                    try {
                        JSONObject jsonObj = new JSONObject(JSONString);
                        if (jsonObj.has("data")) {
                            JSONArray dataArray = jsonObj.getJSONArray("data");

                            for (int i = 0; i < dataArray.length(); i++) {
                                JSONObject p = dataArray.getJSONObject(i);

                                if (p.has("id"))
                                    id = p.getString("id");

                                if (p.has("link"))
                                    url = p.getString("link");

                                if (p.has("message"))
                                    message = p.getString("message");

                                if (p.has("description"))
                                    descpt = p.getString("description");

                                if (p.has("created_time"))
                                    date = p.getString("created_time");

                                if (date != null)
                                    date = date.substring(0, date.length() - 14);

                                if (message == null && descpt != null)
                                    message = descpt;

                                if (message != null) {
                                    message = message.replaceAll("\n\n", "\n");
                                    message = message.replaceAll("\n\n\n", "\n");
                                    message = message.replaceAll("\n\n\n\n", "\n");
                                    Date dte = toDate(date, "yyyy-MM-dd");
                                    news.add(new New(message, id, url, dte, null, "facebook", wasClicked(id), isFavourite(id), isRecent(dte)));
                                }

                                message = id = url = descpt = date = null;
                            }
                        }

                        request = new Request.Builder()
                                .url(BURL)
                                .build();

                        response = client.newCall(request).execute();
                        JSONString = response.body().string();

                        JSONObject dataObject = new JSONObject(JSONString);

                        JSONArray dataArray;

                        if (dataObject.has("posts")) {
                            dataArray = dataObject.getJSONArray("posts");

                            String title;
                            title = date = url = img = null;

                            for (int i = 0; i < dataArray.length(); i++) {
                                JSONObject p = dataArray.getJSONObject(i);
                                if (p.has("title"))
                                    title = p.getString("title");

                                if (p.has("url"))
                                    url = p.getString("url");

                                if (p.has("date"))
                                    date = p.getString("date");

                                if (p.has("content"))
                                    message = p.getString("content");

                                if (p.has("img"))
                                    img = p.getString("img");

                                if (date != null)
                                    date = date.substring(0, 10);

                                if (title != null) {
                                    Date dte = toDate(date, "yyyy-MM-dd");
                                    news.add(new New(title, url, message, dte, img, "blog", wasClicked(url), isFavourite(url), isRecent(dte)));
                                }

                                title = date = message = url = img = null;
                            }
                        }

                        request = new Request.Builder()
                                .url(NURL)
                                .build();

                        response = client.newCall(request).execute();
                        JSONString = response.body().string();
                        dataArray = new JSONArray(JSONString);

                        String title, code;
                        title = url = date = img = code = null;

                        for (int i = 0; i < dataArray.length(); i++) {
                            JSONObject p = dataArray.getJSONObject(i);
                            if (p.has("title"))
                                title = p.getString("title");

                            if (p.has("url"))
                                url = p.getString("url");

                            if (p.has("date"))
                                date = p.getString("date");

                            if (p.has("img"))
                                img = p.getString("img");

                            if (p.has("code"))
                                code = p.getString("code");

                            if (url != null && title != null) {
                                String html = "<html><head></head><body>" + code + "</body></html>";

                                Document doc = Jsoup.parse(html);
                                doc.select("tr").unwrap();
                                doc.select("td").unwrap();

                                Elements el = doc.getElementsByTag("img");
                                for (Element e : el) {
                                    Attributes at = e.attributes();
                                    for (Attribute a : at) {
                                        //if (a.getKey().equals("style")) e.removeAttr(a.getKey());
                                        if (a.getKey().equals("src"))
                                            a.setValue("http://www.ordemenfermeiros.pt" + a.getValue());
                                    }

                                }
                                Date dte = toDate(date, "dd-MM-yyyy");
                                news.add(new New(title, url, doc.toString(), dte, img, "site", wasClicked(url), isFavourite(url), isRecent(dte)));
                            }

                            url = title = date = img = code = null;
                        }

                    } catch (ParseException | JSONException e) {
                        e.printStackTrace();
                    }

                    Collections.sort(news);

                    SharedPreferences.Editor editor = sharedPrefs.edit();
                    Gson gson = new Gson();

                    String json_news = gson.toJson(news);
                    editor.putString("NEWS", json_news);

                    editor.apply();

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return res;
        }

        public boolean isConnectedToServer(String url) { //checks if the server is on
            try{
                URL myUrl = new URL(url);
                URLConnection connection = myUrl.openConnection();
                connection.setConnectTimeout(2000);
                connection.connect();
                return true;
            } catch (Exception e) {
                System.err.println("Error: " + e);
                return false;
            }
        }

        private Boolean wasClicked (String url) { //checks if a new was clicked
            return sharedPrefs.getBoolean("CLICKED" + url, false);
        }

        private Boolean isFavourite (String url) {//checks if a new is favourite
            return sharedPrefs.getBoolean("FAV" + url, false);
        }

        public boolean isRecent(Date date) {//Checks if a new is recent  < 1week
            Calendar currentCalendar = Calendar.getInstance();
            int week = currentCalendar.get(Calendar.WEEK_OF_YEAR);
            int year = currentCalendar.get(Calendar.YEAR);

            Calendar targetCalendar = Calendar.getInstance();
            targetCalendar.setTime(date);
            int targetWeek = targetCalendar.get(Calendar.WEEK_OF_YEAR);
            int targetYear = targetCalendar.get(Calendar.YEAR);
            return week == targetWeek && year == targetYear;
        }

        // This is called from background thread but runs in UI
        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            // Do things like update the progress bar
        }

        // This runs in UI when background thread finishes
        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.putBoolean("INIT",false);
            editor.apply();
            if (!result) {
                if (!isNetworkAvailable())
                        Toast.makeText(getApplicationContext(), "Sem ligação à internet!", Toast.LENGTH_LONG).show();
                    else
                        Toast.makeText(getApplicationContext(), "Serviço temporariamente indisponível.", Toast.LENGTH_LONG).show();
                news = getFromSP();
            }
            if (news != null) {
                int which = sharedPrefs.getInt("FILTER",4);
                if (which != 4)
                    filterBy(which);
                else
                    displayNews(news);
            }
            else
                Toast.makeText(getApplicationContext(), "Serviço temporariamente indisponível.", Toast.LENGTH_LONG).show();
            if (pd.isShowing())
                pd.dismiss();
        }
    }

    private boolean isNetworkAvailable() {//Checks if there is internet
        ConnectivityManager connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void filterBy (int which) { //filters news by..
        ArrayList<New> auxN = new ArrayList<>(news);
        if (which != 0 && which != 4) {
            String type;
            if (which == 1)
                type = "blog";
            else {
                if (which == 2)
                    type = "site";
                else
                    type = "facebook";
            }
            Iterator<New> i = auxN.iterator();
            while (i.hasNext()) {
                if (!i.next().getFrom().equals(type))
                    i.remove();
            }
        }
        else {
            if (which == 0) {
                Iterator<New> i = auxN.iterator();
                while (i.hasNext()) {
                    if (!i.next().getFavourite()) {
                        i.remove();
                    }
                }
            }
        }
        displayNews(auxN);
    }

    private void sortBy() { //Sorts by
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final CharSequence[] opts = {"Favoritos", "Mensagens", "Notícias", "Posts", "Todos"};
        int def = sharedPrefs.getInt("FILTER", 4);

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Filtrar items por: ")
               .setIcon(R.mipmap.ic_launcher)
               .setSingleChoiceItems(opts, def, new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialog, int which) {
                       SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                       sharedPrefs.edit().putInt("FILTER", which).apply();
                       filterBy(which);
                       dialog.dismiss();
                       if (which != 4)
                           getSupportActionBar().setSubtitle(opts[which]);
                       else
                           getSupportActionBar().setSubtitle("Ordem dos Enfermeiros");
                   }
               })
               .setNegativeButton("Cancelar",
               new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialog, int which) {
                       dialog.dismiss();
                   }
               });

        builder.show();
    }

    private void shareApp() {
        Intent emailIntent = new Intent();
        emailIntent.setAction(Intent.ACTION_SEND);
        String URL = "https://play.google.com/store/apps/details?id=norte.pt.ordemenfermeiros.srnoe";
        String TITLE = "Já conhece a nova App da SRN-OE?";
        emailIntent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(URL));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, TITLE);
        emailIntent.setType("message/rfc822");
        PackageManager pm = getPackageManager();
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        Intent openInChooser = Intent.createChooser(emailIntent, "Partilhar por:");
        Map<String, String> dimensions = new HashMap<>();
        dimensions.put("partilhas", "app");
        ParseAnalytics.trackEventInBackground("Partilhas", dimensions);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 42)
            displayNews(news);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.action_settings:
                startActivity(new Intent(this, Settings.class));
                return true;

            case R.id.action_contacts:
                startActivity(new Intent(this, Contacts.class));
                return true;

            case R.id.action_sortby:
                sortBy();
                return true;

            case R.id.action_shareapp:
                shareApp();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (pd.isShowing())
            pd.dismiss();
    }
}