package ru.rkomi.kpt2cad.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.CoordinateTransform;
import org.locationtech.proj4j.CoordinateTransformFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

import ru.rkomi.kpt2cad.KnownTransformation;
import ru.rkomi.kpt2cad.Proj4TransformationEngine;

@Configuration
public class Proj4Config {

	@Bean
	Proj4TransformationEngine proj4TransformationEngine(
			Map<KnownTransformation, CoordinateTransform> knownTransformations) {
		Proj4TransformationEngine engine = new Proj4TransformationEngine();
		engine.setTransformations(knownTransformations);

		return engine;
	}

	@Bean
	Map<KnownTransformation, CoordinateTransform> knownTransformations(
			Map<String, CoordinateReferenceSystem> knownCRS) {

		Map<KnownTransformation, CoordinateTransform> map = new EnumMap<>(KnownTransformation.class);
		CoordinateTransformFactory factory = new CoordinateTransformFactory();

		CoordinateReferenceSystem wgs84 = knownCRS.get("PROJ_WGS84");
		CoordinateReferenceSystem p42 = knownCRS.get("PROJ_P42");
		CoordinateReferenceSystem msk11q4 = knownCRS.get("PROJ_MSK11_Q4");
		CoordinateReferenceSystem msk11q5 = knownCRS.get("PROJ_MSK11_Q5");
		CoordinateReferenceSystem msk11q6 = knownCRS.get("PROJ_MSK11_Q6");

		map.put(KnownTransformation.WGS84_P42, factory.createTransform(wgs84, p42));
		map.put(KnownTransformation.P42_WGS84, factory.createTransform(p42, wgs84));
		map.put(KnownTransformation.MSK11_Q4_WGS84, factory.createTransform(msk11q4, wgs84));
		map.put(KnownTransformation.MSK11_Q5_WGS84, factory.createTransform(msk11q5, wgs84));
		map.put(KnownTransformation.MSK11_Q6_WGS84, factory.createTransform(msk11q6, wgs84));

		return Collections.unmodifiableMap(map);

	}

	@Bean(name = "PROJ_WGS84")
	CoordinateReferenceSystem wgs84(CRSFactory factory) {
		return factory.createFromParameters("WGS84", readProj(wgs84_resource));
	}

	@Bean(name = "PROJ_P42")
	CoordinateReferenceSystem p42(CRSFactory factory) {
		return factory.createFromParameters("P42", readProj(p42_resource));
	}

	@Bean(name = "PROJ_MSK11_Q4")
	CoordinateReferenceSystem msk11q4(CRSFactory factory) {
		return factory.createFromParameters("MSK11_Q4", readProj(msk11q4_resource));
	}

	@Bean(name = "PROJ_MSK11_Q5")
	CoordinateReferenceSystem msk11q5(CRSFactory factory) {
		return factory.createFromParameters("MSK11_Q5", readProj(msk11q5_resource));
	}

	@Bean(name = "PROJ_MSK11_Q6")
	CoordinateReferenceSystem msk11q6(CRSFactory factory) {
		return factory.createFromParameters("MSK11_Q6", readProj(msk11q6_resource));
	}

	@Bean
	CRSFactory crsFactory() {
		return new CRSFactory();
	}

	@Value("classpath:proj/wgs84.prj")
	private Resource wgs84_resource;

	@Value("classpath:proj/p42.prj")
	private Resource p42_resource;

	@Value("classpath:proj/msk11_q4.prj")
	private Resource msk11q4_resource;

	@Value("classpath:proj/msk11_q5.prj")
	private Resource msk11q5_resource;

	@Value("classpath:proj/msk11_q6.prj")
	private Resource msk11q6_resource;

	private String readProj(Resource resource) {
		try (InputStream stream = resource.getInputStream()) {
			return StreamUtils.copyToString(stream, Charset.defaultCharset());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
