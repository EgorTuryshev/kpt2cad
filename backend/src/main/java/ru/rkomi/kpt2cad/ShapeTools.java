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

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

import ru.rkomi.kpt2cad.xslt.GeometryFeature;

public class ShapeTools {

    public static void saveConvertedGeometriesAsShapefile(List<GeometryFeature> geometryFeatures, String shapefilePath) throws IOException {
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName("Zem_Uch");
        typeBuilder.setCRS(DefaultGeographicCRS.WGS84);

        typeBuilder.add("the_geom", MultiPolygon.class);

        // Устанавливаем длину 254 символа для строковых атрибутов
        typeBuilder.length(254).add("src_file", String.class);
        typeBuilder.length(254).add("DateUpload", String.class);
        typeBuilder.length(254).add("cad_num", String.class);
        typeBuilder.length(254).add("cad_qrtr", String.class);
        typeBuilder.length(254).add("area", String.class);
        typeBuilder.length(254).add("sk_id", String.class);
        typeBuilder.length(254).add("category", String.class);
        typeBuilder.length(254).add("permit_us", String.class);
        typeBuilder.length(254).add("address", String.class);

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
            newDataStore.setCharset(Charset.forName("CP1251"));

            String typeName = newDataStore.getTypeNames()[0];
            SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);

            if (featureSource instanceof SimpleFeatureStore) {
                SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;

                SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);
                DefaultFeatureCollection collection = new DefaultFeatureCollection();

                for (GeometryFeature geometryFeature : geometryFeatures) {
                    featureBuilder.add(geometryFeature.getGeometry());  // the_geom

                    // Обрезаем значения строковых атрибутов до 254 символов
                    featureBuilder.add(truncateString(geometryFeature.getSrcFile(), 254));      // src_file
                    featureBuilder.add(truncateString(geometryFeature.getDateUpload(), 254));   // DateUpload
                    featureBuilder.add(truncateString(geometryFeature.getCadNum(), 254));       // cad_num
                    featureBuilder.add(truncateString(geometryFeature.getCadQrtr(), 254));      // cad_qrtr
                    featureBuilder.add(truncateString(geometryFeature.getArea(), 254));         // area
                    featureBuilder.add(truncateString(geometryFeature.getSkId(), 254));         // sk_id
                    featureBuilder.add(truncateString(geometryFeature.getCategory(), 254));     // category
                    featureBuilder.add(truncateString(geometryFeature.getPermitUse(), 254));    // permit_us
                    featureBuilder.add(truncateString(geometryFeature.getAddress(), 254));      // address

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

    private static String truncateString(String value, int maxLength) {
        if (value != null && value.length() > maxLength) {
            return value.substring(0, maxLength);
        }
        return value;
    }
}
