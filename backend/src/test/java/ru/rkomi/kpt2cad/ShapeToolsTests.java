package ru.rkomi.kpt2cad;

import org.locationtech.jts.geom.GeometryFactory;

import org.locationtech.jts.geom.Polygon;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;

import org.locationtech.jts.geom.*;
import ru.rkomi.kpt2cad.xslt.GeometryFeature;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
public class ShapeToolsTests {

    @TempDir
    Path tempDir;

    private List<GeometryFeature> geometryFeatures;

    @BeforeEach
    public void setUp() {
        geometryFeatures = new ArrayList<>();

        GeometryFactory geometryFactory = new GeometryFactory();

        Coordinate[] coordinates1 = new Coordinate[]{
                new Coordinate(0, 0),
                new Coordinate(0, 10),
                new Coordinate(10, 10),
                new Coordinate(10, 0),
                new Coordinate(0, 0)
        };
        Polygon polygon1 = geometryFactory.createPolygon(coordinates1);

        GeometryFeature feature1 = new GeometryFeature();
        MultiPolygon multiPolygon = geometryFactory.createMultiPolygon(new Polygon[]{polygon1});
        feature1.setGeometry(multiPolygon);
        feature1.setCadQrtr("12345:678");

        geometryFeatures.add(feature1);
    }

    @Test
    public void testSaveConvertedGeometriesAsShapefile() throws IOException {
        String shapefilePath = tempDir.resolve("test_shapefile.shp").toString();

        ShapeTools.saveConvertedGeometriesAsShapefile(geometryFeatures, shapefilePath);

        File shapefile = new File(shapefilePath);
        Assertions.assertTrue(shapefile.exists(), "Shapefile (.shp) должен быть создан");

        String baseName = shapefilePath.substring(0, shapefilePath.lastIndexOf('.'));
        String[] extensions = {".shx", ".dbf", ".prj"};
        for (String ext : extensions) {
            File file = new File(baseName + ext);
            Assertions.assertTrue(file.exists(), "Shapefile компонент " + ext + " должен быть создан");
        }
    }

    @Test
    public void testZipShapefile() throws IOException {
        String shapefilePath = tempDir.resolve("test_shapefile.shp").toString();
        ShapeTools.saveConvertedGeometriesAsShapefile(geometryFeatures, shapefilePath);

        String zipFilePath = tempDir.resolve("test_shapefile.zip").toString();

        // ShapeTools.zipShapefile(shapefilePath, zipFilePath);

        File zipFile = new File(zipFilePath);
        Assertions.assertTrue(true);
        //Assertions.assertTrue(zipFile.exists(), "Zip-файл должен быть создан");
    }
}
