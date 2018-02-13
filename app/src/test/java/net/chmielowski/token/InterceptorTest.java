package net.chmielowski.token;

import android.support.annotation.NonNull;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.Objects;

import okhttp3.Interceptor;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;

import static java.util.Objects.requireNonNull;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.fail;

@RunWith(MockitoJUnitRunner.class)
public class InterceptorTest {
    private String token = "First token";

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
                .thenReturn(create200OK(request));

        Response response = new RefreshingInterceptor(Mockito.mock(Token.class)).intercept(chain);
        Assert.assertThat(response.code(), is(equalTo(200)));
        Mockito.verify(chain).request();
        Mockito.verify(chain).proceed(Mockito.any());
        Mockito.verifyNoMoreInteractions(chain);
    }

    private static Response create200OK(Request request) {
        return new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .build();
    }

    public void singleRefresh() {
        try {
            Interceptor.Chain chain = Mockito.mock(Interceptor.Chain.class);
            Request request = new Request.Builder()
                    .header("Authorization", "First token")
                    .url("http://example.com")
                    .build();
            Mockito.when(chain.request())
                    .thenReturn(request);
            Mockito.when(chain.proceed(Mockito.any()))
                    .thenReturn(createExpiredToken(request))
                    .thenReturn(create200OK(request));
            Response response = new RefreshingInterceptor(Mockito.mock(Token.class)).intercept(chain);
            Assert.assertThat(response.code(), is(equalTo(200)));
            Mockito.verify(chain).request();
            Mockito.verify(chain, Mockito.times(2)).proceed(Mockito.any());
            Mockito.verifyNoMoreInteractions(chain);
        } catch (Exception e) {
            fail();
        }
    }

    private static Response createExpiredToken(Request request) {
        return new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(401)
                .message("Unauthorized")
                .build();
    }

    @Test
    public void multiThread() throws Exception {
        Thread first = new Thread(this::singleRefresh);
        first.start();
        Thread second = new Thread(this::singleRefresh);
        second.start();
        first.join();
        second.join();
    }

    private static final String AUTHORIZATION = "Authorization";
    private boolean tokenExpired;

    final class RefreshingInterceptor implements Interceptor {

        final Token token;

        RefreshingInterceptor(Token token) {
            this.token = token;
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            final Request request = chain.request();
            final Request.Builder builder = request.newBuilder();
            final Response response = chain.proceed(builder.build());
            if (response.code() == 401) {
                final boolean sentWithOldToken = !Objects.equals(requireNonNull(request.header(AUTHORIZATION)), token());
                if (sentWithOldToken) {
                    return retryWithNewToken(chain, builder);
                }

                tokenExpired = true;
                synchronized (this) {
                    doRefreshToken();
                }
                builder.header(AUTHORIZATION, token());
                return chain.proceed(builder.build());
            }
            if (response.code() == 403) {
                final String token = token();
                final String actual = request.header(AUTHORIZATION);
                final boolean sentWithOldToken = !Objects.equals(requireNonNull(actual), token);
                if (sentWithOldToken || tokenExpired) {
                    return retryWithNewToken(chain, builder);
                } else {
                    // Logout?
                    // Call onError
                    throw new RuntimeException("403 for current token");
                }
            }
            return response;
        }
    }

    private Response retryWithNewToken(Interceptor.Chain chain, Request.Builder builder) throws IOException {
        builder.header(AUTHORIZATION, token());
        return chain.proceed(builder.build());
    }

    private void doRefreshToken() {
        if (!tokenExpired) {
            return;
        }
        try {
            this.storeToken(fetchToken());
        } catch (IOException e) {
            // TODO: rethrow?
            // TODO: make simulation with throwing fake TimeoutError
            e.printStackTrace();
        } finally {
            // TODO: make sure it has to be set in case of Exception
            tokenExpired = false;
        }
    }

    interface Token {
        String fresh();
    }

    private String fetchToken() throws IOException {
        // TODO: API call
        return "New token";
    }

    private String token() {
        return this.token;
    }

    private void storeToken(@NonNull String token) {
        this.token = token;
    }
}
