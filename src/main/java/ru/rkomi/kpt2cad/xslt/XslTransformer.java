package ru.rkomi.kpt2cad.xslt;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;

public class XslTransformer {

	public void transform(String xmlFilePath, String xsltFilePath, String outputFilePath, String sourceFileName, int swapXY, int skipEmptyGeom) {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Source xmlSource = new StreamSource(xmlFilePath);
		Source xsltSource = new StreamSource(xsltFilePath);

		try {
			Transformer transformer = transformerFactory.newTransformer(xsltSource);

			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setParameter("source_file", sourceFileName);
			transformer.setParameter("swap_xy", swapXY);
			transformer.setParameter("skip_empty_geom", skipEmptyGeom);

			StreamResult result = new StreamResult(outputFilePath);
			transformer.transform(xmlSource, result);
			System.out.println("XSLT transformation completed successfully.");
		} catch (TransformerException e) {
			throw new RuntimeException("Error during XSLT transformation", e);
		}
	}
}