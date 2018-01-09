package com.example.android.newsfeed;

import android.content.AsyncTaskLoader;
import android.content.Context;

import java.util.List;

/**
 * Created by mgdog on 10/07/2017.
 */

class NewsItemLoader extends AsyncTaskLoader<List<NewsItem>>{

    private String mUrl;

    NewsItemLoader(Context context, String url){
        super(context);
        mUrl = url;
    }

    /**
     * Force the loader to load when its starts
     */
    @Override
    public void onStartLoading(){
        forceLoad();
    }

    /**
     * Queries guardian Api with URL in background
     * @return
     */
    @Override
    public List<NewsItem> loadInBackground(){

        if(mUrl.isEmpty()){
            return null;
        }

        //Fetch results of query
        return QueryUtils.fetchNewsItems(mUrl);
    }



}
