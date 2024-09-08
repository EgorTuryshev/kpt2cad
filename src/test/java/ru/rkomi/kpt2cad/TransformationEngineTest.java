package ru.rkomi.kpt2cad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withPrecision;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class TransformationEngineTest {

	@Autowired
	Proj4TransformationEngine proj4TransformationEngine;

	@Autowired
	GeotoolsTransformationEngine geotoolsTransformationEngine;

	GeometryFactory geometryFactory = new GeometryFactory();

	@ParameterizedTest
	@MethodSource("knownPoints")
	void proj4TransformationEngineTest(KnownTransformation transformation, double srcX, double srcY, double destX,
			double destY) {

		testTransform(proj4TransformationEngine, transformation, srcX, srcY, destX, destY);
	}

	@ParameterizedTest
	@MethodSource("knownPoints")
	void geotoolsTransformationEngineTest(KnownTransformation transformation, double srcX, double srcY, double destX,
			double destY) {

		testTransform(geotoolsTransformationEngine, transformation, srcX, srcY, destX, destY);
	}

	private void testTransform(TransformationEngine engine, KnownTransformation transformation, double srcX,
			double srcY, double destX, double destY) {

		Point p = (Point) engine.transform(transformation, geometryFactory.createPoint(new Coordinate(srcX, srcY)));
		assertThat(p.getX()).isEqualTo(destX, withPrecision(6d));
		assertThat(p.getY()).isEqualTo(destY, withPrecision(6d));
	}

	private static Stream<Arguments> knownPoints() {
		return Stream.of(
				// Point #1
				Arguments.of(KnownTransformation.WGS84_P42, p1_wgs84_x, p1_wgs84_y, p1_p42_x, p1_p42_y),
				Arguments.of(KnownTransformation.P42_WGS84, p1_p42_x, p1_p42_y, p1_wgs84_x, p1_wgs84_y),
				// Point #2
				Arguments.of(KnownTransformation.MSK11_Q4_WGS84, 4442499.0447, 629037.2421, 50.83369907, 61.66640579),
				// Point #3
				Arguments.of(KnownTransformation.MSK11_Q5_WGS84, 5289418.39, 848024.02, 53.8016706475, 63.6162921557)
				// TODO: add test for MSK11_Q6_WGS84
		);
	}

	// Контрольные точки СТО Роскартография 3.5-2020 (Приложение Ж)

	static final double p1_wgs84_x = 44.03420944; // 44° 02' 03,154"
	static final double p1_wgs84_y = 56.29180389; // 56° 17' 30,494"

	static final double p1_p42_x = 44.03599139; // 44° 02' 09,569"
	static final double p1_p42_y = 56.29164361; // 56° 17' 29,917"
}
