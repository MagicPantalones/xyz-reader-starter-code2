package com.example.xyzreader.remote;

import android.annotation.SuppressLint;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.example.xyzreader.utils.DataUtils;
import com.example.xyzreader.data.ItemsContract;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
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
            ItemsContract.Items.BODY,
    };

    private static final int _ID = 0;
    private static final int TITLE = 1;
    private static final int PUBLISHED_DATE = 2;
    private static final int AUTHOR = 3;
    private static final int THUMB_URL = 4;
    private static final int PHOTO_URL = 5;
    private static final int ASPECT_RATIO = 6;
    private static final int BODY = 7;

    private final Context context;
    private DataListener dataListener;

    private Disposable dbQueryDisposable;
    private Disposable dataFetchDisposable;
    private Disposable dbInsertDisposable;

    private boolean deleteFromDb = false;

    public enum ErrorType {
        REQUEST_TIME_OUT,
        NO_NETWORK_CONNECTION
    }

    public interface DataListener {
        void onConnectionError(ErrorType error);
        void onDataAvailable(List<Book> books);
    }

    public interface StaticDataListener {
        void onQueryReturned(List<Book> books);
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
        deleteFromDb = true;
        DataUtils.dispose(dataFetchDisposable);
        dataFetchDisposable = fetchData();
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
        List<Book> books = createListFromCursor(cursor);
        if (dataListener != null && !books.isEmpty()) {
            dataListener.onDataAvailable(books);
        } else if (isInternetConnected()){
            dataFetchDisposable = fetchData();
        }
    }

    private static List<Book> createListFromCursor(Cursor cursor) {
        List<Book> books = new ArrayList<>();
        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                Book book = new Book();
                book.setId(cursor.getInt(_ID));
                book.setTitle(cursor.getString(TITLE));
                book.setAuthor(cursor.getString(AUTHOR));
                book.setBody(cursor.getString(BODY));
                book.setThumb(cursor.getString(THUMB_URL));
                book.setPhoto(cursor.getString(PHOTO_URL));
                book.setAspect_ratio(cursor.getFloat(ASPECT_RATIO));
                book.setPublished_date(cursor.getString(PUBLISHED_DATE));
                books.add(book);
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
                .subscribe(books -> dbInsertDisposable = addToDataBase(books),
                        e -> dataListener.onConnectionError(ErrorType.NO_NETWORK_CONNECTION));
    }


    private Disposable addToDataBase(List<Book> books) {
        if (dataListener != null) dataListener.onDataAvailable(books);

        ArrayList<ContentProviderOperation> cpo = new ArrayList<>();

        Uri dirUri = ItemsContract.Items.buildDirUri();

        if (deleteFromDb) {
            // Delete all items
            cpo.add(ContentProviderOperation.newDelete(dirUri).build());
            deleteFromDb = false;
        }

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
            cpo.add(ContentProviderOperation.newInsert(dirUri).withValues(values).build());
        }

        return Observable.create(emitter ->
                context.getContentResolver().applyBatch(ItemsContract.CONTENT_AUTHORITY, cpo))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(contentProviderResults -> {});
    }

    private boolean isInternetConnected() {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        boolean connected = networkInfo != null && networkInfo.isConnected();

        if (!connected && dataListener != null) {
            dataListener.onConnectionError(ErrorType.NO_NETWORK_CONNECTION);
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
                            dataListener.onConnectionError(ErrorType.REQUEST_TIME_OUT);
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

    @SuppressLint("CheckResult")
    public static void queryForBooks(@NonNull Context context,
                                     @NonNull StaticDataListener listener) {
        Uri uri = ItemsContract.Items.buildDirUri();
        //noinspection ConstantConditions
        Observable.just(context.getContentResolver().query(
                uri,
                PROJECTION,
                null,
                null,
                null))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(cursor -> listener.onQueryReturned(createListFromCursor(cursor)),
                        e -> Log.e(TAG, "Error quering DB", e));
    }

    @SuppressLint("CheckResult")
    public static void queryOneBook(Context context, int id, StaticDataListener listener) {

        Uri uri = ItemsContract.Items.buildItemUri(id);
        //noinspection ConstantConditions
        Observable.just(context.getContentResolver().query(
                uri,
                PROJECTION,
                null,
                null,
                null))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(cursor -> listener.onQueryReturned(createListFromCursor(cursor)));
    }
}
