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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "https://https://xn--80ahlaoar.xn--p1ai/")
public class FileController {

    private static final String BASE_DIRECTORY = Paths.get("").toAbsolutePath().toString();
    // Максимальное количество папок сессий
    private static final int MAX_SESSIONS = 10;
    // Путь к директории с XSL файлами
    private static final String XSL_DIRECTORY = BASE_DIRECTORY + "/xsl";
    // Путь к директории с PRJ файлами
    private static final String PRJ_DIRECTORY = BASE_DIRECTORY + "/prj";
    // Путь к директории с CPG файлами
    private static final String CPG_DIRECTORY = BASE_DIRECTORY + "/cpg";
    // Путь к директории для сохранения выходных файлов
    private static final String OUTPUT_DIRECTORY = BASE_DIRECTORY + "/output";

    @Autowired
    private Proj4TransformationEngine transformationEngine;

    @Autowired
    private CoordinateSystemDictionary coordinateSystemDictionary;

    private void cleanupOldSessions() throws IOException {
        Path outputDir = Paths.get(OUTPUT_DIRECTORY);

        // Получаем список всех папок в директории OUTPUT_DIRECTORY, отсортированных по времени последней модификации
        List<Path> sessionFolders = Files.list(outputDir)
                .filter(Files::isDirectory)
                .sorted(Comparator.comparingLong(p -> p.toFile().lastModified()))
                .collect(Collectors.toList());

        // Если количество папок превышает лимит, удаляем самые старые
        while (sessionFolders.size() > MAX_SESSIONS) {
            Path oldestSession = sessionFolders.get(0);
            deleteDirectoryRecursively(oldestSession);
            sessionFolders.remove(0);
        }
    }

    private void deleteDirectoryRecursively(Path path) throws IOException {
        Files.walk(path)
                .sorted(Comparator.reverseOrder())  // сначала удаляем файлы, потом директорию
                .map(Path::toFile)
                .forEach(File::delete);
    }

    private List<Path> extractXmlFromZip(MultipartFile zipFile, Path outputDir) throws IOException {
        List<Path> extractedXmlFiles = new ArrayList<>();

        // Создаем временную директорию для извлечения файлов
        Path tempDir = Files.createTempDirectory(outputDir, "zip_extract_");

        try (InputStream is = new ByteArrayInputStream(zipFile.getBytes());
             ZipInputStream zis = new ZipInputStream(is)) {

            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {
                String fileName = Paths.get(zipEntry.getName()).getFileName().toString();
                if (!zipEntry.isDirectory() && fileName.toLowerCase().endsWith(".xml")) {
                    Path extractedFile = tempDir.resolve(fileName);
                    Files.createDirectories(extractedFile.getParent());

                    try (OutputStream os = Files.newOutputStream(extractedFile)) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            os.write(buffer, 0, len);
                        }
                    }
                    extractedXmlFiles.add(extractedFile);

                    Path sessionFile = outputDir.resolve(fileName);
                    Files.move(extractedFile, sessionFile, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        } finally {
            // Удаляем временную директорию
            Files.walk(tempDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }

        return extractedXmlFiles;
    }

    private void zipFolders(List<Path> folders, String zipFilePath) throws IOException {
        Set<String> allowedExtensions = new HashSet<>(Arrays.asList(".shp", ".dbf", ".prj", ".shx", ".cpg"));

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

            // Определяем тип файла
            String originalFilename = file.getOriginalFilename();
            if (originalFilename != null && originalFilename.toLowerCase().endsWith(".zip")) {
                List<Path> extractedXmlFiles = extractXmlFromZip(file, sessionDir);
                if (extractedXmlFiles.isEmpty()) {
                    return ResponseEntity.badRequest().body("ZIP файл не содержит XML-файлов.");
                }

                return ResponseEntity.ok().body("XML файлы извлечены из ZIP архива: " + extractedXmlFiles.size());

            } else if (originalFilename != null && originalFilename.toLowerCase().endsWith(".xml")) {
                Path savedFile = sessionDir.resolve(originalFilename);
                Files.write(savedFile, file.getBytes());
                return ResponseEntity.ok().body("XML файл загружен успешно.");
            } else {
                return ResponseEntity.badRequest().body("Поддерживаются только XML и ZIP файлы.");
            }
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Ошибка при обработке файла.");
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
            // Получаем список загруженных XML-файлов (включая извлечённые из ZIP)
            List<Path> uploadedFiles = Files.list(sessionDir)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().toLowerCase().endsWith(".xml"))
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

                // Копирование статического .cpg файла
                Path staticCpgPath = Paths.get(CPG_DIRECTORY, "ansi1251.cpg");
                Path destinationCpgPath = fileOutputDir.resolve(baseFileName + ".cpg");
                Files.copy(staticCpgPath, destinationCpgPath, StandardCopyOption.REPLACE_EXISTING);

                // Добавляем папку в список для архивации
                foldersToZip.add(fileOutputDir);
            }

            // Создаем общий ZIP-архив
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy_HH-mm", new Locale("ru"));
            String xslFileNameWithoutExtension = xslFile.replaceAll("[^a-zA-Z0-9_\\-]", "_").replace(".xsl", "");
            String zipFileName = xslFileNameWithoutExtension + "_" + now.format(formatter) + ".zip";
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

            cleanupOldSessions();

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