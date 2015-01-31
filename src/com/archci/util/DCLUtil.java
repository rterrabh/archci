package com.archci.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jface.text.Document;

import com.archci.ast.DCLDeepDependencyVisitor;
import com.archci.exception.DCLException;
import com.archci.exception.ParseException;
//import org.eclipse.jface.dialogs.MessageDialog;
//import org.eclipse.swt.widgets.Shell;
//import com.archci.builder.DCLBuilder;

public final class DCLUtil {
	public static final String NOME_APLICACAO = ".: archici :.";
	public static final String DC_FILENAME = "architecture.dcl";
	public static final String DCLDATA_FOLDER = "dcldata";
	
	private DCLUtil() {
	}

//VERIFICA SE O DCL TA ATIVADO (NAO PRECISA)	
/*	public static boolean isDclEnabled(IProject project) throws CoreException{
		ICommand[] commands = project.getDescription().getBuildSpec();
		
		boolean flag = false;
		for (ICommand c : commands) {
			if (c.getBuilderName().equals(DCLBuilder.BUILDER_ID)) {
				flag = true;
			}
		}
		return flag;
	}
*/	
	
	public static Collection<String> getPathFromFile(File classpath) throws IOException{
		Collection<String> classEntriesPathFromFile = new LinkedList<String>();
		
		String sCurrentLine;
		 
		BufferedReader br = new BufferedReader(new FileReader(classpath));

		while ((sCurrentLine = br.readLine()) != null) {
			Pattern findPath = Pattern.compile("\\bpath=\"(.*?)\"");
			
			Matcher matcher = findPath.matcher(sCurrentLine);
			while (matcher.find()) {
				String result = matcher.group(1);
				if (result.endsWith(".jar")){
					if(result.startsWith("ECLIPSE_HOME")) { 
						//do something to find where eclipse is installed
						//TEMPORARY SOLUTION:
						classEntriesPathFromFile.add("/Users/arthurfp/Downloads/eclipse"+"/"+result.substring(result.indexOf("/")+1));
					} else if(result.startsWith("/")){
						classEntriesPathFromFile.add(result);
					}
					else {
						classEntriesPathFromFile.add(classpath.getParentFile().getAbsolutePath()+"/"+result);
					}
				}
			}
		}
		
		return classEntriesPathFromFile;
		
	}
	
	
	public static Collection<String> getPath(File folder) throws IOException, ParseException, CoreException, DCLException{
		Collection<String> classEntriesPath = new LinkedList<String>();
		
		boolean foundClasspathFile=false;
		
		Stack<File> stack3 = new Stack<File>();
		stack3.push(folder);
			while(!stack3.isEmpty()) {
				File child = stack3.pop();
				if (child.isDirectory()) {
					for(File f : child.listFiles()) stack3.push(f);
				//} else if (child.isFile() && (child.getName().endsWith(".jar") || child.getName().endsWith(".class"))) {
				} else if (child.isFile() && child.getName().endsWith(".classpath")) {
						foundClasspathFile=true;
						classEntriesPath.addAll(getPathFromFile(child));
				} /*else if(child.isFile() && child.getName().equals("plugin.xml")) {
					isAPluginProject = true;
				}*/
			}

		
		if(!foundClasspathFile){
			Stack<File> stack = new Stack<File>();
			stack.push(folder);
				while(!stack.isEmpty()) {
					File child = stack.pop();
					if (child.isDirectory()) {
						for(File f : child.listFiles()) stack.push(f);
					//} else if (child.isFile() && (child.getName().endsWith(".jar") || child.getName().endsWith(".class"))) {
					} else if (child.isFile() && child.getName().endsWith(".jar")) {
							classEntriesPath.add(child.getAbsolutePath());
					}
				}
		}
		
		//PARA PLUG-INS
		/*boolean isAPluginProject = false;
		  if (isAPluginProject){
			String path = "/Users/arthurfp/Downloads/eclipse/plugins/";
			File folder2 = new File(path);
				
			Stack<File> stack2 = new Stack<File>();
			stack2.push(folder2);
				while(!stack2.isEmpty()) {
					File child2 = stack2.pop();
					if (child2.isDirectory()) {
						for(File f : child2.listFiles()) stack2.push(f);
					//} else if (child.isFile() && (child.getName().endsWith(".jar") || child.getName().endsWith(".class"))) {
					} else if (child2.isFile() && child2.getName().endsWith(".jar")) {
							classEntriesPath.add(child2.getAbsolutePath());
					}
				}
		}*/
		
		//CARREGA AS BIBLIOTECAS JDT EMBUTIDO NO ECLIPSE
		//TEMPORARY SOLUTION
		String path = "/Users/arthurfp/Downloads/eclipse/plugins/";
		File folder2 = new File(path);
			
		Stack<File> stack2 = new Stack<File>();
		stack2.push(folder2);
			while(!stack2.isEmpty()) {
				File child2 = stack2.pop();
				if (child2.isDirectory()) {
					for(File f : child2.listFiles()) stack2.push(f);
				//} else if (child.isFile() && (child.getName().endsWith(".jar") || child.getName().endsWith(".class"))) {
				} else if (child2.isFile() && child2.getName().endsWith(".jar")) {
						classEntriesPath.add(child2.getAbsolutePath());
				}
			}
		
			
		return classEntriesPath;
		}
	
