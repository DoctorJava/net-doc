package net.jakartaee.netdoc;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Util {
    private static final Logger logger = LoggerFactory.getLogger( Util.class );  
	final static int BUFFER = 2048;
	
	public static final String[] ignoreExt = new String[] { "gif", "png", "jpg", "jpeg", "svg", "css", "scss"};
	public static final Set<String> IGNORE_EXT = new HashSet<>(Arrays.asList(ignoreExt));
	
	public static final String HR_START = ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>";
	public static final String HR_END 	= "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<";
	
	public static String withSlashStar(String path) {
		if ( path.endsWith("*") ) return path;
		else if ( path.endsWith("/") ) return path + "*";
		else if ( path.endsWith("\\") ) return path + "*";
		else  return path + "/*"; 
	}

//	public static void unzip(String zipFilePath, File tempDir) throws IOException {
//		try {
//			BufferedOutputStream dest = null;
//			FileInputStream fis = new FileInputStream(zipFilePath);
//			ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
//			ZipEntry entry;
//			while ((entry = zis.getNextEntry()) != null) {
//				int dotIndex =  entry.getName().lastIndexOf('.') + 1;
//				
//				if ( dotIndex <= 0 ) continue;
//				if (IGNORE_EXT.contains(entry.getName().substring(dotIndex) ) ) continue; 
//				
//				System.out.println("Extracting: " + entry + " with extension " + entry.getName().substring(dotIndex));
//				int count;
//				byte data[] = new byte[BUFFER];
//				// write the files to the disk
//				String outfilePath = tempDir.getAbsolutePath() + "/" + entry.getName().replaceAll("/",".");
//				System.out.println("Writing file: " + outfilePath);
//				FileOutputStream fos = new FileOutputStream(outfilePath);
//				dest = new BufferedOutputStream(fos, BUFFER);
//				while ((count = zis.read(data, 0, BUFFER)) != -1) {
//					dest.write(data, 0, count);
//				}
//				dest.flush();
//				dest.close();
//			}
//			zis.close();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
	
    private static final int BUFFER_SIZE = 4096;

    
    public static void unjar(String jarFilePath, File tempDir) throws IOException {
    	String destDirectory = tempDir.getAbsolutePath();
        File destDir = new File(destDirectory);
        if (!destDir.exists()) {
            destDir.mkdir();
        }
        logger.info("Unjaring file: " + jarFilePath + " to temp directory: " + tempDir);

    }
 
    
//    public static void zipDir(String zipFilePath, String dir) throws Exception {
//    	File dirObj = new File(dir);
//    	zipFilePath = "D:!temp\bingo.zip";
//    	File zipFile = new File(zipFilePath);
//    	if(!zipFile.exists()){ zipFile.createNewFile(); }
//    	else{ logger.info("File already exists"); }
//    	
//    	try ( FileOutputStream fos = new FileOutputStream(zipFile,false);
//    		  ZipOutputStream  zos = new ZipOutputStream(fos); ){
//            
//            logger.info("Creating : " + zipFile);
//            addDir(dirObj, zos);
//            zos.close();
//            fos.close();
//            logger.info("Done Creating : " + zos + " from " + fos);
//   		
//    	}
//    	catch(Exception e) {
//    		e.printStackTrace();
//    	}
//       
//      }
    
	public static void zipDir(String zipFileName, String dir) throws Exception {
		File dirObj = new File(dir);
		ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFileName));
		logger.info("Creating : " + zipFileName);
		addDir(dirObj, zos);
		zos.finish();
		//out.close();
	}

    private static void addDir(File dirObj, ZipOutputStream out) throws IOException {
        File[] files = dirObj.listFiles();
        byte[] tmpBuf = new byte[1024];

        for (int i = 0; i < files.length; i++) {
          if (files[i].isDirectory()) {
            addDir(files[i], out);
            continue;
          }
          
          FileInputStream in = new FileInputStream(files[i].getAbsolutePath());
         
          String filePath = files[i].getPath();
          if ( filePath.contains(":")) filePath = filePath.substring( filePath.indexOf(":") + 1);  // Strip drive from Windows Path (D:/) because it is illegal for zip file
          if ( filePath.startsWith("/") || filePath.startsWith("\\") ) filePath = filePath.substring(1);  // Strip drive from Windows Path (D:/) because it is illegal for zip file

          logger.info(" Adding: " + filePath);
          out.putNextEntry(new ZipEntry(filePath));
          
          int len;
          while ((len = in.read(tmpBuf)) > 0) {
            out.write(tmpBuf, 0, len);
          }
          out.closeEntry();
          in.close();
        }
      }
    
    //public void unzip(String zipFilePath, String destDirectory) throws IOException {
    public static void unzip(String zipFilePath, File tempDir) throws IOException {
    	String destDirectory = tempDir.getAbsolutePath();
        File destDir = new File(destDirectory);
        if (!destDir.exists()) {
            destDir.mkdir();
        }
        logger.info("Unzipping file: " + zipFilePath + " to temp directory: " + tempDir);
        ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath));
        ZipEntry entry = zipIn.getNextEntry();
        // iterates over entries in the zip file
        while (entry != null) {
            String filePath = destDirectory + File.separator + entry.getName();  	// Error extracting file: C:\Users\scott\AppData\Local\Temp\netdoc\META-INF/MANIFEST.MF
            //String filePath = destDirectory + "/" + entry.getName();
            if (!entry.isDirectory()) {
                // if the entry is a file, extracts it
                extractFile(zipIn, filePath);
            } else {
                // if the entry is a directory, make the directory
                File dir = new File(filePath);
                dir.mkdir();
            }
            zipIn.closeEntry();
            entry = zipIn.getNextEntry();
        }
        zipIn.close();
    }
    /**
     * Extracts a zip entry (file entry)
     * @param zipIn
     * @param filePath
     * @throws IOException
     */
    private static void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
    	try (
    			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));)
    	{
            byte[] bytesIn = new byte[BUFFER_SIZE];
            int read = 0;
            while ((read = zipIn.read(bytesIn)) != -1) {
                bos.write(bytesIn, 0, read);
            }   		
    	}catch ( Exception e) {
    		logger.error("EEEEEEEEEEEEError extracting file: " + filePath + " with error: " + e.getMessage());
    	}

    }
	
	public static File createTempDir(String dirStr) throws IOException {
		String tempDirStr = File.createTempFile("temp-file", "tmp").getParent()+"/" + dirStr;
		File tempDir  = new File(tempDirStr);
		if (tempDir.exists()) {
			//emptyDir(tempDir);
			deleteDir(tempDir);
		} else {
			boolean createdTempFolder = tempDir.mkdirs();
			if (!createdTempFolder) throw new IOException();
			logger.info("Created Temp Directory: "+tempDir.getAbsolutePath());			
		}
		return tempDir;
	}

