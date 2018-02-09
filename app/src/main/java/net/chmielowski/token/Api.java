package net.chmielowski.token;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;

interface Api {
    @GET("/data")
    Call<Response<Data>> data();

    class Data {
        String name;
    }
}