	public static Collection<String> getSource(File folder) throws IOException, ParseException, CoreException, DCLException{
		Collection<String> sourceEntriesPath = new LinkedList<String>();
		
		Stack<File> stack = new Stack<File>();
		stack.push(folder);
			while(!stack.isEmpty()) {
				File child = stack.pop();
				if (child.isDirectory()) {
					if (child.getName().endsWith("src")) {
						sourceEntriesPath.add(child.getAbsolutePath());
					}
					else {
						for(File f : child.listFiles()) stack.push(f);
					}
				}
			}
		
		return sourceEntriesPath;
	}
	
	public static File getDCLFile(File projectPath){
		Stack<File> stack = new Stack<File>();
		stack.push(projectPath);
			while(!stack.isEmpty()) {
				File child = stack.pop();
				if (child.isDirectory()) {
				  for(File f : child.listFiles())
					  stack.push(f);
				} else if (child.isFile() && child.getName().endsWith(".dcl"))
					return child;				
			}
		return null;
	}
	
	/**
	 * DCL2 Adjust the name of the class to make the identification easier It is
	 * done by converting all "/" to "."
	 * 
	 * Still "converts" the primitive types to your Wrapper.
	 * 
	 * @param className
	 *            Name of the class
	 * @return Adjusted class name
	 */
	public static String adjustClassName(String className) {
		if (className.startsWith("boolean") || className.startsWith("byte") || className.startsWith("short")
				|| className.startsWith("long") || className.startsWith("double") || className.startsWith("float")) {
			return "java.lang." + className.toUpperCase().substring(0, 1) + className.substring(1);
		} else if (className.startsWith("int")) {
			return "java.lang.Integer";
		} else if (className.startsWith("int[]")) {
			return "java.lang.Integer[]";
		} else if (className.startsWith("char")) {
			return "java.lang.Character";
		} else if (className.startsWith("char[]")) {
			return "java.lang.Character[]";
		}
		return className.replaceAll("/", ".");
	}

	/**
	 * DCL2 Checks whether the given resource is a Java source file.
	 * 
	 * @param resource
	 *            * The resource to check.
	 * @return <code>true</code> if the given resource is a Java source file,
	 *         <code>false</code> otherwise.
	 */

	//VERIFICA SE O IRESOURCE PASSADO EH UM CODIGO JAVA
	//DESATIVADO POR ORA
	/*
	public static boolean isJavaFile(IResource resource) {
		if (resource == null || (resource.getType() != IResource.FILE)) {
			return false;
		}
		String ex = resource.getFileExtension();
		return "java".equalsIgnoreCase(ex); //$NON-NLS-1$
	}
	*/

