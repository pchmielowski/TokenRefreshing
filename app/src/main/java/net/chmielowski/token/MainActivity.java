package net.chmielowski.token;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.Locale;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Api api = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:5000/")
                .client(new OkHttpClient.Builder()
                        .addInterceptor(new HttpLoggingInterceptor()
                                .setLevel(HttpLoggingInterceptor.Level.BODY))
                        .addInterceptor(this::addToken)
                        .build())
                .addConverterFactory(GsonConverterFactory.create(new GsonBuilder().create()))
                .build()
                .create(Api.class);

        findViewById(R.id.button)
                .setOnClickListener(view -> api.data().enqueue(new Callback<retrofit2.Response<Api.Data>>() {
                    @Override
                    public void onResponse(Call<retrofit2.Response<Api.Data>> call, retrofit2.Response<retrofit2.Response<Api.Data>> response) {
                        Log.d("pchm", "MainActivity::onResponse");
                    }

                    @Override
                    public void onFailure(Call<retrofit2.Response<Api.Data>> call, Throwable t) {
                        Log.d("pchm", "MainActivity::onFailure");
                    }
                }));
    }

    private Response addToken(Interceptor.Chain chain) throws IOException {
        return chain.proceed(chain.request()
                .newBuilder()
                .header("Authorization", "EME5PA4XRS")
                .build());
    }
}
