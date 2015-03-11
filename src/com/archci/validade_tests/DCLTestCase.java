package com.archci.validade_tests;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;

import org.junit.BeforeClass;

import com.archci.parser.DCLParser;
import com.archci.core.Architecture;
import com.archci.core.DependencyConstraint;
import com.archci.core.DependencyConstraint.ArchitecturalDrift;

/**
 * Base class for jUnit Test
 */

public abstract class DCLTestCase extends TestCase {
	protected Architecture architecture;
	
	protected static File file;
	
	@BeforeClass
	public static void SetUp(){
		file = new File(testUtil.sourcePath+"");
	}

	protected List<ArchitecturalDrift> validateSystem(final String dependencyContraintToBeValidated) throws Exception {
		file = new File(testUtil.sourcePath+"");
		this.architecture = new Architecture(file);

		/*this.architecture
				.getModules()
				.putAll(DCLParser
						.parseModules(new ByteArrayInputStream(
								("module typeDeclarationExamples: typeDeclarationExamples.*\r\n"
								+ "module markerAnnotationExamples: markerAnnotationExamples.*\r\n"
								+ "module classInstanceCreationExamples: classInstanceCreationExamples.*\r\n"
								+ "module fieldDeclarationExamples: fieldDeclarationExamples.*\r\n"
								+ "module methodDeclarationExamples: methodDeclarationExamples.*\r\n"
								+ "module variableDeclarationStatementExamples: variableDeclarationStatementExamples.*\r\n"
								+ "module methodInvocationExamples: methodInvocationExamples.*\r\n"
								+ "module fieldAccessExamples: fieldAccessExamples.*\r\n"
								+ "module qualifiedNameExamples: qualifiedNameExamples.*\r\n"
								+ "module parameterizedTypeExamples: parameterizedTypeExamples.*\r\n"
								+ "module externalJARExamples: externalJARExamples.*")
										.getBytes())));
	*/
		
		this.architecture
		.getModules()
		.putAll(DCLParser
				.parseModules(new ByteArrayInputStream(
						"module MA: com.example.a.*\r\nmodule MB: com.example.b.*\r\nmodule MC: com.example.c.*\r\nmodule MD: com.example.d.*\r\nmodule MEX: com.example.ex.*"
								.getBytes())));

		architecture.getDependencyConstraints().clear();
		architecture.getDependencyConstraints().addAll(
				DCLParser.parseDependencyConstraints(new ByteArrayInputStream(dependencyContraintToBeValidated.getBytes())));

		assertEquals(6, this.architecture.getModules().size());
		assertEquals(1, this.architecture.getDependencyConstraints().size());


		List<ArchitecturalDrift> architecturalDrifts = new LinkedList<DependencyConstraint.ArchitecturalDrift>();

		for (String classUnderValidation : architecture.getProjectClasses()) {
			for (DependencyConstraint dc : architecture.getDependencyConstraints()) {
				Collection<ArchitecturalDrift> result = dc.validate(classUnderValidation, architecture.getModules(),
						architecture.getProjectClasses(), architecture.getDependencies(classUnderValidation), architecture.getITypeBindings());
				if (result != null && !result.isEmpty()) {
					architecturalDrifts.addAll(result);
				}
			}
		}

		return architecturalDrifts;
	}

}
