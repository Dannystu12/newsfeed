package com.example.android.newsfeed;

import android.graphics.Bitmap;

import java.net.URL;
import java.util.LinkedHashMap;

/**
 * This class holds an ordered cache of images replacing old images with new ones FIFO
 */

class Cache {

    int mMaxSize;
    LinkedHashMap<URL, Bitmap> mCache;

    /**
     * Initialise cache to max number of images
     * @param noOfImages
     */
    public Cache(int noOfImages){
        mMaxSize = noOfImages;
        mCache = new LinkedHashMap<>(noOfImages);
    }

    public void addImage(URL url, Bitmap bitmap){


            //Check to see if the image is already in the cache
            if(mCache.containsKey(url)){
                return;
            }

            //If the cache is full delete the first entry
            if(mCache.size() == mMaxSize){
                mCache.remove(mCache.keySet().toArray()[0]);
            }

            //Add the new entry
            mCache.put(url, bitmap);
    }

    /**
     * Returns bitmap assosciated with a given URL and returns null otherwise
     * @param url
     * @return
     */
    public Bitmap getImage(URL url){
        return mCache.get(url);
    }
}

