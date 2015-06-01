package norte.pt.ordemenfermeiros.srnoe;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;


public class Contacts extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);

        getSupportActionBar().setTitle("Contactos SRN OE");

        ImageButton fb = (ImageButton)findViewById(R.id.fbBTN);
        ImageButton bw = (ImageButton)findViewById(R.id.bwBTN);

        fb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "A abrir página do Facebook..", Toast.LENGTH_LONG).show();
                String facebookUrl = "https://www.facebook.com/pages/Sec%C3%A7%C3%A3o-Regional-do-Norte-Ordem-dos-Enfermeiros/497566220296597?fref=ts";
                try {
                    int versionCode = getPackageManager().getPackageInfo("com.facebook.katana", 0).versionCode;
                    if (versionCode >= 3002850) {
                        Uri uri = Uri.parse("fb://facewebmodal/f?href=" + facebookUrl);
                        startActivity(new Intent(Intent.ACTION_VIEW, uri));
                    } else {
                        // open the Facebook app using the old method (fb://profile/id or fb://page/id)
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(facebookUrl)));
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    // Facebook is not installed. Open the browser
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(facebookUrl)));
                }
            }
        });

        bw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "A abrir website...", Toast.LENGTH_LONG).show();
                Intent intent =  new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.ordemenfermeiros.pt/sites/norte/Paginas/default.aspx"));
                startActivity(intent);

            }
        });

        ImageButton maps = (ImageButton) findViewById(R.id.mapsIB);
        maps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onclickMaps();
            }
        });

        ImageButton tele = (ImageButton) findViewById(R.id.teleIB);
        tele.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                call();
            }
        });

        TextView email = (TextView) findViewById(R.id.emailTV);
        email.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                        "mailto", " srnorte@ordemenfermeiros.pt", null));
                startActivity(Intent.createChooser(intent, "Escolha um cliente de email:"));
            }
        });

        TextView membros = (TextView) findViewById(R.id.membrosTV);
        membros.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                        "mailto", "g.membrosnorte@ordemenfermeiros.pt", null));
                startActivity(Intent.createChooser(intent, "Escolha um cliente de email:"));
            }
        });
    }

    private void onclickMaps() {
        Intent intent =  new Intent(this,GooglePlayServicesActivity.class);
        startActivity(intent);
    }

    private void call() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);


        // set dialog message
        alertDialogBuilder
                .setMessage("Deseja telefonar à Ordem dos Enfermeiros?")
                .setCancelable(false)
                .setNegativeButton("Não",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        // if this button is clicked, just close
                        // the dialog box and do nothing
                        dialog.cancel();
                    }
                })
                .setPositiveButton("Sim",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        Intent intent = new Intent(Intent.ACTION_DIAL);
                        intent.setData(Uri.parse("tel:" + "225 072 710"));
                        startActivity(intent);
                    }
                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }
}
