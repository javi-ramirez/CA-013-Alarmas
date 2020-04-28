package com.example.ca_013_alarmas;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView txtDescripcion, txtSonido, txtLugar;
    private TimePicker tpHora;
    private ToggleButton tbnActivado;
    private Button btnSonido, btnLugar, btnAnterior, btnSiguiente;
    private String alarmas[][];
    private int indiceAlarmas;
    private Thread hilo;
    private boolean ejecutando = true;
    private MediaPlayer sonido;
    private Activity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtDescripcion=(TextView) findViewById(R.id.txtNombreAlarma);
        txtSonido=(TextView) findViewById(R.id.txtSonido);
        txtLugar=(TextView)findViewById(R.id.txtLugar);
        tpHora =(TimePicker) findViewById(R.id.timePicker1);
        tbnActivado=(ToggleButton) findViewById(R.id.tbnActivado);
        btnSonido=(Button) findViewById(R.id.btnSonido);
        btnLugar=(Button) findViewById(R.id.btnLugar);
        btnSiguiente=(Button)findViewById(R.id.btnSiguiente);
        btnAnterior=(Button)findViewById(R.id.btnAnterior);
        activity=this;


        //Se inicializa el arreglo y se le asigna una hora por default
        alarmas=new String [5][5];
        for(int i=0; i < 5; i++)
        {
            alarmas[i][0]="";
            alarmas[i][1]="";
            alarmas[i][2]="";
            alarmas[i][3]="1200";
            alarmas[i][4]="false";
        }
        try
        {
            File ruta_directorio= Environment.getExternalStorageDirectory();
            File f = new File(ruta_directorio.getAbsolutePath(), "alarmas.dat");
            BufferedReader fin= new BufferedReader(new InputStreamReader(new FileInputStream(f)));
            String texto;
            indiceAlarmas =0;
            while(indiceAlarmas < 5)
            {
                texto=fin.readLine();
                if(texto !=null)
                {
                    alarmas[indiceAlarmas][0]=texto;
                }

                texto=fin.readLine();
                if(texto !=null)
                {
                    alarmas[indiceAlarmas][1]=texto;
                }

                texto=fin.readLine();
                if(texto !=null)
                {
                    alarmas[indiceAlarmas][2]=texto;
                }

                texto=fin.readLine();
                if(texto !=null)
                {
                    alarmas[indiceAlarmas][3]=texto;
                }

                texto=fin.readLine();
                if(texto !=null)
                {
                    alarmas[indiceAlarmas][4]=texto;
                }

                indiceAlarmas++;
            }
            fin.close();

        }
        catch (Exception e)
        {
            Log.e("Archivo",e.toString());
        }

        indiceAlarmas=0;
        establecerDatos1();

        btnSiguiente.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                guardarDatosArreglo();
                indiceAlarmas++;
                if(indiceAlarmas ==5)
                {
                    indiceAlarmas=0;
                }
                establecerDatos();
            }
        });

        btnAnterior.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                guardarDatosArreglo();
                indiceAlarmas--;
                if(indiceAlarmas==-1)
                {
                    indiceAlarmas=4;
                }
                establecerDatos();
            }
        });

        btnSonido.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("audio/*");
                startActivityForResult(intent, 1);
            }
        });

        btnLugar.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                {
                    ActivityCompat.requestPermissions(getParent(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION,}, 1000);
                }
                else
                    {
                    locationStart();
                }
            }
        });

        hilo=new Thread(new Runnable() {
            @Override
            public void run() {
                try
                {
                    while (ejecutando)
                    {
                        consultarAlarmas();
                        SystemClock.sleep(1000);
                    }
                }
                catch(Exception e)
                {
                    Log.e("Error Hilo", e.toString());
                }
            }
        });

        hilo.start();
    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data){
        if(resultCode ==Activity.RESULT_OK)
        {
            txtSonido.setText(data.getData().toString());
        }
        if (requestCode==2)
        {
            Place place =PlacePicker.getPlace(data, this);
            String toastMsg =String.format("Place: %s", place.getAddress());
            txtLugar.setText(toastMsg);
            Toast.makeText(this, toastMsg, Toast.LENGTH_LONG).show();
        }
    }

    public void establecerDatos(){
        txtDescripcion.setText(alarmas[indiceAlarmas][0]);
        txtSonido.setText(alarmas[indiceAlarmas][1]);
        txtLugar.setText(alarmas[indiceAlarmas][2]);
        tpHora.setCurrentHour(Integer.parseInt(alarmas[indiceAlarmas][3].substring(0,2)));
        tpHora.setCurrentMinute(Integer.parseInt(alarmas[indiceAlarmas][3].substring(2,4)));
        tbnActivado.setChecked(Boolean.parseBoolean(alarmas[indiceAlarmas][4]));
    }

    public void establecerDatos1(){
        txtDescripcion.setText(alarmas[indiceAlarmas][0].substring(9,alarmas[indiceAlarmas][0].length()));
        txtSonido.setText(alarmas[indiceAlarmas][1]);
        txtLugar.setText(alarmas[indiceAlarmas][2]);
        tpHora.setCurrentHour(Integer.parseInt(alarmas[indiceAlarmas][3].substring(0,2)));
        tpHora.setCurrentMinute(Integer.parseInt(alarmas[indiceAlarmas][3].substring(2,4)));
        tbnActivado.setChecked(Boolean.parseBoolean(alarmas[indiceAlarmas][4]));
    }

    public void guardarDatosArreglo(){
        alarmas[indiceAlarmas][0] = txtDescripcion.getText().toString();
        alarmas[indiceAlarmas][1]=txtSonido.getText().toString();
        alarmas[indiceAlarmas][2]=txtLugar.getText().toString();
        if(tpHora.getCurrentHour() < 10)
        {
            alarmas[indiceAlarmas][3]="0" + tpHora.getCurrentHour();
        }
        else
            {
            alarmas[indiceAlarmas][3]=String.valueOf(tpHora.getCurrentHour());
        }

        if(tpHora.getCurrentMinute() < 10)
        {
            alarmas[indiceAlarmas][3]+="0" + tpHora.getCurrentMinute();
        }
        else
            {
            alarmas[indiceAlarmas][3] +=String.valueOf(tpHora.getCurrentMinute());
        }

        alarmas[indiceAlarmas][4]= String.valueOf(tbnActivado.isChecked());

        guardarArchivo();
    }

    public void guardarArchivo(){
        String filename = "miarchivo";
        String string = "Alarma";
        FileOutputStream outputStream;
        try
        {
            outputStream=openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(string.getBytes());
            outputStream.close();
        }
        catch (Exception e)
        {
            Log.e("Error",e.toString());
        }

        String texto ="";
        String archivo= "miArchivo";
        File arch;
        File ruta_archivo = Environment.getExternalStorageDirectory();
        File localfile = new File(ruta_archivo.getAbsolutePath());

        if(!localfile.exists())
        {
            localfile.mkdirs();
        }

        texto =(archivo +".txt");
        arch =new File (localfile,texto);

        try
        {
            arch.createNewFile();
        }
        catch(Exception e)
        {
            Log.e("Error",e.toString());
        }
    }

    private void consultarAlarmas(){
        String horaActual, minutoActual;
        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        horaActual = sdf.format(now).substring(0,2);
        minutoActual= sdf.format(now).substring(3,5);

        for(int i=0; i<5; i++)
        {
            if(alarmas[i][4]=="true")
            {
                if(horaActual.compareTo(alarmas[i][3].substring(0,2))==0 && minutoActual.compareTo(alarmas[i][3].substring(2,4))==0)
                {
                    if (sonido == null)
                    {
                        sonido= MediaPlayer.create(getApplicationContext(), Uri.parse(alarmas[i][1]));
                        sonido.start();
                    }
                    else
                        {
                        if(!sonido.isPlaying())
                        {
                            sonido.release();
                            sonido=null;
                            sonido=MediaPlayer.create(getApplicationContext(),Uri.parse(alarmas[i][1]));
                            sonido.start();
                        }
                    }

                    agregarNotificacion(i);
                }
            }
        }
    }

    private void locationStart()
    {
        LocationManager mlocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Localizacion Local = new Localizacion();
        Local.setMainActivity(this);
        final boolean gpsEnabled = mlocManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!gpsEnabled)
        {
            Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(settingsIntent);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,}, 1000);
            return;
        }
        mlocManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, (LocationListener) Local);
        mlocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, (LocationListener) Local);

        txtLugar.setText("");
    }


    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        if (requestCode == 1000)
        {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                locationStart();
                return;
            }
        }
    }

    public void setLocation(Location loc)
    {
        if (loc.getLatitude() != 0.0 && loc.getLongitude() != 0.0)
        {
            try
            {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> list = geocoder.getFromLocation(
                        loc.getLatitude(), loc.getLongitude(), 1);
                if (!list.isEmpty())
                {
                    Address DirCalle = list.get(0);
                    txtLugar.setText(DirCalle.getAddressLine(0));
                }
            }
            catch (IOException e)
            {
                Log.e("Error",e.toString());
            }
        }
    }

    public class Localizacion implements LocationListener
    {
        MainActivity mainActivity;

        public MainActivity getMainActivity()
        {
            return mainActivity;
        }

        public void setMainActivity(MainActivity mainActivity)
        {
            this.mainActivity = mainActivity;
        }

        //Método para cuando el usuario o e dispositivo en este cso cambia de ubicación
        @Override
        public void onLocationChanged(Location loc)
        {
            loc.getLatitude();
            loc.getLongitude();
            String Text = "Mi ubicacion actual es: " + "\n Lat = "
                    + loc.getLatitude() + "\n Long = " + loc.getLongitude();
            //  mensaje1.setText(Text);
            this.mainActivity.setLocation(loc);
        }

        //Mensaje a mostrar en caso de que el GPS del dispositivo este desactivado
        @Override
        public void onProviderDisabled(String provider)
        {
            txtLugar.setText("GPS Desactivado");
        }

        //Mensaje a mostrar en caso de que el GPS del dispositiv es activado
        @Override
        public void onProviderEnabled(String provider)
        {
            txtLugar.setText("GPS Activado");
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras)
        {
            switch (status)
            {
                case LocationProvider.AVAILABLE:
                    Log.d("debug", "LocationProvider.AVAILABLE");
                    break;
                case LocationProvider.OUT_OF_SERVICE:
                    Log.d("debug", "LocationProvider.OUT_OF_SERVICE");
                    break;
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    Log.d("debug", "LocationProvider.TEMPORARILY_UNAVAILABLE");
                    break;
            }
        }
    }
    public void agregarNotificacion(int i){
        int notificationID = 1000+i;
        Intent intent = new Intent(this,com.example.ca_013_alarmas.MainActivity.class);
        intent.putExtra("notificationID",notificationID);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,0,intent,0);
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        CharSequence ticker = alarmas[i][0];
        CharSequence content = "Alarma";
        CharSequence contentText = (alarmas[i][0]+"-"+alarmas[i][2]);
        Notification noti = new Notification.Builder(this).setContentTitle(content).setContentText(contentText).setSmallIcon(R.mipmap.ic_launcher).setContentIntent(pendingIntent).setAutoCancel(true).getNotification();
        nm.notify(notificationID,noti);

    }
}
