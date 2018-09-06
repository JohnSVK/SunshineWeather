package com.example.android.sunshineweather;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.support.v7.widget.ShareActionProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.prefs.Preferences;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ForecastFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 */
public class ForecastFragment extends Fragment {

    public static final String FORECAST = "forecast";

    private OnFragmentInteractionListener mListener;
    private ArrayAdapter<String> mForecastAdapter;

    public ForecastFragment() {
        // Required empty public constructor
    }

    @Override
    public void onStart() {
        super.onStart();

        //update weather data
        updateWeatherData();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //enable options menu
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater){
        menuInflater.inflate(R.menu.menu_forecast, menu);
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();

        if(id == R.id.action_refresh){
            updateWeatherData();
        }
        if(id == R.id.action_settings){
            Intent settingsIntent = new Intent(getContext(), SettingsActivity.class);
            startActivity(settingsIntent);
        }
        if(id == R.id.action_map){
            openPreferredLocationMap();
        }

        return true;
    }

    private void openPreferredLocationMap(){
        // get location from preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        String location = prefs.getString(getString(R.string.pref_location_key), getString(R.string.pref_location_default));

        // build map url
        Uri mapUri = Uri.parse("geo:0,0?").buildUpon().appendQueryParameter("q",location).build();

        // start implicit intent
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, mapUri);
        if(mapIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivity(mapIntent);
        }
    }

    private void updateWeatherData(){
        //get location preference
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        String locationStr = prefs.getString(getString(R.string.pref_location_key), getString(R.string.pref_location_default));

        //fetch weather data and update UI
        FetchWeatherTask weatherTask = new FetchWeatherTask();
        weatherTask.execute(locationStr);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        //initialize adapter
        mForecastAdapter = new ArrayAdapter<>(getActivity(), R.layout.list_item_forecast,
                R.id.list_item_forecast_textview);

        //attach adapter
        ListView listViewForecast = (ListView) rootView.findViewById(R.id.listview_forecast);
        listViewForecast.setAdapter(mForecastAdapter);
        listViewForecast.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String forecast = mForecastAdapter.getItem(i);
                Intent detailIntent = new Intent(getContext(), DetailActivity.class);
                detailIntent.putExtra(ForecastFragment.FORECAST, forecast);
                startActivity(detailIntent);
            }
        });

        // Inflate the layout for this fragment
        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

        //private String baseUrl = "http://api.openweathermap.org/data/2.5/forecast/daily?q=94043&mode=json&units=metric&cnt=7";
        private final String APIKEY = BuildConfig.OPEN_WEATHER_MAP_API_KEY;

        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();
        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("Loading weather data...");
            progressDialog.show();
        }

        @Override
        protected String[] doInBackground(String... params) {

            if(params.length == 0){
                return null;
            }
            if(params[0].isEmpty()){
                return null;
            }

            String format = "json";
            String units = "metric";
            int numDays = 7;

            final String FORECAST_BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?";
            final String QUERY_PARAM = "q";
            final String FORMAT_PARAM = "mode";
            final String UNITS_PARAM = "units";
            final String DAYS_PARAM = "cnt";
            final String APPID_PARAM = "APPID";

            Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                    .appendQueryParameter(QUERY_PARAM, params[0])
                    .appendQueryParameter(FORMAT_PARAM, format)
                    .appendQueryParameter(UNITS_PARAM, units)
                    .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                    .appendQueryParameter(APPID_PARAM, APIKEY)
                    .build();

            String requestUrl = builtUri.toString();

            //perform GET HTTP request and return result string
            String forecastJsonStr = HttpConnectionHelper.requestGetResult(requestUrl);
            //Log.e("DATA", forecastJsonStr);

            try {
                String forecastDataStr[] = getForecastDataFromJson(forecastJsonStr);
                Log.e("DAY FORECAST", forecastDataStr[0]);
                return forecastDataStr;
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String... result) {
            progressDialog.dismiss();

            //update the listview
            mForecastAdapter.clear();
            mForecastAdapter.addAll(result);
            mForecastAdapter.notifyDataSetChanged();
        }

        private String[] getForecastDataFromJson(String forecastJsonStr) throws JSONException {
            ArrayList<String> daysForecastStrList = new ArrayList<>();

            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_TEMPERATURE = "temp";
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";
            final String OWM_DESCRIPTION = "main";

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray forecastArray = forecastJson.getJSONArray(OWM_LIST);

            for(int i=0;i<forecastArray.length();i++){
                JSONObject dayForecastJson = forecastArray.getJSONObject(i);
                JSONObject weatherObj = dayForecastJson.getJSONArray(OWM_WEATHER).getJSONObject(0);
                JSONObject temperatureObj = dayForecastJson.getJSONObject(OWM_TEMPERATURE);

                //day weather label
                String weatherDaytime = getDateStrByIndex(i);
                //day weather description
                String weatherDescription = weatherObj.getString(OWM_DESCRIPTION);
                //day weather temperature
                double temp_max = Math.round(temperatureObj.getDouble(OWM_MAX));
                double temp_min = Math.round(temperatureObj.getDouble(OWM_MIN));

                //units conversion
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                String unitsStr = prefs.getString(getString(R.string.pref_units_key), getString(R.string.pref_units_default));
                if(unitsStr.equals(getString(R.string.pref_units_imperial))){
                    temp_max = Math.round(temp_max*1.8 + 32);
                    temp_min = Math.round(temp_min*1.8 + 32);
                }
                String weatherTemperature = (int)temp_max + "/" + (int)temp_min;


                daysForecastStrList.add(weatherDaytime + " - " + weatherDescription + " - " + weatherTemperature);
            }

            return daysForecastStrList.toArray(new String[0]);
        }

        //format long date to date string
        private String getDateStrByIndex(int dayIndex){
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE, dayIndex);
            Date date = calendar.getTime();
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE MMM dd");

            return simpleDateFormat.format(date);
        }
    }
}
