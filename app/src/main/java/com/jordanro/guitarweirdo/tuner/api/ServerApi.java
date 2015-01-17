package com.jordanro.guitarweirdo.tuner.api;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

/**
 * Created by swu on 1/16/15.
 */
public class ServerApi {
    private static final String BASE_URL = "http://SITE";

    private static AsyncHttpClient client = new AsyncHttpClient();

    // get JSON notes, turn it into an array
    public static void get(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.post(getAbsoluteUrl(url), params, responseHandler);
    }

    // send index of pause + new note
    public static void post(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.post(getAbsoluteUrl(url), params, responseHandler);
    }

    private static String getAbsoluteUrl(String relativeUrl) {
        return BASE_URL + relativeUrl;
    }
}
