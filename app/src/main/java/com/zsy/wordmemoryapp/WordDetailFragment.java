package com.zsy.wordmemoryapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

/**
 * 单词详情页：接收列表页传入的 word 参数，展示后可以返回列表。
 */
public class WordDetailFragment extends Fragment {

    public static final String ARG_WORD = "word";

    public WordDetailFragment() {
        super(R.layout.fragment_word_detail);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String word = "abandon";
        Bundle args = getArguments();
        if (args != null && args.getString(ARG_WORD) != null) {
            word = args.getString(ARG_WORD);
        }

        TextView tvDetailWord = view.findViewById(R.id.tvDetailWord);
        tvDetailWord.setText(word);

        Button btnBackToList = view.findViewById(R.id.btnBackToList);
        btnBackToList.setOnClickListener(v ->
                NavHostFragment.findNavController(WordDetailFragment.this).navigateUp());
    }
}
