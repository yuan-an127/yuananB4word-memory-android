package com.zsy.wordmemoryapp;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "DeepSeekTest";
    private static final String DB_TAG = "WordDbTest";

    private final DeepSeekApi deepSeekApi = new DeepSeekApi();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private TextView tvTestResult;
    private TextView tvDbResult;
    private Button btnTestConnection;
    private Button btnTestDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvTestResult = findViewById(R.id.tvTestResult);
        tvDbResult = findViewById(R.id.tvDbResult);
        btnTestConnection = findViewById(R.id.btnTestConnection);
        btnTestDb = findViewById(R.id.btnTestDb);

        btnTestConnection.setOnClickListener(v -> testDeepSeekConnection());
        btnTestDb.setOnClickListener(v -> testDatabase());
    }

    @Override
    protected void onDestroy() {
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
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
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

        executor.execute(() -> {
            String resultText;
            try {
                WordDao dao = AppDatabase.getInstance(getApplicationContext()).wordDao();

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
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                tvDbResult.setText(text);
                btnTestDb.setEnabled(true);
            });
        });
    }
}
