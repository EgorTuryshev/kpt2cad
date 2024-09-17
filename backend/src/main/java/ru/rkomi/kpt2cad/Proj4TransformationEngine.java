package ru.rkomi.kpt2cad;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.util.GeometryTransformer;
import org.locationtech.proj4j.CoordinateTransform;

import java.util.HashMap;
import java.util.Map;

public class Proj4TransformationEngine extends AbstractTransformationEngine<CoordinateTransform> {

	@Override
	public Geometry transform(KnownTransformation transformation, Geometry geometryToTransform) {
		GeometryTransformer geometryTransformer = getGeometryTransformer(transformation);
		return geometryTransformer.transform(geometryToTransform);
	}

	private Map<KnownTransformation, GeometryTransformer> transformers = new HashMap<>();
	
	private GeometryTransformer getGeometryTransformer(KnownTransformation knownTransformation) {
		GeometryTransformer transformer = transformers.get(knownTransformation);
		if (transformer != null) {
			return transformer;
		}
		
		CoordinateTransform coordinateTransform = this.getTransformation(knownTransformation);

		transformer = new Proj4GeometryTransformer(coordinateTransform);
		transformers.put(knownTransformation, transformer);

		return transformer;
	}

}
