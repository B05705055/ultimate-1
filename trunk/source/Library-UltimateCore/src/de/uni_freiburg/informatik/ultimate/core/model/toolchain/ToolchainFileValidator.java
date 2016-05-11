/*
 * Copyright (C) 2015 Björn Buchhold
 * Copyright (C) 2015 Christian Simon
 * Copyright (C) 2014-2016 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2014-2016 University of Freiburg
 * 
 * This file is part of the Library-UltimateCore.
 * 
 * The Library-UltimateCore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The Library-UltimateCore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the Library-UltimateCore. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the Library-UltimateCore, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the Library-UltimateCore grant you additional permission 
 * to convey the resulting work.
 */

package de.uni_freiburg.informatik.ultimate.core.model.toolchain;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.SAXException;

/**
 *
 * @author Björn Buchhold
 * @author Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 *
 */
public class ToolchainFileValidator {

	private static final String TOOLCHAIN_PACKAGE = "de.uni_freiburg.informatik.ultimate.core.model.toolchain";
	private static final String TOOLCHAIN_URI = "/de/uni_freiburg/informatik/ultimate/core/model/toolchain/toolchain.xsd";

	public ToolchainListType createEmptyToolchain() {
		final ObjectFactory objFac = new ObjectFactory();
		return objFac.createToolchainListType();
	}

	/**
	 * This constructor creates a toolchain from an XML file.
	 * 
	 * @param xmlfile
	 *            an xml file compliant with toolchain.xsd
	 * @throws JAXBException
	 * @throws FileNotFoundException
	 * @throws SAXException
	 * @throws MalformedURLException
	 */
	@SuppressWarnings({ "unchecked" })
	public ToolchainListType loadValidatedToolchain(final String xmlfile)
			throws JAXBException, FileNotFoundException, SAXException {
		final JAXBContext jc = JAXBContext.newInstance(TOOLCHAIN_PACKAGE);
		final Unmarshaller unmarshaller = jc.createUnmarshaller();
		final URL fullPathString = getClass().getResource(TOOLCHAIN_URI);
		unmarshaller.setSchema(SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(fullPathString));

		final JAXBElement<ToolchainListType> doc = (JAXBElement<ToolchainListType>) unmarshaller
				.unmarshal(new FileInputStream(xmlfile));

		return doc.getValue();
	}

	/**
	 * This method marshals a toolchain into an xml file.
	 * 
	 * @param xmlfile
	 * @throws JAXBException
	 * @throws FileNotFoundException
	 */
	public void saveToolchain(String xmlfile, ToolchainListType toolchainInstance)
			throws JAXBException, FileNotFoundException {
		final ObjectFactory mObjectFactory = new ObjectFactory();
		final JAXBContext jc = JAXBContext.newInstance(TOOLCHAIN_PACKAGE);
		final JAXBElement<ToolchainListType> newdoc = mObjectFactory.createToolchain(toolchainInstance);
		final Marshaller marshaller = jc.createMarshaller();
		marshaller.marshal(newdoc, new FileOutputStream(xmlfile));
	}

}
