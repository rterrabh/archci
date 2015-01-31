package com.archci.tests;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.util.ArrayList;

import org.junit.BeforeClass;
import org.junit.Test;

import com.archci.ast.DCLDeepDependencyVisitor;
import com.archci.dependencies.Dependency;
import com.archci.exception.DCLException;

public class ClassInstanceCreationVisitorTest2 extends DCLDeepDependencyVisitor {
	
private static File file;

@BeforeClass
public static void SetUp(){
	file = new File(testUtil.sourcePath+"classInstanceCreationExamples/C3.java");
	}
	
	public ClassInstanceCreationVisitorTest2() throws DCLException {
		super(file, null, new String[]{testUtil.sourcePath});
	}

	@Test
	public void testMarkerAnnotationVisitor() {
		ArrayList<String> dependencies = new ArrayList<String>();
		
		for(Dependency dep : this.getDependencies())
			dependencies.add(dep.toString());
		
		
		assertThat(dependencies, hasItems("'C3' contains the method 'MethodTest' that creates an "
				+ "object of 'classInstanceCreationExamples.C3'",
				"'C3' contains the method 'MethodTest' that creates an "
				+ "object of 'classInstanceCreationExamples.C2''"));
	}

}
