package com.example.ai_test_generator.service;

import lombok.NonNull;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class TestGeneratorService {

    private final OllamaChatModel chatModel;

    @Value("${openapi.file.path:./openapi.json}")
    private String openApiPath;

    // Конструктор с инжекцией бинов из конфигурации
    public TestGeneratorService(OllamaChatModel chatModel) {
        this.chatModel = chatModel;
        System.out.println("✅ TestGeneratorService is initialized with the model: " + chatModel.getClass());
    }


    private static @NonNull Prompt getAiPrompt(String prompt) {
        SystemMessage systemMessage = new SystemMessage("""
                Your role is a Java code compiler. Your output must be a valid Java file.
                No words, sentences, or explanations.
                """);

        UserMessage userMessage = new UserMessage(prompt);
        return new Prompt(List.of(systemMessage, userMessage));
    }

    public String generateTestClass() throws Exception {
        Path openApiFile = Paths.get(openApiPath);
        if (!Files.exists(openApiFile)) {
            throw new RuntimeException("OpenAPI file not found: " + openApiPath);
        }

        String openApiContent = Files.readString(openApiFile);
        String prompt = buildPrompt(openApiContent);

        Prompt aiPrompt = getAiPrompt(prompt);

        ChatResponse response = chatModel.call(aiPrompt);
        var usage = response.getMetadata().getUsage();
        System.out.println("Prompt tokens: " + usage.getPromptTokens());
        System.out.println("Generated tokens: " + usage.getCompletionTokens());
        System.out.println("Total tokens: " + usage.getTotalTokens());
        return response.getResult().getOutput().getText();
    }

    private String buildPrompt(String openApiContent) {
        String template = """
                package com.example.ai_test_generator;
                
                import com.example.ai_test_generator.BaseApiTest;
                import io.restassured.http.ContentType;
                import io.restassured.response.ValidatableResponse;
                import org.junit.jupiter.api.Test;
                import org.junit.jupiter.api.DisplayName;
                import com.fasterxml.jackson.databind.ObjectMapper;
                import javax.xml.parsers.DocumentBuilderFactory;
                import javax.xml.parsers.DocumentBuilder;
                import org.w3c.dom.Document;
                import org.w3c.dom.Element;
                import javax.xml.transform.TransformerFactory;
                import javax.xml.transform.Transformer;
                import javax.xml.transform.dom.DOMSource;
                import javax.xml.transform.stream.StreamResult;
                import java.io.StringWriter;
                import java.util.HashMap;
                import java.util.ArrayList;
                import java.util.Arrays;
                import java.util.Map;
                import java.util.List;
                
                import static io.restassured.RestAssured.given;
                import static org.hamcrest.MatcherAssert.assertThat;
                import static org.hamcrest.Matchers.*;
                
                public class GeneratedApiTest extends BaseApiTest {
                
                GENERATE TEST METHODS BELOW
                """;

        return String.format("""
                You must generate a complete Java class for automated tests.
                
                CLASS START (copy this template LITERALLY, without changes):
                %s
                
                NOW, BASED ON THIS OPENAPI SPECIFICATION:
                %s
                
                GENERATE TEST METHODS WITH UNIQUE METHOD NAMES FOR EACH ENDPOINT
                WITH DIFFERENT VERIFICATIONS OF STATUS CODES AND RESPONSE BODIES (IF EXPECTED)
                AND PLACE THEM INSIDE THE CLASS.
                USE:
                    1. TO CREATE REQUEST BODY use HashMap, example:
                    HashMap<String, Object> requestBody = new HashMap<>();
                    requestBody.put("field1", "value1");
                    requestBody.put("field2", 123);
                    requestBody.put("field3", true);
                    // Do not use new JSONObject() - only HashMap!
                    
                    USE THESE HEADERS (token and cookie are in the base class):
                    .header("X-SC-AT", token)
                    .header("Cookie", cookie)
                    
                    2. EXAMPLES FOR RESPONSE PARSING AND VERIFICATIONS:
                    Verifications for REST Assured + Hamcrest (flat JSON)
                    get("/something").then().assertThat()
                         .body("event", equalTo("users_update"));

                    For nested JSON
                    get("/something").then().assertThat()
                         .body("events[0].type", equalTo("smthElse"));

                    Classic assertThat with Hamcrest
                    assertThat("Status code", response.statusCode(), is(200));
                    assertThat("ID is not empty", userId, greaterThan(0));
                    assertThat("Name matches", name, equalTo("expected"));
                    assertThat("List is not empty", items, hasSize(greaterThan(0)));
                    
                    3. If response is a file - use byte format.
                
                    4. FOR XML RESPONSES EXAMPLE:
                    String responseBody = response.asString();
                    Document doc = DocumentBuilderFactory.newInstance()
                        .newDocumentBuilder()
                        .parse(new InputSource(new StringReader(responseBody)));
                    String value = doc.getElementsByTagName("tag").item(0).getTextContent();
                
                For each endpoint, generate test methods for positive and negative verifications.
                CONTINUE generation until you describe ALL endpoints. 
                Do NOT use '...' or comments like 'others by analogy'. 
                RETURN THE COMPLETE CLASS, STARTING WITH package AND ENDING WITH THE LAST BRACE }.
                """, template, openApiContent);
    }
}
