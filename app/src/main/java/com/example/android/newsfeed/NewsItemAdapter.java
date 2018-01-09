package com.example.android.newsfeed;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.util.List;

/**
 * Created by Daniel on 05/07/2017.
 */

class NewsItemAdapter extends ArrayAdapter<NewsItem> {

    private String LOG_TAG = NewsItemAdapter.class.getName();

    //Cache of thumbnail downloads
    private Cache cache;

    /**
     * Set up adapter with list of news items
     *
     * @param context
     * @param newsItems
     */
    NewsItemAdapter(Context context, List<NewsItem> newsItems) {
        super(context, 0, newsItems);
        //Build a new image cache
        cache = new Cache(20);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        View listItem = convertView;
        Holder holder;

        if (listItem == null) {
            listItem = LayoutInflater.from(getContext()).inflate(R.layout.news_list_item, parent, false);

            //Get references for holder
            holder = new Holder();
            holder.headline = (TextView) listItem.findViewById(R.id.headline_text);
            holder.section = (TextView) listItem.findViewById(R.id.section_text);
            holder.date = (TextView) listItem.findViewById(R.id.date_text);
            holder.thumbnail = (ImageView) listItem.findViewById(R.id.thumbnail);

            //Link the holder to the list item view
            listItem.setTag(holder);
        } else {
            //If the view is being recycled fetch the holder that contains references to each element
            holder = (Holder) listItem.getTag();
        }

        NewsItem newsItem = getItem(position);

        holder.headline.setText(newsItem.getHeadline());
        holder.section.setText(newsItem.getSection());

        //Put date into localised date format
        String date = DateFormat.getDateInstance(DateFormat.SHORT).format(newsItem.getWebPublicationDate());
        holder.date.setText(date);

        URL thumbnailUrl = newsItem.getThumbnailUrl();

        Bitmap thumbnail = cache.getImage(thumbnailUrl);

        //If the thumbnail is not in the cache we need to download it
        if (thumbnail == null) {
            download(thumbnailUrl, holder.thumbnail);
        } else {
            holder.thumbnail.setImageBitmap(thumbnail);
        }

        return listItem;
    }


    /**
     * Class to holder view references to make list updating more efficient
     */
    private static class Holder {
        TextView headline;
        TextView section;
        TextView date;
        ImageView thumbnail;
    }

    /**
     * This method sets up new download task if applicable
     *
     * @param url
     * @param imageView
     */
    private void download(URL url, ImageView imageView) {
        if (cancelPotentialDownload(url, imageView)) {
            //Create download task
            ImageDownloaderTask task = new ImageDownloaderTask(imageView, cache);

            //Set imageview to use the image downloaded
            DownloadedDrawable downloadedDrawable = new DownloadedDrawable(task,
                    ContextCompat.getColor(this.getContext(), R.color.colorPrimaryDark));
            imageView.setImageDrawable(downloadedDrawable);
            task.execute(url);
        }
    }

    //** Bind download to the image view while being downloaded
    static class DownloadedDrawable extends ColorDrawable {
        private final WeakReference<ImageDownloaderTask> bitmapDownloaderTaskReference;

        DownloadedDrawable(ImageDownloaderTask bitmapDownloaderTask, int placeholderColor) {
            //First set image background to placeholder
            super(placeholderColor);

            //Store a reference to the downloader task
            bitmapDownloaderTaskReference =
                    new WeakReference<ImageDownloaderTask>(bitmapDownloaderTask);
        }

        ImageDownloaderTask getBitmapDownloaderTask() {
            return bitmapDownloaderTaskReference.get();
        }
    }

    /**
     * This method checks if an imagedownloader task has been created and if it has it cancels the
     * task if it does not have a url or the new url does not match the existing one
     *
     * @param url
     * @param imageView
     * @return
     */
    private static boolean cancelPotentialDownload(URL url, ImageView imageView) {
        ImageDownloaderTask bitmapDownloaderTask = getBitmapDownloaderTask(imageView);

        if (bitmapDownloaderTask != null) {
            URL bitmapUrl = bitmapDownloaderTask.getUrl();
            if ((bitmapUrl == null) || (!bitmapUrl.equals(url))) {
                //Cancel old download of different image
                bitmapDownloaderTask.cancel(true);
            } else {
                // The same URL is already being downloaded.
                return false;
            }
        }
        return true;
    }

    /**
     * Because we attach an instance of Download Drawable to the image view we can
     * request the image download task reference held by DownloadDrawable
     */
    private static ImageDownloaderTask getBitmapDownloaderTask(ImageView imageView) {
        if (imageView != null) {
            Drawable drawable = imageView.getDrawable();
            if (drawable instanceof DownloadedDrawable) {
                //We ask for the download task
                DownloadedDrawable downloadedDrawable = (DownloadedDrawable) drawable;
                return downloadedDrawable.getBitmapDownloaderTask();
            }
        }
        return null;
    }

    /**
     * Task to download bitmap images
     */
    class ImageDownloaderTask extends AsyncTask<URL, Void, Bitmap> {

        //Store reference to the image view to update
        //A weak reference isnt strong enough to keep an object in memory if nothing else refers to it
        private final WeakReference<ImageView> mImageViewReference;

        //Create a reference to the image cache so we can store images
        private final WeakReference<Cache> mCachceRef;

        private URL mDownloadUrl;


        public ImageDownloaderTask(ImageView imageView, Cache cache) {
            mImageViewReference = new WeakReference<ImageView>(imageView);
            mCachceRef = new WeakReference<Cache>(cache);
        }

        @Override
        protected Bitmap doInBackground(URL... params) {

            mDownloadUrl = params[0];

            try {
                return downloadBitmap(mDownloadUrl);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error closing input stream", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {

            //Dont update if the task has been cancelled
            if (isCancelled()) {
                bitmap = null;
            }

            //Check if reference to image view still exists
            if (mImageViewReference != null) {
                ImageView imageView = mImageViewReference.get();
                if (imageView != null) {
                    //Get the latest bitmap image downloader instance tied to the image view
                    ImageDownloaderTask imageDownloaderTask = getBitmapDownloaderTask(imageView);

                    //If the bitmap exists and the valid image downlaoader is this one then update the image
                    if (bitmap != null && this == imageDownloaderTask) {
                        imageView.setImageBitmap(bitmap);
                        mCachceRef.get().addImage(mDownloadUrl, bitmap);
                    }
                }
            }
        }

        //** Get URL being downloaded
        public URL getUrl() {
            return mDownloadUrl;
        }

        //** Download bitmap from specified URL */
        private Bitmap downloadBitmap(URL url) throws IOException {
            HttpURLConnection urlConnection = null;
            InputStream inputStream = null;
            try {
                //Try and create a http connection
                urlConnection = (HttpURLConnection) url.openConnection();
                int statusCode = urlConnection.getResponseCode();
                if (statusCode != HttpURLConnection.HTTP_OK) {
                    return null;
                }

                // Get inputstream and build a bitmap
                inputStream = urlConnection.getInputStream();
                if (inputStream != null) {
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    return bitmap;
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error downloading image from " + url, e);
                //This finally always executes even if bitmap is returned above
            } finally {

                if (urlConnection != null) {
                    urlConnection.disconnect();
                }

                if (inputStream != null) {
                    inputStream.close();
                }
            }
            return null;
        }

    }
}