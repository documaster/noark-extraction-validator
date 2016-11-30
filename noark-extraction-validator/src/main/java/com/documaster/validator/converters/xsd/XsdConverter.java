/**
 * Noark Extraction Validator
 * Copyright (C) 2016, Documaster AS
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.documaster.validator.converters.xsd;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import com.documaster.validator.converters.Converter;
import com.documaster.validator.exceptions.ConversionException;
import com.documaster.validator.logging.StreamRedirection;
import com.documaster.validator.storage.model.Field;
import com.documaster.validator.storage.model.Item;
import com.documaster.validator.storage.model.ItemDef;
import com.sun.codemodel.JCodeModel;
import com.sun.tools.xjc.api.S2JJAXBModel;
import com.sun.tools.xjc.api.SchemaCompiler;
import com.sun.tools.xjc.api.XJC;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

/**
 * Parses XSD Schemas and stores them into {@link ItemDef} definitions.
 * <p/>
 * Accepts a list of schema files to be converted.
 * <p/>
 * The implementation is such that fields of extending classes will simply be
 * added to the root {@link ItemDef} definition.
 */
public class XsdConverter implements Converter {

	private static final Logger LOGGER = LoggerFactory.getLogger(XsdConverter.class);

	private File tempDir;

	private File sourceDir;

	private Set<String> processedClasses;

	private Map<String, ItemDef> itemDefs = new HashMap<>();

	private List<Item> items = new ArrayList<>();

	@Override
	public Map<String, ItemDef> getItemDefs() {

		return itemDefs;
	}

	@Override
	public List<Item> getItems() {

		return items;
	}

	@Override
	public void convert(List<File> files) throws IOException, ClassNotFoundException {

		LOGGER.info("Converting XSD schemas ...");

		processedClasses = new HashSet<>();
		for (File xsdSchema : files) {
			performConversion(xsdSchema);
		}
		processedClasses.clear();

		LOGGER.info("XSD schemas conversion finished.");
	}

	private void performConversion(File xsdSchema) throws IOException, ClassNotFoundException {

		createTemporaryDirectories();

		convertXsdToJavaSource(xsdSchema, sourceDir);
		compileJavaSource();
		createItemDefinitions();

		FileUtils.deleteQuietly(tempDir);
	}

	private void createTemporaryDirectories() throws IOException {

		tempDir = Files.createTempDirectory(null).toFile();
		validateTemporaryDirectory(tempDir);

		sourceDir = new File(tempDir, "source");
		boolean created = sourceDir.mkdirs();
		if (!created) {
			throw new ConversionException("Could not create necessary temporary directories");
		}
	}

	// The deprecation suppression is due to the SchemaCompiler.getOptions() method. The
	// method is safe to use as it will not be discontinued. Furthermore, we need it so that we can
	// resolve class name conflicts in an easy fashion (i.e. without using bindings). See
	// SchemaCompiler#getOptions() for more information.
	@SuppressWarnings("deprecation")
	private void convertXsdToJavaSource(File xsdSchema, File sourceDir) throws IOException {

		SchemaCompiler sc = XJC.createSchemaCompiler();

		sc.setErrorListener(new ConversionErrorListener(XsdConverter.class));

		InputSource is;
		try (FileInputStream fis = new FileInputStream(xsdSchema)) {

			is = new InputSource(fis);
			is.setSystemId(xsdSchema.toURI().toString());

			sc.getOptions().automaticNameConflictResolution = true;
			sc.parseSchema(is);

			S2JJAXBModel model = sc.bind();
			JCodeModel jCodeModel = model.generateCode(null, null);

			StreamRedirection.silenceSystemStreams(StreamRedirection.SystemStream.OUT);
			StreamRedirection.redirectSystemStream(StreamRedirection.SystemStream.ERR, LOGGER,
					StreamRedirection.LogLevel.ERROR);

			jCodeModel.build(sourceDir);

			StreamRedirection
					.resetSystemStreams(StreamRedirection.SystemStream.OUT, StreamRedirection.SystemStream.ERR);
		}
	}

	private void compileJavaSource() {

		List<File> sourceFiles = new ArrayList<>(FileUtils.listFiles(sourceDir, new String[] { "java" }, true));

		List<String> sourceFilePaths = new ArrayList<>();
		for (File sourceFile : sourceFiles) {
			sourceFilePaths.add(sourceFile.getAbsolutePath());
		}

		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		if (compiler == null) {
			throw new ConversionException("A Java Compiler must exist in the environment.");
		}

		int result = compiler.run(null, null, null, sourceFilePaths.toArray(new String[sourceFilePaths.size()]));

		if (result != 0) {
			throw new ConversionException("Conversion of XSD schemas failed due to compilation issues.");
		}
	}

