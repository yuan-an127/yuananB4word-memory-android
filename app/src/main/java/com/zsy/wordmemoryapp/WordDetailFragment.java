package com.zsy.wordmemoryapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * 单词详情页：wordId > 0 为「编辑已有单词」，wordId = -1 为「新增单词」。
 * 表单做非空校验，保存 / 删除都通过 ViewModel 写入 Room。
 */
public class WordDetailFragment extends Fragment {

    public static final String ARG_WORD_ID = "wordId";
    public static final long NEW_WORD_ID = -1L;

    private WordDetailViewModel viewModel;

    private TextInputLayout tilWord;
    private TextInputLayout tilMeaning;
    private TextInputEditText etWord;
    private TextInputEditText etMeaning;
    private MaterialButton btnDelete;

    private long wordId = NEW_WORD_ID;
    private Word currentWord;

    public WordDetailFragment() {
        super(R.layout.fragment_word_detail);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(WordDetailViewModel.class);

        TextView tvDetailTitle = view.findViewById(R.id.tvDetailTitle);
        tilWord = view.findViewById(R.id.tilWord);
        tilMeaning = view.findViewById(R.id.tilMeaning);
        etWord = view.findViewById(R.id.etWord);
        etMeaning = view.findViewById(R.id.etMeaning);
        btnDelete = view.findViewById(R.id.btnDeleteWord);
        MaterialButton btnSave = view.findViewById(R.id.btnSave);
        MaterialButton btnBack = view.findViewById(R.id.btnBack);

        Bundle args = getArguments();
        wordId = args == null ? NEW_WORD_ID : args.getLong(ARG_WORD_ID, NEW_WORD_ID);

        boolean isNew = wordId <= NEW_WORD_ID;
        tvDetailTitle.setText(isNew ? R.string.title_new_word : R.string.title_edit_word);
        btnDelete.setVisibility(isNew ? View.GONE : View.VISIBLE);

        if (!isNew) {
            viewModel.load(wordId);
        }

        viewModel.getWord().observe(getViewLifecycleOwner(), word -> {
            if (word == null) {
                return;
            }
            currentWord = word;
            etWord.setText(word.getWord());
            etMeaning.setText(word.getMeaning());
        });

        viewModel.getMessage().observe(getViewLifecycleOwner(), message -> {
            if (!TextUtils.isEmpty(message)) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getDone().observe(getViewLifecycleOwner(), done -> {
            if (Boolean.TRUE.equals(done)) {
                NavHostFragment.findNavController(WordDetailFragment.this).navigateUp();
            }
        });

        btnSave.setOnClickListener(v -> saveWord());
        btnDelete.setOnClickListener(v -> confirmDelete());
        btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(WordDetailFragment.this).navigateUp());
    }

    /** 表单校验：单词、释义都不能为空；通过后交给 ViewModel 保存。 */
    private void saveWord() {
        String wordText = etWord.getText() == null ? "" : etWord.getText().toString().trim();
        String meaning = etMeaning.getText() == null ? "" : etMeaning.getText().toString().trim();

        tilWord.setError(null);
        tilMeaning.setError(null);

        if (TextUtils.isEmpty(wordText)) {
            tilWord.setError(getString(R.string.error_word_required));
            etWord.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(meaning)) {
            tilMeaning.setError(getString(R.string.error_meaning_required));
            etMeaning.requestFocus();
            return;
        }

        int difficulty = currentWord == null ? 3 : currentWord.getDifficulty();
        int memoryState = currentWord == null ? Word.STATE_LEARNING : currentWord.getMemoryState();
        viewModel.save(wordId, wordText, meaning, difficulty, memoryState);
    }

    private void confirmDelete() {
        if (currentWord == null) {
            return;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.dialog_delete_title)
                .setMessage(getString(R.string.dialog_delete_message, currentWord.getWord()))
                .setPositiveButton(R.string.dialog_delete_confirm,
                        (dialog, which) -> viewModel.delete(currentWord))
                .setNegativeButton(R.string.dialog_delete_cancel, null)
                .show();
    }
}
