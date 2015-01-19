package com.archci.ast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jface.text.Document;

import com.archci.dependencies.AccessFieldDependency;
import com.archci.dependencies.AccessMethodDependency;
import com.archci.dependencies.AnnotateClassDependency;
import com.archci.dependencies.AnnotateFieldDependency;
import com.archci.dependencies.AnnotateFormalParameterDependency;
import com.archci.dependencies.AnnotateMethodDependency;
import com.archci.dependencies.AnnotateVariableDependency;
import com.archci.dependencies.CreateFieldDependency;
import com.archci.dependencies.CreateMethodDependency;
import com.archci.dependencies.DeclareFieldDependency;
import com.archci.dependencies.DeclareLocalVariableDependency;
import com.archci.dependencies.DeclareParameterDependency;
import com.archci.dependencies.DeclareParameterizedTypeDependency;
import com.archci.dependencies.DeclareReturnDependency;
import com.archci.dependencies.Dependency;
import com.archci.dependencies.ExtendDirectDependency;
import com.archci.dependencies.ExtendIndirectDependency;
import com.archci.dependencies.ImplementDirectDependency;
import com.archci.dependencies.ImplementIndirectDependency;
import com.archci.dependencies.ThrowDependency;
import com.archci.exception.DCLException;

public class DCLDeepDependencyVisitor extends ASTVisitor {
	private List<Dependency> dependencies;

	//private CompilationUnit unit;
	private CompilationUnit fullClass;
	private String className;

