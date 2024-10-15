package ru.rkomi.kpt2cad.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "https://https://xn--80ahlaoar.xn--p1ai/")
public class VersionController {

    private static final String BASE_DIRECTORY = Paths.get("").toAbsolutePath().toString();
    private static final String CHANGELOG_PATH = BASE_DIRECTORY + "/CHANGELOG.md";

    @GetMapping("/version")
    public ResponseEntity<Map<String, String>> getVersion() {
        Map<String, String> versionInfo = new HashMap<>();
        versionInfo.put("version", "0.0.3");
        return ResponseEntity.ok(versionInfo);
    }
    @GetMapping("/changelog")
    public ResponseEntity<String> getChangelog() {
        try {
            Path changelogPath = Path.of(CHANGELOG_PATH);
            String changelogContent = Files.readString(changelogPath);
            return ResponseEntity.ok(changelogContent);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Ошибка загрузки changelog");
        }
    }
}
