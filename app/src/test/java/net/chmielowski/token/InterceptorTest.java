package net.chmielowski.token;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

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
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@RunWith(MockitoJUnitRunner.class)
public class InterceptorTest {
    private static final String NEW_TOKEN = "New token";
    private String token = "First token";

    @Test
    public void happyPath() throws Exception {
        Interceptor.Chain chain = mock(Interceptor.Chain.class);
        Request request = createRequest();
        when(chain.request())
                .thenReturn(request);
        when(chain.proceed(any()))
                .thenReturn(create200OK(request));

        Response response = new RefreshingInterceptor(mock(Token.class)).intercept(chain);
        assertThat(response.code(), is(equalTo(200)));
        verify(chain).request();
        verify(chain).proceed(any());
        verifyNoMoreInteractions(chain);
    }

    private static <T> T mock(Class<T> classToMock) {
        return Mockito.mock(classToMock, withSettings().verboseLogging());
    }

    private static Response create200OK(Request request) {
        return new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .build();
    }

    public void singleRefresh(RefreshingInterceptor interceptor, Interceptor.Chain chain) throws IOException {
        // Arrange

        // Act
        Response response = interceptor.intercept(chain);

        // Assert
        assertThat(response.code(), is(equalTo(200)));
        verify(chain).request();
        verify(chain, times(2)).proceed(any());
        verifyNoMoreInteractions(chain);
    }

    @Test
    public void multiThread() throws Exception {
        Token token = mock(Token.class);

        RefreshedToken refreshToken = new RefreshedToken();
        when(token.fresh())
                .thenAnswer(refreshToken);
        RefreshingInterceptor interceptor = new RefreshingInterceptor(token);


        Interceptor.Chain chain = mock(Interceptor.Chain.class);
        Request request = createRequest();
        when(chain.request()).thenReturn(request, request).thenThrow(AssertionError.class);
        when(chain.proceed(any()))
                // Requests with old token
                .thenReturn(createExpiredToken(request))
                .then(__ -> {
                    refreshToken.triggerResponse();
                    return createExpiredToken(request);
                })

                // Requests with new token
                .thenReturn(create200OK(request))
                .thenReturn(create200OK(request))
                .thenThrow(AssertionError.class);


        AsyncTest t1 = start(() -> singleRefresh(interceptor, chain));
        AsyncTest t2 = start(() -> singleRefresh(interceptor, chain));
        t1.join();
        t2.join();

        verify(token, times(1)).fresh();
    }

    private static Response createExpiredToken(Request request) {
        return new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(401)
                .message("Unauthorized")
                .build();
    }

    private static Request createRequest() {
        return new Request.Builder()
                .header("Authorization", "First token")
                .url("http://example.com")
                .build();
    }

    private static final String AUTHORIZATION = "Authorization";
    private boolean tokenExpired;

    private static class RefreshedToken implements Answer<String> {

        @Override
        public String answer(InvocationOnMock invocation) throws Throwable {
            System.out.println("request for refresh token");
            synchronized (this) {
                wait();
            }
            System.out.println("will triggerResponse with refreshed token");
            return NEW_TOKEN;
        }

        void triggerResponse() throws InterruptedException {
            System.out.println("triggerResponse() called");
            synchronized (this) {
                notify();
            }
        }
    }

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
    private volatile Exception error;

    private AsyncTest(ThrowingRunnable test) {
        thread = new Thread(() -> {
            try {
                test.run();
            } catch (Exception e) {
                error = e;
            }
        });
    }

    static AsyncTest start(ThrowingRunnable test) {
        return new AsyncTest(test).start();
    }

    private AsyncTest start() {
        assert thread != null;
        thread.start();
        return this;
    }

    void join() throws Exception {
        assert thread != null;
        thread.join();
        if (error != null) {
            //noinspection ConstantConditions
            throw error;
        }
    }

    @FunctionalInterface
    interface ThrowingRunnable {
        void run() throws Exception;
    }
}