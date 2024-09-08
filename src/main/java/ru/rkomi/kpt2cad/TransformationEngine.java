package ru.rkomi.kpt2cad;

import org.locationtech.jts.geom.Geometry;

public interface TransformationEngine {
	Geometry transform(KnownTransformation transformation, Geometry geometryToTransform);
}
