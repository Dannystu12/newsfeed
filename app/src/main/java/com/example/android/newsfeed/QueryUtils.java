package com.example.android.newsfeed;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Daniel on 06/07/2017.
 */

class QueryUtils {
    private static final String LOG_TAG = QueryUtils.class.getName();

    private static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    //** Private Constructor to stop instantiation
    private QueryUtils(){}

    public static List<NewsItem> fetchNewsItems(String url){

        //conver String to actual URL
        URL queryUrl = convertStringToUrl(url);

        String jsonResponse = null;

        try{
            jsonResponse = makeHttpRequest(queryUrl);
        }catch(IOException e){
            Log.e(LOG_TAG, "Error Making Http Request", e);
        }

        return extractFeaturesFromJson(jsonResponse);
    }

    /**
     * Parses JSON reponse string and builds a list of new item objects
     * @param jsonResponse
     * @return
     */
    private static List<NewsItem> extractFeaturesFromJson(String jsonResponse){

        //Build empty list of results
        List<NewsItem> newsItems = new ArrayList<>();

        try{
            //Get list of articles from response
            JSONObject jsonResponseObject = new JSONObject(jsonResponse);
            jsonResponseObject = jsonResponseObject.getJSONObject("response");
            JSONArray resultsList = jsonResponseObject.getJSONArray("results");

            //Loop through each article result
            for(int i = 0; i < resultsList.length(); i++){
                JSONObject result = (JSONObject) resultsList.get(i);

                String section = result.getString("sectionName");
                Date webPublicationDate = DATE_FORMAT.parse(result.getString("webPublicationDate"));
                URL url = new URL(result.getString("webUrl"));

                JSONObject fields = result.getJSONObject("fields");
                String headline = fields.getString("headline");
                URL thumbnailUrl = new URL(fields.getString("thumbnail"));

                //Create new newsItem object and add it to our results list
                NewsItem newsItem = new NewsItem(headline,section, webPublicationDate, url, thumbnailUrl);
                newsItems.add(newsItem);
            }
        }catch(JSONException e){
            Log.e(LOG_TAG, "Error parsing JSON", e);
        }catch(ParseException e){
            Log.e(LOG_TAG, "Error parsing date from JSON", e);
        }catch(MalformedURLException e){
            Log.e(LOG_TAG, "Error parsing URL from JSON", e);
        }

        return newsItems;
    }

    /**
     * Attempts to convert string url to a URL object. Returns null if an issue
     * @param url
     * @return
     */
    private static URL convertStringToUrl(String url){

        //If the string is empty just return
        if (url.isEmpty()) {
            return null;
        }

        URL convertedUrl = null;

        //Try and convert the String to a URL
        try {
            convertedUrl = new URL(url);
        } catch (MalformedURLException e) {
            Log.e(LOG_TAG, "Error converting string to url", e);
        }

        return convertedUrl;
    }


    /**
     * Makes a Http GET request and returns json response string
     * @param url
     * @return
     * @throws IOException
     */
    private static String makeHttpRequest(URL url) throws IOException{

        String jsonResponse = "";

        if(url == null){
            return jsonResponse;
        }

        HttpURLConnection connection = null;
        InputStream inputStream = null;

        //Try and open a connection
        try{
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setReadTimeout(10000);
            connection.setConnectTimeout(15000);

            //Make the connection
            connection.connect();

            //Extract the JSON from connection if response is fine
            if(connection.getResponseCode() == HttpURLConnection.HTTP_OK){
                inputStream = connection.getInputStream();
                jsonResponse = readFromStream(inputStream);
            }
        }catch(IOException e){
            Log.e(LOG_TAG, "Error oppening HTTP connection or reading stream", e);
        } finally {
            //Close connection and inputstream
            if(connection != null){
                connection.disconnect();
            }

            if(inputStream != null){
                inputStream.close();
            }
        }

        return jsonResponse;
    }


    /**
     * Reads text from an input stream and returns string response
     * @param inputStream
     * @return
     * @throws IOException
     */
    private static String readFromStream(InputStream inputStream) throws  IOException{

        StringBuilder json = new StringBuilder();

        if(inputStream != null) {
            //Create reader to read text from stream
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String line = reader.readLine();

            while (line != null) {
                json.append(line);
                line = reader.readLine();
            }
        }

        return json.toString();
    }

}