	private void createItemDefinitions() throws IOException, ClassNotFoundException {

		List<File> classFiles = new ArrayList<>(FileUtils.listFiles(sourceDir, new String[] { "class" }, true));

		Map<String, Set<String>> references = new HashMap<>();

		try (URLClassLoader classLoader = URLClassLoader.newInstance(new URL[] { sourceDir.toURI().toURL() })) {
			for (File classFile : classFiles) {
				Class<?> cls = Class.forName(getClassNameFromFile(sourceDir, classFile), false, classLoader);
				if (processedClasses.contains(cls.getName())) {
					continue;
				}
				createItemDefinition(cls, references);
				processedClasses.add(cls.getName());
			}
		}

		// Create references
		for (Map.Entry<String, Set<String>> referenceMap : references.entrySet()) {
			if (!itemDefs.containsKey(referenceMap.getKey())) {
				continue;
			}
			for (String targetReference : referenceMap.getValue()) {
				if (!itemDefs.containsKey(targetReference)) {
					continue;
				}
				ItemDef sourceItemDef = itemDefs.get(referenceMap.getKey());
				ItemDef targetItemDef = itemDefs.get(targetReference);
				sourceItemDef.createReferenceTo(targetItemDef);
			}
		}
	}

	/**
	 * Creates an item definition from the compiled classes.
	 */
	private void createItemDefinition(Class<?> cls, Map<String, Set<String>> references) {

		// Ignore the object factory created by default by XJC
		if (cls.getSimpleName().equalsIgnoreCase("qname")) {
			return;
		}

		if (cls.isEnum()) {
			createItemDefinitionForEnum(cls);
		} else {
			String itemDefFullName = new ItemDef(cls).getFullName();
			if (itemDefs.containsKey(itemDefFullName)) {
				addFieldsOfClassToItemDef(cls, itemDefs.get(itemDefFullName), references);
			} else {
				createItemDefinitionForClass(cls, references);
			}
		}
	}

	private void createItemDefinitionForEnum(Class<?> enumCls) {

		LOGGER.debug("Creating an item definition from enum {}", enumCls.getName());

		ItemDef itemDef = new ItemDef(enumCls);
		itemDef.addField("value", Field.FieldType.getFromJavaType(String.class));

		for (Object constant : itemDef.getBaseClass().getEnumConstants()) {
			try {
				Object value = constant.getClass().getMethod("value").invoke(constant);

				Item item = new Item(itemDef);
				item.add("value", value.toString());

				items.add(item);
			} catch (Exception e) {
				LOGGER.warn("Could not retrieve enum value.", e);
			}
		}

		itemDefs.put(itemDef.getFullName(), itemDef);
	}

	private void createItemDefinitionForClass(Class<?> cls, Map<String, Set<String>> references) {

		LOGGER.debug("Creating an item definition from class {}", cls.getName());

		ItemDef itemDef = new ItemDef(cls);
		addFieldsOfClassToItemDef(cls, itemDef, references);

		itemDefs.put(itemDef.getFullName(), itemDef);
	}

	private void addFieldsOfClassToItemDef(Class<?> cls, ItemDef itemDef, Map<String, Set<String>> references) {

		for (java.lang.reflect.Field field : cls.getDeclaredFields()) {

			Class<?> fieldCls = field.getType();
			if (Field.FieldType.getFromJavaType(fieldCls) != null) {

				// Basic Java type
				itemDef.addField(field.getName(), Field.FieldType.getFromJavaType(fieldCls));

			} else if (Collection.class.isAssignableFrom(field.getType())) {

				// Collection
				ParameterizedType listType = (ParameterizedType) field.getGenericType();
				fieldCls = (Class<?>) listType.getActualTypeArguments()[0];

				if (Field.FieldType.getFromJavaType(fieldCls) != null) {
					itemDef.addField(field.getName(), Field.FieldType.getFromJavaType(String.class));
				} else {
					createReference(new ItemDef(fieldCls), itemDef, references);
				}
			} else if (fieldCls.isEnum()) {
				// Enum
				itemDef.addField(field.getName(), Field.FieldType.getFromJavaType(String.class));
			} else {
				// Another Java type for which a reference must be created
				createReference(new ItemDef(fieldCls), itemDef, references);
			}
		}
	}

	private void createReference(ItemDef referenceItemDef, ItemDef srcItemDef, Map<String, Set<String>> references) {

		if (!references.containsKey(referenceItemDef.getFullName())) {
			references.put(referenceItemDef.getFullName(), new HashSet<String>());
		}
		references.get(referenceItemDef.getFullName()).add(srcItemDef.getFullName());
	}

	private void validateTemporaryDirectory(File dir) {

		Path dirPath = dir.toPath();
		if (!dir.isDirectory() || !Files.isWritable(dirPath) || !Files.isReadable(dirPath)) {
			throw new ConversionException(
					MessageFormat.format(
							"The specified directory ({0}) does not exist or is not readable/writeable.",
							dir.getAbsolutePath()));
		}
	}

	private String getClassNameFromFile(File sourceDir, File classFile) {

		Path classFilePath = classFile.toPath();

		Path sourcePath = sourceDir.toPath();

		String relativePath = sourcePath.relativize(classFilePath).toString();

		return relativePath.substring(0, relativePath.lastIndexOf('.')).replace(File.separator, ".");
	}
}
