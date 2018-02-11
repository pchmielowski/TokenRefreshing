package net.chmielowski.token;

import io.reactivex.Single;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.POST;

interface Api {
    @GET("/data")
    Single<Response<Data>> data();

    @GET("/data2")
    Single<Response<Data>> data2();

    @POST("/login")
    Single<Response<Token>> login();

    @POST("/refresh")
    Call<Response<Token>> refresh();

    class Data {
        String name;
    }

    class Token {
        String token;
    }
}
