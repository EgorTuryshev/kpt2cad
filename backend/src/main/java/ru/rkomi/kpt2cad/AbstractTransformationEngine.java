package ru.rkomi.kpt2cad;

import java.util.Map;

public abstract class AbstractTransformationEngine<T> implements TransformationEngine {
	private Map<KnownTransformation, T> transformations;

	public void setTransformations(Map<KnownTransformation, T> transformations) {
		if (transformations == null) {
			throw new IllegalArgumentException("transformations parameter can not be a null");
		}

		this.transformations = transformations;
	}

	protected T getTransformation(KnownTransformation knownTransformation) {
		if (this.transformations == null) {
			throw new RuntimeException("Transformations not found. Call 'setTransformations' method first");
		}

		T transform = transformations.get(knownTransformation);
		if (transform == null) {
			throw new RuntimeException("Transformation not set for " + knownTransformation.name());
		}

		return transform;
	}
}
