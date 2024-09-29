package ru.rkomi.kpt2cad.controllers;

import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Autowired;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.MultiPolygon;

import ru.rkomi.kpt2cad.KnownTransformation;
import ru.rkomi.kpt2cad.xslt.XslTransformer;
import ru.rkomi.kpt2cad.xslt.GeometryFeature;
import ru.rkomi.kpt2cad.xslt.GeometryParser;
import ru.rkomi.kpt2cad.xslt.CoordinateSystemDictionary;
import ru.rkomi.kpt2cad.Proj4TransformationEngine;
import ru.rkomi.kpt2cad.ShapeTools;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class FileController {

    private static final String XSL_DIRECTORY = "G:\\Desktop\\JXSLT\\KPT2CAD\\kpt2cad\\xsl"; // Путь к директории с XSL файлами
    private static final String OUTPUT_DIRECTORY = "G:\\Desktop\\JXSLT\\KPT2CAD\\kpt2cad\\output"; // Путь к директории для сохранения выходных файлов

    @Autowired
    private Proj4TransformationEngine transformationEngine;

    @Autowired
    private CoordinateSystemDictionary coordinateSystemDictionary;

    @GetMapping("/xsl-files")
    public ResponseEntity<List<String>> getXslFiles() {
        List<String> xslFiles = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(XSL_DIRECTORY), "*.xsl")) {
            for (Path entry : stream) {
                xslFiles.add(entry.getFileName().toString());
            }
        } catch (IOException e) {
            return ResponseEntity.status(500).body(new ArrayList<>());
        }

        return ResponseEntity.ok(xslFiles);
    }

    @PostMapping("/upload")
    public ResponseEntity<?> handleFileUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("sessionId") String sessionId) {
        try {
            // Создаем директорию для сессии, если она не существует
            Path sessionDir = Paths.get(OUTPUT_DIRECTORY, sessionId);
            if (!Files.exists(sessionDir)) {
                Files.createDirectories(sessionDir);
            }

            // Сохраняем загруженный файл в директорию сессии
            Path savedFile = sessionDir.resolve(file.getOriginalFilename());
            Files.write(savedFile, file.getBytes());

            return ResponseEntity.ok().build();

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Ошибка при сохранении файла");
        }
    }

    @PostMapping("/convert")
    public ResponseEntity<Resource> handleFileConversion(
            @RequestBody Map<String, String> requestBody) {
        try {
            String sessionId = requestBody.get("sessionId");
            String xslFile = requestBody.get("xslFile");

            Path sessionDir = Paths.get(OUTPUT_DIRECTORY, sessionId);
            if (!Files.exists(sessionDir)) {
                return ResponseEntity.badRequest().body(null);
            }

            Path xslFilePath = Paths.get(XSL_DIRECTORY, xslFile);
            if (!Files.exists(xslFilePath)) {
                return ResponseEntity.badRequest().body(null);
            }

            // Получаем список загруженных файлов
            List<Path> uploadedFiles = Files.list(sessionDir)
                    .filter(Files::isRegularFile)
                    .toList();

            if (uploadedFiles.isEmpty()) {
                return ResponseEntity.badRequest().body(null);
            }

            // Обрабатываем все загруженные файлы
            List<GeometryFeature> allGeometryFeatures = new ArrayList<>();

            for (Path inputFile : uploadedFiles) {
                XslTransformer transformer = new XslTransformer();
                String transformedFileName = "transformed_" + inputFile.getFileName().toString();
                String transformedFilePath = sessionDir.resolve(transformedFileName).toString();
                transformer.transform(inputFile.toString(), xslFilePath.toString(), transformedFilePath, inputFile.getFileName().toString(), 1, 1);

                GeometryParser geometryParser = new GeometryParser();
                List<GeometryFeature> geometryFeatures = geometryParser.parse(transformedFilePath);

                for (GeometryFeature feature : geometryFeatures) {
                    String cadQuarter = feature.getCadQrtr();
                    String cadRegionCode = cadQuarter.substring(0, 5);

                    KnownTransformation knownTransformation = coordinateSystemDictionary.getCoordinateSystem(cadRegionCode);

                    if (knownTransformation != null) {
                        Geometry inputGeometry = feature.getGeometry();
                        System.out.println("Input geometry type: " + inputGeometry.getGeometryType());

                        Geometry transformedGeometry = transformationEngine.transform(knownTransformation, feature.getGeometry());

                        if (transformedGeometry == null || transformedGeometry.isEmpty()) {
                            throw new IllegalArgumentException("Transformed geometry is null or empty");
                        }
                        System.out.println("Transformed geometry type: " + transformedGeometry.getGeometryType());

                        if (transformedGeometry instanceof MultiPolygon) {
                            feature.setGeometry((MultiPolygon) transformedGeometry);
                        } else if (transformedGeometry instanceof Polygon) {
                            GeometryFactory geometryFactory = new GeometryFactory();
                            MultiPolygon multiPolygon = geometryFactory.createMultiPolygon(new Polygon[]{(Polygon) transformedGeometry});
                            feature.setGeometry(multiPolygon);
                        } else {
                            throw new IllegalArgumentException("Transformed geometry is of unexpected type: " + transformedGeometry.getGeometryType());
                        }
                    }
                }

                allGeometryFeatures.addAll(geometryFeatures);
            }

            String outputFileName = "converted_result.shp";
            String outputFilePath = sessionDir.resolve(outputFileName).toString();

            ShapeTools.saveConvertedGeometriesAsShapefile(allGeometryFeatures, outputFilePath);

            String zipFileName = "converted_result.zip";
            String zipFilePath = sessionDir.resolve(zipFileName).toString();
            ShapeTools.zipShapefile(outputFilePath, zipFilePath);

            InputStreamResource resource = new InputStreamResource(new FileInputStream(zipFilePath));

            // Очистка временных файлов после обработки
            Files.walk(sessionDir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + zipFileName)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }
}