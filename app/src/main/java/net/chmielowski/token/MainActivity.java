package net.chmielowski.token;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.google.gson.GsonBuilder;

import java.io.IOException;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Authenticator;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    @Nullable
    private String token;
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

        findViewById(R.id.get_data)
                .setOnClickListener(view -> api.data()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe());

        findViewById(R.id.login)
                .setOnClickListener(view -> api.login()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(response -> this.token = response.body().token));
    }

    private void setText(String text) {
        new Handler(getMainLooper())
                .post(() -> this.<TextView>findViewById(R.id.text)
                        .append(String.format("%s%n", text)));
    }

    private Response refreshToken(final Interceptor.Chain chain) throws IOException {
        final Request.Builder builder = chain.request().newBuilder();
        final Response response = chain.proceed(builder.build());
        if (response.code() == 401) {
            doRefreshToken();
            builder.header("Authorization", this.token);
            return chain.proceed(builder.build());
        }
        return response;
    }

    private void doRefreshToken() {
        this.token = api.refresh()
                .blockingGet()
                .body()
                .token;
    }

    private Response addToken(Interceptor.Chain chain) throws IOException {
        return this.token == null
                ? chain.proceed(chain.request())
                : chain.proceed(chain.request()
                .newBuilder()
                .header("Authorization", this.token)
                .build());
    }
}
