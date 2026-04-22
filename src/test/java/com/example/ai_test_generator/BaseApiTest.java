package com.example.ai_test_generator;

import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import io.qameta.allure.Attachment;
import io.qameta.allure.Step;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public abstract class BaseApiTest {

    @Value("${api.base.url:https://default-api.com}")
    protected String baseUrl;

    protected static RestAssuredConfig config;
    protected static String cookie;
    protected static String token;

    // Storage for the last request/response (populated by filter)
    protected ThreadLocal<String> lastRequestMethod = new ThreadLocal<>();
    protected ThreadLocal<String> lastRequestUrl = new ThreadLocal<>();
    protected ThreadLocal<String> lastRequestHeaders = new ThreadLocal<>();
    protected ThreadLocal<String> lastRequestBody = new ThreadLocal<>();
    protected ThreadLocal<Integer> lastResponseStatus = new ThreadLocal<>();
    protected ThreadLocal<String> lastResponseHeaders = new ThreadLocal<>();
    protected ThreadLocal<String> lastResponseBody = new ThreadLocal<>();


    @BeforeAll
    @Step("Setup before all tests")
    static void setAllUp() {
        cookie = getCookie();
        token = getToken();
        config = RestAssuredConfig.newConfig()
                .sslConfig(new SSLConfig().relaxedHTTPSValidation().allowAllHostnames());
    }

    @BeforeEach
    @Step("Setup before test")
    void setUp() {
        // Очищаем хранилища
        clearAttachments();
        // Добавляем фильтр для автоматического захвата запросов/ответов
        RestAssured.filters(new AllureCaptureFilter());
    }

    @AfterEach
    @Step("Generating Allure attachments")
    void tearDown() {
        if (lastRequestMethod.get() != null) {
            attachRequest(
                    lastRequestMethod.get(),
                    lastRequestUrl.get(),
                    lastRequestHeaders.get(),
                    lastRequestBody.get()
            );
        }

        if (lastResponseStatus.get() != null) {
            attachResponse(
                    lastResponseStatus.get(),
                    lastResponseBody.get(),
                    lastResponseHeaders.get()
            );
        }

        clearAttachments();
        RestAssured.filters().clear();
    }

    private void clearAttachments() {
        lastRequestMethod.remove();
        lastRequestUrl.remove();
        lastRequestHeaders.remove();
        lastRequestBody.remove();
        lastResponseStatus.remove();
        lastResponseHeaders.remove();
        lastResponseBody.remove();
    }

    @Attachment(value = "Request", type = "text/plain")
    private String attachRequest(String method, String url, String headers, String body) {
        return String.format("🔵 REQUEST:\nMethod: %s\nURL: %s\nHeaders:\n%s\nBody:\n%s",
                method, url, headers, body);
    }

    @Attachment(value = "Response", type = "text/plain")
    private String attachResponse(int statusCode, String body, String headers) {
        return String.format("🟢 RESPONSE:\nStatus: %d\nHeaders:\n%s\nBody:\n%s",
                statusCode, headers, body);
    }

    // INTERNAL FILTER - automatically intercepts all requests
    private class AllureCaptureFilter implements Filter {
        @Override
        public Response filter(FilterableRequestSpecification requestSpec,
                               FilterableResponseSpecification responseSpec,
                               FilterContext ctx) {

            lastRequestMethod.set(requestSpec.getMethod());
            lastRequestUrl.set(requestSpec.getURI());
            lastRequestHeaders.set(requestSpec.getHeaders() != null ?
                    requestSpec.getHeaders().toString() : "[no headers]");

            Object requestBody = requestSpec.getBody();
            if (requestBody != null) {
                lastRequestBody.set(requestBody.toString());
            } else {
                lastRequestBody.set("[no body]");
            }

            Response response = ctx.next(requestSpec, responseSpec);

            try {
                if (response.getBody() != null && response.getBody().asString() != null) {
                    String body = response.getBody().asPrettyString();
                    lastResponseBody.set(body.isEmpty() ? "[empty body]" : body);
                } else {
                    lastResponseBody.set("[no body]");
                }
            } catch (Exception e) {
                lastResponseBody.set("[error reading body: " + e.getMessage() + "]");
            }

            lastResponseStatus.set(response.getStatusCode());
            lastResponseHeaders.set(response.getHeaders() != null ?
                    response.getHeaders().toString() : "[no headers]");

            return response;
        }
    }


    private static String getCookie() {
        // TODO: your implementation
        return "";
    }

    private static String getToken() {
        // TODO: your implementation
        return "";
    }
}
