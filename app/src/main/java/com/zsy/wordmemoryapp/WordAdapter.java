package com.zsy.wordmemoryapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * 单词列表适配器：展示单词、释义、记忆状态，并提供“掌握 / 删除 / 点击进详情”事件。
 */
public class WordAdapter extends RecyclerView.Adapter<WordAdapter.WordViewHolder> {

    public interface OnWordActionListener {
        void onWordClick(Word word);

        void onToggleMastered(Word word);

        void onDelete(Word word);
    }

    private List<Word> words = new ArrayList<>();
    private final OnWordActionListener listener;

    public WordAdapter(OnWordActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Word> newWords) {
        words = newWords == null ? new ArrayList<>() : new ArrayList<>(newWords);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public WordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_word, parent, false);
        return new WordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WordViewHolder holder, int position) {
        holder.bind(words.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return words.size();
    }

    static class WordViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvWord;
        private final TextView tvMeaning;
        private final TextView tvState;
        private final Button btnMastered;
        private final Button btnDelete;

        WordViewHolder(@NonNull View itemView) {
            super(itemView);
            tvWord = itemView.findViewById(R.id.tvItemWord);
            tvMeaning = itemView.findViewById(R.id.tvItemMeaning);
            tvState = itemView.findViewById(R.id.tvItemState);
            btnMastered = itemView.findViewById(R.id.btnItemMastered);
            btnDelete = itemView.findViewById(R.id.btnItemDelete);
        }

        void bind(Word word, OnWordActionListener listener) {
            tvWord.setText(word.getWord());
            tvMeaning.setText(word.getMeaning());

            boolean mastered = word.getMemoryState() == Word.STATE_MASTERED;
            tvState.setText(mastered ? R.string.state_mastered : R.string.state_learning);
            btnMastered.setText(mastered ? R.string.btn_unmark_mastered : R.string.btn_mark_mastered);

            itemView.setOnClickListener(v -> listener.onWordClick(word));
            btnMastered.setOnClickListener(v -> listener.onToggleMastered(word));
            btnDelete.setOnClickListener(v -> listener.onDelete(word));
        }
    }
}
