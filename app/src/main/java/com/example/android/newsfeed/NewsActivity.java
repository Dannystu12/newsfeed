package com.example.android.newsfeed;

import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static android.content.Intent.ACTION_VIEW;

public class NewsActivity extends AppCompatActivity implements LoaderCallbacks<List<NewsItem>>{

    private final String LOG_TAG = NewsActivity.class.getName();
    //News loader id
    private final int NEWS_LOADER_ID = 1;

    //Guardian API key
    private final String API_KEY = "c0c86898-4718-4693-9051-ab2875f72ae7";

    //Base query string
    private static final String BASE_QUERY_URL = "https://content.guardianapis.com/search?";

    private NewsItemAdapter newsItemAdapter;

    //Variable for error message view
    private TextView errorMessageView;

    //Variable to hold swipe refresh container for results list view
    private SwipeRefreshLayout swipeLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news);

        //Build blank list of news Items
        List<NewsItem> newsItems = new ArrayList<>();

        //Build array adapter and link it to the list view
        ListView listView = (ListView) findViewById(R.id.news_items_list);
        newsItemAdapter = new NewsItemAdapter(this, newsItems);
        listView.setAdapter(newsItemAdapter);

        //Create listner to take user to news item clicked on
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                //Get news item that has been clicked on
                NewsItem newsItem = (NewsItem) parent.getItemAtPosition(position);

                //Create intent to launch browser
                Intent intent = new Intent(ACTION_VIEW, Uri.parse(newsItem.getUrl().toString()));
                startActivity(intent);
            }
        });

        errorMessageView = (TextView) findViewById(R.id.error_text_view);

        //Set up pull down refresh
        swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Refresh();
            }
        });

        //set color for loading on refresh
        swipeLayout.setColorSchemeColors(ContextCompat.getColor(this,R.color.colorAccent));

        //Force refresh on launch
        Refresh();
    }

    /**
     * Tells avtivity to set up options menu on launch
     * Not this is an override meaning by default it does nothing
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


    /**
     * Takes a selected menu item an launches it if it is the settings menu
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();

        // If the settings menu item is chosen then we can start the settings activity
        if(id == R.id.action_settings){
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);

    }

    /**
     * Refreshes data in feed
     */
    private void Refresh(){

        //Make error message invisible
        errorMessageView.setVisibility(View.GONE);

        //Clear results pane
        newsItemAdapter.clear();

        //Set refresh circle to appear
        swipeLayout.setRefreshing(true);

        //Check internet connection, executing query if connected
        if(checkConnection()) {
            //Run a loader to query the Guardian API
            getLoaderManager().restartLoader(NEWS_LOADER_ID, null, NewsActivity.this);
        }else{
            //Stop refreshing of swipe refresh
            swipeLayout.setRefreshing(false);
            errorMessageView.setText(getString(R.string.no_connection));
            errorMessageView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Checks users connection and return true if connected to internet
     * @return
     */
    private boolean checkConnection(){
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    /**
     * Create a loader object with query URL
     * @param id
     * @param args
     * @return
     */
    @Override
    public Loader<List<NewsItem>> onCreateLoader(int id, Bundle args){


        //Get user preferences and build query from them
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        //Get preferences
        Set<String> sections = sharedPrefs.getStringSet(getString(R.string.settings_section_key), null);

        //Get base uri and convert to form that can be added to
        Uri baseUri = Uri.parse(BASE_QUERY_URL);
        Uri.Builder uriBuilder = baseUri.buildUpon();


        uriBuilder.appendQueryParameter("format", "json");
        uriBuilder.appendQueryParameter("show-fields", "headline,thumbnail");
        uriBuilder.appendQueryParameter("order-by", "newest");
        uriBuilder.appendQueryParameter("page-size", "25");

        if(sections != null && !sections.isEmpty()){
            StringBuilder sectionsToQuery = new StringBuilder();
            boolean firstIteration = true;

            for(String section : sections){

                //Add comma seperation in front of each entry after first
                if(!firstIteration){
                    sectionsToQuery.append("|");
                }else{
                    firstIteration = false;
                }
                sectionsToQuery.append(section);
            }

            //Add sections to Uri
            uriBuilder.appendQueryParameter("section", sectionsToQuery.toString());

        }

        uriBuilder.appendQueryParameter("api-key", API_KEY);

        return new NewsItemLoader(NewsActivity.this, uriBuilder.toString());
    }


    /**
     * Update the results list with results of the query
     * @param loader
     * @param data
     */
    @Override
    public void onLoadFinished(Loader<List<NewsItem>> loader, List<NewsItem> data){

        //Stop refreshing of swipe refresh
        swipeLayout.setRefreshing(false);

        //Update results if we received any
        if(data != null && !data.isEmpty()){
            newsItemAdapter.addAll(data);
        } else {
            errorMessageView.setText(getString(R.string.no_results));
            errorMessageView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<NewsItem>> loader){

    }


}