	/**
	 * DCL2 Checks whether the given resource is a Java class file.
	 * 
	 * @param resource
	 *            The resource to check.
	 * @return <code>true</code> if the given resource is a class file,
	 *         <code>false</code> otherwise.
	 */
	//VERIFICA SE O IRESOURCE PASSADO EH UM CLASS JAVA
	//DESATIVADO POR ORA
	/*
	public static boolean isClassFile(IResource resource) {
		if (resource == null || (resource.getType() != IResource.FILE)) {
			return false;
		}
		String ex = resource.getFileExtension();
		return "class".equalsIgnoreCase(ex); //$NON-NLS-1$

	}
	*/
	
	/**
	 * DCL2 Returns all class files inside a specific folder
	 * 
	 * @param folder
	 *            Startup folder
	 * @return List of class files
	 * @throws CoreException
	 */
	//RETORNA TODAS AS CLASSES (IFILE) JAVA DENTRO DE UMA PASTA
	//DESATIVADO POR ORA
	/*
	public static Collection<IFile> getAllClassFiles(IFolder folder) throws CoreException {
		Collection<IFile> projectClassResources = new HashSet<IFile>();

		for (IResource resource : folder.members()) {
			if (resource.getType() == IResource.FOLDER) {
				projectClassResources.addAll(getAllClassFiles((IFolder) resource));
			} else if (isClassFile(resource)) {
				projectClassResources.add((IFile) resource);
			}
		}

		return projectClassResources;
	}
	*/

	/**
	 * DCL2 Returns all class files inside the project
	 * 
	 * @param project
	 *            Java Project
	 * @return List of class files
	 * @throws IOException 
	 * @throws CoreException
	 */
	//RETORNA TODAS AS CLASSES (IFILE) JAVA DENTRO DE UM PROJETO
	//DESATIVADO POR ORA
	/*
	public static Collection<IFile> getAllClassFiles(IProject project) throws CoreException {
		IJavaProject javaProject = JavaCore.create(project);
		IPath binDir = javaProject.getOutputLocation();
		return DCLUtil.getAllClassFiles(project.getFolder(binDir.removeFirstSegments(1)));
	}
	*/

	//RETORNA O NOME DA CLASSE DE UM ARQUIVO JAVA
	public static String getClassName(CompilationUnit cUnit, File f) throws IOException {
	    PackageDeclaration classPackage = cUnit.getPackage();
		
		String pack;
		if (classPackage!=null)
			pack = classPackage.getName() + ".";
		else
			pack = "";

		String clazz = FilenameUtils.removeExtension(f.getName());

		return pack + clazz;
	}

	//RETORNA UM COLLECTION DE STRING COM OS ARQUIVOS(FILES) DO UM DIRETORIO DE PROJETO
	public static Collection<File> getFilesFromProject(final File projectPath) throws CoreException, IOException {
		final Collection<File> result = new LinkedList<File>();
		
		Stack<File> stack = new Stack<File>();
		stack.push(projectPath);
			while(!stack.isEmpty()) {
				File child = stack.pop();
				if (child.isDirectory()) {
				  for(File f : child.listFiles())
					  stack.push(f);
				} else if (child.isFile() && child.getName().endsWith(".java")) {
					result.add(child);
				}
			}
		return result;
	}
	
	//METODO ALTERNATIVO PARA PEGAR NOME DAS CLASSES DE UM PROJETO -- DESATIVADO POR ORA
	// TODO: VERIFICAR QUAL DOS DOIS METODOS EH MAIS EFICIENTE
	/*
	public static Collection<String> getClassNames(final File projectPath) throws CoreException, IOException {
		final Collection<String> result = new LinkedList<String>();
		
		Stack<File> stack = new Stack<File>();
		stack.push(projectPath);
			while(!stack.isEmpty()) {
				File child = stack.pop();
				if (child.isDirectory()) {
				  for(File f : child.listFiles())
					  stack.push(f);
				} else if (child.isFile() && child.getName().endsWith(".java")) {
					File aux = child;
					String s="";
					String clazz="";
					
					while(!aux.getParentFile().getName().endsWith("src")){
						s = aux.getParentFile().getName()+"."+s;
						aux = aux.getParentFile();
					}
					clazz = s+child.getName();
					result.add(clazz.substring(0, clazz.indexOf(".java")));
				}
			}
		return result;
	}
	*/
	
