package com.zsy.wordmemoryapp;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 设置页：空 UI 为主，保留实验 2 的 DeepSeek API / Room 数据库调试入口。
 */
public class SettingsFragment extends Fragment {

    private static final String TAG = "DeepSeekTest";
    private static final String DB_TAG = "WordDbTest";

    private final DeepSeekApi deepSeekApi = new DeepSeekApi();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private TextView tvTestResult;
    private TextView tvDbResult;
    private Button btnTestConnection;
    private Button btnTestDb;

    public SettingsFragment() {
        super(R.layout.fragment_settings);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvTestResult = view.findViewById(R.id.tvTestResult);
        tvDbResult = view.findViewById(R.id.tvDbResult);
        btnTestConnection = view.findViewById(R.id.btnTestConnection);
        btnTestDb = view.findViewById(R.id.btnTestDb);

        btnTestConnection.setOnClickListener(v -> testDeepSeekConnection());
        btnTestDb.setOnClickListener(v -> testDatabase());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    /** 【测试连接】按钮：在子线程请求 DeepSeek，把结果写回 TextView，同时打印 Logcat。 */
    private void testDeepSeekConnection() {
        Log.i(TAG, "开始测试 DeepSeek API 连接...");
        btnTestConnection.setEnabled(false);
        tvTestResult.setText("正在连接 DeepSeek API ...");

        executor.execute(() -> {
            String resultText;
            try {
                deepSeekApi.testConnection();
                Log.i(TAG, "✅ 请求成功：DeepSeek API 可正常访问");
                resultText = "✅ 请求成功：DeepSeek API 可正常访问";
            } catch (DeepSeekApi.ApiKeyException e) {
                Log.e(TAG, "❌ 密钥错误：" + e.getMessage());
                resultText = "❌ 密钥错误：" + e.getMessage() + "\n请检查 DeepSeekApi 中的 API_KEY";
            } catch (DeepSeekApi.NetworkTimeoutException e) {
                Log.e(TAG, "⏱️ 网络超时：" + e.getMessage());
                resultText = "⏱️ 网络超时：请检查手机网络后重试";
            } catch (IOException e) {
                Log.e(TAG, "❌ 请求失败：" + e.getMessage());
                resultText = "❌ 请求失败：" + e.getMessage();
            } catch (Exception e) {
                Log.e(TAG, "❌ 未知错误：" + e);
                resultText = "❌ 未知错误：" + e.getMessage();
            }

            final String text = resultText;
            postToUi(() -> {
                tvTestResult.setText(text);
                btnTestConnection.setEnabled(true);
            });
        });
    }

    /**
     * 【测试数据库】按钮：插入示例单词与艾宾浩斯复习记录，查询待复习单词，
     * 把结果写回 TextView，并在 Logcat（TAG = WordDbTest）打印详细过程。
     */
    private void testDatabase() {
        Log.i(DB_TAG, "开始测试 Room 数据库：Word / ReviewRecord / WordDao / 迁移配置 ...");
        btnTestDb.setEnabled(false);
        tvDbResult.setText("正在测试数据库 ...");

        final Context appContext = requireContext().getApplicationContext();
        executor.execute(() -> {
            String resultText;
            try {
                WordDao dao = AppDatabase.getInstance(appContext).wordDao();

                // 清空旧数据，方便重复点击测试（先删子表，避免外键约束）
                dao.deleteAllReviewRecords();
                dao.deleteAllWords();

                long now = System.currentTimeMillis();

                // 1. 单词实体：单词 / 释义 / 难度 / 记忆状态
                long abandonId = dao.insertWord(new Word("abandon", "v. 放弃；抛弃", 3, Word.STATE_LEARNING));
                long efficientId = dao.insertWord(new Word("efficient", "adj. 高效的", 2, Word.STATE_UNLEARNED));
                long resilientId = dao.insertWord(new Word("resilient", "adj. 有韧性的", 4, Word.STATE_LEARNING));

                // 2. 复习记录实体：abandon 已到期待复习，其余按艾宾浩斯间隔排到以后
                dao.insertReviewRecord(ReviewRecord.pending(abandonId, 0, now - 1000L));
                dao.insertReviewRecord(ReviewRecord.pending(efficientId, 0,
                        now + ReviewRecord.intervalOf(0)));
                dao.insertReviewRecord(ReviewRecord.pending(resilientId, 1,
                        now + ReviewRecord.intervalOf(1)));

                // 3. 查询待复习单词
                List<Word> dueWords = dao.getDueWords(now);
                StringBuilder dueNames = new StringBuilder();
                for (Word word : dueWords) {
                    if (dueNames.length() > 0) {
                        dueNames.append("、");
                    }
                    dueNames.append(word.getWord());
                }

                // 4. 更新记忆状态，验证 DAO 的“改”
                dao.updateMemoryState(abandonId, Word.STATE_MASTERED);

                String summary = "单词 " + dao.countWords() + " 个，待复习 " + dueWords.size()
                        + " 个" + (dueWords.isEmpty() ? "" : "（" + dueNames + "）");
                Log.i(DB_TAG, "✅ 数据库测试成功：" + summary);
                Log.i(DB_TAG, "全部单词：" + dao.getAllWords());
                Log.i(DB_TAG, "abandon 的复习记录：" + dao.getReviewRecordsForWord(abandonId));

                resultText = "✅ 数据库测试成功\n" + summary + "\n详细日志见 Logcat（TAG：" + DB_TAG + "）";
            } catch (Exception e) {
                Log.e(DB_TAG, "❌ 数据库测试失败：" + e);
                resultText = "❌ 数据库测试失败：" + e.getMessage();
            }

            final String text = resultText;
            postToUi(() -> {
                tvDbResult.setText(text);
                btnTestDb.setEnabled(true);
            });
        });
    }

    /** Fragment 已经销毁时不再回写 UI，避免 NullPointerException。 */
    private void postToUi(Runnable action) {
        if (!isAdded()) {
            return;
        }
        requireActivity().runOnUiThread(() -> {
            if (isAdded()) {
                action.run();
            }
        });
    }
}
