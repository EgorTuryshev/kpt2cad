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
import java.util.*;

import ru.rkomi.kpt2cad.xslt.GeometryFeature;

import java.nio.charset.StandardCharsets;

public class ShapeTools {

    public static void saveConvertedGeometriesAsShapefile(List<GeometryFeature> geometryFeatures, String shapefilePath) throws IOException {
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName("Zem_Uch");
        typeBuilder.setCRS(DefaultGeographicCRS.WGS84);

        typeBuilder.add("the_geom", MultiPolygon.class);

        typeBuilder.add("src_file", String.class);
        typeBuilder.add("DateUpload", String.class);
        typeBuilder.add("cad_num", String.class);
        typeBuilder.add("cad_qrtr", String.class);
        typeBuilder.add("area", String.class);
        typeBuilder.add("sk_id", String.class);
        typeBuilder.add("category", String.class);
        typeBuilder.add("permit_us", String.class);
        typeBuilder.add("address", String.class);

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
                    featureBuilder.add(geometryFeature.getGeometry());          // the_geom
                    featureBuilder.add(geometryFeature.getSrcFile());           // src_file
                    featureBuilder.add(geometryFeature.getDateUpload());        // DateUpload
                    featureBuilder.add(geometryFeature.getCadNum());            // cad_num
                    featureBuilder.add(geometryFeature.getCadQrtr());           // cad_qrtr
                    featureBuilder.add(geometryFeature.getArea());              // area
                    featureBuilder.add(geometryFeature.getSkId());              // sk_id
                    featureBuilder.add(geometryFeature.getCategory());          // category
                    featureBuilder.add(geometryFeature.getPermitUse());         // permit_us
                    featureBuilder.add(geometryFeature.getAddress());

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
}
