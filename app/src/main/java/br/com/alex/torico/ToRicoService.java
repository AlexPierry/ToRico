package br.com.alex.torico;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Bundle;
import android.app.Service;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class ToRicoService extends Service implements Runnable{
    private float grana;
    private float valorHoraExtra;
    private String tempo;
    private SharedPreferences prefs;

    private Handler myHandler;
    private Handler activityHandler;

    public void onCreate(){
        Log.i("ToRico", "onCreate do Serviço");
        myHandler = new Handler();
        loadSharedPrefs();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new ServiceBinder();
    }

    public void setActivityHandler(Handler handler){
        activityHandler = handler;
    }

    @Override
    public void run() {
        myHandler.postAtTime(this, SystemClock.uptimeMillis() + 1000);

        Log.i("ToRico", "Passou no Run");

        DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        Date dataAux;
        try{
            dataAux = dateFormat.parse(tempo);
        } catch (ParseException e) {
            e.printStackTrace();
            return;
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(dataAux);
        cal.add(Calendar.SECOND, 1);

        grana += (valorHoraExtra / 60 / 60);
        tempo = dateFormat.format(cal.getTime());

        setSharedPrefs();

        if (activityHandler != null) {
            Bundle bundle = new Bundle();
            bundle.putFloat("grana_extra", grana);
            bundle.putString("horas_extras", tempo);
            Message msg = new Message();
            msg.setData(bundle);
            activityHandler.sendMessage(msg);
        }
    }

    public void startToRico(){
        loadSharedPrefs();
        myHandler.post(this);
    }

    public void stopToRico(){
        grana = 0;
        tempo = "00:00:00";
        setSharedPrefs();
        myHandler.removeCallbacks(this);
    }

    public void pauseToRico(){
        myHandler.removeCallbacks(this);
    }

    @Override
    public void onDestroy(){
        myHandler.removeCallbacks(this);
    }

    private void loadSharedPrefs(){
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        float salarioBruto = Float.parseFloat(prefs.getString("salario_bruto", "0"));
        int horasMensais = Integer.parseInt(prefs.getString("horas_mensais", "0"));
        float horaExtra = Float.parseFloat(prefs.getString("hora_extra", "0"));
        float valorHoraNormal = salarioBruto / horasMensais;

        valorHoraExtra = valorHoraNormal * ((horaExtra / 100) + 1);

        grana = prefs.getFloat("grana_extra", 0);
        tempo = prefs.getString("horas_extras", "00:00:00");
    }

    private void setSharedPrefs(){
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("horas_extras", tempo);
        editor.putFloat("grana_extra", grana);
        editor.commit();
    }


    public class ServiceBinder extends Binder{
        public ToRicoService getService(){
            return ToRicoService.this;
        }
    }
}
