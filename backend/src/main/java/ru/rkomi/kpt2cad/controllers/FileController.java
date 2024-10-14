package ru.rkomi.kpt2cad.controllers;

import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.MultiPolygon;

import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
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
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "https://https://xn--80ahlaoar.xn--p1ai/")
public class FileController {

    private static final String BASE_DIRECTORY = Paths.get("").toAbsolutePath().toString();
    // Путь к директории с XSL файлами
    private static final String XSL_DIRECTORY = BASE_DIRECTORY + "/xsl";
    // Путь к директории с PRJ файлами
    private static final String PRJ_DIRECTORY = BASE_DIRECTORY + "/prj";
    // Путь к директории для сохранения выходных файлов
    private static final String OUTPUT_DIRECTORY = BASE_DIRECTORY + "/output";

    @Autowired
    private Proj4TransformationEngine transformationEngine;

    @Autowired
    private CoordinateSystemDictionary coordinateSystemDictionary;

    private void zipFolders(List<Path> folders, String zipFilePath) throws IOException {
        Set<String> allowedExtensions = new HashSet<>(Arrays.asList(".shp", ".dbf", ".prj", ".shx"));

        try (FileOutputStream fos = new FileOutputStream(zipFilePath);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            zos.setLevel(Deflater.BEST_COMPRESSION);

            for (Path folder : folders) {
                Files.walk(folder)
                        .filter(path -> !Files.isDirectory(path))
                        .forEach(path -> {
                            String fileName = path.getFileName().toString();
                            String extension = fileName.substring(fileName.lastIndexOf('.')).toLowerCase();

                            if (allowedExtensions.contains(extension)) {
                                String zipEntryName = folder.getFileName().resolve(folder.relativize(path)).toString().replace("\\", "/");
                                try {
                                    zos.putNextEntry(new ZipEntry(zipEntryName));
                                    Files.copy(path, zos);
                                    zos.closeEntry();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                try {
                                    Files.deleteIfExists(path);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
            }
        }
    }

    @GetMapping("/xsl-files")
    @CrossOrigin(origins = "https://https://xn--80ahlaoar.xn--p1ai/")
    public ResponseEntity<List<String>> getXslFiles() {
        System.out.println(BASE_DIRECTORY);
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

    @PostMapping("/delete")
    public ResponseEntity<?> handleFileDelete(
            @RequestBody Map<String, String> requestBody) {
        String sessionId = requestBody.get("sessionId");
        String fileName = requestBody.get("fileName");

        Path sessionDir = Paths.get(OUTPUT_DIRECTORY, sessionId);
        if (!Files.exists(sessionDir)) {
            return ResponseEntity.badRequest().body("Сессия не найдена");
        }

        Path filePath = sessionDir.resolve(fileName);
        try {
            Files.deleteIfExists(filePath);
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Ошибка при удалении файла");
        }
    }

    @PostMapping("/upload")
    @CrossOrigin(origins = "https://https://xn--80ahlaoar.xn--p1ai/")
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
    @CrossOrigin(origins = "https://https://xn--80ahlaoar.xn--p1ai/")
    public ResponseEntity<StreamingResponseBody> handleFileConversion(
            @RequestBody Map<String, String> requestBody) {
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

        try {
            // Получаем список загруженных файлов
            List<Path> uploadedFiles = Files.list(sessionDir)
                    .filter(Files::isRegularFile)
                    .toList();

            if (uploadedFiles.isEmpty()) {
                return ResponseEntity.badRequest().body(null);
            }

            // Директория для временных файлов обработки
            Path processingDir = sessionDir.resolve("processing");
            if (!Files.exists(processingDir)) {
                Files.createDirectories(processingDir);
            }

            // Список папок для архивации
            List<Path> foldersToZip = new ArrayList<>();

            // Обрабатываем каждый файл отдельно
            for (Path inputFile : uploadedFiles) {
                String baseFileName = inputFile.getFileName().toString().replaceFirst("[.][^.]+$", "");  // Имя без расширения

                // Создаем отдельную папку для каждого файла
                Path fileOutputDir = processingDir.resolve(baseFileName);
                if (!Files.exists(fileOutputDir)) {
                    Files.createDirectories(fileOutputDir);
                }

                // Пути к выходным файлам
                String transformedFileName = "transformed_" + baseFileName + ".xml";
                String transformedFilePath = fileOutputDir.resolve(transformedFileName).toString();

                String outputFileName = baseFileName + ".shp";
                String outputFilePath = fileOutputDir.resolve(outputFileName).toString();

                // Трансформация XML с помощью XSLT
                XslTransformer transformer = new XslTransformer();
                transformer.transform(inputFile.toString(), xslFilePath.toString(), transformedFilePath, inputFile.getFileName().toString(), 1, 1);

                // Парсинг геометрии
                GeometryParser geometryParser = new GeometryParser();
                List<GeometryFeature> geometryFeatures = geometryParser.parse(transformedFilePath);

                // Обработка геометрии и трансформация координат
                for (GeometryFeature feature : geometryFeatures) {
                    String cadQuarter = feature.getCadQrtr();
                    if (cadQuarter == null || cadQuarter.length() < 5) {
                        System.out.println("cadQuarter is null or too short for feature with cadNum: " + feature.getCadNum());
                        continue;
                    }
                    String cadRegionCode = cadQuarter.substring(0, 5);

                    KnownTransformation knownTransformation = coordinateSystemDictionary.getCoordinateSystem(cadRegionCode);

                    if (knownTransformation != null) {
                        Geometry inputGeometry = feature.getGeometry();
                        Geometry transformedGeometry = transformationEngine.transform(knownTransformation, inputGeometry);

                        if (transformedGeometry == null || transformedGeometry.isEmpty()) {
                            throw new IllegalArgumentException("Transformed geometry is null or empty");
                        }

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

                // Сохранение в Shapefile
                ShapeTools.saveConvertedGeometriesAsShapefile(geometryFeatures, outputFilePath);

                // Копирование статического .prj файла
                Path staticPrjPath = Paths.get(PRJ_DIRECTORY, "wgs84.prj");
                Path destinationPrjPath = fileOutputDir.resolve(baseFileName + ".prj");
                Files.copy(staticPrjPath, destinationPrjPath, StandardCopyOption.REPLACE_EXISTING);

                // Добавляем папку в список для архивации
                foldersToZip.add(fileOutputDir);
            }

            // Создаем общий ZIP-архив
            String zipFileName = "converted_results.zip";
            String zipFilePath = sessionDir.resolve(zipFileName).toString();

            zipFolders(foldersToZip, zipFilePath);

            // Создаем StreamingResponseBody для отправки файла и удаления после отправки
            StreamingResponseBody responseBody = outputStream -> {
                try (InputStream inputStream = new FileInputStream(zipFilePath)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    outputStream.flush();
                } finally {
                    // После отправки файла можно безопасно удалить его и временные директории
                    // Удаляем zip-файл
                    Files.deleteIfExists(Paths.get(zipFilePath));

                    // Удаляем директорию processing
                    if (Files.exists(processingDir)) {
                        Files.walk(processingDir)
                                .sorted(Comparator.reverseOrder())
                                .map(Path::toFile)
                                .forEach(File::delete);
                        Files.deleteIfExists(processingDir);
                    }
                }
            };

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + zipFileName)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(responseBody);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }
}