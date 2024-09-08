package ru.rkomi.kpt2cad.controllers;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.core.io.Resource;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;

import ru.rkomi.kpt2cad.KnownTransformation;
import ru.rkomi.kpt2cad.xslt.XslTransformer;
import ru.rkomi.kpt2cad.xslt.GeometryFeature;
import ru.rkomi.kpt2cad.xslt.GeometryParser;
import ru.rkomi.kpt2cad.xslt.CoordinateSystemDictionary;
import ru.rkomi.kpt2cad.Proj4TransformationEngine;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class FileController {

    private static final String XSL_DIRECTORY = "G:\\Desktop\\JXSLT\\KPT2CAD\\kpt2cad\\xsl"; // Путь к директории с XSL файлами
    private static final String OUTPUT_DIRECTORY = "G:\\Desktop\\JXSLT\\KPT2CAD\\kpt2cad\\output"; // Путь к директории для сохранения выходных файлов

    @GetMapping("/xsl-files")
    public ResponseEntity<List<String>> getXslFiles() {
        List<String> xslFiles = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(XSL_DIRECTORY), "*.xsl")) {
            for (Path entry : stream) {
                xslFiles.add(entry.getFileName().toString());
            }
        } catch (IOException e) {
            return ResponseEntity.status(500).body(new ArrayList<>()); // В случае ошибки возвращаем пустой список с кодом 500
        }

        return ResponseEntity.ok(xslFiles);
    }

    @PostMapping("/upload")
    public ResponseEntity<Resource> handleFileUpload(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("xslFile") String xslFile) throws Exception {
        try {
            Path xslFilePath = Paths.get(XSL_DIRECTORY, xslFile);
            if (!Files.exists(xslFilePath)) {
                return ResponseEntity.badRequest().body(null);
            }

            List<Path> savedFiles = new ArrayList<>();
            for (MultipartFile file : files) {
                Path tempFile = Files.createTempFile(file.getOriginalFilename(), ".tmp");
                Files.write(tempFile, file.getBytes());
                savedFiles.add(tempFile);
            }

            XslTransformer transformer = new XslTransformer();
            String transformedFileName = "transformed_result.xml";
            String transformedFilePath = Paths.get(OUTPUT_DIRECTORY, transformedFileName).toString();
            transformer.transform(savedFiles.get(0).toString(), xslFilePath.toString(), transformedFilePath, files[0].getOriginalFilename(), 1, 1);

            GeometryParser geometryParser = new GeometryParser();
            List<GeometryFeature> geometryFeatures = geometryParser.parse(transformedFilePath);

            Proj4TransformationEngine transformationEngine = new Proj4TransformationEngine();
            CoordinateSystemDictionary coordinateSystemDictionary = new CoordinateSystemDictionary();

            for (GeometryFeature feature : geometryFeatures) {
                String cadQuarter = feature.getCadQrtr();
                String cadRegionCode = cadQuarter.substring(0, 5);

                KnownTransformation knownTransformation = coordinateSystemDictionary.getCoordinateSystem(cadRegionCode);

                if (knownTransformation != null) {
                    Geometry transformedGeometry = transformationEngine.transform(knownTransformation, feature.getGeometry());
                    if (transformedGeometry instanceof MultiPolygon) {
                        feature.setGeometry((MultiPolygon) transformedGeometry);
                    } else {
                        throw new IllegalArgumentException("Transformed geometry is not of type MultiPolygon");
                    }

                }
            }

            String outputFileName = "converted_result.xml";
            String outputFilePath = Paths.get(OUTPUT_DIRECTORY, outputFileName).toString();
            saveConvertedGeometries(geometryFeatures, outputFilePath);

            // Создание ресурса для возврата файла
            InputStreamResource resource = new InputStreamResource(new FileInputStream(outputFilePath));

            // Удаление временных файлов
            for (Path savedFile : savedFiles) {
                Files.deleteIfExists(savedFile);
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + outputFileName)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);

        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null); // В случае ошибки возвращаем код 500
        }
    }

    private void saveConvertedGeometries(List<GeometryFeature> geometryFeatures, String outputFilePath) throws IOException {
        // TODO: Логика сохранения

        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputFilePath))) {
            for (GeometryFeature feature : geometryFeatures) {
                writer.write(feature.toString());
                writer.newLine();
            }
        }
    }
}
