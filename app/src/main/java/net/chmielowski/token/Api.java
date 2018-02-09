package net.chmielowski.token;

import io.reactivex.Single;
import retrofit2.Response;
import retrofit2.http.GET;

interface Api {
    @GET("/data")
    Single<Response<Data>> data();

    class Data {
        String name;
    }
}
