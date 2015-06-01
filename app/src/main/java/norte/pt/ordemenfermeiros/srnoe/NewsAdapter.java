package norte.pt.ordemenfermeiros.srnoe;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.parse.ParseAnalytics;
import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

class NewsAdapter extends BaseAdapter {


    private final ArrayList<New> data;
    private static LayoutInflater inflater = null;

    private final Context context;

    public NewsAdapter(Activity a, ArrayList<New> d, Context c) {
        data = d;
        inflater = (LayoutInflater) a.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.context = c;
    }

    public int getCount() {
        return data.size();
    }

    public Object getItem(int position) {
        return position;
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(final int position, View convertView, ViewGroup parent) {

        View vi;

        if(convertView == null)
            vi = inflater.inflate(R.layout.news_card, parent, false);
        else
            vi = convertView;

        final New entry = data.get(position);

        TextView text = (TextView) vi.findViewById(R.id.newsTextTV);
        text.setEllipsize(TextUtils.TruncateAt.END);
        text.setMaxLines(3);
        text.setText(entry.getTitle());

        ImageView pic = (ImageView)vi.findViewById(R.id.newPic);
        ImageButton share = (ImageButton)vi.findViewById(R.id.shareIB);
        ImageButton event = (ImageButton)vi.findViewById(R.id.eventIB);
        final ImageButton fav = (ImageButton)vi.findViewById(R.id.favIB);

        final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        if (sharedPrefs.getBoolean("FAV" + entry.getUrl(), false))
            fav.setBackgroundResource(R.mipmap.ic_action_important_y);
        else
            fav.setBackgroundResource(R.mipmap.ic_action_important_dark);

        fav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Map<String, String> dimensions = new HashMap<>();
                if (sharedPrefs.getBoolean("FAV" + entry.getUrl(), false)) {
                    dimensions.put("unfav", entry.getFrom());

                    fav.setBackgroundResource(R.mipmap.ic_action_important_dark);
                    sharedPrefs.edit().putBoolean("FAV" + entry.getUrl(), false).apply();
                    Toast.makeText(context, "Removido dos favoritos.", Toast.LENGTH_SHORT).show();
                }
                else {
                    dimensions.put("fav", entry.getFrom());

                    fav.setBackgroundResource(R.mipmap.ic_action_important_y);
                    sharedPrefs.edit().putBoolean("FAV" + entry.getUrl(), true).apply();
                    Toast.makeText(context, "Adicionado aos favoritos!", Toast.LENGTH_SHORT).show();
                }
                ParseAnalytics.trackEventInBackground("Favoritos", dimensions);
            }
        });

        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (entry.getFrom().equals("facebook")){
                    Intent fbshare = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://www.facebook.com/" + entry.getUrl()));
                    ((Activity) context).startActivityForResult(fbshare, 42);
                }
                else
                    openShare(entry);
            }
        });

        text.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onNewClick(entry);
            }
        });

        pic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onNewClick(entry);
            }
        });

        event.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                event(entry);
            }
        });

        TextView date = (TextView)vi.findViewById(R.id.dateTV);
        date.setText(entry.getStringDate());
        if (entry.getFrom().equals("facebook"))
            date.setCompoundDrawablesWithIntrinsicBounds(null, null, context.getResources().getDrawable(R.mipmap.icon_facebook), null);
        else {
            if (entry.getFrom().equals("blog"))
                date.setCompoundDrawablesWithIntrinsicBounds(null, null, context.getResources().getDrawable(R.mipmap.icon_notif), null);
            else
                date.setCompoundDrawablesWithIntrinsicBounds(null, null, context.getResources().getDrawable(R.mipmap.icon_site), null);
        }

        if (entry.isRecent() && !entry.getClicked()) {
            vi.setBackgroundResource(R.drawable.card_background_clicked);
            date.setTextColor(Color.parseColor("#777777"));
        }
        else {
            vi.setBackgroundResource(R.drawable.card_background);
            date.setTextColor(Color.parseColor("#BBBBBB"));
        }

        String imageURL = entry.getImage();

        Picasso.with(context).cancelRequest(pic);
        if (imageURL != null) {
            Picasso.with(context)
                    .load(imageURL)
                    .placeholder(R.mipmap.placeholder)
                    .resize(400,225)
                    .centerCrop()
                    .into(pic);
        }
        else {
            pic.setImageDrawable(null);
            pic.setPadding(0,0,0,0);
            pic.setBackgroundResource(0);
        }

        return vi;
    }

    private void onNewClick (New entry) {
        Map<String, String> dimensions = new HashMap<>();
        dimensions.put("acesso", entry.getFrom());
        ParseAnalytics.trackEventInBackground("Acessos", dimensions);
        if (!entry.getClicked()) {
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
            sharedPrefs.edit().putBoolean("CLICKED" + entry.getUrl(), true).apply();
        }
        if (entry.getFrom().equals("facebook")) {
            if (isNetworkAvailable()) {
                ((Activity) context).startActivityForResult(getOpenFacebookIntent(context, entry.getUrl()), 42);
            }
            else
                Toast.makeText(context, "Sem ligação à internet!", Toast.LENGTH_LONG).show();
        }
        else {
            Intent intent = new Intent(context, ViewNew.class);
            intent.putExtra("url", entry.getUrl());
            intent.putExtra("title", entry.getTitle());
            intent.putExtra("content", entry.getContent());
            ((Activity) context).startActivityForResult(intent, 42);
        }
    }

    private static Intent getOpenFacebookIntent(Context context, String postid) {
        Toast.makeText(context, "A abrir página..", Toast.LENGTH_LONG).show();
        try {
            context.getPackageManager().getPackageInfo("com.facebook.katana", 0);
            return new Intent(Intent.ACTION_VIEW, Uri.parse("fb://post/" + postid));
        } catch (Exception e) {
            return new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/" + postid));
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void event (final New n) {
        final Calendar currentTime = Calendar.getInstance();
        final int year = currentTime.get(Calendar.YEAR);
        final int month = currentTime.get(Calendar.MONTH);
        final int day = currentTime.get(Calendar.DAY_OF_MONTH);
        final DatePickerDialog mdp = new DatePickerDialog(context, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, final int yearOfTime, final int monthOfYear, final int dayOfMonth) {

                final int hour = currentTime.get(Calendar.HOUR_OF_DAY);
                final int min = currentTime.get(Calendar.MINUTE);
                final TimePickerDialog tpd = new TimePickerDialog(context, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {

                        Intent intent = new Intent(Intent.ACTION_EDIT);
                        intent.setType("vnd.android.cursor.item/event");
                        String begin = dayOfMonth+"/"+(monthOfYear+1)+"/"+yearOfTime+" "+hourOfDay+":"+minute;
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                        Date start;
                        try {
                            start = sdf.parse(begin);
                            intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start.getTime());
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        intent.putExtra(CalendarContract.Events.ALL_DAY, false);
                        String title = n.getTitle();
                        try {
                            title = title.substring(0, title.indexOf("\n"));
                        } catch (Exception ignored) {}
                        intent.putExtra(CalendarContract.Events.TITLE, title);
                        context.startActivity(intent);
                        Map<String, String> dimensions = new HashMap<>();
                        dimensions.put("eventos", n.getFrom());
                        ParseAnalytics.trackEventInBackground("Eventos", dimensions);
                    }
                }, hour, min, true);
                tpd.setTitle("Hora para o lembrete:");
                tpd.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancelar", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == DialogInterface.BUTTON_NEGATIVE) {
                            tpd.hide();
                        }
                    }
                });
                tpd.show();
            }
        }, year, month, day);
        mdp.setTitle("Data para o lembrete:");
        mdp.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancelar", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (which == DialogInterface.BUTTON_NEGATIVE) {
                    mdp.hide();
                }
            }
        });
        mdp.show();
    }

    private void openShare (New n) {
        String URL = n.getUrl();
        String TITLE = n.getTitle();

        Intent emailIntent = new Intent();
        emailIntent.setAction(Intent.ACTION_SEND);

        emailIntent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(URL));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, TITLE);
        emailIntent.setType("message/rfc822");

        PackageManager pm = context.getPackageManager();
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");


        Intent openInChooser = Intent.createChooser(emailIntent, "Partilhar por:");

        Map<String, String> dimensions = new HashMap<>();
        dimensions.put("partilhas", n.getFrom());
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
        context.startActivity(openInChooser);
    }
}
