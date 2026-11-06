package com.zsy.wordmemoryapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

/**
 * 单词列表页：空 UI，仅提供一个入口跳转到单词详情页，用于验证「列表 -> 详情」导航。
 */
public class WordListFragment extends Fragment {

    public WordListFragment() {
        super(R.layout.fragment_word_list);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button btnOpenDetail = view.findViewById(R.id.btnOpenDetail);
        btnOpenDetail.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString(WordDetailFragment.ARG_WORD, "abandon");
            NavHostFragment.findNavController(WordListFragment.this)
                    .navigate(R.id.action_wordList_to_wordDetail, args);
        });
    }
}
