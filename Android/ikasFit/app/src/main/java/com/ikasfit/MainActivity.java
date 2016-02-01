package com.ikasfit;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;
import com.parse.Parse;
import com.parse.ParseObject;
import com.parse.ParseUser;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.text.DateFormat.getDateInstance;
import static java.text.DateFormat.getTimeInstance;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "IkasFit"; //Tag de aplicacion para el LOG

    private GoogleApiClient mClient = null; // Variable de la conexion con googleFit

    private Button btnSinc;

    private int googleFitConexion; // 0 no ,1 si

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "Abriendo aplicacion");
        googleFitConexion = 0;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Prepara el contenido del layout
        conexionLayout();
        //Conexion con parse
        Parse.initialize(this);
        ParseUser.enableAutomaticUser();
        //Crear conxion con google Fit
        crearClienteGF();


    }

    private void conexionLayout(){
        btnSinc = (Button) findViewById(R.id.btnSinc);
        btnSinc.setOnClickListener(btnClickSincD);
    }

    View.OnClickListener btnClickSincD = new View.OnClickListener() {
        public void onClick(View v) {
            sincData();

        }
    };

    private void sincData() {
        if (googleFitConexion == 1) {
            new getDeltaSteps().execute();
        }else{
            CharSequence text = "Todabia no se ha realizado la conexión con googleFit";
            Toast toast =  Toast.makeText(getApplicationContext(),text, Toast.LENGTH_LONG);
            toast.show();
        }
    }


    private void crearClienteGF(){


        if (mClient == null) {

            mClient = new GoogleApiClient.Builder(this)
                    .addApi(Fitness.HISTORY_API)
                    .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ))
                    .addConnectionCallbacks(
                            new GoogleApiClient.ConnectionCallbacks() {
                                @Override
                                public void onConnected(Bundle bundle) {
                                    googleFitConexion =1;
                                    CharSequence text = "Conectado con googleFit";
                                    Toast toast =  Toast.makeText(getApplicationContext(),text, Toast.LENGTH_LONG);
                                    toast.show();
                                }

                                @Override
                                public void onConnectionSuspended(int i) {
                                    if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST) {
                                        Log.i(TAG, "Connection lost.  Cause: Network Lost.");
                                    } else if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED) {
                                        Log.i(TAG, "Connection lost.  Reason: Service Disconnected");
                                    }
                                }
                            }
                    )
                    .enableAutoManage(this, 0, new GoogleApiClient.OnConnectionFailedListener() {
                        @Override
                        public void onConnectionFailed(ConnectionResult result) {
                            Log.i(TAG, "Google Play services connection failed. Cause: " +
                                    result.toString());
                            Log.i(TAG, "Exception while connecting to Google Play services: " +
                                    result.getErrorMessage());
                        }
                    })
                    .build();
        }

    }

    //Clase para la ejecucion de codigo en segundo plano
    private class getDeltaSteps extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... params) {
            DataReadRequest readRequest = queryFitnessData(); //Obtenemos los resultados de las query
            DataReadResult dataReadResult =
                    Fitness.HistoryApi.readData(mClient, readRequest).await(1, TimeUnit.MINUTES);
            Log.i(TAG, "Intentando imprimir datos");
            uploadData(dataReadResult);
            return null;
        }
    }


    //Una vez obtenidos los datos los subimos a parse
    private void uploadData(DataReadResult dataReadResult){

        // [START parse_read_data_result]
        // If the DataReadRequest object specified aggregated data, dataReadResult will be returned
        // as buckets containing DataSets, instead of just DataSets.
        if (dataReadResult.getBuckets().size() > 0) {
            for (Bucket bucket : dataReadResult.getBuckets()) {
                List<DataSet> dataSets = bucket.getDataSets();
                for (DataSet dataSet : dataSets) {
                    dumpDataSet(dataSet);
                }
            }
        } else if (dataReadResult.getDataSets().size() > 0) {
            for (DataSet dataSet : dataReadResult.getDataSets()) {
                dumpDataSet(dataSet);
            }
        }
        // [END parse_read_data_result]

    }
    private void dumpDataSet(DataSet dataSet) {
        DateFormat dateFormat = getTimeInstance();

        //Cambiar el formato de fecha? fecha con dias
        for (DataPoint dp : dataSet.getDataPoints()) {
            ParseObject testObject = new ParseObject("PasosDia");
            testObject.put("Steps", dp.getValue(Field.FIELD_STEPS).toString());
            testObject.put("StartDate",dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));
            testObject.put("EndDate",dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)));
            testObject.put("Usuario",ParseUser.getCurrentUser());
            testObject.saveInBackground();
        }

    }

    //Especificamos los datos que queremos obtener
    private DataReadRequest queryFitnessData() {
        // [START build_read_data_request]
        // Creacion del rango de tiempo, con fecha de inicio y final.
        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
        long endTime = cal.getTimeInMillis(); //Fecha final, obtenemos la de ahora --
        cal.add(Calendar.WEEK_OF_YEAR, -1); //Aqui podemos cambiar el valor de inicio de la diferencia de pasos
        long startTime = cal.getTimeInMillis();

        java.text.DateFormat dateFormat = getDateInstance();
        Log.i(TAG, "Range Start: " + dateFormat.format(startTime));
        Log.i(TAG, "Range End: " + dateFormat.format(endTime));
        // Creacion del objeto readRequest con el tipo de dato que obtendremos, utilizaremos el rango de tiempo para obtener los pasos realizados
        DataReadRequest readRequest = new DataReadRequest.Builder()
                // The data request can specify multiple data types to return, effectively
                // combining multiple data queries into one call.
                // In this example, it's very unlikely that the request is for several hundred
                // datapoints each consisting of a few steps and a timestamp.  The more likely
                // scenario is wanting to see how many steps were walked per day, for 7 days.
                //.read(DataType.TYPE_DISTANCE_DELTA)
                .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                        // Analogous to a "Group By" in SQL, defines how data should be aggregated.
                        // bucketByTime allows for a time span, whereas bucketBySession would allow
                        // bucketing by "sessions", which would need to be defined in code.
                .bucketByTime(1, TimeUnit.DAYS) //devolvera por cada dia el numero de pasos realizados, aqui se cambia el periodo de cada suma,(cada dia, cada dos, cada hora etc...)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();
        // [END build_read_data_request]
        return readRequest;
    }




}
