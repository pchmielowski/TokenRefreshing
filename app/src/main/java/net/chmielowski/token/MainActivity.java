package net.chmielowski.token;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.widget.TextView;

import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.Objects;
import java.util.Random;
import java.util.stream.IntStream;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private static final String AUTHORIZATION = "Authorization";
    private Api api;
    private boolean tokenExpired;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        api = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:5000/")
                .client(new OkHttpClient.Builder()
//                        .addInterceptor(new HttpLoggingInterceptor(this::appendText)
//                                .setLevel(HttpLoggingInterceptor.Level.BASIC))
                        .addInterceptor(this::addToken)
                        .addInterceptor(this::refreshToken)
                        .build())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(new GsonBuilder().create()))

                .build()
                .create(Api.class);

        findViewById(R.id.clear)
                .setOnClickListener(view -> this.<TextView>findViewById(R.id.text)
                        .setText(null));

        findViewById(R.id.get_data)
                .setOnClickListener(view -> getData());

        findViewById(R.id.login)
                .setOnClickListener(view -> api.login()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(response -> this.storeToken(response.body().token)));
    }

    private void getData() {
        IntStream.rangeClosed(1, Integer.parseInt(String.valueOf(((TextView) findViewById(R.id.repetition)).getText())))
                .forEach(__ -> api.data()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .map(___ -> "Ignoring value...")
                        .onErrorReturnItem("Ignoring value...")
                        .subscribe());
    }

    private void appendText(SpannableStringBuilder text) {
        new Handler(getMainLooper())
                .post(() -> this.<TextView>findViewById(R.id.text)
                        .append(text));
    }

    private Response refreshToken(final Interceptor.Chain chain) throws IOException {
        final Request request = chain.request();
        final int color = generateColor();
        log(color, "--> /%s%n  %s%n", request.url().toString().split("5000/")[1], request.header(AUTHORIZATION));
        final Request.Builder builder = request.newBuilder();
        final Response response = chain.proceed(builder.build());
        log(color, "<-- /%s %d%n", request.url().toString().split("5000/")[1], response.code());
        if (response.code() == 401) {
            tokenExpired = true;
            doRefreshToken();
            builder.header(AUTHORIZATION, token());
            return chain.proceed(builder.build());
        }
        if (response.code() == 403) {
            if (sentWithOldToken(request.header(AUTHORIZATION))) {
                return retryWithNewToken(chain, builder);
            } else {
                // Logout?
                throw new RuntimeException("403 for current token");
            }
        }
        return response;
    }

    private int generateColor() {
        final Random random = new Random();
        final int bound = 0xFF;
        final int r = random.nextInt(bound);
        final int g = random.nextInt(bound);
        final int b = random.nextInt(bound);
        return Color.rgb(r, g, b);
    }


    private void log(int color, String format, Object... args) {
        final SpannableStringBuilder builder = new SpannableStringBuilder();
        builder.append(String.format(format, args));
        builder.setSpan(new ForegroundColorSpan(color), 0, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        appendText(builder);
    }

    private Response retryWithNewToken(Interceptor.Chain chain, Request.Builder builder) throws IOException {
        builder.header(AUTHORIZATION, token());
        return chain.proceed(builder.build());
    }

    private boolean sentWithOldToken(String sent) {
        return !Objects.equals(Objects.requireNonNull(sent), token());
    }

    private synchronized void doRefreshToken() {
        if (!tokenExpired) {
            return;
        }
        this.storeToken(api.refresh()
                .blockingGet()
                .body()
                .token);
        tokenExpired = false;
    }

    private Response addToken(Interceptor.Chain chain) throws IOException {
        return chain.proceed(chain.request()
                .newBuilder()
                .header(AUTHORIZATION, this.token())
                .build());
    }

    @NonNull
    private String token() {
        return Objects.requireNonNull(
                getSharedPreferences()
                        .getString("token", null));
    }

    @SuppressLint("ApplySharedPref") // Because I want it to be synchronous
    private void storeToken(@NonNull String token) {
        getSharedPreferences()
                .edit()
                .putString("token", token)
                .commit();
    }

    private SharedPreferences getSharedPreferences() {
        return getSharedPreferences("default", MODE_PRIVATE);
    }
}
