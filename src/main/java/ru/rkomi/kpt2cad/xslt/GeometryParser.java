package ru.rkomi.kpt2cad.xslt;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.locationtech.jts.geom.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GeometryParser {

	private GeometryFactory geometryFactory;

	public GeometryParser() {
		this.geometryFactory = new GeometryFactory();
	}

	public List<GeometryFeature> parse(String filePath) throws Exception {
		SAXBuilder saxBuilder = new SAXBuilder();
		Document document = saxBuilder.build(new File(filePath));

		Element rootElement = document.getRootElement();
		List<Element> features = rootElement.getChild("ShapeFile").getChild("Features").getChildren("Feature");

		List<GeometryFeature> geometryFeatures = new ArrayList<>();

		for (Element feature : features) {
			Element attributes = feature.getChild("Attributes");

			String srcFile = getAttributeValue(attributes, "src_file");
			String dateUpload = getAttributeValue(attributes, "DateUpload");
			String cadNum = getAttributeValue(attributes, "cad_num");
			String cadQrtr = getAttributeValue(attributes, "cad_qrtr");
			String area = getAttributeValue(attributes, "area");
			String skId = getAttributeValue(attributes, "sk_id");
			String category = getAttributeValue(attributes, "category");
			String permitUse = getAttributeValue(attributes, "permit_use");
			String address = getAttributeValue(attributes, "address");

			Element shell = feature.getChild("Geometry").getChild("Shell");
			List<Element> coordinates = shell.getChildren("Coordinate");

			List<Coordinate> coordinateList = new ArrayList<>();
			for (Element coord : coordinates) {
				double x = Double.parseDouble(coord.getAttributeValue("x"));
				double y = Double.parseDouble(coord.getAttributeValue("y"));
				coordinateList.add(new Coordinate(x, y));
			}

			LinearRing shellRing = geometryFactory.createLinearRing(coordinateList.toArray(new Coordinate[0]));
			Polygon polygon = geometryFactory.createPolygon(shellRing, null);
			MultiPolygon multiPolygon = geometryFactory.createMultiPolygon(new Polygon[]{polygon});

			GeometryFeature geometryFeature = new GeometryFeature(srcFile, dateUpload, cadNum, cadQrtr, area, skId, category, permitUse, address, multiPolygon);
			geometryFeatures.add(geometryFeature);
		}

		return geometryFeatures;
	}

	private String getAttributeValue(Element attributes, String attributeName) {
		for (Element attribute : attributes.getChildren("Attribute")) {
			if (attributeName.equals(attribute.getAttributeValue("name"))) {
				return attribute.getText();
			}
		}
		return null;
	}
}

