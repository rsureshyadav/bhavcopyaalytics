package com.amum.download;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.amum.source.InputSourceOneModeling;
import com.amum.util.AmumUtil;

public class DailyReportDownloader {

    public static void main(String[] args) throws IOException, ParseException {
        List<String> urlList = new ArrayList<>();
        Properties prop = new Properties();
        InputStream input = null;
        input = new FileInputStream("conf/config.properties");
        prop.load(input);
        
        urlList=urlBuilder(prop);
        urlDownloader(urlList,prop);
        
    }

    private static void urlDownloader(List<String> urlList,Properties prop) throws ParseException {
        String saveDir=prop.getProperty("dailyrprt.dest.dir");
        System.out.println("saveDir>>>>"+saveDir);
        for(String targetURL:urlList){
            System.out.println(targetURL);
                try {
                    downloadFile(targetURL, saveDir);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
        }
        
    }
    public static void downloadFile(String fileURL, String saveDir)
            throws IOException, ParseException {

        URL url = new URL(fileURL);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        int responseCode = httpConn.getResponseCode();
        int BUFFER_SIZE = 4096;

        // always check HTTP response code first
        if (responseCode == HttpURLConnection.HTTP_OK) {
            String fileName = "";
            String disposition = httpConn.getHeaderField("Content-Disposition");
            String contentType = httpConn.getContentType();
            int contentLength = httpConn.getContentLength();
 
            if (disposition != null) {
                // extracts file name from header field
                int index = disposition.indexOf("filename=");
                if (index > 0) {
                    fileName = disposition.substring(index + 10,
                            disposition.length() - 1);
                }
            } else {
                // extracts file name from URL
                fileName = fileURL.substring(fileURL.lastIndexOf("/") + 1,
                        fileURL.length());
            }
 
            System.out.println("Content-Type = " + contentType);
            System.out.println("Content-Disposition = " + disposition);
            System.out.println("Content-Length = " + contentLength);
            System.out.println("fileName = " + fileName);
 
            // opens input stream from the HTTP connection
            InputStream inputStream = httpConn.getInputStream();
            AmumUtil.createDir(saveDir + File.separator +LocalDate.now());
            String saveFilePath = saveDir + File.separator +LocalDate.now()+ File.separator + fileName;
             System.out.println(">>>>"+saveFilePath);
            // opens an output stream to save into file
            FileOutputStream outputStream = new FileOutputStream(saveFilePath);
 
            int bytesRead = -1;
            byte[] buffer = new byte[BUFFER_SIZE];
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
 
            outputStream.close();
            inputStream.close();
 
            System.out.println("File downloaded");
            unzip(saveDir+"/"+LocalDate.now()+"/"+fileName,saveDir+"/"+LocalDate.now());
            deleteZip(saveDir+"/"+LocalDate.now()+"/"+fileName);
            executeSourceOneModeling(saveDir+"/"+LocalDate.now());
        } else {
            System.out.println("No file to download. Server replied HTTP code: " + responseCode);
        }
        httpConn.disconnect();
    }
    
    private static void executeSourceOneModeling(String inputPath) throws ParseException, IOException {
    	File f = null;
        File[] paths;
    	f = new File(inputPath);
        paths = f.listFiles();
        for(File path:paths) {
        	
        	 Map<String,String> nameMap  = InputSourceOneModeling.getS1FileName(path);
	          System.out.println(nameMap.get("FILE_NAME"));
	          InputSourceOneModeling.createOutputFile(path,nameMap.get("FILE_NAME"));
        }
		
	}

	private static void deleteZip(String filepath) {
        Path path= FileSystems.getDefault().getPath(filepath);
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
    private static void unzip(String zipFilePath, String destDir) {
        File dir = new File(destDir);
        // create output directory if it doesn't exist
        if(!dir.exists()) dir.mkdirs();
        FileInputStream fis;
        //buffer for read and write data to file
        byte[] buffer = new byte[1024];
        try {
            fis = new FileInputStream(zipFilePath);
            ZipInputStream zis = new ZipInputStream(fis);
            ZipEntry ze = zis.getNextEntry();
            while(ze != null){
                String fileName = ze.getName();
                File newFile = new File(destDir + File.separator + fileName);
                System.out.println("Unzipping to "+newFile.getAbsolutePath());
                //create directories for sub directories in zip
                new File(newFile.getParent()).mkdirs();
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
                }
                fos.close();
                //close this ZipEntry
                zis.closeEntry();
                ze = zis.getNextEntry();
            }
            //close last ZipEntry
            zis.closeEntry();
            zis.close();
            fis.close();
            System.out.println("Unzip Completed");
        } catch (IOException e) {
            e.printStackTrace();
        }
         
    }
 
    private static List<String> urlBuilder(Properties prop) throws IOException, ParseException {
        List<String> urlList = new ArrayList<>();
        long dateCount=0;
        String actualDate = AmumUtil.getLatestInputFile(prop);
        actualDate=actualDate.substring(actualDate.indexOf("cm")+2);
        actualDate=actualDate.replace("bhav.csv", "");

        Date lastDate = new SimpleDateFormat("ddMMMyyyy",Locale.ENGLISH).parse(actualDate);
        Date curDate = new Date();
        dateCount = daysBetween(lastDate,curDate);
        

        for(int i=0; i<=dateCount;i++){
            LocalDate currentDate = LocalDate.now().minusDays(i);
            DayOfWeek dow = currentDate.getDayOfWeek(); 
            
            LocalDate ld = LocalDate.of(currentDate.getYear(), currentDate.getMonth(), currentDate.getDayOfMonth());
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("ddMMMyyyy");
            String date = fmt.format(ld);
            
            DateTimeFormatter fmtMonth = DateTimeFormatter.ofPattern("MMM");
            String month = fmtMonth.format(ld);
            
          if(!dow.toString().equalsIgnoreCase("SATURDAY") && !dow.toString().equalsIgnoreCase("SUNDAY") ){
                String url = "https://www.nseindia.com/content/historical/EQUITIES/"+currentDate.getYear()+"/"+month.toUpperCase()+"/cm"+date.toUpperCase()+"bhav.csv.zip";
                urlList.add(url);
            }
        }
        return urlList;
    }
    
    private static  long daysBetween(Date one, Date two) {
        long difference =  (one.getTime()-two.getTime())/86400000;
        return Math.abs(difference);
    }



}
