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
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Data data = (Data) o;

            return name != null ? name.equals(data.name) : data.name == null;
        }

        @Override
        public int hashCode() {
            return name != null ? name.hashCode() : 0;
        }

        String name;
    }

    class Token {
        String token;
    }
}
