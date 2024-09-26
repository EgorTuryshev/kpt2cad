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
import java.util.ArrayList;
import java.util.List;

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

            ShapeTools.saveConvertedGeometriesAsShapefile(geometryFeatures, outputFilePath);

            String zipFileName = "converted_result.zip";
            String zipFilePath = Paths.get(OUTPUT_DIRECTORY, zipFileName).toString();
            ShapeTools.zipShapefile(outputFilePath, zipFilePath);

            InputStreamResource resource = new InputStreamResource(new FileInputStream(zipFilePath));

            // Удаление временных файлов
            for (Path savedFile : savedFiles) {
                Files.deleteIfExists(savedFile);
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + zipFileName)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);

        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }
}
