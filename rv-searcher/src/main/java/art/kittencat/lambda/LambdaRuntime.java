package art.kittencat.lambda;

import com.google.common.base.Preconditions;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class LambdaRuntime {
    private final String awsLambdaRuntimeApi;
    private final String nextUrl;

    public LambdaRuntime(String awsLambdaRuntimeApi) {
        this.awsLambdaRuntimeApi = Preconditions.checkNotNull(awsLambdaRuntimeApi);
        this.nextUrl = String.format("http://%s/2018-06-01/runtime/invocation/next", awsLambdaRuntimeApi);
    }

    public void start() throws IOException {
        while (true) {
            NextResponse nextResponse = getNext();
            postResponse(nextResponse.requestId, nextResponse.invocation);
        }
    }

    private NextResponse getNext() throws IOException {
        HttpGet httpGet = new HttpGet(nextUrl);
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            return httpClient.execute(httpGet, (response -> {
                String requestId = Preconditions.checkNotNull(response.getHeader("Lambda-Runtime-Aws-Request-Id")).getValue();
                String invocation = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                return new NextResponse(requestId, invocation);
            }));
        }
    }

    private void postResponse(String requestId, String data) throws IOException {
        String responseUrl = String.format("http://%s/2018-06-01/runtime/invocation/%s/response",
                awsLambdaRuntimeApi, requestId);
        HttpPost httpPost = new HttpPost(responseUrl);
        httpPost.setEntity(new StringEntity(data, ContentType.APPLICATION_JSON));
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            httpClient.execute(httpPost, (response -> response));
        }
    }

    static class NextResponse {
        private final String requestId;
        private final String invocation;

        public NextResponse(String requestId, String invocation) {
            this.requestId = requestId;
            this.invocation = invocation;
        }
    }

    public static void main(String[] args) throws IOException {
        LambdaRuntime lambdaRuntime = new LambdaRuntime(System.getenv("AWS_LAMBDA_RUNTIME_API"));
        lambdaRuntime.start();
    }
}
