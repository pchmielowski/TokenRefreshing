package net.chmielowski.token;

import com.google.gson.GsonBuilder;

import org.junit.Test;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class ExampleUnitTest {
    @Test
    public void test() throws Exception {
        MockWebServer server = new MockWebServer();

        final Api.Data data = new Api.Data();
        data.name = "Hello";
        server.enqueue(new MockResponse().setBody("{ \"name\": \"Hello\" }"));

        server.start();

        HttpUrl baseUrl = server.url("/get/");

        new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(new GsonBuilder().create()))
                .build()

                .create(TestService.class)
                .get()
                .test()
                .assertValue(response -> response.body().equals(data));

        server.shutdown();
    }

}