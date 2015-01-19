package Tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({VariableDeclarationStatementVisitorTest1.class, VariableDeclarationStatementVisitorTest2.class, 
		
		TypeDeclarationVisitorTest1.class, TypeDeclarationVisitorTest2.class, TypeDeclarationVisitorTest3.class, 
		TypeDeclarationVisitorTest4.class, TypeDeclarationVisitorTest5.class, TypeDeclarationVisitorTest6.class,
		
		QualifiedNameVisitorTest1.class, QualifiedNameVisitorTest2.class,
		
		ParameterizedTypeVisitorTest1.class, ParameterizedTypeVisitorTest2.class,

		MethodInvocationVisitorTest1.class, MethodInvocationVisitorTest2.class,
		
		MethodDeclarationVisitorTest1.class, MethodDeclarationVisitorTest2.class, MethodDeclarationVisitorTest3.class,
		MethodDeclarationVisitorTest4.class, MethodDeclarationVisitorTest5.class,

		MarkerAnnotationVisitorTest1.class, MarkerAnnotationVisitorTest2.class, MarkerAnnotationVisitorTest3.class,
		MarkerAnnotationVisitorTest4.class,

		FieldDeclarationVisitorTest1.class,

		FieldAccessVisitorTest1.class,
		
		ClassInstanceCreationVisitorTest1.class, ClassInstanceCreationVisitorTest2.class, 
		ClassInstanceCreationVisitorTest3.class,
		
		ExternalJARTest1.class
})
public class AllTests {

}
