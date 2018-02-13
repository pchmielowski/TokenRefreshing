package net.chmielowski.token;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

public class InterceptorTest {
    @Test
    public void happyPath() throws Exception {
        Interceptor.Chain chain = Mockito.mock(Interceptor.Chain.class);
        Request request = new Request.Builder()
                .header("Authorization", "First token")
                .url("http://example.com")
                .build();
        Mockito.when(chain.request())
                .thenReturn(request);
        Mockito.when(chain.proceed(Mockito.any()))
                .thenReturn(new Response.Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("")
                        .build());
        Response response = refreshToken(chain);
        Assert.assertThat(response.code(), is(equalTo(200)));
        Mockito.verify(chain).request();
        Mockito.verify(chain).proceed(Mockito.any());
        Mockito.verifyNoMoreInteractions(chain);
    }

    @Test
    public void singleRefresh() throws Exception {
        Interceptor.Chain chain = Mockito.mock(Interceptor.Chain.class);
        Request request = new Request.Builder()
                .header("Authorization", "First token")
                .url("http://example.com")
                .build();
        Mockito.when(chain.request())
                .thenReturn(request);
        Mockito.when(chain.proceed(Mockito.any()))
                .thenReturn(new Response.Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_1_1)
                        .code(401)
                        .message("")
                        .build())
                .thenReturn(new Response.Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("")
                        .build());
        Response response = refreshToken(chain);
        Assert.assertThat(response.code(), is(equalTo(200)));
        Mockito.verify(chain).request();
        Mockito.verify(chain, Mockito.times(2)).proceed(Mockito.any());
        Mockito.verifyNoMoreInteractions(chain);
    }

    private Response refreshToken(final Interceptor.Chain chain) throws IOException {
        Request request = chain.request();
        final Response response = chain.proceed(request);
        if (response.code() == 401) {
            return chain.proceed(request.newBuilder()
                    .header("Authorization", "Dupa")
                    .build());
        }
        return response;
    }
}
