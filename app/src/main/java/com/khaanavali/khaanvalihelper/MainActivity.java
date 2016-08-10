package com.khaanavali.khaanvalihelper;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
public class MainActivity extends AppCompatActivity {


    private static final String PLACES_API_DEBUG = "http://kuruva.herokuapp.com/v1/admin/coverageArea";
    private static final String PLACES_API_RELEASE = "http://oota.herokuapp.com/v1/admin/coverageArea";
    private static final String TAG_SUBAREAS = "subAreas";
    private static final String TAG_NAME = "name";
    private static final String LOG_TAG = "Autocomplete";
    private ArrayList<String> mCityCoverage;
    ListView listView,addresslistview;
    SearchView search;
    LocationAdapter dataAdapter;
    TextView textview;
    Button btnSubmit,btnrefresh;
    private Switch mySwitch;
    private TextView switchStatus;
    private String url;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        mCityCoverage =  new ArrayList<String>();
        switchStatus = (TextView) findViewById(R.id.switchStatus);
        mySwitch = (Switch) findViewById(R.id.switch1);


        textview = (TextView)findViewById(R.id.textView4);
        btnSubmit= (Button) findViewById(R.id.btn_add);
        btnrefresh= (Button) findViewById(R.id.btn_refress);
        listView = (ListView) findViewById(R.id.area_listView);
        listView.setAdapter(dataAdapter);


        search = (SearchView)findViewById(R.id.searchView1);
        search.setQueryHint("Search Location");

        search.setIconified(false);
        url= PLACES_API_DEBUG;
        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                dataAdapter.filter(query);
                textview.setText(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                dataAdapter.filter(query);
                return false;
            }
        });
        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                String area =  textview.getText().toString();
                Toast.makeText(getApplicationContext(), area, Toast.LENGTH_LONG).show();
                textview.setText("");
                addthiscity(area);
            }
        });
        btnrefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                    getCityCoverage();
            }
        });

        //set the switch to ON
        mySwitch.setChecked(false);
        //attach a listener to check for changes in state
        mySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {

                if(isChecked){
                    url= PLACES_API_RELEASE;
                    switchStatus.setText("Release");
                }else{
                    url= PLACES_API_DEBUG;
                    switchStatus.setText("Debug");
                }

            }
        });

        //check the current state before we display the screen
        if(mySwitch.isChecked()){
            url= PLACES_API_RELEASE;
            switchStatus.setText("Release");
        }
        else {
            url= PLACES_API_DEBUG;
            switchStatus.setText("Debug");
        }
        getCityCoverage();
    }

    public void onReceiveCity()
    {
        dataAdapter = new LocationAdapter(this,
                R.layout.area_list,mCityCoverage);

        listView.setAdapter(dataAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
               // goBackwithAreaName(mCityCoverage.get(position));
            }
        });

    }

    public void getCityCoverage()
    {
        mCityCoverage.clear();

        new JSONAsyncTask().execute(url);
    }

    public  class JSONAsyncTask extends AsyncTask<String, Void, Boolean> {

        ProgressDialog dialog;

        public  JSONAsyncTask()
        {

        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = new ProgressDialog(MainActivity.this);
            dialog.setMessage("Loading, please wait");
           dialog.setTitle("Connecting server");
            dialog.show();
            dialog.setCancelable(false);
        }

        @Override
        protected Boolean doInBackground(String... urls) {
            try {

                //------------------>>
                HttpGet httppost = new HttpGet(urls[0]);
                HttpClient httpclient = new DefaultHttpClient();
                HttpResponse response = httpclient.execute(httppost);

                // StatusLine stat = response.getStatusLine();
                int status = response.getStatusLine().getStatusCode();

                if (status == 200) {
                    HttpEntity entity = response.getEntity();

                    String data = EntityUtils.toString(entity);
                    JSONArray jarray = new JSONArray(data);

                    for (int i = 0; i < jarray.length(); i++) {
                        JSONObject object = jarray.getJSONObject(i);
//9003692586
                        if(object.has(TAG_SUBAREAS)) {
                            JSONArray subAreasArray = object.getJSONArray(TAG_SUBAREAS);
                            for (int j = 0; j < subAreasArray.length(); j++) {
                                JSONObject city_object = subAreasArray.getJSONObject(j);
                                if(city_object.has(TAG_NAME)) {
                                    mCityCoverage.add(city_object.get(TAG_NAME).toString());
                                }
                            }
                        }
                    }
                    return true;
                }
            }  catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return false;
        }

        protected void onPostExecute(Boolean result) {
            dialog.cancel();
            if (result == false)
                Toast.makeText(getApplicationContext(), "Unable to fetch data from server", Toast.LENGTH_LONG).show();
            else
            {
                onReceiveCity();
            }

        }
    }
    public void addthiscity(String areaname)
    {
        new PUTJSONAsyncTask().execute(url,areaname);
    }
    public  class PUTJSONAsyncTask extends AsyncTask<String, Void, Boolean> {

        ProgressDialog dialog;

        public PUTJSONAsyncTask() {

        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = new ProgressDialog(MainActivity.this);
            dialog.setMessage("Loading, please wait");
            dialog.setTitle("Connecting server");
            dialog.show();
            dialog.setCancelable(false);
        }

        @Override
        protected Boolean doInBackground(String... urls) {
            try {
                ArrayList<NameValuePair> postParameters = new ArrayList<NameValuePair>();
                postParameters.add(new BasicNameValuePair("cityName", "Bangalore"));
                postParameters.add(new BasicNameValuePair("areaName", urls[1]));

                //------------------>>
                HttpPut httpput = new HttpPut(urls[0]);
                HttpClient httpclient = new DefaultHttpClient();
                UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(postParameters);
                httpput.setEntity(formEntity);
                HttpResponse response = httpclient.execute(httpput);

                // StatusLine stat = response.getStatusLine();
                int status = response.getStatusLine().getStatusCode();

                if (status == 200) {
                    HttpEntity entity = response.getEntity();

                    String data = EntityUtils.toString(entity);

                    return true;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }
        protected void onPostExecute(Boolean result) {
            dialog.cancel();
            if (result == false)
                Toast.makeText(getApplicationContext(), "Unable to fetch data from server", Toast.LENGTH_LONG).show();
            else
            {
                Toast.makeText(getApplicationContext(), "successfully added", Toast.LENGTH_LONG).show();
            }

        }
    }
    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
