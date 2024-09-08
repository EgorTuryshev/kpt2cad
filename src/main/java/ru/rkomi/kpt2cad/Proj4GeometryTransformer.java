package ru.rkomi.kpt2cad;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.util.GeometryTransformer;
import org.locationtech.proj4j.CoordinateTransform;
import org.locationtech.proj4j.ProjCoordinate;

/**
 * Класс предназначен для трансформации классов геометрий JTS посредством Proj4
 */
public class Proj4GeometryTransformer extends GeometryTransformer {

	private final CoordinateTransform transform;

	public Proj4GeometryTransformer(CoordinateTransform transform) {
		if (transform == null) {
			throw new IllegalArgumentException("transform parameter can not be a null");
		}
		this.transform = transform;
	}

	@Override
	protected CoordinateSequence transformCoordinates(CoordinateSequence coords, Geometry parent) {

		if (coords.size() == 0) {
			return null;
		}

		Coordinate[] transformedCoords = new Coordinate[coords.size()];

		for (int i = 0; i < coords.size(); i++) {
			transformedCoords[i] = transformCoordinate(coords, i);
		}

		return parent.getFactory().getCoordinateSequenceFactory().create(transformedCoords);
	}

	private Coordinate transformCoordinate(CoordinateSequence coords, int index) {
		ProjCoordinate targetCoordinate = new ProjCoordinate();

		double x = coords.getX(index);
		double y = coords.getY(index);

		ProjCoordinate sourceCoordinate = new ProjCoordinate(x, y);

		transform.transform(sourceCoordinate, targetCoordinate);

		return new Coordinate(targetCoordinate.x, targetCoordinate.y);
	}

}
