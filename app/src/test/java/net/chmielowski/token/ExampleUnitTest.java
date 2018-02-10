package net.chmielowski.token;

import android.support.annotation.NonNull;

import com.google.gson.GsonBuilder;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Objects;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

import static java.util.Objects.requireNonNull;

public class ExampleUnitTest {


    private static final String AUTHORIZATION = "Authorization";
    private static final String EXPIRED_TOKEN = "ABCD";
    private static final String REFRESHED_TOKEN = "QWERT";
    private Api api;
    private boolean tokenExpired;
    private String token = EXPIRED_TOKEN;

    @Test
    public void getsData() throws Exception {
        MockWebServer server = new MockWebServer();


        final Dispatcher dispatcher = new Dispatcher() {

            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                final String path = request.getPath();
                switch (path) {
                    case "/data":
                        if (Objects.equals(request.getHeader(AUTHORIZATION), EXPIRED_TOKEN))
                            return new MockResponse().setResponseCode(401);
                        if (Objects.equals(request.getHeader(AUTHORIZATION), REFRESHED_TOKEN))
                            return new MockResponse().setBody("{ \"name\": \"Hello\" }");
                        throw new IllegalStateException();
                    case "/refresh":
                        if (Objects.equals(request.getHeader(AUTHORIZATION), EXPIRED_TOKEN))
                            return new MockResponse().setBody("{ \"token\": \"" + REFRESHED_TOKEN + "\" }");
                        throw new IllegalStateException();
                }
                throw new IllegalStateException();
            }
        };
        server.setDispatcher(dispatcher);
        server.start();

        api = new Retrofit.Builder()
                .baseUrl(server.url("/"))
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

        Assert.assertEquals(EXPIRED_TOKEN, server.takeRequest().getHeader(AUTHORIZATION));
        Assert.assertEquals(EXPIRED_TOKEN, server.takeRequest().getHeader(AUTHORIZATION));
        Assert.assertEquals(REFRESHED_TOKEN, server.takeRequest().getHeader(AUTHORIZATION));

        server.shutdown();
    }

    private Response refreshToken(final Interceptor.Chain chain) throws IOException {
        final Request request = chain.request();
        final Request.Builder builder = request.newBuilder();
        final Response response = chain.proceed(builder.build());
        if (response.code() == 401) {
            final boolean sentWithOldToken = !Objects.equals(requireNonNull(getToken(request)), token());
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
            final boolean sentWithOldToken = !Objects.equals(requireNonNull(getToken(request)), token);
            if (sentWithOldToken) {
                return retryWithNewToken(chain, builder);
            } else {
                throw new RuntimeException("403 for current token");
            }
        }
        return response;
    }

    private static String getToken(Request request) {
        return request.header(AUTHORIZATION);
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
        return token;
    }

    private void storeToken(@NonNull String token) {
        this.token = token;
    }
}