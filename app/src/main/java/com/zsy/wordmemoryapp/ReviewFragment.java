package com.zsy.wordmemoryapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * 复习测试页：展示艾宾浩斯算法生成的当日复习队列，
 * 用户点击「记住了 / 忘记了」后由算法计算下一次复习时间。
 */
public class ReviewFragment extends Fragment implements ReviewAdapter.OnReviewActionListener {

    private ReviewViewModel viewModel;
    private ReviewAdapter adapter;
    private View loading;
    private TextView tvEmpty;
    private RecyclerView rvQueue;

    public ReviewFragment() {
        super(R.layout.fragment_review);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ReviewViewModel.class);

        rvQueue = view.findViewById(R.id.rvReviewQueue);
        loading = view.findViewById(R.id.pbReviewLoading);
        tvEmpty = view.findViewById(R.id.tvReviewEmpty);

        adapter = new ReviewAdapter(this);
        rvQueue.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvQueue.setAdapter(adapter);

        viewModel.getQueue().observe(getViewLifecycleOwner(), words -> {
            adapter.submitList(words);
            boolean empty = words == null || words.isEmpty();
            tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            rvQueue.setVisibility(empty ? View.GONE : View.VISIBLE);
        });

        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            boolean show = Boolean.TRUE.equals(isLoading);
            loading.setVisibility(show ? View.VISIBLE : View.GONE);
        });

        viewModel.getMessage().observe(getViewLifecycleOwner(), message -> {
            if (!TextUtils.isEmpty(message)) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.loadQueue();
    }

    @Override
    public void onResume() {
        super.onResume();
        // 从别的页面回来时刷新队列（可能刚新增了单词）
        if (viewModel != null) {
            viewModel.loadQueue();
        }
    }

    @Override
    public void onRemember(Word word) {
        viewModel.review(word, ReviewRecord.RESULT_KNOWN);
    }

    @Override
    public void onForget(Word word) {
        viewModel.review(word, ReviewRecord.RESULT_FORGOT);
    }
}