	//RETORNA UM COLLECTION DE IFILE DO IPROJECT
	//NAO UTILIZADO MAIS
	/*
	@Deprecated
	public static Collection<IFile> getJavaClasses(final IProject project) throws CoreException {
		final Collection<IFile> result = new LinkedList<IFile>();
		project.accept(new IResourceVisitor() {

			@Override
			public boolean visit(IResource resource) {
				if (resource instanceof IFile && resource.getName().endsWith(".java")) {
					result.add((IFile)resource);
				}
				return true;
			}
		});
		return result;
	}
	 */

	/**
	 * DCL2 The method returns the respective IFile of a java source file
	 * 
	 * If the className is an internal class, the parent class will be returned
	 * 
	 * @param javaProject
	 *            Java Project
	 * @param className
	 *            Name of the class in the following format: #org.Foo#
	 * @return Class IFile resource
	 * @throws JavaModelException
	 */
	//RETORNA O IFILE DE UM IJAVAPROJECT, INFORMANDO O NOME DA CLASSE
	//NAO NECESSARIO POR ORA
	/*
	public static IFile getFileFromClassName(IJavaProject javaProject, final String className) throws JavaModelException {
		for (IPackageFragmentRoot folder : javaProject.getAllPackageFragmentRoots()) {
			if (folder.getKind() == IPackageFragmentRoot.K_SOURCE) {
				IPath path = folder.getPath();
				path = path.removeFirstSegments(1);

				// If was internal class, consider the parent class
				if (className.contains("$")) {
					path = path.append(className.substring(0, className.indexOf('$')).replaceAll("[.]", "" + IPath.SEPARATOR) + ".java");
				} else {
					path = path.append(className.replaceAll("[.]", "" + IPath.SEPARATOR) + ".java");
				}

				IFile file = javaProject.getProject().getFile(path);
				if (file.exists()) {
					return file;
				}
			}

		}
		return null;
	}
	*/

	/**
	 * DCL2 Method responsible to log error
	 * 
	 */
	//CRIA LOG DE ERRO -- DESATIVADO POR ORA
	/*
	public static String logError(IProject project, Throwable thrownExeption) {
		if (project == null) {
			throw new NullPointerException("project cant be null");
		}
		if (thrownExeption == null) {
			throw new NullPointerException("thrownExeption cant be null");
		}

		final IFile logErrorFile = project.getFile("dclcheck_"
				+ DateUtil.dateToStr(new Date(), "yyyyMMdd-HHmmss")
				+ "_error.log");

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		thrownExeption.printStackTrace(new PrintWriter(outputStream, true));
		InputStream source = new ByteArrayInputStream(
				outputStream.toByteArray());
		try {
			logErrorFile.create(source, false, null);
			source.close();
			outputStream.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return logErrorFile.getName();
	}
*/

	/**
	 * DCL2 Returns the module definition from the Java API
	 * 
	 * @return $java DCL constraint
	 */
	
	//RETORNA DEFINICAO DE MODULOS DO JAVA API 
	public static String getJavaModuleDefinition() {
		return "java.**,javax.**,org.ietf.jgss.**,org.omg.**,org.w3c.dom.**,org.xml.sax.**,boolean,char,short,byte,int,float,double,void";
	}

