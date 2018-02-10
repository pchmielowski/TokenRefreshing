package net.chmielowski.token;

import android.support.annotation.NonNull;

import com.google.gson.GsonBuilder;

import org.junit.Test;

import java.io.IOException;
import java.util.Objects;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

import static java.util.Objects.requireNonNull;

public class ExampleUnitTest {


    private static final String AUTHORIZATION = "Authorization";
    private Api api;
    private boolean tokenExpired;

    @Test
    public void getsData() throws Exception {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(401));
        server.enqueue(new MockResponse().setBody("{ \"token\": \"QWERT\" }"));
        server.enqueue(new MockResponse().setBody("{ \"name\": \"Hello\" }"));
        server.start();

        api = new Retrofit.Builder()
                .baseUrl(server.url("/get/"))
                .client(new OkHttpClient.Builder()
                        .addInterceptor(new HttpLoggingInterceptor()
                                .setLevel(HttpLoggingInterceptor.Level.BODY))
                        .addInterceptor(this::addToken)
                        .addInterceptor(this::refreshToken)
                        .build())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(new GsonBuilder().create()))

                .build()
                .create(Api.class);

        api.data()
                .test()
                .assertValue(response -> {
                    final Api.Data data = new Api.Data();
                    data.name = "Hello";
                    return data.equals(response.body());
                });

        server.shutdown();
    }

    private Response refreshToken(final Interceptor.Chain chain) throws IOException {
        final Request request = chain.request();
        final Request.Builder builder = request.newBuilder();
        final Response response = chain.proceed(builder.build());
        if (response.code() == 401) {
            final boolean sentWithOldToken = !Objects.equals(requireNonNull(request.header(AUTHORIZATION)), token());
            if (sentWithOldToken) {
                return retryWithNewToken(chain, builder);
            }

            tokenExpired = true;
            doRefreshToken();
            builder.header(AUTHORIZATION, token());
            final Request updatedRequest = builder.build();
            return chain.proceed(updatedRequest);
        }
        if (response.code() == 403) {
            final String token = token();
            final boolean sentWithOldToken = !Objects.equals(requireNonNull(request.header(AUTHORIZATION)), token);
            if (sentWithOldToken) {
                return retryWithNewToken(chain, builder);
            } else {
                throw new RuntimeException("403 for current token");
            }
        }
        return response;
    }

    private Response retryWithNewToken(Interceptor.Chain chain, Request.Builder builder) throws IOException {
        builder.header(AUTHORIZATION, token());
        return chain.proceed(builder.build());
    }

    @SuppressWarnings("ConstantConditions")
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
        return "ABCD";
    }

    private void storeToken(@NonNull String token) {
        // TODO
    }
}