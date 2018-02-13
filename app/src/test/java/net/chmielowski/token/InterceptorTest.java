package net.chmielowski.token;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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
import static net.chmielowski.token.AsyncTest.start;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class InterceptorTest {
    private static final String NEW_TOKEN = "New token";
    private String token = "First token";

    @Test
    public void happyPath() throws Exception {
        Interceptor.Chain chain = mock(Interceptor.Chain.class);
        Request request = new Request.Builder()
                .header("Authorization", "First token")
                .url("http://example.com")
                .build();
        when(chain.request())
                .thenReturn(request);
        when(chain.proceed(Mockito.any()))
                .thenReturn(create200OK(request));

        Response response = new RefreshingInterceptor(mock(Token.class)).intercept(chain);
        Assert.assertThat(response.code(), is(equalTo(200)));
        verify(chain).request();
        verify(chain).proceed(Mockito.any());
        Mockito.verifyNoMoreInteractions(chain);
    }

    private static <T> T mock(Class<T> classToMock) {
        return Mockito.mock(classToMock);
    }

    private static Response create200OK(Request request) {
        return new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .build();
    }

    public void singleRefresh(RefreshingInterceptor interceptor) {
        try {
            Interceptor.Chain chain = mock(Interceptor.Chain.class);
            Request request = new Request.Builder()
                    .header("Authorization", "First token")
                    .url("http://example.com")
                    .build();
            when(chain.request())
                    .thenReturn(request);
            when(chain.proceed(Mockito.any()))
                    .thenReturn(createExpiredToken(request))
                    .thenReturn(create200OK(request));
            Response response = interceptor.intercept(chain);
            Assert.assertThat(response.code(), is(equalTo(200)));
            verify(chain).request();
            verify(chain, times(2)).proceed(Mockito.any());
            Mockito.verifyNoMoreInteractions(chain);
        } catch (Exception e) {
            throw new AssertionError(e);
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

    // TODO: sometimes fails
    @Test
    public void multiThread() throws Exception {
        Token token = mock(Token.class);

        when(token.fresh())
                .thenReturn(NEW_TOKEN);
        RefreshingInterceptor interceptor = new RefreshingInterceptor(token);
        AsyncTest t1 = start(() -> singleRefresh(interceptor));
        AsyncTest t2 = start(() -> singleRefresh(interceptor));
        t1.join();
        t2.join();

        verify(token, times(1)).fresh();
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

        private void doRefreshToken() {
            if (!tokenExpired) {
                return;
            }
            try {
                storeToken(token.fresh());
            } catch (IOException e) {
                // TODO: rethrow?
                // TODO: make simulation with throwing fake TimeoutError
                e.printStackTrace();
            } finally {
                // TODO: make sure it has to be set in case of Exception
                tokenExpired = false;
            }
        }

    }

    private Response retryWithNewToken(Interceptor.Chain chain, Request.Builder builder) throws IOException {
        builder.header(AUTHORIZATION, token());
        return chain.proceed(builder.build());
    }

    interface Token {
        @NonNull
        String fresh() throws IOException;
    }

    private String token() {
        return this.token;
    }

    private void storeToken(@NonNull String token) {
        this.token = token;
    }
}

class AsyncTest {
    @Nullable
    private Thread thread;
    @Nullable
    private volatile AssertionError error;

    private AsyncTest(Runnable test) {
        thread = new Thread(() -> {
            try {
                test.run();
            } catch (AssertionError e) {
                error = e;
            }
        });
    }

    static AsyncTest start(Runnable test) {
        return new AsyncTest(test).start();
    }

    private AsyncTest start() {
        assert thread != null;
        thread.start();
        return this;
    }

    void join() throws InterruptedException, AssertionError {
        assert thread != null;
        thread.join();
        if (error != null) {
            //noinspection ConstantConditions
            throw error;
        }
    }
}