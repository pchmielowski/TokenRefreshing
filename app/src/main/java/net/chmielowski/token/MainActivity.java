package net.chmielowski.token;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private Api api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        api = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:5000/")
                .client(new OkHttpClient.Builder()
                        .addInterceptor(new HttpLoggingInterceptor(this::setText)
                                .setLevel(HttpLoggingInterceptor.Level.BASIC))
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
                        .doOnSubscribe(__ -> appendNewLine())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(response -> this.setToken(response.body().token)));
    }

    private void getData() {
        IntStream.rangeClosed(1, 5)
                .forEach(__ -> api.data()
                        .doOnSubscribe(___ -> appendNewLine())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .map(___ -> "Ignoring value...")
                        .onErrorReturnItem("Ignoring value...")
                        .subscribe());
    }

    private void appendNewLine() {
        new Handler(getMainLooper())
                .post(() -> this.<TextView>findViewById(R.id.text)
                        .append(String.format("%n")));
    }

    private void setText(String text) {
        new Handler(getMainLooper())
                .post(() -> this.<TextView>findViewById(R.id.text)
                        .append(String.format("%s%n", text.split(" \\(")[0])));
    }

    private Response refreshToken(final Interceptor.Chain chain) throws IOException {
        final Request.Builder builder = chain.request().newBuilder();
        final Response response = chain.proceed(builder.build());
        if (response.code() == 401) {
            doRefreshToken();
            builder.header("Authorization", this.getToken());
            return chain.proceed(builder.build());
        }
        return response;
    }

    private void doRefreshToken() {
        this.setToken(api.refresh()
                .blockingGet()
                .body()
                .token);
    }

    private Response addToken(Interceptor.Chain chain) throws IOException {
        return this.getToken() == null
                ? chain.proceed(chain.request())
                : chain.proceed(chain.request()
                .newBuilder()
                .header("Authorization", this.getToken())
                .build());
    }

    @Nullable
    private String getToken() {
        return getSharedPreferences()
                .getString("token", null);
    }

    private void setToken(@Nullable String token) {
        getSharedPreferences()
                .edit()
                .putString("token", token)
                .apply();
    }

    private SharedPreferences getSharedPreferences() {
        return getSharedPreferences("default", MODE_PRIVATE);
    }
}
