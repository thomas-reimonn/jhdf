/*
 * This file is part of jHDF. A pure Java library for accessing HDF5 files.
 *
 * http://jhdf.io
 *
 * Copyright (c) 2024 James Mudd
 *
 * MIT License see 'LICENSE' file
 */
package io.jhdf.h5dump;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.jhdf.HdfFile;
import io.jhdf.TestUtils;
import io.jhdf.WritableHdfFile;
import io.jhdf.api.Attribute;
import io.jhdf.api.Dataset;
import io.jhdf.api.Group;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static io.jhdf.TestUtils.toDoubleArray;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class H5Dump {

	private static final Logger logger = LoggerFactory.getLogger(H5Dump.class);

	private static final XmlMapper XML_MAPPER;
	static {
		XML_MAPPER =  new XmlMapper();
		XML_MAPPER.disable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES);
		XML_MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
	}

	public static HDF5FileXml dumpAndParse(Path path) throws IOException, InterruptedException {
		logger.info("Reading [{}] with h5dump", path.toAbsolutePath());
		ProcessBuilder processBuilder = new ProcessBuilder();
		processBuilder.command("h5dump", "--format=%.10lf", "--xml", path.toAbsolutePath().toString());
		processBuilder.redirectErrorStream(true); // get stderr as well
		logger.info("Starting h5dump process [{}]", processBuilder.command());
		Process process = processBuilder.start();
  		String xmlString = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
		process.waitFor(30, TimeUnit.SECONDS);
		logger.info("h5dump returned [{}] output [{}]", process.exitValue(), xmlString);
		// Validate
		assertThat(process.exitValue(), is(equalTo(0)));
		assertThat(xmlString, is(not(blankOrNullString())));
		// Parse the XML
        return XML_MAPPER.readValue(xmlString, HDF5FileXml.class);
	}

	public static void assetXmlAndHdfFileMatch(HDF5FileXml hdf5FileXml, HdfFile hdfFile) {
		// First validate the root group size
		compareGroups(hdf5FileXml.rootGroup, hdfFile);
	}

	public static void compareGroups(GroupXml groupXml, Group group) {
		// First validate the group size
		assertThat(groupXml.children(), is(equalTo(group.getChildren().size())));
		assertThat(groupXml.getObjectId(), is(equalTo(group.getAddress())));
		for (GroupXml childGroup : groupXml.groups) {
			compareGroups(childGroup, (Group) group.getChild(childGroup.name));
		}
		for (DatasetXml dataset : groupXml.datasets) {
			compareDatasets(dataset, (Dataset) group.getChild(dataset.name));
		}
		for (AttributeXml attribute : groupXml.attributes) {
			compareAttributes(attribute, group.getAttribute(attribute.name));
		}
	}

	private static void compareAttributes(AttributeXml attributeXml, Attribute attribute) {
		logger.info("Comparing attribute [{}] on node [{}]", attribute.getName(), attribute.getNode().getPath());
		assertThat(attributeXml.name, is(equalTo(attribute.getName())));
		assertThat(attributeXml.getDimensions(), is(equalTo(attribute.getDimensions())));
		assertArrayEquals(toDoubleArray(attributeXml.getData()), toDoubleArray(attribute.getData()), 0.002);
	}

	private static void compareDatasets(DatasetXml datasetXml, Dataset dataset) {
		logger.info("Comparing dataset [{}] on node [{}]", dataset.getName(), dataset.getPath());
		assertThat(datasetXml.getObjectId(), is(equalTo(dataset.getAddress())));
		assertThat(datasetXml.getDimensions(), is(equalTo(dataset.getDimensions())));
		assertArrayEquals(toDoubleArray(datasetXml.getData()), toDoubleArray(dataset.getData()), 0.002);
	}


}
