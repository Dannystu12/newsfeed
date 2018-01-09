package com.example.android.newsfeed;

import java.net.URL;
import java.util.Date;

/**
 * Class to represent news item in app
 */

class NewsItem {

    //Represents article headline
    private String mHeadline;

    //Represents article category
    private String mSection;

    //Represents date article was published
    private Date mWebPublicationDate;

    //Represents the url of the article
    private URL mUrl;

    //Represents the thumbnail of the article
    private URL mThumbnailUrl;

    public NewsItem(String headline, String section, Date webPublicationDate,
                    URL url, URL thumbnailUrl){

        mHeadline = headline;
        mSection = section;
        mWebPublicationDate = webPublicationDate;
        mUrl = url;
        mThumbnailUrl = thumbnailUrl;
    }

    public String getHeadline(){
        return mHeadline;
    }

    public String getSection(){
        return mSection;
    }

    public Date getWebPublicationDate(){
        return mWebPublicationDate;
    }

    public URL getUrl(){
        return mUrl;
    }

    public URL getThumbnailUrl(){
        return mThumbnailUrl;
    }

}
