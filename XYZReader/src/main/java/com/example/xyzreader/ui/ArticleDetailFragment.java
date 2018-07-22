package com.example.xyzreader.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.transition.Slide;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.remote.DataProvider;
import com.example.xyzreader.utils.DataUtils;
import com.example.xyzreader.remote.Book;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import ru.noties.markwon.Markwon;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment {

    public static final String ARG_BOOK_ID = "book_id";
    private static final String TAG = ArticleDetailFragment.class.getSimpleName();

    private int bookId;

    @BindView(R.id.article_body)
    TextView bodyView;
    @BindView(R.id.book_progress)
    ProgressBar progressBar;

    Disposable detailDisposable;
    LoadingListener listener;

    public interface LoadingListener {
        void onLoadComplete();
    }

    public ArticleDetailFragment() {
        /*
         * Mandatory empty constructor for the fragment manager to instantiate the
         * fragment (e.g. upon screen orientation changes).
         */
    }

    public static ArticleDetailFragment newInstance(int position) {
        Bundle arguments = new Bundle();
        arguments.putInt(ARG_BOOK_ID, position);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            bookId = getArguments().getInt(ARG_BOOK_ID);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);
        ButterKnife.bind(this, mRootView);

        DataProvider.queryOneBook(getContext(), bookId, this::startTextParsing);
        return mRootView;
    }

    private void startTextParsing(List<Book> books) {
        Book book = books.get(0);

        detailDisposable = Observable.just(book.getBody().substring(0, 1000))
                .subscribeOn(Schedulers.io())
                .flatMap(s -> Observable.just(Markwon.markdown(getContext(), s)))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::bindViews, Throwable::printStackTrace);
    }

    private void bindViews(CharSequence sequence) {
        bodyView.setText(sequence);
        bodyView.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
        if (listener != null) listener.onLoadComplete();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof LoadingListener) listener = (LoadingListener) context;
    }

    @Override
    public void onDetach() {
        DataUtils.dispose(detailDisposable);
        listener = null;
        super.onDetach();
    }
}
