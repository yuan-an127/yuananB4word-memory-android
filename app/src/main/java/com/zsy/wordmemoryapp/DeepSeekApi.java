package com.zsy.wordmemoryapp;

import android.util.Log;

import com.google.gson.Gson;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DeepSeekApi {
    private static final String TAG = "DeepSeekApi";

    // 👉 这里替换成你的DeepSeek API密钥
    private static final String API_KEY = "347f91e4-50f4-4aa7-b98f-4f0b321a9cb3";
    private static final String API_URL = "https://api.deepseek.com/v1/chat/completions";

    // 连通性测试用的最小请求体：只要 1 个 token，几乎不消耗额度
    private static final String TEST_BODY =
            "{\"model\":\"deepseek-chat\",\"messages\":[{\"role\":\"user\",\"content\":\"ping\"}],\"max_tokens\":1}";

    // 显式设置超时，便于把“网络超时”和“密钥错误”区分开
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
    private final Gson gson = new Gson();

    /** 密钥错误（HTTP 401/403 或 API_KEY 未配置）。 */
    public static class ApiKeyException extends IOException {
        public ApiKeyException(String message) {
            super(message);
        }
    }

    /** 网络超时（连接超时或读取超时）。 */
    public static class NetworkTimeoutException extends IOException {
        public NetworkTimeoutException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    static class Message {
        String role;
        String content;
        Message(String r, String c) {
            role = r;
            content = c;
        }
    }

    static class RequestBodyModel {
        String model = "deepseek-chat";
        List<Message> messages;
        float temperature = 0.3f;
    }

    static class ResponseModel {
        List<Choice> choices;
    }

    static class Choice {
        Message message;
    }

    /**
     * 单独测试与 DeepSeek API 的连通性，不依赖词典业务逻辑。
     *
     * <p>成功：返回响应 JSON，并打印“请求成功”日志；
     * 密钥错误：抛出 {@link ApiKeyException}，并打印“密钥错误”日志；
     * 网络超时：抛出 {@link NetworkTimeoutException}，并打印“网络超时”日志。</p>
     */
    public String testConnection() throws IOException {
        if (API_KEY == null || API_KEY.isEmpty() || API_KEY.contains("替换")) {
            Log.e(TAG, "❌ 密钥错误：API_KEY 未配置，请先在 DeepSeekApi 中填入密钥");
            throw new ApiKeyException("API_KEY 未配置");
        }

        Request request = new Request.Builder()
                .url(API_URL)
                .header("Authorization", "Bearer " + API_KEY)
                .post(RequestBody.create(TEST_BODY, MediaType.parse("application/json;charset=utf-8")))
                .build();

        long start = System.currentTimeMillis();
        try (Response response = client.newCall(request).execute()) {
            int code = response.code();
            String body = response.body() == null ? "" : response.body().string();

            if (code == 401 || code == 403) {
                Log.e(TAG, "❌ 密钥错误：HTTP " + code + "，API_KEY 无效或没有权限");
                throw new ApiKeyException("HTTP " + code + "：API_KEY 无效或没有权限");
            }
            if (!response.isSuccessful()) {
                Log.e(TAG, "❌ 请求失败：HTTP " + code + "，" + body);
                throw new IOException("HTTP " + code + "：" + body);
            }

            Log.i(TAG, "✅ 请求成功：HTTP " + code + "，耗时 " + (System.currentTimeMillis() - start) + "ms");
            return body;
        } catch (ApiKeyException e) {
            // 上面已经打印过“密钥错误”日志，这里原样抛出
            throw e;
        } catch (SocketTimeoutException e) {
            Log.e(TAG, "⏱️ 网络超时：连接或读取 DeepSeek API 超时，" + e.getMessage());
            throw new NetworkTimeoutException("连接 DeepSeek API 超时", e);
        } catch (InterruptedIOException e) {
            // OkHttp 写超时等场景也会抛 InterruptedIOException
            Log.e(TAG, "⏱️ 网络超时：" + e.getMessage());
            throw new NetworkTimeoutException("请求 DeepSeek API 超时", e);
        } catch (IOException e) {
            Log.e(TAG, "❌ 网络请求异常：" + e);
            throw e;
        }
    }

    //查询单词释义
    public String getWordExplain(String word) throws IOException {
        RequestBodyModel req = new RequestBodyModel();
        req.messages = List.of(
                new Message("system", "你是词典，只返回单词简洁中文释义，不要多余内容"),
                new Message("user", "解释单词：" + word)
        );
        String jsonStr = gson.toJson(req);
        MediaType mediaType = MediaType.parse("application/json;charset=utf-8");
        RequestBody body = RequestBody.create(jsonStr, mediaType);

        Request request = new Request.Builder()
                .url(API_URL)
                .header("Authorization", "Bearer " + API_KEY)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("请求失败");
            ResponseModel result = gson.fromJson(response.body().string(), ResponseModel.class);
            return result.choices.get(0).message.content;
        }
    }
}
