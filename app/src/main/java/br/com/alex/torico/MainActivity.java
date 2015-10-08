package br.com.alex.torico;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Message;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.os.Handler;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    ToRicoService toRicoService;

    private Handler handler;
    private Button btStop;
    private Button btPause;
    private Button btStart;
    private TextView txtHora;
    private TextView txtGrana;
    private ListView lista;
    private ArrayAdapter<String> adapter;

    ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ToRicoService.ServiceBinder binder = (ToRicoService.ServiceBinder) service;
            toRicoService = binder.getService();
            toRicoService.setActivityHandler(handler);
            loadPreferences();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i("ToRico", "Service Disconnected");
            toRicoService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btPause = (Button) findViewById(R.id.btPause);
        btStart = (Button) findViewById(R.id.btStart);
        btStop = (Button) findViewById(R.id.btStop);

        txtHora = (TextView) findViewById(R.id.txtHoras);
        txtGrana = (TextView) findViewById(R.id.txtValor);

        lista = (ListView) findViewById(R.id.listView);

        startService(new Intent(this, ToRicoService.class));

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, android.R.id.text1, new ArrayList<String>(){});
        lista.setAdapter(adapter);

        handler = new Handler(){
            @Override
            public void handleMessage(Message msg){
                super.handleMessage(msg);
                DecimalFormat floatFormat = new DecimalFormat("0.00");
                txtHora.setText(msg.getData().getString("horas_extras"));
                txtGrana.setText("R$ " + String.valueOf(floatFormat.format(msg.getData().getFloat("grana_extra"))));

                btPause.setEnabled(true);
                btStop.setEnabled(true);
                btStart.setEnabled(false);
            }
        };
    }

    public void StartToRico(View v){
        if (toRicoService != null) {
            toRicoService.startToRico();
            btStop.setEnabled(true);
            btPause.setEnabled(true);
            btStart.setEnabled(false);
        }
    }

    public void PauseToRico(View v){
        if (toRicoService != null){
            toRicoService.pauseToRico();
            btStop.setEnabled(true);
            btPause.setEnabled(false);
            btStart.setEnabled(true);
        }
    }

    public void StopToRico(View v){
        if (toRicoService != null){
            confirmDialog(getApplicationContext());
        }
    }

    private void confirmDialog(Context context){

        final AlertDialog alert = new AlertDialog.Builder(this).create();
        alert.setTitle("Atenção");
        alert.setMessage("Você gostaria mesmo de parar o contador?");
        //alert.setIcon(R.drawable.warning_icon);
        alert.setCancelable(false);
        alert.setCanceledOnTouchOutside(false);

        alert.setButton(DialogInterface.BUTTON_POSITIVE, "Sim",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        alert.dismiss();
                        toRicoService.stopToRico();
                        btStop.setEnabled(false);
                        btPause.setEnabled(false);
                        btStart.setEnabled(true);
                        Intent intent = new Intent(MainActivity.this, ToRicoService.class);
                        stopService(intent);
                        listInsertItem();
                    }
                });

        alert.setButton(DialogInterface.BUTTON_NEGATIVE, "Não",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        alert.dismiss();
                    }
                });

        alert.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = new Intent(this, ToRicoService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        toRicoService.setActivityHandler(null);
        unbindService(mConnection);
        Log.i("ToRico", "onPause");
    }

    private void loadPreferences(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        DecimalFormat floatFormat = new DecimalFormat("0.00");
        txtHora.setText(prefs.getString("horas_extras", "00:00:00"));
        txtGrana.setText("R$ " + String.valueOf(floatFormat.format(prefs.getFloat("grana_extra", 0))));
    }

    private void listInsertItem(){
        String data = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(Calendar.getInstance().getTime());

        adapter.add(data + "   " + txtHora.getText().toString() + "   " + txtGrana.getText().toString());
    }
}
