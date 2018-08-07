package com.example.xyzreader.remote;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.xyzreader.utils.DataUtils;
import com.example.xyzreader.data.ItemsContract;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Call;
import okhttp3.EventListener;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;


public class DataProvider {

    private static final String TAG = DataProvider.class.getSimpleName();
    private static final String BASE_URL = "https://go.udacity.com/";
    private static final int CONNECTION_TIMEOUT = 10;

    private static final String[] PROJECTION = {
            ItemsContract.Items._ID,
            ItemsContract.Items.TITLE,
            ItemsContract.Items.PUBLISHED_DATE,
            ItemsContract.Items.AUTHOR,
            ItemsContract.Items.THUMB_URL,
            ItemsContract.Items.PHOTO_URL,
            ItemsContract.Items.ASPECT_RATIO,
    };

    private static final int _ID = 0;
    private static final int TITLE = 1;
    private static final int PUBLISHED_DATE = 2;
    private static final int AUTHOR = 3;
    private static final int THUMB_URL = 4;
    private static final int PHOTO_URL = 5;
    private static final int ASPECT_RATIO = 6;

    private final Context context;
    private DataListener dataListener;

    private Disposable dbQueryDisposable;
    private Disposable dataFetchDisposable;
    private Disposable dbInsertDisposable;


    public enum ErrorType {
        REQUEST_TIME_OUT,
        NO_NETWORK_CONNECTION
    }

    public interface DataListener {
        void onConnectionError(ErrorType error, @Nullable Throwable throwable);
        void onDataAvailable(List<BookCover> books);
    }

    interface ApiCalls {
        @GET("xyz-reader-json")
        Observable<List<Book>> getBookList();
    }


    public DataProvider(Context context, DataListener listener) {
        this.context = context;
        this.dataListener = listener;
    }

    public void init() {
        dbQueryDisposable = queryDbForBooks();
    }

    public void dispose() {
        DataUtils.dispose(dataFetchDisposable, dbQueryDisposable, dbInsertDisposable);
    }

    public void refreshList() {
        deleteFromDb();
    }

    private Disposable queryDbForBooks() {
        Uri uri = ItemsContract.Items.buildDirUri();
        //noinspection ConstantConditions
        return Observable.just(context.getContentResolver().query(
                uri,
                PROJECTION,
                null,
                null,
                ItemsContract.Items.DEFAULT_SORT))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleDbResponse, e -> Log.e(TAG, "Error quering DB", e));

    }

    private void handleDbResponse(Cursor cursor) {
        List<BookCover> bookCovers = createListFromCursor(cursor);
        if (dataListener != null && !bookCovers.isEmpty()) {
            dataListener.onDataAvailable(bookCovers);
        } else if (isInternetConnected()){
            dataFetchDisposable = fetchData();
        }
    }

    private static List<BookCover> createListFromCursor(Cursor cursor) {
        List<BookCover> books = new ArrayList<>();
        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                BookCover bookCover = new BookCover();
                bookCover.setId(cursor.getInt(_ID));
                bookCover.setTitle(cursor.getString(TITLE));
                bookCover.setAuthor(cursor.getString(AUTHOR));
                bookCover.setThumb(cursor.getString(THUMB_URL));
                bookCover.setPhoto(cursor.getString(PHOTO_URL));
                bookCover.setPublishedDate(cursor.getString(PUBLISHED_DATE));
                bookCover.setAspectRatio(cursor.getFloat(ASPECT_RATIO));
                books.add(bookCover);
            }
            cursor.close();
        }
        return books;
    }


    private Disposable fetchData() {
        return getRetrofitClient().create(ApiCalls.class)
                .getBookList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleFetchSuccess,
                        e -> dataListener.onConnectionError(ErrorType.NO_NETWORK_CONNECTION, e));
    }

    private void handleFetchSuccess(List<Book> books) {
        dbInsertDisposable = addToDataBase(books);
    }


    private Disposable addToDataBase(List<Book> books) {

        ContentValues[] cv = new ContentValues[books.size()];

        Uri dirUri = ItemsContract.Items.buildDirUri();

        int i = 0;
        for (Book book : books) {
            ContentValues values = new ContentValues();
            values.put(ItemsContract.Items.SERVER_ID, book.getId());
            values.put(ItemsContract.Items.AUTHOR, book.getAuthor());
            values.put(ItemsContract.Items.TITLE, book.getTitle());
            values.put(ItemsContract.Items.BODY, book.getBody());
            values.put(ItemsContract.Items.THUMB_URL, book.getThumb());
            values.put(ItemsContract.Items.PHOTO_URL, book.getPhoto());
            values.put(ItemsContract.Items.ASPECT_RATIO, book.getAspect_ratio());
            values.put(ItemsContract.Items.PUBLISHED_DATE, book.getPublished_date());
            cv[i] = values;
            i++;
        }

        return Observable.just(context.getContentResolver().bulkInsert(dirUri, cv))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        this::handleInsertSuccess,
                        throwable -> Log.e(TAG, "Error inserting to DB: ", throwable));
    }

    private void handleInsertSuccess(int numRows) {
        Log.d(TAG, "Num rows inserted: " + numRows);
        DataUtils.dispose(dbQueryDisposable);
        dbQueryDisposable = queryDbForBooks();
    }

    private void deleteFromDb(){
        context.getContentResolver().delete(ItemsContract.Items.buildDirUri(), null, null);
        DataUtils.dispose(dataFetchDisposable);
        dataFetchDisposable = fetchData();
    }

    private boolean isInternetConnected() {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        boolean connected = networkInfo != null && networkInfo.isConnected();

        if (!connected && dataListener != null) {
            dataListener.onConnectionError(ErrorType.NO_NETWORK_CONNECTION,
                    new ConnectException("Internet check error"));
        }

        return connected;
    }


    private Retrofit getRetrofitClient() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(CONNECTION_TIMEOUT, TimeUnit.SECONDS)
                .eventListener(new EventListener() {
                    @Override
                    public void connectFailed(Call call, InetSocketAddress inetSocketAddress,
                                              Proxy proxy, Protocol protocol, IOException ioe) {
                        super.connectFailed(call, inetSocketAddress, proxy, protocol, ioe);
                        if (dataListener != null) {
                            dataListener.onConnectionError(ErrorType.REQUEST_TIME_OUT,
                                    new ConnectException("Retrofit client"));
                        }
                    }
                })
                .retryOnConnectionFailure(true)
                .build();

        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(okHttpClient)
                .build();
    }
}