//	public static void deleteDir(File dir) {
//		emptyDir(dir);
//		dir.delete();
//		logger.info("Deleted Temp Directory: "+dir.getAbsolutePath());
//	}
//	
//	public static void emptyDir(File dir) {
//		String[]entries = dir.list();
//		for(String s: entries){
//		    File currentFile = new File(dir.getPath(),s);
//		    currentFile.delete();
//			logger.info("Deleted file: "+currentFile.getAbsolutePath());
//		}		
//	}

	public static boolean  deleteDir(File directoryToBeDeleted) {
		File[] allContents = directoryToBeDeleted.listFiles();
		if (allContents != null) {
			for (File file : allContents) {
				deleteDir(file);
			}
		}
		return directoryToBeDeleted.delete();
	}
	
	public static String runCommand(boolean isLinux, String cmd, boolean isVerbose) throws IOException {
		StringBuffer output = new StringBuffer();
		logger.info("Running command: " + cmd);
        ProcessBuilder processBuilder = new ProcessBuilder();
        if ( isVerbose ) {
            System.out.println(HR_START);
            System.out.println("Running "+ ( isLinux ? "LINUX" : "WINDOWS" ) +" command: ");
            System.out.println();
            System.out.println(cmd);
            System.out.println();       	
        }
        //
        // This code HUNG on Windows 
        //
//       if ( isLinux )
//        	processBuilder.command("sh", "-c", cmd);
//        else
//        	processBuilder.command("cmd.exe", "/c", cmd);			// Windows
//
//        try {
//
//            Process process = processBuilder.start();
//
//            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//
//            String line;
//            while ((line = reader.readLine()) != null) {
//                System.out.println(line);
//                output.append(line);
//            }
//
//            int exitCode = process.waitFor();
//            logger.debug("\nExited with error code : " + exitCode);
//            if ( isVerbose ) System.out.println("\nExited with error code : " + exitCode);
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
		final File tmp = File.createTempFile("netdocOut", null);
		try {
			tmp.deleteOnExit();

			if (isLinux)
				processBuilder.command("sh", "-c", cmd).redirectErrorStream(true).redirectOutput(tmp);
			else
				processBuilder.command("cmd.exe", "/c", cmd).redirectErrorStream(true).redirectOutput(tmp); // Windows

			final Process process = processBuilder.start();
			final int exitCode = process.waitFor();

			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(tmp)));
			String line = "";
			while ((line = reader.readLine()) != null) {
				if (isVerbose) System.out.println(line);
				output.append(line);
			}
			reader.close();
			tmp.delete();
			logger.debug("\nExited with error code : " + exitCode);
			if (isVerbose) System.out.println("\nExited with error code : " + exitCode);
		} catch (Exception e) {
			logger.error( "EEEEEEEEEEEEError in runCommand: " + processBuilder.toString() + " with error: " + e.getMessage());
		} finally {
			tmp.delete();
		}
		if (isVerbose) System.out.println(HR_END);

		return output.toString();

    }
	
	public static String convertJsonToJs(String json) {			// TODO: This doesn't work with JSON that has <CR> because comma isn't replaced with semi-colon
		String bracketStr = json.replace("\"connections\":", " const connections = ")
								.replace("\"servlets\":", " const servlets = ")
							    .replace("\"services\":", " const services = ")
							    .replace("\"sockets\":", " const sockets = ")
							    .replace("\"info\":", " const info = ")
							    .replace(", const", "; const");								// Replace commas with semi-colon between the objects.  Semicolon necessary if no line breaks
		return bracketStr.substring(1, bracketStr.length() - 1);							// Need to remove the first and last brackets {}
		
	}
	