	/**
	 * DCL2 Checks if a className is contained in the Java API
	 * 
	 * @param className
	 *            Name of the class
	 * @return true if it is, no otherwise
	 */
	//VERIFICA SE UM CLASSNAME ESTA CONTIDO NO JAVA API
	public static boolean isFromJavaAPI(final String className) {
		for (String javaModulePkg : getJavaModuleDefinition().split(",")) {
			String prefix = javaModulePkg.substring(0, javaModulePkg.indexOf(".**"));
			if (className.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}

	public static String getNumberWithExactDigits(int originalNumber, int numDigits) {
		String s = "" + originalNumber;
		while (s.length() < numDigits) {
			s = "0" + s;
		}
		return s;
	}

	// public static void createReport(IProject project, Architecture av,
	// List<ArchitecturalDrift> architecturalDrifts, long inicio, long termino)
	// throws IOException {
	//
	// final IFile dcFile = project.getFile("dclcheck_"
	// + DateUtils.dateToStr(new Date(), "yyyyMMdd-HHmmss") + "_report.log");
	//
	// StringBuilder str = new StringBuilder();
	//
	// str.append("dclcheck v1.0.2 (20081029):\n");
	// str.append("General Informations:\n");
	// str.append("Start Time:\t" + DateUtils.dateToStr(new Date(inicio),
	// DateUtils.fullPattern)
	// + "\n");
	// str.append("End Time:\t" + DateUtils.dateToStr(new Date(termino),
	// DateUtils.fullPattern)
	// + "\n");
	// str.append("Spent Time:\t" + ((termino - inicio) / 1000.0) +
	// " seconds\n");
	// str.append("\n\n\n");
	// // double numDepEst = av.getNumberOfEstabilishedDependencies();
	// // writer.println("Estabilished Dependencies: " + numDepEst);
	// // writer.println("Violated Dependencies: " +
	// // architecturalDrifts.size());
	// // writer.println("Architectural Conformacao: "
	// // + (numDepEst - architecturalDrifts.size()) / numDepEst);
	//
	// if (architecturalDrifts != null && !architecturalDrifts.isEmpty()) {
	//
	// Set<DependencyConstraint> dcList = new TreeSet<DependencyConstraint>();
	// for (ArchitecturalDrift ad : architecturalDrifts) {
	// dcList.add(ad.getDependencyConstraint());
	// }
	// str.append("\n\n\nSummarized results:\n");
	// str.append("DC\tNUMBER OF ARCHITECTURAL DRIFTS\n");
	// for (DependencyConstraint dc : dcList) {
	// int count = 0;
	// for (ArchitecturalDrift ad : architecturalDrifts) {
	// if (ad.getDependencyConstraint().equals(dc)) {
	// count++;
	// }
	// }
	// str.append(dc + "\t" + count + "\n");
	// }
	//
	// str.append("\n\n\n\n");
	// str.append("DC\tNUMBER OF CLASSES WITH ARCHITECTURAL DRIFTS\n");
	// for (DependencyConstraint dc : dcList) {
	// Set<String> cnList = new HashSet<String>();
	// for (ArchitecturalDrift ad : architecturalDrifts) {
	// if (ad.getDependencyConstraint().equals(dc)) {
	// cnList.add(ad.getClassName());
	// }
	// }
	// str.append(dc + "\t" + cnList.size() + "\n");
	// }
	//
	// str.append("\n\n\n\n");
	// str.append("DC\tCLASS NAME\tNUMBER OF ARCHITECTURAL DRIFT IN CLASS\n");
	// for (DependencyConstraint dc : dcList) {
	// Set<String> cnList = new HashSet<String>();
	// for (ArchitecturalDrift ad : architecturalDrifts) {
	// if (ad.getDependencyConstraint().equals(dc)) {
	// cnList.add(ad.getClassName());
	// }
	// }
	//
	// for (String className : cnList) {
	// int count = 0;
	// for (ArchitecturalDrift ad : architecturalDrifts) {
	// if (ad.getDependencyConstraint().equals(dc)
	// && ad.getClassName().equals(className)) {
	// count++;
	// }
	// }
	// str.append(dc + "\t" + className + "\t" + count + "\n");
	// }
	// }
	//
	// str.append("\n\n\n");
	// str.append("Found architectural drifts (result in Eclipse):\n");
	// str.append("DC\tCLASS NAME\tLINE NUMBER\tMESSAGE\n");
	// for (ArchitecturalDrift ad : architecturalDrifts) {
	// str.append(ad.getDependencyConstraint() + "\t" + ad.getClassName() + "\t"
	// + ad.getLineNumber() + "\t" + ad.getMessage() + "\n");
	// }
	//
	// }
	//
	// InputStream source = new ByteArrayInputStream(str.toString().getBytes());
	// try {
	// dcFile.create(source, false, null);
	// IDE.openEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(),
	// dcFile);
	// } catch (CoreException e1) {
	// e1.printStackTrace();
	// }
	//
	// }

	/**
	 * DCL2 Show a message
	 * 
	 * @param shell
	 * @param message
	 */
	//EXIBE MENSAGEM --DESATIVADO POR ORA
	/*
	public static void showMessage(Shell shell, String message) {
		MessageDialog.openInformation(shell, NOME_APLICACAO, message);
	}
	 */
	
	/**
	 * DCL2 Show an error
	 * 
	 * @param shell
	 * @param message
	 */
	//EXIBE ERRO --DESATIVADO POR ORA
	/*
	public static void showError(Shell shell, String message) {
		MessageDialog.openError(shell, NOME_APLICACAO, message);
	}
	 */
	
	/**
	 * DCL2 Returns all dependencies from the class class
	 * 
	 * @param classes
	 *            List of classes
	 * @return List of dependencies
	 */
	//NAO MAIS UTILIZADO
	/*
	@Deprecated
	public static Collection<Dependency> getDependenciesUsingASM(IFile file) throws CoreException, IOException {
		/*
		 * final DCLDeepDependencyVisitor cv = new DCLDeepDependencyVisitor();
		 * final Collection<Dependency> dependencies = new
		 * LinkedList<Dependency>();
		 * 
		 * IJavaProject javaProject = JavaCore.create(file.getProject()); IPath
		 * binDir = javaProject.getOutputLocation(); IPath path =
		 * binDir.removeFirstSegments
		 * (1).append(file.getProjectRelativePath().removeFirstSegments(1));
		 * IFile binFile =
		 * javaProject.getProject().getFile(path.removeFileExtension
		 * ().addFileExtension("class"));
		 * 
		 * ClassReader cr = new DCLClassReader(binFile.getContents());
		 * cr.accept(cv, 0); dependencies.addAll(cv.getDependencies());
		 * 
		 * return dependencies;
		 *
		* return null;
	* }
*/

	/**
	 * DCL2 Returns all dependencies from the class class
	 * 
	 * @param classes
	 *            List of classes
	 * @return List of dependencies
	 * @throws ParseException 
	 */
	
	//RETORNA TODOS OS OBJETOS DODCLDEEPDEPENDENCYVISITOR (AST)
	public static DCLDeepDependencyVisitor useAST(File f, String[] classPathEntries, String[] sourcePathEntries) throws CoreException, IOException, DCLException, ParseException {
		return new DCLDeepDependencyVisitor(f, classPathEntries, sourcePathEntries);
	}
	
	
	public static CompilationUnit getCompilationUnitFromAST(File file, String[] classPathEntries, String[] sourcePathEntries) throws IOException{
	
		
		String[] encodings = new String[sourcePathEntries.length];
		for(int i=0; i < sourcePathEntries.length; i++){
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
	    
	    parser.setKind(ASTParser.K_COMPILATION_UNIT);
	    parser.setSource(document.get().toCharArray());
	    parser.setResolveBindings(true);
	    
	    parser.setEnvironment(classPathEntries, sourcePathEntries, encodings, true);
	    parser.setUnitName("Dependency-Tool");
	    parser.setBindingsRecovery(true);
	    
	    return (CompilationUnit) parser.createAST(null);
	}
	
	public static Set<ITypeBinding> getSubTypes(List<ITypeBinding> typeBindings, String desc){
		Set<ITypeBinding> subTypes = new HashSet<ITypeBinding>();
		
		for (ITypeBinding typeBind : typeBindings){
			
			if(typeBind.getQualifiedName().equals(desc)){
				subTypes.addAll(Arrays.asList(typeBind.getDeclaredTypes()));
			}
			else{
				Set<ITypeBinding> superTypeBind = new HashSet<ITypeBinding>();
				
				ITypeBinding superclass = typeBind;
				boolean superMatch = false;
				
				while(superclass!=null && !superMatch){ 
					
					superTypeBind.add(superclass);
					
					ITypeBinding[] indirectInterfaceBinds = superclass.getInterfaces();
					
					for(ITypeBinding iib: indirectInterfaceBinds){
						if(iib.getQualifiedName().equals(desc)){
							subTypes.addAll(superTypeBind);
							superTypeBind.clear();
						}
					}
					
					superclass = superclass.getSuperclass();
					
					if(superclass.getQualifiedName().equals(desc)) 
						superMatch = true;
				}
				
				if(superMatch) subTypes.addAll(superTypeBind);
				
			}
		}	
		return subTypes;
	}

	/**
	 * Checks if a specific class is contained in a list of classes, RE or
	 * packages
	 */
	public static boolean hasClassNameByDescription(final String className, final String moduleDescription,
			final Map<String, String> modules, final Collection<String> projectClassNames, final List<ITypeBinding> typeBindings) {
		
		for (String desc : moduleDescription.split(",")) {
			desc = desc.trim();

			if ("$system".equals(desc)) {
				/*
				 * If it's $system, any class
				 */
				return projectClassNames.contains(className);
			} else if (modules.containsKey(desc)) {
				/*
				 * If it's a module, call again the same method to return with
				 * its description
				 */
				if (hasClassNameByDescription(className, modules.get(desc), modules, projectClassNames, typeBindings)) {
					return true;
				}
			} else if (desc.endsWith("**")) {
				/* If it refers to any class in any package below one specific */
				desc = desc.substring(0, desc.length() - 2);
				if (className.startsWith(desc)) {
					return true;
				}
			} else if (desc.endsWith("*")) {
				/* If it refers to classes inside one specific package */
				desc = desc.substring(0, desc.length() - 1);
				if (className.startsWith(desc) && !className.substring(desc.length()).contains(".")) {
					return true;
				}
			} else if (desc.startsWith("\"") && desc.endsWith("\"")) {
				/* If it refers to regular expression */
				desc = desc.substring(1, desc.length() - 1);
				if (className.matches(desc)) {
					return true;
				}
			} else if (desc.endsWith("+")) {		
				/* If it refers to subtypes */
				desc = desc.substring(0, desc.length() - 1); //TODO: -1 ou -2 ??? -- TESTAR
				Set<ITypeBinding> listSubTypes = new HashSet<ITypeBinding>();

				listSubTypes.addAll(getSubTypes(typeBindings, desc));
								
					StringBuilder strBuilder = new StringBuilder();
					for (ITypeBinding t : listSubTypes) {
						strBuilder.append(t.getQualifiedName() + ",");
					}
					if (strBuilder.length() > 0) {
						strBuilder.deleteCharAt(strBuilder.length() - 1);
					}
					modules.put(desc + "+", strBuilder.toString());
					if (hasClassNameByDescription(className, modules.get(desc + "+"), modules, projectClassNames, typeBindings)) {
						return true;
					}
			} else {
				/* If it refers to a specific class */
				if (desc.equals(className)) {
					return true;
				}
			}
		}

		return false;
	}
	
	public static String getPackageFromClassName(final String className) {
		if (className.contains(".")) {
			return className.substring(0, className.lastIndexOf('.'));
		}
		return className;
	}

	public static String getSimpleClassName(final String qualifiedClassName) {
		return qualifiedClassName.substring(qualifiedClassName.lastIndexOf(".") + 1);
	}

}
