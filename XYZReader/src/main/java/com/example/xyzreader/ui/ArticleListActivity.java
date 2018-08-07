package com.example.xyzreader.ui;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.utils.BookCover;
import com.example.xyzreader.data.network.DataProvider;
import com.example.xyzreader.utils.DataUtils;
import com.example.xyzreader.utils.GlideApp;

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
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
@SuppressLint("SimpleDateFormat")
public class ArticleListActivity extends AppCompatActivity implements DataProvider.DataListener {

    private static final String EXTRA_COVERS = "covers";

    private static final String TAG = ArticleListActivity.class.toString();

    @BindView(R.id.swipe_refresh_layout)
    SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.recycler_view)
    RecyclerView recyclerView;
    @BindView(R.id.main_container)
    ViewGroup mainContainer;

    private Snackbar errorSnack;
    private BookListAdapter bookListAdapter;
    private DataProvider dataProvider;
    private Unbinder unbinder;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar startOfEpoch = new GregorianCalendar(2,1,1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);
        unbinder = ButterKnife.bind(this);
        dataProvider = new DataProvider(this, this);

        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimaryLight,
                R.color.colorPrimaryDark);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            bookListAdapter.setBooks(new ArrayList<>());
            dataProvider.refreshList();
            swipeRefreshLayout.setRefreshing(true);
        });

        dataProvider.init();
        swipeRefreshLayout.setRefreshing(true);

        int columnCount = getResources().getInteger(R.integer.list_column_count);
        StaggeredGridLayoutManager sglm = new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
        sglm.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_NONE);
        recyclerView.setLayoutManager(sglm);

    }

    @Override
    protected void onDestroy() {
        dataProvider.dispose();
        unbinder.unbind();
        super.onDestroy();
    }


    @Override
    public void onConnectionError(DataProvider.ErrorType error, Throwable throwable) {
        if (error == DataProvider.ErrorType.NO_NETWORK_CONNECTION) {
            errorSnack = Snackbar.make(mainContainer, getString(R.string.error_no_network),
                    Snackbar.LENGTH_INDEFINITE);
        } else if (error == DataProvider.ErrorType.REQUEST_TIME_OUT){
            errorSnack = Snackbar.make(mainContainer, getString(R.string.error_time_out),
                    Snackbar.LENGTH_INDEFINITE);
        }

        if (throwable != null) {
            Log.e(TAG, "Connection error: ", throwable);
        }

        if (swipeRefreshLayout.isRefreshing()) swipeRefreshLayout.setRefreshing(false);
        errorSnack.setAction(R.string.snack_retry, v -> {
            if (errorSnack.isShown()) errorSnack.dismiss();
            dataProvider.refreshList();
            swipeRefreshLayout.setRefreshing(true);
        });

        errorSnack.show();
    }

    @Override
    public void onDataAvailable(List<BookCover> bookCovers) {
        recyclerView.setAdapter(null);
        bookListAdapter = new BookListAdapter();
        bookListAdapter.setHasStableIds(true);
        recyclerView.setAdapter(bookListAdapter);
        bookListAdapter.setBooks(bookCovers);
        if (swipeRefreshLayout.isRefreshing()) swipeRefreshLayout.setRefreshing(false);
        if (errorSnack != null && errorSnack.isShown()) errorSnack.dismiss();

    }

    private class BookListAdapter extends RecyclerView.Adapter<BookViewHolder> {
        private List<BookCover> bookCovers = new ArrayList<>();

        BookListAdapter() {}

        @Override
        public long getItemId(int position) {
            return bookCovers.get(position).getId();
        }

        @NonNull
        @Override
        public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.list_item_article, parent, false);
            BookViewHolder vh = new BookViewHolder(view);

            vh.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(ArticleListActivity.this,
                        ArticleDetailActivity.class);
                intent.putExtra(EXTRA_COVERS, bookCovers.get(vh.getAdapterPosition()));
                startActivity(intent, ActivityOptions
                        .makeSceneTransitionAnimation(ArticleListActivity.this).toBundle());
            });

            return vh;
        }

        private Date parsePublishedDate(int position) {
            try {
                String date = bookCovers.get(position).getPublishedDate();
                return dateFormat.parse(date);
            } catch (ParseException ex) {
                Log.e(TAG, ex.getMessage());
                Log.i(TAG, "passing today's date");
                return new Date();
            }
        }

        @Override
        public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
            BookCover bookCover = bookCovers.get(position);

            holder.titleView.setText(bookCover.getTitle());

            Date publishedDate = parsePublishedDate(position);
            if (!publishedDate.before(startOfEpoch.getTime())) {

                holder.subtitleView.setText(String.format(getString(R.string.byline),
                        DateUtils.getRelativeTimeSpanString(
                                publishedDate.getTime(),
                                System.currentTimeMillis(),
                                DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString(), bookCover.getAuthor()));
            } else {
                holder.subtitleView.setText(String.format(getString(R.string.byline),
                        outputFormat.format(publishedDate), bookCover.getAuthor()));
            }

            GlideApp.with(holder.thumbnailView)
                    .load(bookCover.getThumb())
                    .into(holder.thumbnailView);

            ConstraintSet set = new ConstraintSet();
            set.clone(holder.aspectLayout);
            set.setDimensionRatio(holder.thumbnailView.getId(),
                    DataUtils.convertToFraction(bookCover.getAspectRatio()));
            set.applyTo(holder.aspectLayout);
        }

        @Override
        public int getItemCount() { return bookCovers.size(); }

        void setBooks(List<BookCover> bookCovers) {
            this.bookCovers = bookCovers;
            notifyDataSetChanged();
        }
    }

    public static class BookViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.thumbnail)
        ImageView thumbnailView;
        @BindView(R.id.article_title)
        TextView titleView;
        @BindView(R.id.article_subtitle)
        TextView subtitleView;
        @BindView(R.id.aspect_wrapper)
        ConstraintLayout aspectLayout;

        BookViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}