//	public static String convertJsToHtml(String js, boolean isSingleFile) {
//		final String before = "<!DOCTYPE html><html><title>Net Doc</title> <script type=\"text/javascript\" src=\"js/templates/servlet.js\"></script> <script type=\"text/javascript\" src=\"js/templates/service.js\"></script> <script type=\"text/javascript\" src=\"js/templates/connection.js\"></script> <script type=\"text/javascript\" src=\"js/templates/socket.js\"></script> <link rel=\"stylesheet\" href=\"netdoc.css\">";
//		final String after = "<body><h1><center>Net Doc Report</center></h1><div id=\"app\"></div> <script>document.getElementById(\"app\").innerHTML=`<h1>Servlets</h1><ul>${servlets.map(servletTemplate).join(\"\")}</ul><h1>Web Services</h1><ul>${services.map(serviceTemplate).join(\"\")}</ul><h1>Net Connections</h1><ul>${connections.map(connectionTemplate).join(\"\")}</ul><h1>Web Sockets</h1><ul>${sockets.map(socketTemplate).join(\"\")}</ul>`;</script></body>";
//		return before + "<script>" + js + "</script>" + after;
//	}
	public static String convertJsToHtml(String js, boolean isSingleFile) {
		String returnStr = "<!DOCTYPE html><html>";
		final String head = ( isSingleFile ? getSingleHTMLHead(js) : getHTMLHead(js));
		final String body = "<body><h1><center>Net Doc Report</center></h1><div id=\"app\"></div> <script>document.getElementById(\"app\").innerHTML=`<h1>Servlets</h1><ul>${servlets.map(servletTemplate).join(\"\")}</ul><h1>Web Services</h1><ul>${services.map(serviceTemplate).join(\"\")}</ul><h1>Net Connections</h1><ul>${connections.map(connectionTemplate).join(\"\")}</ul><h1>Web Sockets</h1><ul>${sockets.map(socketTemplate).join(\"\")}</ul>`;</script></body>";
		returnStr += head + body + "</html>";
		return returnStr;
	}
	private static String getHTMLHead(String js) {
		String returnStr = "<head>";
		returnStr += "<title>Net Doc</title> ";
		returnStr += "<script type=\"text/javascript\" src=\"js/templates/servlet.js\"></script>";
		returnStr += "<script type=\"text/javascript\" src=\"js/templates/service.js\"></script>";
		returnStr += "<script type=\"text/javascript\" src=\"js/templates/connection.js\"></script> ";
		returnStr += "<script type=\"text/javascript\" src=\"js/templates/socket.js\"></script>";
		returnStr += "<link rel=\"stylesheet\" href=\"netdoc.css\">";
		returnStr += "<script>" + js + "</script>";
		return returnStr + "</head>";
	}
	private static String getSingleHTMLHead(String js) {
		String returnStr = "<head>";
		returnStr += "<title>Net Doc</title> ";
		returnStr += "<script>function showServletPatterns(t){return t?`\\n\\t\\t<p><strong>URL Patterns: </strong>\\n\\t\\t\\t${t.map(t=>` ${t.path} `).join(\"\")}\\t\\t\\n\\t\\t</p>\\n\\t`:\"\"}function servletTemplate(t){if(t)return`\\n<li>\\n<h2>${t.className}</h2>\\n<p><strong>Package: </strong>${t.packageName}</p>\\n<p><strong>Methods: </strong>${t.methods}</p>\\n${showServletPatterns(t.urlPatterns)}\\n</li>\\n<hr/>\\n`}</script>";
		returnStr += "<script>function showServiceMethods(t){return t?`\\n\\t\\t<p><strong>Methods: </strong>\\n\\t\\t\\t\\t${t.map(t=>` ${t.verb} ,`).join(\"\")}\\t\\t\\n\\t\\t</p>\\n\\t`:\"\"}function showServicePatterns(t){return t?`\\n\\t\\t<p><strong>URL Patterns: </strong>\\n\\t\\t\\t${t.map(t=>` ${t.path} `).join(\"\")}\\t\\t\\n\\t\\t</p>\\n\\t`:\"\"}function serviceTemplate(t){if(t)return`\\n\\t\\t\\t<li>\\n\\t\\t\\t\\t<h2>${t.className}</h2>\\n\\t\\t\\t\\t<p><strong>Package: </strong>${t.packageName}</p>\\n\\t\\t\\t\\t${showServiceMethods(t.methods)}\\n\\t\\t\\t\\t${showServicePatterns(t.urlPatterns)}\\n\\t\\t\\t</li>\\n\\t\\t\\t<hr/> \\n\\t\\t`}</script>";
		returnStr += "<script>function showConnectionMethods(n){if(n)return`\\n<p><strong>Methods: </strong>\\n${n.map(n=>` ${n.verb} ), `).join(\"\")}\\t\\t\\n</p>\\n`}function showConnectionPatterns(n){if(n)return`\\n<p><strong>URL Patterns: </strong>\\n${n.map(n=>` ${n.path} `).join(\"\")}\\t\\t\\n</p>\\n`}function connectionTemplate(n){if(n)return`\\n<li>\\n<h2>${n.className}</h2>\\n<p><strong>Package: </strong>${n.packageName}</p>\\n<p><strong>Types:</strong>\\n${n.hasUrlConnection?\"UrlConnection, \":\"\"}\\n${n.hasSockets?\"Sockets, \":\"\"}\\n${n.hasServerSockets?\"Server Sockets, \":\"\"}\\n</p>\\n</li>\\n<hr/> \\n`}</script> ";
		returnStr += "<script>function showSocketEndpoints(n){return n?`\\n<p><strong>Endpoints: </strong>\\n${n.map(n=>` ${n.path} `).join(\"\")}\\t\\t\\n</p>\\n`:\"\"}function socketTemplate(n){if(n)return`\\n<li>\\n<h2>${n.className}</h2>\\n<p><strong>Package: </strong>${n.packageName}</p>\\n${showSocketEndpoints(n.endpoints)}\\n</li>\\n<hr/> \\n`}</script>";
		returnStr += "<style>body{font-family:sans-serif;background-color:#dedede;color:#333;padding:20px 0 28px 0;margin:0}li{padding:10px;overflow:auto}li:hover{background:#eee;cursor:pointer}.app-title{text-align:center;font-weight:400;font-size:2.6rem}.animal{max-width:650px;padding:20px;margin:30px auto;background-color:#fff;box-shadow:3px 3px 3px rgba(0,0,0,.1);overflow:hidden}.animal h4{margin:0 0 6px 0}.pet-name{font-size:2rem;font-weight:400;margin:0 0 1rem 0}.species{font-size:.9rem;color:#888;vertical-align:middle}.foods-list{margin:0;padding:0;position:relative;left:17px;font-size:.85rem;line-height:1.5}.footer{font-size:.7rem;color:#888;text-align:center;margin:0}</style>";
		returnStr += "<script>" + js + "</script>";
		return returnStr + "</head>";
	}
}
