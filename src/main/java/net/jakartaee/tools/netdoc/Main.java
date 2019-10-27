package net.jakartaee.tools.netdoc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import groovy.json.JsonOutput;

import org.apache.commons.cli.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger( Main.class );  
	private static final String PROPS_FILE = "net-doc.props";
	private static final String SYNTAX = "java -jar net-doc-jee.jar ";
	private static final String FINISH_MSG = "Finished.";
	private static final String TEMP_DIR = "netdoc";
	//private static final String TEMP_DIR1 = "netdoc1";
	//private static final String TEMP_DIR2 = "netdoc2";
	
	private static Properties props = new Properties();		
	
	private enum SOURCE_TYPE { A, C, S }

//	  private static void zipDir(String zipFileName, String dir) throws Exception {
//		    File dirObj = new File(dir);
//		    ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFileName));
//		    System.out.println("Creating : " + zipFileName);
//		    addDir(dirObj, out);
//		    out.close();
//		  }
//
//		  static void addDir(File dirObj, ZipOutputStream out) throws IOException {
//		    File[] files = dirObj.listFiles();
//		    byte[] tmpBuf = new byte[1024];
//
//		    for (int i = 0; i < files.length; i++) {
//		      if (files[i].isDirectory()) {
//		        addDir(files[i], out);
//		        continue;
//		      }
//		      FileInputStream in = new FileInputStream(files[i].getAbsolutePath());
//		      System.out.println(" Adding: " + files[i].getAbsolutePath());
//		      out.putNextEntry(new ZipEntry(files[i].getAbsolutePath()));
//		      int len;
//		      while ((len = in.read(tmpBuf)) > 0) {
//		        out.write(tmpBuf, 0, len);
//		      }
//		      out.closeEntry();
//		      in.close();
//		    }
//		  }
		  
	public static void main(String[] args) throws Exception {
		
		String mainCmd = SYNTAX + String.join(" ", Arrays.asList(args));
		logger.info(mainCmd);
		
		CommandLine cl = CliOptions.generateCommandLine(args);
		System.out.println();
		String propFile = PROPS_FILE;
		if (cl.getOptionValue(CliOptions.PROP_FILE) != null ) propFile = cl.getOptionValue(CliOptions.PROP_FILE);

		try ( InputStream fis = new FileInputStream(propFile); ) {
			props.load(fis);
			logger.info("Got prop SOURCE_DIR: " + props.getProperty(CliOptions.SOURCE_DIR));
		} catch (IOException e) {
			props.setProperty(CliOptions.SOURCE_TYPE, "A");
			props.setProperty(CliOptions.SOURCE_DIR, ".");
			props.setProperty(CliOptions.SUBPACKAGES, ".");
			props.setProperty(CliOptions.CFR_JAR, "cfr-0.147.jar");
			
		}
		
		boolean isVerbose = false;
		boolean isKeepTemp = false;
		boolean isLinux = false;
		try (BufferedReader buf = new BufferedReader(new InputStreamReader(System.in))) {
			if (cl.hasOption(CliOptions.HELP)) {
				CliOptions.printHelp(SYNTAX);
				finish();
			} 

			if (cl.hasOption(CliOptions.VERBOSE)) isVerbose = true;
			if (cl.hasOption(CliOptions.KEEP_TEMP)) isKeepTemp = true;
			if (cl.hasOption(CliOptions.IS_LINUX)) isLinux = true;
			if (cl.getOptionValue(CliOptions.CFR_JAR) != null )  props.setProperty(CliOptions.CFR_JAR, cl.getOptionValue(CliOptions.CFR_JAR));
			
            if (cl.hasOption(CliOptions.INTERACTIVE)) {
                handlePropInput(buf,CliOptions.SOURCE_TYPE, false);
                handlePropInput(buf,CliOptions.SOURCE_DIR, true);
                if ( props.getProperty(CliOptions.SOURCE_TYPE).toUpperCase().startsWith("A") ) {
                	handlePropInput(buf,CliOptions.SOURCE_FILE, false);
                	String appName = props.getProperty(CliOptions.SOURCE_FILE);
                	int lastDot = appName.lastIndexOf(".");
                	if ( lastDot > 0 ) appName = appName.substring(0, lastDot);
                	props.setProperty(CliOptions.APP_NAME, appName );
                } else {
                	String appName = props.getProperty(CliOptions.SOURCE_DIR);
                	int lastSlash = appName.lastIndexOf("/");			// TODO: Handle Windows backslash too?
                   	if ( lastSlash == appName.length() - 1 ) appName = appName.substring(0,lastSlash);	// Trim last slash 
                   	lastSlash = appName.lastIndexOf("/");
                	if ( lastSlash > 0 ) appName = appName.substring(lastSlash+1);
                	props.setProperty(CliOptions.APP_NAME, appName );              	
                }

                handlePropInput(buf,CliOptions.APP_NAME, false);

                handlePropInput(buf,CliOptions.CLASSPATH, false);
                
               // handlePropInput(buf,CliOptions.IS_LINUX, false);
                String filePath = props.getProperty(CliOptions.SOURCE_DIR) + props.getProperty(CliOptions.SOURCE_FILE);
 
                handlePropInput(buf,CliOptions.SUBPACKAGES, false);
                if (  props.getProperty(CliOptions.SUBPACKAGES) == null || props.getProperty(CliOptions.SUBPACKAGES).length() <=0 ) 
                	props.setProperty(CliOptions.SUBPACKAGES, "."); 			// Include ALL Subpackages

                //if ( fileFolderExists("sample") ) abort("Aborting program.  The directory of the source java/class/jar files (sample) does not exist.");
                //if ( fileFolderExists(filePath) ) abort("Aborting program.  The directory of the source java/class/jar files (" + filePath + ") does not exist.");
                
                logger.info("Running: " + SYNTAX + " -s " +   props.getProperty(CliOptions.SOURCE_DIR));
            } else {
            	if (props.getProperty(CliOptions.SOURCE_DIR) != null) {
    				if (!fileFolderExists( props.getProperty(CliOptions.SOURCE_DIR)))
    					abort("Aborting program.  The source directory (" +  props.getProperty(CliOptions.SOURCE_DIR) + ") does not exist.");
    			} else {
    				abort("Aborting program.  The root source directory (-s) option is required.");
    			}
            }
            
            // TODO (5/7/2019): Don't know how to pass the output location to the Doclet yet.. Use enviroment variable?
//            if (cl.hasOption(CliOptions.OUTPUT)) {
//            	props.setProperty(CliOptions.OUTPUT, cl.getOptionValue(CliOptions.OUTPUT));
//            }
			
			OutputStream output = new FileOutputStream(PROPS_FILE);
			props.store(output,  null);

//	        switch( props.getProperty(CliOptions.SOURCE_TYPE).toUpperCase() ) 
//	        { 
//	            case "A": runArchive(isLinux, isKeepTemp); break; 
//	            case "C": runClasses(isLinux, isKeepTemp); break; 
//	            case "S": runSource(isLinux, isKeepTemp); break; 
//	            default: 
//    				abort("Unknown 'source-type'.  Valid values are A) web/jar archive, C) classes, or S) source files.");
//	        } 
			SOURCE_TYPE sourceType = Enum.valueOf(SOURCE_TYPE.class, props.getProperty(CliOptions.SOURCE_TYPE).toUpperCase());
			
			System.out.println("---------- NetDoc Scanning Properties ----------");
			System.out.println("File: " + propFile);
			System.out.println();
			System.out.println(props.toString().replace(", ", "\n").replace("{", "").replace("}", ""));  
			System.out.println("------------------------------------------------");
			
			run(sourceType, isLinux, isKeepTemp, isVerbose);
			
		} catch (Exception e) {
			e.printStackTrace();
			//return;
		} 

	}
	
	private static void run(SOURCE_TYPE sourceType, boolean isLinux, boolean keepTemp, boolean isVerbose) throws IOException {
		File tempDir = Util.createTempDir(TEMP_DIR);
		String tempPath = tempDir.getAbsolutePath().replace("\\","/");		// replace windows backslash because either works with the cmd

		String cfrJar = props.getProperty(CliOptions.CFR_JAR);

		String decompiledPath = tempPath + "/decompiled";

		String classPath = Util.withSlashStar(props.getProperty(CliOptions.CLASSPATH));
		
		String subPackages = props.getProperty(CliOptions.SUBPACKAGES);
		String sourceDir = props.getProperty(CliOptions.SOURCE_DIR);			

				
        try {
        	switch( sourceType )
			{
				case A:
					classPath += ";lib/*;" + tempPath + "/WEB-INF/lib/*;" + tempPath + "/BOOT-INF/lib/*;";
					String filePath = sourceDir + props.getProperty(CliOptions.SOURCE_FILE);
					Util.unzip(filePath, tempDir);
					
					String runDecompileA = String.format("java -jar lib/%s -cp %s --outputdir %s", cfrJar, filePath, decompiledPath);
					logger.debug("Running: " + runDecompileA);
					Util.runCommand(isLinux, runDecompileA, isVerbose);
					break;
				case C:
					classPath += ";lib/*;" + tempPath + "/WEB-INF/lib/*;" + tempPath + "/BOOT-INF/lib/*;";
					String zipPath = tempPath + "/" + props.getProperty(CliOptions.APP_NAME)+".jar";
		        	String runJar = String.format("jar cvf %s %s", zipPath, sourceDir);
					logger.debug("Running: " + runJar);
					Util.runCommand(isLinux, runJar, isVerbose);
	
					String runDecompileC = String.format("java -jar lib/%s -cp %s --outputdir %s", cfrJar, zipPath, decompiledPath);
					logger.debug("Running: " + runDecompileC);
					Util.runCommand(isLinux, runDecompileC, isVerbose);

					break;
				case S:
					classPath += ";lib/*;";
					decompiledPath = props.getProperty(CliOptions.SOURCE_DIR);
					break;
			}
			
			String runJavaDoc = String.format("javadoc -doclet net.jakartaee.tools.netdoc.JeeScannerDoclet -docletpath lib/net-doc-jee-doclet.jar -subpackages %s -sourcepath \"%s\" -classpath \"%s\" ", subPackages, decompiledPath, classPath);
			logger.debug("Running: " + runJavaDoc);
			String gotOutput = Util.runCommand(isLinux, runJavaDoc, isVerbose);
			
			String START_AFTER = "Constructing Javadoc information...";
			int iJson = gotOutput.indexOf(START_AFTER);
			logger.debug("Found START_AFTER at: " + iJson);
			if ( iJson > 0 ) {
				gotOutput = gotOutput.substring(iJson + START_AFTER.length());
				String TRIM_AFTER ="}]}]}";		// TODO:  This is a hack.  The Javadoc sometimes writes "# Warnings" after the closing json
				int iTrim = gotOutput.indexOf(TRIM_AFTER);
				gotOutput = gotOutput.substring(0,iTrim + TRIM_AFTER.length());				
			}
			logger.debug("Got JSON Output from ARCHIVE: ");
			logger.debug(gotOutput);

			outputReports(gotOutput, props.getProperty(CliOptions.APP_NAME));

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {			// TODO: This doesn't get executed with javadoc command exits with error.
			if ( !keepTemp ) {
				Util.deleteDir(tempDir);							
			}
		}
		

	}
	
//	private static void runSource(boolean isLinux, boolean keepTemp) throws IOException {
//        try {
//			String sourceDir = props.getProperty(CliOptions.SOURCE_DIR);
//			String classPath = props.getProperty(CliOptions.CLASSPATH);
//			String subPackages = props.getProperty(CliOptions.SUBPACKAGES);
//			// if ( subPackages == null || subPackages.length() <=0 ) subPackages = ".";  This check is done in the Main method
//			
//			String runJavaDoc = String.format("javadoc -doclet net.jakartaee.tools.netdoc.JeeScannerDoclet -docletpath lib/net-doc-jee-doclet.jar -subpackages %s -sourcepath %s -classpath \"./lib/*;%s/*\"", subPackages, sourceDir, classPath);
//			log.debug("Running: " + runJavaDoc);
//			String gotOutput = Util.runCommand(isLinux, runJavaDoc);
//			
//			String START_AFTER = "Constructing Javadoc information...";
//			int iJson = gotOutput.indexOf(START_AFTER);
//			System.out.println("Found START_AFTER at: " + iJson);
//			if ( iJson > 0 ) {
//				gotOutput = gotOutput.substring(iJson + START_AFTER.length());
//				String TRIM_AFTER ="}]}]}";		// TODO:  This is a hack.  The Javadoc sometimes writes "# Warnings" after the closing json
//				int iTrim = gotOutput.indexOf(TRIM_AFTER);
//				gotOutput = gotOutput.substring(0,iTrim + TRIM_AFTER.length());				
//			}
//			System.out.println("Got JSON Output from SOURCE: ");
//			System.out.println(gotOutput);
//			System.out.println();
//			outputReports(gotOutput, props.getProperty(CliOptions.APP_NAME));
//
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} 
//		
//
//	}
//	
//	private static void runClasses(boolean isLinux, boolean keepTemp) throws IOException {
//		
//		//
//		// TODO: Maybe it is easiest to jar up the folder and the call CFR, rather than figuring out how to call CFR recursively on a folder
//		//
//		
//		File tempDir1 = Util.createTempDir(TEMP_DIR1);
//		//String t1 = tempDir1.getAbsolutePath().replace("\\","/");		// replace windows backslash because either works with the cmd
//		String t1 = tempDir1.getAbsolutePath();		//  windows backslash is necessary for the zipDir
//		String zipPath = t1 + "\\" + props.getProperty(CliOptions.APP_NAME);
//		String decompiledPath = t1 + "/decompiled";
//
//		String cfrJar = props.getProperty(CliOptions.CFR_JAR);
//		String sourceDir = props.getProperty(CliOptions.SOURCE_DIR);
//		String classpath = props.getProperty(CliOptions.CLASSPATH);
//
//		String subPackages = props.getProperty(CliOptions.SUBPACKAGES);
//
//				
//        try {
//			//Util.deleteDir(tempDir1);	// Be sure to clean out old temp dirs if they exist
//			//Util.deleteDir(tempDir2);		
//        	
//			//Util.zipDir(zipPath, sourceDir);  THis didn't work
//        	
//        	String runJar = String.format("jar cvf %s.jar %s", zipPath, sourceDir);
//			log.debug("Running: " + runJar);
//			Util.runCommand(isLinux, runJar);
//			
//			//String filePath = props.getProperty(CliOptions.SOURCE_DIR) + props.getProperty(CliOptions.SOURCE_FILE);
//			//String runDecompile = "java -jar lib/cfr-0.146.jar -cp " + zipPath + " --outputdir " + decompiledPath;
//			String runDecompile = String.format("java -jar lib/%s -cp %s --outputdir %s", cfrJar, zipPath, decompiledPath);
//			log.debug("Running: " + runDecompile);
//			Util.runCommand(isLinux, runDecompile);
//
//			
//			//String runJavaDoc = String.format("javadoc -doclet net.jakartaee.tools.netdoc.JeeScannerDoclet -docletpath lib/net-doc-jee-doclet.jar -subpackages %s -sourcepath %s\\WEB-INF\\classes -classpath \"./lib/*;%s\\WEB-INF\\lib\\*\"", props.getProperty(CliOptions.SUBPACKAGES), tempDir2.getAbsolutePath(), tempDir2.getAbsolutePath());
//			String runJavaDoc = String.format("javadoc -doclet net.jakartaee.tools.netdoc.JeeScannerDoclet -docletpath lib/net-doc-jee-doclet.jar -subpackages %s -sourcepath \"%s\" -classpath \"%s\" ", subPackages, decompiledPath, classpath);
//			log.debug("Running: " + runJavaDoc);
//			String gotOutput = Util.runCommand(isLinux, runJavaDoc);
//			
//			String START_AFTER = "Constructing Javadoc information...";
//			int iJson = gotOutput.indexOf(START_AFTER);
//			System.out.println("Found START_AFTER at: " + iJson);
//			if ( iJson > 0 ) {
//				gotOutput = gotOutput.substring(iJson + START_AFTER.length());
//				String TRIM_AFTER ="}]}]}";		// TODO:  This is a hack.  The Javadoc sometimes writes "# Warnings" after the closing json
//				int iTrim = gotOutput.indexOf(TRIM_AFTER);
//				gotOutput = gotOutput.substring(0,iTrim + TRIM_AFTER.length());				
//			}
//			System.out.println("Got JSON Output from ARCHIVE: ");
//			System.out.println(gotOutput);
//			System.out.println();
//			outputReports(gotOutput, props.getProperty(CliOptions.APP_NAME));
//
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} finally {			// TODO: This doesn't get executed with javadoc command exits with error.
//			if ( !keepTemp ) {
//				//Util.deleteDir(tempDir1);	
//				Util.deleteDir(tempDir1);							
//			}
//		}
//		
//
//	}	
//
//	
//	private static void runArchive(boolean isLinux, boolean keepTemp) throws IOException {
//		//File tempDir1 = Util.createTempDir(TEMP_DIR1);
//		File tempDir2 = Util.createTempDir(TEMP_DIR2);
//		String t2 = tempDir2.getAbsolutePath().replace("\\","/");		// replace windows backslash because either works with the cmd
//		//String t2 = tempDir2.getAbsolutePath();		
//
//		//String sourcepath = t2 + "/WEB-INF/classes;" + t2 + "/BOOT-INF/classes*;";
//		String cfrJar = props.getProperty(CliOptions.CFR_JAR);
//		String decompiledPath = t2 + "/decompiled";
//
//		String classpath = "./lib/*;" + t2 + "/WEB-INF/lib/*;" + t2 + "/BOOT-INF/lib/*;";
//			   classpath += props.getProperty(CliOptions.CLASSPATH);
//
//		String subPackages = props.getProperty(CliOptions.SUBPACKAGES);
//
//				
//        try {
//			//Util.deleteDir(tempDir1);	// Be sure to clean out old temp dirs if they exist
//			//Util.deleteDir(tempDir2);			
//			
//			String filePath = props.getProperty(CliOptions.SOURCE_DIR) + props.getProperty(CliOptions.SOURCE_FILE);
//			
//			//String runUnjar = String.format("jar xvf %s -C %s",filePath, tempDir2);
//			//log.debug("Running: " + runUnjar);
//			//Util.runCommand(isLinux, runUnjar);
//			
//			Util.unzip(filePath, tempDir2);
//
//			String runDecompile = String.format("java -jar lib/%s -cp %s --outputdir %s", cfrJar, filePath, decompiledPath);
//			log.debug("Running: " + runDecompile);
//			Util.runCommand(isLinux, runDecompile);
//
//			
//			//String runJavaDoc = String.format("javadoc -doclet net.jakartaee.tools.netdoc.JeeScannerDoclet -docletpath lib/net-doc-jee-doclet.jar -subpackages %s -sourcepath %s\\WEB-INF\\classes -classpath \"./lib/*;%s\\WEB-INF\\lib\\*\"", props.getProperty(CliOptions.SUBPACKAGES), tempDir2.getAbsolutePath(), tempDir2.getAbsolutePath());
//			String runJavaDoc = String.format("javadoc -doclet net.jakartaee.tools.netdoc.JeeScannerDoclet -docletpath lib/net-doc-jee-doclet.jar -subpackages %s -sourcepath \"%s\" -classpath \"%s\" ", subPackages, decompiledPath, classpath);
//			log.debug("Running: " + runJavaDoc);
//			String gotOutput = Util.runCommand(isLinux, runJavaDoc);
//			
//			String START_AFTER = "Constructing Javadoc information...";
//			int iJson = gotOutput.indexOf(START_AFTER);
//			System.out.println("Found START_AFTER at: " + iJson);
//			if ( iJson > 0 ) {
//				gotOutput = gotOutput.substring(iJson + START_AFTER.length());
//				String TRIM_AFTER ="}]}]}";		// TODO:  This is a hack.  The Javadoc sometimes writes "# Warnings" after the closing json
//				int iTrim = gotOutput.indexOf(TRIM_AFTER);
//				gotOutput = gotOutput.substring(0,iTrim + TRIM_AFTER.length());				
//			}
//			System.out.println("Got JSON Output from ARCHIVE: ");
//			System.out.println(gotOutput);
//			System.out.println();
//			outputReports(gotOutput, props.getProperty(CliOptions.APP_NAME));
//
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} finally {			// TODO: This doesn't get executed with javadoc command exits with error.
//			if ( !keepTemp ) {
//				//Util.deleteDir(tempDir1);	
//				Util.deleteDir(tempDir2);							
//			}
//		}
//		
//
//	}	
	private static void outputReports(String json, String info) throws IOException {
		String OUT_JSON = "out/net-doc-jee-report_"+info+".json";
		String OUT_HTML = "out/net-doc-jee-report_"+info+".html";
		String OUT_MHTML = "out/net-doc-jee-report_"+info+".mhtml";
		
		try(BufferedWriter writer = new BufferedWriter(new FileWriter(OUT_JSON))){
		    writer.write(JsonOutput.prettyPrint(json)); 
		    System.out.println("Output JSON file: " + OUT_JSON);
		}
			
		try(BufferedWriter writer = new BufferedWriter(new FileWriter(OUT_HTML))){
		    writer.write(Util.convertJsToHtml( Util.convertJsonToJs(json), false )); 
		    System.out.println("Output HTML file: " + OUT_HTML);
		}
		
		try(BufferedWriter writer = new BufferedWriter(new FileWriter(OUT_MHTML))){
		    writer.write(Util.convertJsToHtml( Util.convertJsonToJs(json), true )); 
		    System.out.println("Output HTML file: " + OUT_MHTML);
		}		
	}
	private static void abort(String message) {
		System.err.println(message);
		CliOptions.printUsage(SYNTAX);
		CliOptions.printHelp(SYNTAX);
		System.exit(-1);
	}

	private static void finish() {
		logger.debug(FINISH_MSG);
		System.exit(0);
	}
	
	private static boolean fileFolderExists(String path) {
		File someFile = new File(path);
		return someFile.exists();
	}
	
    private static void handlePropInput(BufferedReader buf, String key, boolean hasTrailingSlash) throws IOException {
        System.out.print("Enter the " + key + " (" + props.getProperty(key) + "): ");
        String entry = buf.readLine();
        if (!entry.equals("")) {
            if (hasTrailingSlash && (!entry.endsWith("/") && !entry.endsWith("\\")) )  props.setProperty( key, entry + "/" );
            else props.setProperty( key, entry);
        }      
        

    }
//    private static String getSourceFileEntry(BufferedReader buf) throws IOException {

    //        System.out.print("Enter the source file java/class/jar files (" + props.getProperty(CliOptions.SOURCE_FILE) + "): ");
//        String entry = buf.readLine();
//        if (!entry.equals("")) {
//            //if (!entry.endsWith("/") && !entry.endsWith("\\")) entry = entry + "/";
//
//            log.debug("Got dir2: " + props.getProperty(CliOptions.SOURCE_DIR));
//
//        }
//        return entry;
//    }   
//    private static String getOutputDirEntry(BufferedReader buf) throws IOException {
//        System.out.print("Enter Directory for output file. Leave blank to create out.json in current directory. ");
//        String entry = buf.readLine();
//        if (!entry.equals("")) {
//            if (!entry.endsWith("/") && !entry.endsWith("\\")) entry = entry + "/";
//            if (!fileFolderExists(entry))
//                abort("Aborting program.  The output directory (" + entry + ") does not exist.");

    //        }
//        return entry;
//    }
}
