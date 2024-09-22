package ru.rkomi.kpt2cad.controllers;

import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.data.DefaultTransaction;
import org.geotools.feature.DefaultFeatureCollection;
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

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.api.data.Transaction;
import org.geotools.feature.simple.*;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class FileController {

    private static final String XSL_DIRECTORY = "G:\\Desktop\\JXSLT\\KPT2CAD\\kpt2cad\\xsl";
    private static final String OUTPUT_DIRECTORY = "G:\\Desktop\\JXSLT\\KPT2CAD\\kpt2cad\\output";

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

            String outputFileName = "converted_result.shp";
            String outputFilePath = Paths.get(OUTPUT_DIRECTORY, outputFileName).toString();
            saveConvertedGeometriesAsShapefile(geometryFeatures, outputFilePath);

            // Создаем zip с Shapefile
            String zipFileName = "converted_result.zip";
            String zipFilePath = Paths.get(OUTPUT_DIRECTORY, zipFileName).toString();
            zipShapefile(outputFilePath, zipFilePath);

            InputStreamResource resource = new InputStreamResource(new FileInputStream(zipFilePath));

            // Удаляем временные файлы
            for (Path savedFile : savedFiles) {
                Files.deleteIfExists(savedFile);
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + zipFileName)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);

        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null); // Return code 500 in case of an error
        }
    }

    private void saveConvertedGeometriesAsShapefile(List<GeometryFeature> geometryFeatures, String shapefilePath) throws IOException {
        // Определяем схему shapefile
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName("GeometryFeature");
        typeBuilder.setCRS(DefaultGeographicCRS.WGS84); // Устанавливаем систему координат

        // Добавляем атрибуты
        typeBuilder.add("the_geom", MultiPolygon.class);
        typeBuilder.add("cadQuarter", String.class);
        // Добавьте другие атрибуты из GeometryFeature при необходимости

        final SimpleFeatureType TYPE = typeBuilder.buildFeatureType();

        // Создаем хранилище данных shapefile
        File newFile = new File(shapefilePath);
        ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();

        Map<String, Serializable> params = new HashMap<>();
        params.put("url", newFile.toURI().toURL());
        params.put("create spatial index", Boolean.TRUE);

        ShapefileDataStore newDataStore = null;
        Transaction transaction = new DefaultTransaction("create");

        try {
            newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
            newDataStore.createSchema(TYPE);
            newDataStore.setCharset(StandardCharsets.UTF_8); // Устанавливаем кодировку при необходимости

            String typeName = newDataStore.getTypeNames()[0];
            SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);

            if (featureSource instanceof SimpleFeatureStore) {
                SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;

                SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);
                DefaultFeatureCollection collection = new DefaultFeatureCollection();

                for (int i = 0; i < geometryFeatures.size(); i++) {
                    GeometryFeature geometryFeature = geometryFeatures.get(i);
                    featureBuilder.add(geometryFeature.getGeometry());
                    featureBuilder.add(geometryFeature.getCadQrtr());
                    // Добавьте другие атрибуты при необходимости

                    SimpleFeature feature = featureBuilder.buildFeature(null);
                    collection.add(feature);
                }

                featureStore.setTransaction(transaction);
                featureStore.addFeatures(collection);
                transaction.commit();
            } else {
                System.out.println("Не удалось записать в shapefile");
            }
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new IOException("Ошибка записи shapefile", e);
        } finally {
            if (transaction != null) {
                transaction.close(); // Закрываем транзакцию вручную
            }
            if (newDataStore != null) {
                newDataStore.dispose(); // Закрываем ShapefileDataStore вручную
            }
        }
    }

    private void zipShapefile(String shapefilePath, String zipFilePath) throws IOException {
        File shapefile = new File(shapefilePath);
        String baseName = shapefile.getName().substring(0, shapefile.getName().lastIndexOf('.'));
        File dir = shapefile.getParentFile();

        try (FileOutputStream fos = new FileOutputStream(zipFilePath);
             java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(fos)) {

            for (File file : dir.listFiles()) {
                if (file.getName().startsWith(baseName + ".")) {
                    try (FileInputStream fis = new FileInputStream(file)) {
                        java.util.zip.ZipEntry zipEntry = new java.util.zip.ZipEntry(file.getName());
                        zos.putNextEntry(zipEntry);

                        byte[] bytes = new byte[1024];
                        int length;
                        while ((length = fis.read(bytes)) >= 0) {
                            zos.write(bytes, 0, length);
                        }

                        zos.closeEntry();
                    }
                }
            }
        }
    }
}
