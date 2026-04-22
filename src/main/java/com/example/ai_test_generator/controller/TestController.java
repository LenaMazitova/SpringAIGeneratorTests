package com.example.ai_test_generator.controller;


import com.example.ai_test_generator.service.TestGeneratorService;
import org.springframework.web.bind.annotation.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/tests")
public class TestController {

    private final TestGeneratorService generatorService;

    public TestController(TestGeneratorService generatorService) {
        this.generatorService = generatorService;
    }

    @PostMapping("/generate")
    public String generateTests() throws Exception {
        String testCode = generatorService.generateTestClass();

        // Генерируем уникальное имя
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"));
        String className = "ApiTest_" + timestamp;

        // Сохраняем в test/java
        String filePath = "src/test/java/com/example/ai_test_generator/" + className + ".java";
        Path path = Paths.get(filePath);

        // Подставляем имя класса в код
        testCode = testCode.replace("public class GeneratedApiTest", "public class " + className);

        // Создаём директорию только если её нет
        if (!Files.exists(path.getParent())) {
            Files.createDirectories(path.getParent());
        }

        // Записываем файл (перезаписываем если существует)
        Files.writeString(path, testCode);

        return "Test class created: " + className + "\nPath: " + filePath;
    }
}
