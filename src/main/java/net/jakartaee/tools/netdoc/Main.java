package net.jakartaee.tools.netdoc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;

public class Main {
	private static final String PROPS_FILE = "net-doc.props";
	private static final String SYNTAX = "java -jar net-doc-jee.jar ";
	private static final String FINISH_MSG = "Finished.";
	private static final String TEMP_DIR1 = "netdoc1";
	private static final String TEMP_DIR2 = "netdoc2";
	
	private static Properties props = new Properties();		

	public static void main(String[] args) {
		String mainCmd = SYNTAX + String.join(" ", Arrays.asList(args));
		System.out.println(mainCmd);

		try ( InputStream fis = new FileInputStream(PROPS_FILE); ) {
			props.load(fis);
			System.out.println("Got prop SOURCE_DIR: " + props.getProperty(CliOptions.SOURCE_DIR));
		} catch (IOException e) {
			props.setProperty(CliOptions.SOURCE_DIR, ".");
			props.setProperty(CliOptions.SUBPACKAGES, ".");
		}
		
		
		CommandLine cl = CliOptions.generateCommandLine(args);
		System.out.println();
		//String rootDir = null;
        //String outputFile = "out.json";

		boolean isVerbose = false;
		try (BufferedReader buf = new BufferedReader(new InputStreamReader(System.in))) {
			if (cl.hasOption(CliOptions.HELP)) {
				CliOptions.printHelp(SYNTAX);
				finish();
			} 
//			else if (cl.hasOption(CliOptions.CREATE)) {
//                System.out.print("Enter your PRODUCT watermark: ");
//                String eSomeValue = buf.readLine();
//                if (eSomeValue.equals("")) {
//                    abort("Aborting program.  A valid entry is required.");
//                }
//                finish();
//            }

			if (cl.hasOption(CliOptions.VERBOSE)) isVerbose = true;

            if (cl.hasOption(CliOptions.PROMPT)) {
                handlePropInput(buf,CliOptions.SOURCE_DIR, true);
                handlePropInput(buf,CliOptions.SOURCE_FILE, false);
                String filePath = props.getProperty(CliOptions.SOURCE_DIR) + props.getProperty(CliOptions.SOURCE_FILE);
 
                handlePropInput(buf,CliOptions.SUBPACKAGES, false);
                if (  props.getProperty(CliOptions.SUBPACKAGES) == null ) props.setProperty(CliOptions.SUBPACKAGES, ".");  // If NULL nothhing is included.  Should be all (.) or a subset

                //if ( fileFolderExists("sample") ) abort("Aborting program.  The directory of the source java/class/jar files (sample) does not exist.");
                //if ( fileFolderExists(filePath) ) abort("Aborting program.  The directory of the source java/class/jar files (" + filePath + ") does not exist.");
                
                System.out.println("Running: " + SYNTAX + " -s " +   props.getProperty(CliOptions.SOURCE_DIR));
            } else {
            	if (cl.hasOption(CliOptions.SOURCE_DIR)) {
    				if (!fileFolderExists( cl.getOptionValue(CliOptions.SOURCE_DIR)))
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

            run();

		} catch (Exception e) {
			e.printStackTrace();
			//return;
		} 

	}
	
	private static void run() throws IOException {
		File tempDir1 = Util.createTempDir(TEMP_DIR1);
		File tempDir2 = Util.createTempDir(TEMP_DIR2);
        try {
			String filePath = props.getProperty(CliOptions.SOURCE_DIR) + props.getProperty(CliOptions.SOURCE_FILE);
			
			Util.unzip(filePath, tempDir1);
			//Util.unzip("input/col3.war", tempDir1);
			
			//Util.runCommand("dir");
			Util.runCommand("java -jar lib/jd-cli.jar -od " + tempDir2.getAbsolutePath() + " " + tempDir1.getAbsolutePath());
			
			
			String runJavaDoc = String.format("javadoc -doclet net.jakartaee.tools.netdoc.JeeScannerDoclet -docletpath lib/net-doc-jee-doclet.jar -subpackages %s -sourcepath %s\\WEB-INF\\classes -classpath \"./*;%s\\WEB-INF\\lib\\*\"", props.getProperty(CliOptions.SUBPACKAGES), tempDir2.getAbsolutePath(), tempDir2.getAbsolutePath());
			System.out.println("Running: " + runJavaDoc);
			Util.runCommand(runJavaDoc);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {			// TODO: This doesn't get execure with javadoc command exits with error.
			Util.deleteDir(tempDir1);	
			Util.deleteDir(tempDir2);			
		}
		

	}
	
	private static void abort(String message) {
		System.err.println(message);
		CliOptions.printUsage(SYNTAX);
		CliOptions.printHelp(SYNTAX);
		System.exit(-1);
	}

	private static void finish() {
		System.out.println(FINISH_MSG);
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
//            System.out.println("Got dir2: " + props.getProperty(CliOptions.SOURCE_DIR));
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
