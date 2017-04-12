package com.labs.okey.oneride.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.LruCache;
import android.widget.ImageView;

import com.android.volley.Cache;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;

/**
 * @author Oleg Kleiman
 * created 12-Apr-15.
 */
/*
  * This class is primarily used with Layout DataBinding.
  * Its only method - loadInto() - is intended to be called from
  * @BindingAdapter methods used as extension to XML Layout
 */

public class VolleyImageLoader {

    private static final String LOG_TAG = "VolleyImageLoader";

    public static void loadInto(final ImageView v, String url) {
        RequestQueue requestQueue = Globals.volley.getRequestQueue();

        Cache cache = requestQueue.getCache();
        Cache.Entry entry = cache.get(url);

        if (entry != null) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(entry.data, 0, entry.data.length);
            v.setImageBitmap(bitmap);
        } else {

            ImageLoader imageLoader = new ImageLoader(requestQueue,
                    new ImageLoader.ImageCache() {
                        private final LruCache<String, Bitmap> cache = new LruCache<>(20);

                        @Override
                        public Bitmap getBitmap(String url) {
                            return cache.get(url);
                        }

                        @Override
                        public void putBitmap(String url, Bitmap bitmap) {
                            cache.put(url, bitmap);
                        }
                    });

            if( !url.contains("https") )
                url = url.replace("http", "https");

            imageLoader.get(url,
                    new ImageLoader.ImageListener() {
                        @Override
                        public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                            Bitmap bitmap = response.getBitmap();
                            if (bitmap != null)
                                v.setImageBitmap(bitmap);
                        }

                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Globals.__log(LOG_TAG, error.toString());
                        }
                    });
        }
    }
}
