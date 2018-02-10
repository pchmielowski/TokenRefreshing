package net.chmielowski.token;

import io.reactivex.Single;
import retrofit2.Response;
import retrofit2.http.GET;

interface TestService {
    @GET("/get")
    Single<Response<Api.Data>> get();
}
