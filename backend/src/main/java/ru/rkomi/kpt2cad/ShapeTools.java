package ru.rkomi.kpt2cad;

import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.data.DefaultTransaction;
import org.geotools.feature.DefaultFeatureCollection;
import org.locationtech.jts.geom.MultiPolygon;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.api.data.Transaction;
import org.geotools.feature.simple.*;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;

import ru.rkomi.kpt2cad.xslt.GeometryFeature;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ShapeTools {

    public static void saveConvertedGeometriesAsShapefile(List<GeometryFeature> geometryFeatures, String shapefilePath) throws IOException {
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName("GeometryFeature");
        typeBuilder.setCRS(DefaultGeographicCRS.WGS84);

        typeBuilder.add("the_geom", MultiPolygon.class);
        typeBuilder.add("cadQuarter", String.class);

        final SimpleFeatureType TYPE = typeBuilder.buildFeatureType();

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
            newDataStore.setCharset(StandardCharsets.UTF_8);

            String typeName = newDataStore.getTypeNames()[0];
            SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);

            if (featureSource instanceof SimpleFeatureStore) {
                SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;

                SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);
                DefaultFeatureCollection collection = new DefaultFeatureCollection();

                for (GeometryFeature geometryFeature : geometryFeatures) {
                    featureBuilder.add(geometryFeature.getGeometry());
                    featureBuilder.add(geometryFeature.getCadQrtr());

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
            transaction.rollback();
            throw new IOException("Ошибка записи shapefile", e);
        } finally {
            transaction.close();
            if (newDataStore != null) {
                newDataStore.dispose();
            }
        }
    }

    public static void zipShapefile(String shapefilePath, String zipFilePath) throws IOException {
        File shapefile = new File(shapefilePath);
        String baseName = shapefile.getName().substring(0, shapefile.getName().lastIndexOf('.'));
        File dir = shapefile.getParentFile();

        String zipFileName = new File(zipFilePath).getName();

        String[] extensions = {".shp", ".shx", ".dbf", ".prj", ".sbn", ".sbx", ".cpg"};
        List<String> extensionList = Arrays.asList(extensions);

        try (FileOutputStream fos = new FileOutputStream(zipFilePath);
             java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(fos)) {

            for (File file : Objects.requireNonNull(dir.listFiles())) {
                String fileExtension = file.getName().substring(file.getName().lastIndexOf('.')).toLowerCase();
                if (file.getName().startsWith(baseName + ".") && extensionList.contains(fileExtension) && !file.getName().equals(zipFileName)) {
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
