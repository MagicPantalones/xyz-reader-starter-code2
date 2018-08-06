package com.example.xyzreader.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.ShareCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.request.target.ImageViewTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.xyzreader.R;
import com.example.xyzreader.data.BodyViewModel;
import com.example.xyzreader.remote.BookCover;
import com.example.xyzreader.utils.DataUtils;
import com.example.xyzreader.utils.GlideApp;
import com.example.xyzreader.utils.MarkdownCreator;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static com.example.xyzreader.ui.ArticleDetailFragment.*;

/**
 * An activity representing a single Article detail screen, letting you swipe between articles.
 */
public class ArticleDetailActivity extends AppCompatActivity implements FragMeasureListener {

    private static final String TAG = ArticleDetailActivity.class.getSimpleName();

    private static final String EXTRA_PAGE_NUM = "page_num";
    private static final String EXTRA_COVERS = "covers";
    private static final String EXTRA_PAGE_COUNT = "page_count";

    @BindView(R.id.article_byline_detail)
    TextView bylineView;
    @BindView(R.id.photo_detail)
    ImageView photoView;
    @BindView(R.id.pager)
    ViewPager pager;
    @BindView(R.id.share_fab)
    FloatingActionButton shareFab;
    @BindView(R.id.details_toolbar)
    Toolbar toolbar;
    @BindView(R.id.details_collapsing_tb_layout)
    CollapsingToolbarLayout collapsingToolbar;
    @BindView(R.id.details_app_bar_layout)
    AppBarLayout appBarLayout;

    private BookCover bookCover;
    private int selectedPosition;
    private int defaultColor;
    private int pageCount = 1;

    private MyPagerAdapter mPagerAdapter;
    private MarkdownCreator markdownCreator;
    private BodyViewModel bodyViewModel;
    private Unbinder unbinder;
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
        bodyViewModel = ViewModelProviders.of(this).get(BodyViewModel.class);


        if (savedInstanceState != null) {
            selectedPosition = savedInstanceState.getInt(EXTRA_PAGE_NUM);
            bookCover = savedInstanceState.getParcelable(EXTRA_COVERS);
            pageCount = savedInstanceState.getInt(EXTRA_PAGE_COUNT);
        } else {
            bookCover = getIntent().getParcelableExtra(EXTRA_COVERS);
        }


        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        pager.setOffscreenPageLimit(1);
        pager.setPageMargin(DataUtils.dpToPx(this, 8));
        bindViews(bookCover);

        mPagerAdapter = new MyPagerAdapter(getSupportFragmentManager());
        pager.setAdapter(mPagerAdapter);

        pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                selectedPosition = position;
            }
        });

        ArticleDetailFragment frag = (ArticleDetailFragment) mPagerAdapter.instantiateItem(pager, selectedPosition);
        frag.setListener(this);

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
        outState.putInt(EXTRA_PAGE_NUM, selectedPosition);
        outState.putInt(EXTRA_PAGE_COUNT, pageCount);
    }

    @Override
    protected void onDestroy() {
        if (unbinder != null) unbinder.unbind();
        DataUtils.dispose(paletteDisposable);
        if (markdownCreator != null) markdownCreator.cancel();
        super.onDestroy();
    }

    private Date parsePublishedDate(BookCover book) {
        try {
            String date = book.getPublished_date();
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Log.e(TAG, ex.getMessage());
            Log.i(TAG, "passing today's date");
            return new Date();
        }
    }


    private void bindViews(BookCover book) {
        collapsingToolbar.setTitle(book.getTitle());
        Date publishedDate = parsePublishedDate(book);

        if (!publishedDate.before(startOfEpoch.getTime())) {
            bylineView.setText(String.format(getString(R.string.byline),
                    DateUtils.getRelativeTimeSpanString(publishedDate.getTime(),
                            System.currentTimeMillis(),
                            DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString(), book.getAuthor()));
        } else {
            // If date is before 1902, just show the string
            bylineView.setText(String.format(getString(R.string.byline),
                    outputFormat.format(publishedDate),
                    book.getAuthor()));
        }

        GlideApp.with(this)
                .load(book.getPhoto())
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

    @Override
    public void onPageLaidOut(TextView measuredView) {
        boolean isLandscape = getResources().getBoolean(R.bool.isLandscape);
        if (!bodyViewModel.hasData()) {
            markdownCreator = new MarkdownCreator(this, bookCover.getId(), measuredView,
                    pagination -> {
                        bodyViewModel.setPages(pagination, isLandscape);
                        pageCount = pagination.size();
                        mPagerAdapter.notifyDataSetChanged();
                    });
        }
    }

    private class MyPagerAdapter extends FragmentStatePagerAdapter {

        MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return ArticleDetailFragment.newInstance(position);
        }

        @Override
        public int getCount() {
            return pageCount;
        }
    }
}
