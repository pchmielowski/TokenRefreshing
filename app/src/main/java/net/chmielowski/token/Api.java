package net.chmielowski.token;

import io.reactivex.Single;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.POST;

interface Api {
    @GET("/data")
    Single<Response<Data>> data();

    @POST("/login")
    Single<Response<Token>> login();

    @POST("/refresh")
    Single<Response<Token>> refresh();

    class Data {
        String name;
    }

    class Token {
        String token;
    }
}