	public DCLDeepDependencyVisitor(File file, String[] classPath, String[] sourcePath) throws DCLException {
		try{
			
			this.dependencies = new ArrayList<Dependency>();
			this.className = FilenameUtils.removeExtension(file.getName());
			
			String[] encodings = new String[sourcePath.length];
			for(int i=0; i < sourcePath.length; i++){
				encodings[i] = "UTF-8";
			}
			
			String source = FileUtils.readFileToString(file);
		    Document document = new Document(source);
		    
		    ASTParser parser = ASTParser.newParser(AST.JLS4);
		    
		    @SuppressWarnings("unchecked")
			Map<String, String> options = JavaCore.getDefaultOptions();
			options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_8);
			options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM,
					JavaCore.VERSION_1_8);
			options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_8);
		    parser.setCompilerOptions(options);
		    
		    //parser.setCompilerOptions(JavaCore.getOptions());
		    
		    parser.setKind(ASTParser.K_COMPILATION_UNIT);
		    parser.setSource(document.get().toCharArray());
		    parser.setResolveBindings(true);
		    
		    parser.setEnvironment(classPath, sourcePath, encodings, true);
		    parser.setUnitName("Dependency-Tool");
		    parser.setBindingsRecovery(true);
		    
		    fullClass = (CompilationUnit) parser.createAST(null);
		    
		    this.fullClass.accept(this);
		    
		} catch(Exception e){
			throw new DCLException(e,fullClass);
		}
	}
	

	public final List<Dependency> getDependencies() {
		return this.dependencies;
	}

	public final String getClassName() {
		return this.className;
	}

	@Override
	public boolean visit(TypeDeclaration node) {
		if (!node.isLocalTypeDeclaration() && !node.isMemberTypeDeclaration()) { // Para
																					// evitar
																					// fazer
																					// v�rias
																					// vezes
			try {
				List<AbstractTypeDeclaration> types = fullClass.types();				
				TypeDeclaration typeDeclaration  = (TypeDeclaration) types.get(0);
				ITypeBinding typeBind = typeDeclaration.resolveBinding();
				
				List<ITypeBinding> superTypeBind = new ArrayList<ITypeBinding>();
				
				ITypeBinding superclass = typeBind.getSuperclass();
				
				while(superclass!=null){ 
					superTypeBind.add(superclass);
					superclass = superclass.getSuperclass();
				}

				for (ITypeBinding t : superTypeBind) {
					if (node.getSuperclassType() != null
							&& t.getQualifiedName().equals(node.getSuperclassType().resolveBinding().getQualifiedName())) {
						this.dependencies.add(new ExtendDirectDependency(this.className, t.getQualifiedName(), fullClass
								.getLineNumber(node.getSuperclassType().getStartPosition()), node.getSuperclassType().getStartPosition(),
								node.getSuperclassType().getLength()));
					} else {
						this.dependencies.add(new ExtendIndirectDependency(this.className, t.getQualifiedName(), null, null, null));
					}
				}

				//List<ITypeBinding> superInterfaceBind = new ArrayList<ITypeBinding>();
				List<ITypeBinding> interfaceBinds = new ArrayList<ITypeBinding>();
				ITypeBinding[] directInterfaceBinds = typeBind.getInterfaces();
				
				while(directInterfaceBinds.length!=0){
					
					for (ITypeBinding di : directInterfaceBinds){
						interfaceBinds.add(di);
					}
					
					for (ITypeBinding dii : interfaceBinds){
						directInterfaceBinds = dii.getInterfaces();
					}
				}

				
				//ITypeBinding[] interfaceBinds = typeBind.getInterfaces();

				
				externo: for (ITypeBinding t : interfaceBinds) {
					for (Object it : node.superInterfaceTypes()) {
						switch (((Type) it).getNodeType()) {
						case ASTNode.SIMPLE_TYPE:
							SimpleType st = (SimpleType) it;
							if (t.getQualifiedName().equals(st.getName().resolveTypeBinding().getQualifiedName())) {
								if (!typeDeclaration.isInterface()) {
									this.dependencies.add(new ImplementDirectDependency(this.className, t.getQualifiedName(),
											fullClass.getLineNumber(st.getStartPosition()), st.getStartPosition(), st.getLength()));
								} else {
									this.dependencies.add(new ExtendDirectDependency(this.className, t.getQualifiedName(), fullClass
											.getLineNumber(st.getStartPosition()), st.getStartPosition(), st.getLength()));
								}
								continue externo;
							}
							break;
						case ASTNode.PARAMETERIZED_TYPE:
							ParameterizedType pt = (ParameterizedType) it;
							if (t!= null && t.getQualifiedName() != null && pt != null && pt.getType() != null && pt.getType().resolveBinding() != null &&
/*Tirar duvida (no original era BinaryName
 * mas precise mudar para QualifiedName)*/
									//t.getQualifiedName().equals(pt.getType().resolveBinding().getBinaryName())) {
									t.getQualifiedName().equals(pt.getType().resolveBinding().getQualifiedName())) {
								if (!typeDeclaration.isInterface()) {
									this.dependencies.add(new ImplementDirectDependency(this.className, t.getQualifiedName(),
									//this.dependencies.add(new ImplementDirectDependency(this.className, t.getBinaryName(),
											fullClass.getLineNumber(pt.getStartPosition()), pt.getStartPosition(), pt.getLength()));
								} else {
/*Tirar duvida de como entraria*/									
									this.dependencies.add(new ExtendDirectDependency(this.className, t.getBinaryName(), fullClass
											.getLineNumber(pt.getStartPosition()), pt.getStartPosition(), pt.getLength()));
								}
								continue externo;
							}
							break;
						}
					}
					this.dependencies.add(new ImplementIndirectDependency(this.className, t.getQualifiedName(), null, null, null));
				}
			} catch (Exception e) {
				throw new RuntimeException("AST Parser error.", e);
			}
		}
		return true;
	} 

	@Override
	public boolean visit(MarkerAnnotation node) {
		if (node.getParent().getNodeType() == ASTNode.FIELD_DECLARATION) {
			FieldDeclaration field = (FieldDeclaration) node.getParent();
			this.dependencies.add(new AnnotateFieldDependency(this.className, node.getTypeName().resolveTypeBinding().getQualifiedName(),
					fullClass.getLineNumber(node.getStartPosition()), node.getStartPosition(), node.getLength(),
					((VariableDeclarationFragment) field.fragments().get(0)).getName().getIdentifier()));
		} else if (node.getParent().getNodeType() == ASTNode.METHOD_DECLARATION) {
			MethodDeclaration method = (MethodDeclaration) node.getParent();
			this.dependencies.add(new AnnotateMethodDependency(this.className, node.getTypeName().resolveTypeBinding().getQualifiedName(),
					fullClass.getLineNumber(node.getStartPosition()), node.getStartPosition(), node.getLength(), method.getName()
							.getIdentifier()));
		} else if (node.getParent().getNodeType() == ASTNode.TYPE_DECLARATION) {
			this.dependencies.add(new AnnotateClassDependency(this.className, node.getTypeName().resolveTypeBinding().getQualifiedName(),
					fullClass.getLineNumber(node.getStartPosition()), node.getStartPosition(), node.getLength()));
		} else if (node.getParent().getNodeType() == ASTNode.VARIABLE_DECLARATION_STATEMENT) {
/*tirar duvida sobre como testar*/
			VariableDeclarationStatement st = (VariableDeclarationStatement) node.getParent();
			VariableDeclarationFragment vdf = ((VariableDeclarationFragment) st.fragments().get(0));
			ASTNode relevantParent = this.getRelevantParent(node);
			if (relevantParent.getNodeType() == ASTNode.METHOD_DECLARATION) {
				MethodDeclaration md = (MethodDeclaration) relevantParent;
				this.dependencies.add(new AnnotateVariableDependency(this.className, node.getTypeName().resolveTypeBinding()
						.getQualifiedName(), fullClass.getLineNumber(node.getStartPosition()), node.getStartPosition(), node.getLength(),
						md.getName().getIdentifier(), vdf.getName().getIdentifier()));
			}
		} else if (node.getParent().getNodeType() == ASTNode.SINGLE_VARIABLE_DECLARATION) {
			SingleVariableDeclaration sv = (SingleVariableDeclaration) node.getParent();
			ASTNode relevantParent = this.getRelevantParent(node);
			if (relevantParent.getNodeType() == ASTNode.METHOD_DECLARATION) {
				MethodDeclaration md = (MethodDeclaration) relevantParent;
				this.dependencies.add(new AnnotateFormalParameterDependency(this.className, node.getTypeName().resolveTypeBinding()
						.getQualifiedName(), fullClass.getLineNumber(node.getStartPosition()), node.getStartPosition(), node.getLength(),
						md.getName().getIdentifier(), sv.getName().getIdentifier()));
			}

		}
		return true;
	}

	@Override
	/*tirar duvida (cast)*/
	public boolean visit(SingleMemberAnnotation node) {
		if (node.getParent().getNodeType() == ASTNode.FIELD_DECLARATION) {
			FieldDeclaration field = (FieldDeclaration) node.getParent();
			this.dependencies.add(new AnnotateFieldDependency(this.className, node.getTypeName().resolveTypeBinding().getQualifiedName(),
					fullClass.getLineNumber(node.getStartPosition()), node.getStartPosition(), node.getLength(),
					((VariableDeclarationFragment) field.fragments().get(0)).getName().getIdentifier()));
		} else if (node.getParent().getNodeType() == ASTNode.METHOD_DECLARATION) {
			MethodDeclaration method = (MethodDeclaration) node.getParent();
			this.dependencies.add(new AnnotateMethodDependency(this.className, node.getTypeName().resolveTypeBinding().getQualifiedName(),
					fullClass.getLineNumber(node.getStartPosition()), node.getStartPosition(), node.getLength(), method.getName()
							.getIdentifier()));
		} else if (node.getParent().getNodeType() == ASTNode.TYPE_DECLARATION) {
			this.dependencies.add(new AnnotateClassDependency(this.className, node.getTypeName().resolveTypeBinding().getQualifiedName(),
					fullClass.getLineNumber(node.getStartPosition()), node.getStartPosition(), node.getLength()));
		}
		return true;
	}

	@Override
	public boolean visit(ClassInstanceCreation node) {
		ASTNode relevantParent = getRelevantParent(node);

		switch (relevantParent.getNodeType()) {
		case ASTNode.FIELD_DECLARATION:
			FieldDeclaration fd = (FieldDeclaration) relevantParent;
			this.dependencies.add(new CreateFieldDependency(this.className, this.getTargetClassName(node.getType().resolveBinding()),
					fullClass.getLineNumber(node.getStartPosition()), node.getStartPosition(), node.getLength(),
					((VariableDeclarationFragment) fd.fragments().get(0)).getName().getIdentifier()));
			break;
		case ASTNode.METHOD_DECLARATION:
			MethodDeclaration md = (MethodDeclaration) relevantParent;
			this.dependencies.add(new CreateMethodDependency(this.className, this.getTargetClassName(node.getType().resolveBinding()),
					fullClass.getLineNumber(node.getStartPosition()), node.getStartPosition(), node.getLength(), md.getName()
							.getIdentifier()));
			break;
		case ASTNode.INITIALIZER:
			this.dependencies
					.add(new CreateMethodDependency(this.className, this.getTargetClassName(node.getType().resolveBinding()), fullClass
							.getLineNumber(node.getStartPosition()), node.getStartPosition(), node.getLength(), "initializer static block"));
			break;
		}

		return true;
	}

	@Override
	public boolean visit(FieldDeclaration node) {
		this.dependencies.add(new DeclareFieldDependency(this.className, this.getTargetClassName(node.getType().resolveBinding()),
				fullClass.getLineNumber(node.getType().getStartPosition()), node.getType().getStartPosition(), node.getType().getLength(),
				((VariableDeclarationFragment) node.fragments().get(0)).getName().getIdentifier()));
		return true;
	}

	@Override
	public boolean visit(MethodDeclaration node) {
		for (Object o : node.parameters()) {
			if (o instanceof SingleVariableDeclaration) {
				SingleVariableDeclaration svd = (SingleVariableDeclaration) o;
				this.dependencies.add(new DeclareParameterDependency(this.className, this
						.getTargetClassName(svd.getType().resolveBinding()), fullClass.getLineNumber(svd.getStartPosition()), svd
						.getStartPosition(), svd.getLength(), node.getName().getIdentifier(), svd.getName().getIdentifier()));
				if (svd.getType().getNodeType() == Type.PARAMETERIZED_TYPE) {
					// TODO: Adjust the way that we handle parameter types
					for (Object t : ((ParameterizedType) svd.getType()).typeArguments()) {
						if (t instanceof SimpleType) {
							SimpleType st = (SimpleType) t;
							this.dependencies.add(new DeclareParameterDependency(this.className, this.getTargetClassName(st
									.resolveBinding()), fullClass.getLineNumber(st.getStartPosition()), st.getStartPosition(), st
									.getLength(), node.getName().getIdentifier(), svd.getName().getIdentifier()));
						} else if (t instanceof ParameterizedType) {
							ParameterizedType pt = (ParameterizedType) t;
							this.dependencies.add(new DeclareParameterDependency(this.className, this.getTargetClassName(pt.getType()
									.resolveBinding()), fullClass.getLineNumber(pt.getStartPosition()), pt.getStartPosition(), pt
									.getLength(), node.getName().getIdentifier(), svd.getName().getIdentifier()));
						}
					}
				}

			}
		}
		for (Object o : node.thrownExceptions()) {
			Name name = (Name) o;
			this.dependencies.add(new ThrowDependency(this.className, this.getTargetClassName(name.resolveTypeBinding()), fullClass
					.getLineNumber(name.getStartPosition()), name.getStartPosition(), name.getLength(), node.getName().getIdentifier()));
		}

		if (node.getReturnType2() != null
				&& !(node.getReturnType2().isPrimitiveType() && ((PrimitiveType) node.getReturnType2()).getPrimitiveTypeCode() == PrimitiveType.VOID)) {
			if (!node.getReturnType2().resolveBinding().isTypeVariable()) {
				this.dependencies.add(new DeclareReturnDependency(this.className, this.getTargetClassName(node.getReturnType2()
						.resolveBinding()), fullClass.getLineNumber(node.getReturnType2().getStartPosition()), node.getReturnType2()
						.getStartPosition(), node.getReturnType2().getLength(), node.getName().getIdentifier()));
			} else {
/*tirar duvida de quando entraria*/
				if (node.getReturnType2().resolveBinding().getTypeBounds().length >= 1) {
					this.dependencies.add(new DeclareReturnDependency(this.className, this.getTargetClassName(node.getReturnType2()
							.resolveBinding().getTypeBounds()[0]), fullClass.getLineNumber(node.getReturnType2().getStartPosition()), node
							.getReturnType2().getStartPosition(), node.getReturnType2().getLength(), node.getName().getIdentifier()));
				}
			}

		}
		return true;
	}

	@Override
	public boolean visit(VariableDeclarationStatement node) {
		ASTNode relevantParent = getRelevantParent(node);

		switch (relevantParent.getNodeType()) {
		case ASTNode.METHOD_DECLARATION:
			MethodDeclaration md = (MethodDeclaration) relevantParent;

			this.dependencies.add(new DeclareLocalVariableDependency(this.className, this.getTargetClassName(node.getType()
					.resolveBinding()), fullClass.getLineNumber(node.getStartPosition()), node.getType().getStartPosition(), node.getType()
					.getLength(), md.getName().getIdentifier(), ((VariableDeclarationFragment) node.fragments().get(0)).getName()
					.getIdentifier()));

			break;
		case ASTNode.INITIALIZER:
			this.dependencies.add(new DeclareLocalVariableDependency(this.className, this.getTargetClassName(node.getType()
					.resolveBinding()), fullClass.getLineNumber(node.getStartPosition()), node.getType().getStartPosition(), node.getType()
					.getLength(), "initializer static block", ((VariableDeclarationFragment) node.fragments().get(0)).getName()
					.getIdentifier()));
			break;
		}

		return true;
	}

	@Override
	public boolean visit(MethodInvocation node) {
		ASTNode relevantParent = getRelevantParent(node);

		//IMethodBinding test = node.resolveMethodBinding();
		int isStatic = node.resolveMethodBinding().getModifiers() & Modifier.STATIC;

		switch (relevantParent.getNodeType()) {
		case ASTNode.METHOD_DECLARATION:
			MethodDeclaration md = (MethodDeclaration) relevantParent;
			if (node.getExpression() != null) {
				this.dependencies.add(new AccessMethodDependency(this.className, this.getTargetClassName(node.getExpression()
						.resolveTypeBinding()), fullClass.getLineNumber(node.getStartPosition()), node.getStartPosition(),
						node.getLength(), md.getName().getIdentifier(), node.getName().getIdentifier(), isStatic != 0));
			}
			break;
		case ASTNode.INITIALIZER:
			if (node.getExpression() != null) {
				this.dependencies.add(new AccessMethodDependency(this.className, this.getTargetClassName(node.getExpression()
						.resolveTypeBinding()), fullClass.getLineNumber(node.getStartPosition()), node.getStartPosition(),
						node.getLength(), "initializer static block", node.getName().getIdentifier(), isStatic != 0));
			}
			break;
		}
		return true;
	}

	@Override
	public boolean visit(FieldAccess node) {
		ASTNode relevantParent = getRelevantParent(node);

		int isStatic = node.resolveFieldBinding().getModifiers() & Modifier.STATIC;

		switch (relevantParent.getNodeType()) {
		case ASTNode.METHOD_DECLARATION:
			MethodDeclaration md = (MethodDeclaration) relevantParent;
			this.dependencies.add(new AccessFieldDependency(this.className, this.getTargetClassName(node.getExpression()
					.resolveTypeBinding()), fullClass.getLineNumber(node.getStartPosition()), node.getStartPosition(), node.getLength(), md
					.getName().getFullyQualifiedName(), node.getName().getFullyQualifiedName(), isStatic != 0));
			break;
		case ASTNode.INITIALIZER:
/*tirar duvida de quando entraria*/			
			this.dependencies.add(new AccessFieldDependency(this.className, this.getTargetClassName(node.getExpression()
					.resolveTypeBinding()), fullClass.getLineNumber(node.getStartPosition()), node.getStartPosition(), node.getLength(),
					"initializer static block", node.getName().getFullyQualifiedName(), isStatic != 0));
			break;
		}
		return true;
	}

	@Override
	public boolean visit(QualifiedName node) {
		if ((node.getParent().getNodeType() == ASTNode.METHOD_INVOCATION || node.getParent().getNodeType() == ASTNode.INFIX_EXPRESSION
				|| node.getParent().getNodeType() == ASTNode.VARIABLE_DECLARATION_FRAGMENT || node.getParent().getNodeType() == ASTNode.ASSIGNMENT)
				&& node.getQualifier().getNodeType() != ASTNode.QUALIFIED_NAME) {
			ASTNode relevantParent = getRelevantParent(node);
			int isStatic = node.resolveBinding().getModifiers() & Modifier.STATIC;

			switch (relevantParent.getNodeType()) {
			case ASTNode.METHOD_DECLARATION:
				MethodDeclaration md = (MethodDeclaration) relevantParent;
				this.dependencies.add(new AccessFieldDependency(this.className, this.getTargetClassName(node.getQualifier()
						.resolveTypeBinding()), fullClass.getLineNumber(node.getStartPosition()), node.getStartPosition(),
						node.getLength(), md.getName().getFullyQualifiedName(), node.getName().getFullyQualifiedName(), isStatic != 0));
				break;
			case ASTNode.INITIALIZER:
				this.dependencies.add(new AccessFieldDependency(this.className, this.getTargetClassName(node.getQualifier()
						.resolveTypeBinding()), fullClass.getLineNumber(node.getStartPosition()), node.getStartPosition(),
						node.getLength(), "initializer static block", node.getName().getFullyQualifiedName(), isStatic != 0));
				break;
			}

		}

		return true;
	}

	@Override
	public boolean visit(AnnotationTypeDeclaration node) {
		return super.visit(node);
	}

	@Override
	public boolean visit(AnnotationTypeMemberDeclaration node) {
		return super.visit(node);
	}

	/*tirar duvida de quando entraria*/
	public boolean visit(org.eclipse.jdt.core.dom.NormalAnnotation node) {
		if (node.getParent().getNodeType() == ASTNode.FIELD_DECLARATION) {
			FieldDeclaration field = (FieldDeclaration) node.getParent();
			this.dependencies.add(new AnnotateFieldDependency(this.className, node.getTypeName().resolveTypeBinding().getQualifiedName(),
					fullClass.getLineNumber(node.getStartPosition()), node.getStartPosition(), node.getLength(),
					((VariableDeclarationFragment) field.fragments().get(0)).getName().getIdentifier()));
		} else if (node.getParent().getNodeType() == ASTNode.METHOD_DECLARATION) {
			MethodDeclaration method = (MethodDeclaration) node.getParent();
			this.dependencies.add(new AnnotateMethodDependency(this.className, node.getTypeName().resolveTypeBinding().getQualifiedName(),
					fullClass.getLineNumber(node.getStartPosition()), node.getStartPosition(), node.getLength(), method.getName()
							.getIdentifier()));
		} else if (node.getParent().getNodeType() == ASTNode.TYPE_DECLARATION) {
			this.dependencies.add(new AnnotateClassDependency(this.className, node.getTypeName().resolveTypeBinding().getQualifiedName(),
					fullClass.getLineNumber(node.getStartPosition()), node.getStartPosition(), node.getLength()));
		}
		return true;
	};

	@Override
	public boolean visit(ParameterizedType node) {
		ASTNode relevantParent = this.getRelevantParent(node);
		if (node.getNodeType() == ASTNode.PARAMETERIZED_TYPE) {
			ParameterizedType pt = (ParameterizedType) node;
			if (pt.typeArguments() != null) {
				for (Object o : pt.typeArguments()) {
					Type t = (Type) o;
					if (relevantParent.getNodeType() == ASTNode.METHOD_DECLARATION) {
						MethodDeclaration md = (MethodDeclaration) relevantParent;
						this.dependencies.add(new DeclareParameterizedTypeDependency(this.className, this.getTargetClassName(t
								.resolveBinding()), fullClass.getLineNumber(t.getStartPosition()), t.getStartPosition(), t.getLength(),md.getName().getIdentifier()));
					}else{
						this.dependencies.add(new DeclareParameterizedTypeDependency(this.className, this.getTargetClassName(t
								.resolveBinding()), fullClass.getLineNumber(t.getStartPosition()), t.getStartPosition(), t.getLength()));
					}
				}
			}
		}
		return true;
	}

	private ASTNode getRelevantParent(final ASTNode node) {
		for (ASTNode aux = node; aux != null; aux = aux.getParent()) {
			switch (aux.getNodeType()) {
			case ASTNode.FIELD_DECLARATION:
			case ASTNode.METHOD_DECLARATION:
			case ASTNode.INITIALIZER:
				return aux;
			}
		}
		return node;
	}

	private String getTargetClassName(ITypeBinding type) {
		String result = "";
		if (!type.isAnonymous() && type.getQualifiedName() != null && !type.getQualifiedName().isEmpty()) {
			result = type.getQualifiedName();
		} else if (type.isLocal() && type.getName() != null && !type.getName().isEmpty()) {
			result = type.getName();
		} else if (!type.getSuperclass().getQualifiedName().equals("java.lang.Object") || type.getInterfaces() == null
				|| type.getInterfaces().length == 0) {
			result = type.getSuperclass().getQualifiedName();
		} else if (type.getInterfaces() != null && type.getInterfaces().length == 1) {
			result = type.getInterfaces()[0].getQualifiedName();
		}

		if (result.equals("")) {
			throw new RuntimeException("AST Parser error.");
		} else if (result.endsWith("[]")) {
			result = result.substring(0, result.length() - 2);
		} else if (result.matches(".*<.*>")) {
			result = result.replaceAll("<.*>", "");
		}

		return result;
	}
}
