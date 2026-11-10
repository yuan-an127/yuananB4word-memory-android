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
 * 单词列表页：RecyclerView 展示数据库中的单词，点击条目进入详情页。
 *
 * <p>数据链路：Room -> WordRepository -> WordViewModel(LiveData) -> Fragment。
 * 观察 LiveData 即可在数据变化时自动刷新界面；首次读取完成前展示 Loading UI。</p>
 */
public class WordListFragment extends Fragment implements WordAdapter.OnWordActionListener {

    private WordViewModel viewModel;
    private WordAdapter adapter;

    private EditText etWord;
    private EditText etMeaning;
    private TextView tvDueInfo;
    private TextView tvEmpty;
    private RecyclerView rvWords;
    private View loadingGroup;

    /** 当前是否处于加载中，以及最近一次拿到的单词列表，用于统一决定三种状态的显示。 */
    private boolean loading = true;
    private List<Word> latestWords;

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
        rvWords = view.findViewById(R.id.rvWords);
        loadingGroup = view.findViewById(R.id.loadingGroup);
        Button btnAddWord = view.findViewById(R.id.btnAddWord);

        adapter = new WordAdapter(this);
        rvWords.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvWords.setAdapter(adapter);

        btnAddWord.setOnClickListener(v -> {
            String word = etWord.getText().toString();
            String meaning = etMeaning.getText().toString();
            // 视图层不碰数据库，只交给 ViewModel
            viewModel.addWord(word, meaning, 3);
        });

        // 加载状态：数据库首次读取完成前显示 Loading
        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            loading = isLoading != null && isLoading;
            renderContent();
        });

        // 观察数据库：新增/删除/修改后 Room 会自动推送最新数据
        viewModel.getAllWords().observe(getViewLifecycleOwner(), words -> {
            latestWords = words;
            renderContent();
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

    /** 统一控制三种状态：加载中 / 有数据 / 空列表。 */
    private void renderContent() {
        boolean isEmpty = latestWords == null || latestWords.isEmpty();
        loadingGroup.setVisibility(loading ? View.VISIBLE : View.GONE);
        rvWords.setVisibility(!loading && !isEmpty ? View.VISIBLE : View.GONE);
        tvEmpty.setVisibility(!loading && isEmpty ? View.VISIBLE : View.GONE);
        if (adapter != null) {
            adapter.submitList(latestWords);
        }
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
