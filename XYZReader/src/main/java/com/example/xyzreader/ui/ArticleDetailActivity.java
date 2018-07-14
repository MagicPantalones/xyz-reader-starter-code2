package com.example.xyzreader.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.ShareCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.request.target.ImageViewTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.xyzreader.R;
import com.example.xyzreader.remote.Book;
import com.example.xyzreader.remote.DataProvider;
import com.example.xyzreader.remote.GlideApp;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * An activity representing a single Article detail screen, letting you swipe between articles.
 */
public class ArticleDetailActivity extends AppCompatActivity {

    private static final String TAG = ArticleDetailActivity.class.getSimpleName();

    private static final String KEY_POSITION = "position";

    @BindView(R.id.article_title_detail)
    TextView titleView;
    @BindView(R.id.article_byline_detail)
    TextView bylineView;
    @BindView(R.id.photo_detail)
    ImageView photoView;
    @BindView(R.id.pager)
    ViewPager pager;
    @BindView(R.id.share_fab)
    FloatingActionButton shareFab;

    private List<Book> books = new ArrayList<>();
    private int selectedPosition;
    private int mutedColor;


    private MyPagerAdapter mPagerAdapter;
    private Unbinder unbinder;

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
        unbinder = ButterKnife.bind(this);

        if (savedInstanceState == null &&
                getIntent().getIntExtra(KEY_POSITION, -1) != -1) {
            selectedPosition = getIntent().getIntExtra(KEY_POSITION, -1);
        }

        DataProvider.queryForBookList(this, list -> {
            books = list;
            bindViews(books.get(selectedPosition));

            mPagerAdapter = new MyPagerAdapter(getSupportFragmentManager());
            pager.setAdapter(mPagerAdapter);
            pager.setCurrentItem(selectedPosition);
            pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);
                    selectedPosition = position;
                    bindViews(books.get(selectedPosition));
                }
            });
        });

        shareFab.setOnClickListener(view ->
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(this)
                        .setType("text/plain")
                        .setText("Some sample text")
                        .getIntent(), getString(R.string.action_share))));

    }

    @Override
    protected void onDestroy() {
        if (unbinder != null) unbinder.unbind();
        super.onDestroy();
    }

    private Date parsePublishedDate(Book book) {
        try {
            String date = book.getPublished_date();
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Log.e(TAG, ex.getMessage());
            Log.i(TAG, "passing today's date");
            return new Date();
        }
    }

    private void bindViews(Book book) {

        titleView.setText(book.getTitle());
        Date publishedDate = parsePublishedDate(book);
        if (!publishedDate.before(startOfEpoch.getTime())) {
            bylineView.setText(Html.fromHtml(
                    DateUtils.getRelativeTimeSpanString(
                            publishedDate.getTime(),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + " by <font color='#ffffff'>"
                            + book.getAuthor()
                            + "</font>"));

        } else {
            // If date is before 1902, just show the string
            bylineView.setText(Html.fromHtml(
                    outputFormat.format(publishedDate) + " by <font color='#ffffff'>"
                            + book.getAuthor()
                            + "</font>"));

        }

        GlideApp.with(photoView)
                .load(book.getPhoto())
                .into(new ImageViewTarget<Drawable>(photoView) {
                    @Override
                    protected void setResource(@Nullable Drawable resource) {
                        //Required overridden method.
                    }

                    @Override
                    public void onResourceReady(@NonNull Drawable resource,
                                                @Nullable Transition<? super Drawable> transition) {
                        super.onResourceReady(resource, transition);
                        photoView.setImageDrawable(resource.getCurrent());

                        Bitmap bitmap = ((BitmapDrawable) resource).getBitmap();
                        Palette palette = Palette.from(bitmap).generate();
                        mutedColor = palette.getDarkMutedColor(0xFF333333);
                    }
                });

    }


    private class MyPagerAdapter extends FragmentStatePagerAdapter {
        MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return ArticleDetailFragment.newInstance(books.get(position));
        }

        @Override
        public int getCount() {
            return books.size();
        }
    }
}
