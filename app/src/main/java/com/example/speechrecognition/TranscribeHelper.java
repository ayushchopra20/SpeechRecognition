package com.example.speechrecognition;

import android.content.Context;
import android.util.Base64;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;

import java.io.*;
import java.util.Collections;

public class TranscribeHelper {

    public interface TranscriptionCallback {
        void onSuccess(String result);
        void onError(String error);
    }

    public static void transcribeAudio(Context context, File audioFile, TranscriptionCallback callback) {
        new Thread(() -> {
            try {
                // 1. Load Google credentials
                InputStream credentialsStream = context.getResources().openRawResource(R.raw.credentials);
                GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream)
                        .createScoped(Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));
                credentials.refreshIfExpired();
                String accessToken = credentials.getAccessToken().getTokenValue();

                // 2. Base64 encode the audio file
                byte[] audioBytes = readAllBytes(audioFile);
                String audioBase64 = Base64.encodeToString(audioBytes, Base64.NO_WRAP);

                // 3. Create JSON request
                JsonObject config = new JsonObject();
                config.addProperty("encoding", "LINEAR16");
                config.addProperty("sampleRateHertz", 16000);
                config.addProperty("languageCode", "en-US");

                config.addProperty("languageCode", "en-US"); // Primary language

                JsonArray altLangs = new JsonArray();
                altLangs.add("hi-IN");      // Hindi
                altLangs.add("es-ES");      // Spanish
                altLangs.add("fr-FR");      // French
                config.add("alternativeLanguageCodes", altLangs);

                JsonObject audio = new JsonObject();
                audio.addProperty("content", audioBase64);

                JsonObject requestJson = new JsonObject();
                requestJson.add("config", config);
                requestJson.add("audio", audio);

                // 4. Make HTTP request
                OkHttpClient client = new OkHttpClient();
                RequestBody requestBody = RequestBody.create(
                        requestJson.toString(), MediaType.parse("application/json"));
                Request request = new Request.Builder()
                        .url("https://speech.googleapis.com/v1/speech:recognize")
                        .addHeader("Authorization", "Bearer " + accessToken)
                        .post(requestBody)
                        .build();

                Response response = client.newCall(request).execute();
                String responseBody = response.body().string();
                System.out.println("Full GCP Response: " + responseBody);

                if (response.isSuccessful()) {
                    JsonObject responseJson = JsonParser.parseString(responseBody).getAsJsonObject();

                    if (responseJson.has("results") && !responseJson.get("results").isJsonNull()) {
                        JsonArray results = responseJson.getAsJsonArray("results");

                        if (results.size() > 0) {
                            StringBuilder transcript = new StringBuilder();
                            for (int i = 0; i < results.size(); i++) {
                                JsonObject result = results.get(i).getAsJsonObject();
                                JsonArray alternatives = result.getAsJsonArray("alternatives");
                                if (alternatives != null && alternatives.size() > 0) {
                                    String text = alternatives.get(0).getAsJsonObject()
                                            .get("transcript").getAsString();
                                    transcript.append(text).append("\n");
                                }
                            }
                            callback.onSuccess(transcript.toString());
                        } else {
                            callback.onError("No transcription found.");
                        }
                    } else {
                        callback.onError("Empty response: No 'results' field in response.");
                    }
                } else {
                    callback.onError("Error: " + responseBody);
                }

            } catch (Exception e) {
                e.printStackTrace();
                callback.onError("Exception: " + e.getMessage());
            }
        }).start();
    }

    private static byte[] readAllBytes(File file) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        FileInputStream fis = new FileInputStream(file);
        byte[] buffer = new byte[1024];
        int read;
        while ((read = fis.read(buffer)) != -1) {
            bos.write(buffer, 0, read);
        }
        fis.close();
        return bos.toByteArray();
    }
}
