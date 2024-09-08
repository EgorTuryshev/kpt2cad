package ru.rkomi.kpt2cad;

import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.geometry.jts.JTS;
import org.locationtech.jts.geom.Geometry;

public class GeotoolsTransformationEngine extends AbstractTransformationEngine<MathTransform> {

	@Override
	public Geometry transform(KnownTransformation transformation, Geometry geometryToTransform) {
		MathTransform transform = this.getTransformation(transformation);

		try {
			return JTS.transform(geometryToTransform, transform);
		} catch (TransformException e) {
			throw new RuntimeException(e);
		}
	}
}
