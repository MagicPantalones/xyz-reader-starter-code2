package com.example.xyzreader.ui;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ShareCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.request.target.ImageViewTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.remote.BookCover;
import com.example.xyzreader.utils.DataUtils;
import com.example.xyzreader.utils.GlideApp;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import ru.noties.markwon.Markwon;


/**
 * An activity representing a single Article detail screen, letting you swipe between articles.
 */
public class ArticleDetailActivity extends AppCompatActivity {

    private static final String TAG = ArticleDetailActivity.class.getSimpleName();

    private static final String EXTRA_COVERS = "covers";
    private static final String[] PROJECTION_SINGLE = {ItemsContract.Items.BODY};

    @BindView(R.id.article_byline_detail)
    TextView bylineView;
    @BindView(R.id.photo_detail)
    ImageView photoView;
    @BindView(R.id.text_recycler)
    RecyclerView textRecycler;
    @BindView(R.id.share_fab)
    FloatingActionButton shareFab;
    @BindView(R.id.details_toolbar)
    Toolbar toolbar;
    @BindView(R.id.details_collapsing_tb_layout)
    CollapsingToolbarLayout collapsingToolbar;
    @BindView(R.id.details_app_bar_layout)
    AppBarLayout appBarLayout;
    @BindView(R.id.detail_container)
    View detailContainer;
    @BindView(R.id.text_loader)
    ProgressBar textProgress;

    private BookCover bookCover;
    private int defaultColor;

    private BodyTextAdapter adapter;
    private Unbinder unbinder;
    private Disposable textDisposable;
    private Disposable paletteDisposable;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar startOfEpoch = new GregorianCalendar(2, 1, 1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        setContentView(R.layout.activity_article_detail);

        postponeEnterTransition();

        unbinder = ButterKnife.bind(this);
        defaultColor = DataUtils.SDK_V < 23 ?
                getResources().getColor(R.color.colorPrimary) : getColor(R.color.colorPrimary);


        if (savedInstanceState != null) {
            bookCover = savedInstanceState.getParcelable(EXTRA_COVERS);
        } else {
            bookCover = getIntent().getParcelableExtra(EXTRA_COVERS);
        }


        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        bindViews();
        textDisposable = fetchText();

        shareFab.setOnClickListener(view ->
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(this)
                        .setType("text/plain")
                        .setText("Some sample text")
                        .getIntent(), getString(R.string.action_share))));

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(EXTRA_COVERS, bookCover);
    }

    @Override
    protected void onDestroy() {
        if (unbinder != null) unbinder.unbind();
        DataUtils.dispose(paletteDisposable, textDisposable);
        super.onDestroy();
    }

    private void onFetchSuccess(String[] body) {
        if (body.length < 1) {
            onFetchError(new Exception("No body."));
            return;
        }
        adapter = new BodyTextAdapter(body);
        textRecycler.setAdapter(adapter);
        textProgress.setVisibility(View.GONE);
        textRecycler.setVisibility(View.VISIBLE);
    }

    private void onFetchError(Throwable throwable) {
        if (throwable != null) Log.e(TAG, "Error fetching from DB", throwable);
        Snackbar.make(detailContainer, "Error fetching article text.",
                Snackbar.LENGTH_LONG).show();
    }


    private Disposable fetchText() {
        return Observable.just(prepareText())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onFetchSuccess, this::onFetchError);
    }

    private String[] prepareText() {
        Uri uri = ItemsContract.Items.buildItemUri(bookCover.getId());
        Cursor cursor = getContentResolver().query(
                uri,
                PROJECTION_SINGLE,
                null,
                null,
                null
        );

        if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
            String text = cursor.getString(cursor.getColumnIndex(ItemsContract.Items.BODY));
            cursor.close();
            text = Pattern.compile("\\[.*?\\]", Pattern.DOTALL)
                    .matcher(text).replaceAll("");
            text = text.replaceAll("\\r\\n\\r\\n\\r\\n\\r\\n", "\r\n\r\n");
            return text.split("\\r\\n\\r\\n");
        }
        return new String[0];
    }

    private void bindViews() {
        collapsingToolbar.setTitle(bookCover.getTitle());
        Date publishedDate = parsePublishedDate();

        if (!publishedDate.before(startOfEpoch.getTime())) {
            bylineView.setText(String.format(getString(R.string.byline),
                    DateUtils.getRelativeTimeSpanString(publishedDate.getTime(),
                            System.currentTimeMillis(),
                            DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString(), bookCover.getAuthor()));
        } else {
            // If date is before 1902, just show the string
            bylineView.setText(String.format(getString(R.string.byline),
                    outputFormat.format(publishedDate),
                    bookCover.getAuthor()));
        }

        GlideApp.with(this)
                .load(bookCover.getPhoto())
                .dontAnimate()
                .skipMemoryCache(true)
                .into(new ImageViewTarget<Drawable>(photoView) {

                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        Bitmap bitmap = ((BitmapDrawable) resource).getBitmap();
                        paletteDisposable = Observable.just(Palette.from(bitmap).generate())
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(palette -> {
                                    int color = palette.getDarkMutedColor(defaultColor);
                                    collapsingToolbar.setContentScrimColor(color);
                                    appBarLayout.setBackgroundColor(color);
                                    photoView.setImageDrawable(resource);
                                    startPostponedEnterTransition();
                                });
                    }

                    @Override
                    protected void setResource(@Nullable Drawable resource) {
                        //Required override
                    }
                });
    }

    private Date parsePublishedDate() {
        try {
            String date = bookCover.getPublishedDate();
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Log.e(TAG, ex.getMessage());
            Log.i(TAG, "passing today's date");
            return new Date();
        }
    }

    private class BodyTextAdapter extends RecyclerView.Adapter<BodyTextViewHolder> {

        private String[] text;

        BodyTextAdapter(String[] text) {
            this.text = text;
        }

        @NonNull
        @Override
        public BodyTextViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(ArticleDetailActivity.this).inflate(
                    R.layout.body_view_holder, parent, false);
            return new BodyTextViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull BodyTextViewHolder holder, int position) {
            String rawText = text[position];
            Markwon.setMarkdown(holder.bodyTextView, rawText);
            if (!rawText.equals("")) {
                holder.bodyTextView.append("\r\n");
            }
        }

        @Override
        public int getItemCount() {
            return text.length;
        }

    }

    class BodyTextViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.body_text_view_holder)
        TextView bodyTextView;

        BodyTextViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

    }
}
