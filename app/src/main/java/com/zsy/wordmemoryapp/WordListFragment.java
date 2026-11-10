package com.zsy.wordmemoryapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * 单词列表页：只负责界面交互，数据由 {@link WordViewModel} -> {@link WordRepository} 读写。
 * 验证持久化：添加单词后杀掉 App 再打开，列表中的数据仍然存在（Room 落盘）。
 */
public class WordListFragment extends Fragment implements WordAdapter.OnWordActionListener {

    private WordViewModel viewModel;
    private WordAdapter adapter;

    private EditText etWord;
    private EditText etMeaning;
    private TextView tvDueInfo;
    private TextView tvEmpty;

    public WordListFragment() {
        super(R.layout.fragment_word_list);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(WordViewModel.class);

        etWord = view.findViewById(R.id.etWord);
        etMeaning = view.findViewById(R.id.etMeaning);
        tvDueInfo = view.findViewById(R.id.tvDueInfo);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        Button btnAddWord = view.findViewById(R.id.btnAddWord);

        RecyclerView rvWords = view.findViewById(R.id.rvWords);
        adapter = new WordAdapter(this);
        rvWords.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvWords.setAdapter(adapter);

        btnAddWord.setOnClickListener(v -> {
            String word = etWord.getText().toString();
            String meaning = etMeaning.getText().toString();
            // 视图层不碰数据库，只交给 ViewModel
            viewModel.addWord(word, meaning, 3);
        });

        // 观察数据库：新增/删除/修改后 Room 会自动推送最新数据
        viewModel.getAllWords().observe(getViewLifecycleOwner(), words -> {
            adapter.submitList(words);
            boolean empty = words == null || words.isEmpty();
            tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        });

        viewModel.getDueWords().observe(getViewLifecycleOwner(), this::showDueWords);

        viewModel.getMessage().observe(getViewLifecycleOwner(), message -> {
            if (TextUtils.isEmpty(message)) {
                return;
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            if (message.startsWith("已保存")) {
                etWord.setText("");
                etMeaning.setText("");
            }
        });

        viewModel.refreshDueWords();
    }

    private void showDueWords(List<Word> dueWords) {
        int count = dueWords == null ? 0 : dueWords.size();
        if (count == 0) {
            tvDueInfo.setText(R.string.due_empty);
            return;
        }
        StringBuilder names = new StringBuilder();
        for (int i = 0; i < Math.min(count, 3); i++) {
            if (i > 0) {
                names.append("、");
            }
            names.append(dueWords.get(i).getWord());
        }
        if (count > 3) {
            names.append(" 等");
        }
        tvDueInfo.setText(getString(R.string.due_count_with_names, count, names.toString()));
    }

    // ==================== 列表条目事件 ====================

    @Override
    public void onWordClick(Word word) {
        Bundle args = new Bundle();
        args.putString(WordDetailFragment.ARG_WORD, word.getWord());
        args.putString(WordDetailFragment.ARG_MEANING, word.getMeaning());
        NavHostFragment.findNavController(WordListFragment.this)
                .navigate(R.id.action_wordList_to_wordDetail, args);
    }

    @Override
    public void onToggleMastered(Word word) {
        viewModel.toggleMastered(word);
    }

    @Override
    public void onDelete(Word word) {
        viewModel.deleteWord(word);
    }
}
