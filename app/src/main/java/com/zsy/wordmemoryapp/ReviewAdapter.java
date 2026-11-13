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
 * 当日复习队列适配器：每个词条提供「记住了 / 忘记了」两个操作。
 */
public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    public interface OnReviewActionListener {
        void onRemember(Word word);

        void onForget(Word word);
    }

    private List<Word> words = new ArrayList<>();
    private final OnReviewActionListener listener;

    public ReviewAdapter(OnReviewActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Word> newWords) {
        words = newWords == null ? new ArrayList<>() : new ArrayList<>(newWords);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        holder.bind(words.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return words.size();
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvWord;
        private final TextView tvMeaning;
        private final Button btnRemember;
        private final Button btnForget;

        ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            tvWord = itemView.findViewById(R.id.tvReviewWord);
            tvMeaning = itemView.findViewById(R.id.tvReviewMeaning);
            btnRemember = itemView.findViewById(R.id.btnRemember);
            btnForget = itemView.findViewById(R.id.btnForget);
        }

        void bind(Word word, OnReviewActionListener listener) {
            tvWord.setText(word.getWord());
            tvMeaning.setText(word.getMeaning());
            btnRemember.setOnClickListener(v -> listener.onRemember(word));
            btnForget.setOnClickListener(v -> listener.onForget(word));
        }
    }
}
