package com.example.xyzreader.ui;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.BodyViewModel;
import com.example.xyzreader.utils.Pagination;

import butterknife.BindView;
import butterknife.ButterKnife;
import ru.noties.markwon.Markwon;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment {

    private static final String TAG = ArticleDetailFragment.class.getSimpleName();

    private static final String ARG_PAGE_NUM = "page_num";

    private int pageNum;

    @BindView(R.id.article_body)
    TextView bodyView;
    @BindView(R.id.book_progress)
    ProgressBar progressBar;
    @BindView(R.id.page_num)
    TextView pageNumView;

    BodyViewModel viewModel;
    Observer<Pagination> observer;
    FragMeasureListener measureListener;

    public interface FragMeasureListener {
        void onPageLaidOut(TextView measuredView);
    }

    public ArticleDetailFragment() {
        /*
         * Mandatory empty constructor for the fragment manager to instantiate the
         * fragment (e.g. upon screen orientation changes).
         */
    }

    public static ArticleDetailFragment newInstance(int pageNum) {
        Bundle arguments = new Bundle();
        arguments.putInt(ARG_PAGE_NUM, pageNum);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            pageNum = getArguments().getInt(ARG_PAGE_NUM);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);
        ButterKnife.bind(this, mRootView);

        pageNumView.setText(String.format(getString(R.string.page_num), pageNum + 1));

        viewModel = ViewModelProviders.of(getActivity()).get(BodyViewModel.class);

        if (measureListener != null) {
            bodyView.getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            bodyView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            bodyView.measure(MeasureSpec.EXACTLY, MeasureSpec.EXACTLY);
                            measureListener.onPageLaidOut(bodyView);
                            measureListener = null;
                        }
                    });
        }

        return mRootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        boolean isLandscape = view.getResources().getBoolean(R.bool.isLandscape);
        if (viewModel.getPage(pageNum, isLandscape) != null) {
            setPageText(viewModel.getPage(pageNum, isLandscape));
        } else {
            observer = pagination -> setPageText(pagination.get(pageNum));
            viewModel.registerObserver(getActivity(), observer, isLandscape);
        }
    }

    @Override
    public void onDetach() {
        if (observer != null) viewModel.unregisterObserver(observer);
        super.onDetach();
    }

    public void setPageText(CharSequence pageText){
        Markwon.setText(bodyView, pageText);
        hideLoader();
    }

    private void hideLoader(){
        bodyView.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
    }

    public void setListener(FragMeasureListener listener) {
        this.measureListener = listener;
    }

}
