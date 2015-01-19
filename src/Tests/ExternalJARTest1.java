package Tests;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.hasItems;

import com.archci.ast.DCLDeepDependencyVisitor;
import com.archci.dependencies.Dependency;
import com.archci.exception.DCLException;

public class ExternalJARTest1 extends DCLDeepDependencyVisitor {
	
private static File file;
private static String classPath;

@BeforeClass
public static void SetUp(){
	file = new File(testUtil.sourcePath+"externalJARExamples/L1.java");
	classPath = testUtil.sourcePath+"TerraUtil.jar";
	}
	
	public ExternalJARTest1() throws DCLException {
		super(file, new String[]{classPath}, new String[]{testUtil.sourcePath});
	}

	@Test
	public void testMarkerAnnotationVisitor() {
		ArrayList<String> dependencies = new ArrayList<String>();
		
		for(Dependency dep : this.getDependencies())
			dependencies.add(dep.toString());
		
		
		assertThat(dependencies, hasItems("'L1' contains the method 'Method' that statically invokes "
				+ "the method 'dateToString' of an object of 'com.terra.util.Conversor'"));
	}

}
