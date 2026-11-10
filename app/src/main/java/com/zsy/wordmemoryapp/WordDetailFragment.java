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
 * 单词详情页：接收列表页传入的单词与释义参数，展示后可以返回列表。
 */
public class WordDetailFragment extends Fragment {

    public static final String ARG_WORD = "word";
    public static final String ARG_MEANING = "meaning";

    public WordDetailFragment() {
        super(R.layout.fragment_word_detail);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String word = "abandon";
        String meaning = "（暂无释义）";
        Bundle args = getArguments();
        if (args != null) {
            if (args.getString(ARG_WORD) != null) {
                word = args.getString(ARG_WORD);
            }
            if (args.getString(ARG_MEANING) != null) {
                meaning = args.getString(ARG_MEANING);
            }
        }

        TextView tvDetailWord = view.findViewById(R.id.tvDetailWord);
        tvDetailWord.setText(word);
        TextView tvDetailMeaning = view.findViewById(R.id.tvDetailMeaning);
        tvDetailMeaning.setText(meaning);

        Button btnBackToList = view.findViewById(R.id.btnBackToList);
        btnBackToList.setOnClickListener(v ->
                NavHostFragment.findNavController(WordDetailFragment.this).navigateUp());
    }
}
