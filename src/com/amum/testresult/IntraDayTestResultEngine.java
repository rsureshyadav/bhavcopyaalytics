package com.amum.testresult;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.json.JSONException;
import org.json.JSONObject;

import com.amum.util.AmumUtil;
import com.amum.util.OutputCSVWriter;

public class IntraDayTestResultEngine {

	
	public static DecimalFormat df = new DecimalFormat("###.##");
	public static Map<String, JSONObject> jsonMap = new HashMap<>();
	public static String execute(List<String> inputList) throws IOException{
		StringBuffer buffer = new StringBuffer();
		List<String> newsList;
		Properties prop = new Properties();
		InputStream input = null;
		input = new FileInputStream("conf/config.properties");
		prop.load(input);
		
			for(String symbol :inputList){
					System.out.println("==>"+symbol);
						String jsonString = getJsonObjectInfo(symbol);
						System.out.println("jsonString>>>"+jsonString);
						JSONObject jObject = null;
						try {
							if(jsonString != null){
								jObject = new JSONObject(jsonString);
								if(jObject != null && jObject.getString("l") != null){
								double last_price = Double.parseDouble(jObject.getString("l"));
							 	double prev_close_price = 0;
							 	String pc = jObject.getString("c");
							 	if(pc.contains("-")){
							 		String value = 	pc.replace("-", "");
								 	prev_close_price = Double.parseDouble(value);
								 	prev_close_price = last_price + prev_close_price;
								// 	System.out.println(prev_close_price);	
							 	}else{
							 	String value = 	pc.replace("+", "");
							 	prev_close_price = Double.parseDouble(value);
							 	prev_close_price = last_price - prev_close_price;
							 	//System.out.println(prev_close_price);
							 	}
								//System.out.println("jObject>>>"+jObject);
								//double last_price = Double.parseDouble(jObject.getString("l").replace(",", ""));
								//double prev_close_price = Double.parseDouble(jObject.getString("pcls_fix").replace(",", ""));
								double profitOrLoss = last_price - prev_close_price;
								String volume =jObject.getString("vo");
								volume=volume.replace(",", "");
								if(volume.contains("M")){
									buffer.append("<tr><td>"+symbol+"</td><td>"+last_price+"</td><td>"+df.format(profitOrLoss)+"</td><td>"+volume+"</td></tr>");
								}
							  }
							}
						} 
						catch (JSONException e) {
							e.printStackTrace();
						}
			}
		return buffer.toString();
	}

		
	public static void main(String str[]) throws IOException{
		System.out.println("Execution Started......");
		long startTime = System.currentTimeMillis();
		
		Properties prop = new Properties();
		InputStream input = null;
		input = new FileInputStream("conf/config.properties");
		prop.load(input);
		List<String> outputList = new ArrayList<>();
		List<String> inputList;
		List<String> fileNameList = Arrays.asList(prop.getProperty("test.summary.intraday.name").split("\\s*,\\s*"));
		String headerName = "CURRENT_PRICE,UP_DOWN_AMOUNT,VOLUME,SYMBOL,STOCK_STATUS,NEWS_STATUS,BUY_PRICE,SELL_PRICE";
		outputList.add(headerName);
		inputList = getSymbol(prop,fileNameList.get(0));
			for(String line :inputList){
				String inputArray[] = line.split("\\s*,\\s*");
				if(inputArray.length>1){
					System.out.println("==>"+line);
					String isNull = inputArray[1];
					if(!isNull.equalsIgnoreCase("NULL")){
						String jsonString = getJsonObjectInfo(inputArray[0].toString());
						
						JSONObject jObject = null;
						try {
							if(jsonString != null){
								jObject = new JSONObject(jsonString);
								if(jObject.getString("l") != null&& jObject.getString("l").length()>0){
							 	double last_price = Double.parseDouble(jObject.getString("l"));
							 	double prev_close_price = 0;
							 	String pc = jObject.getString("c");
							 	if(pc.contains("-")){
							 		String value = 	pc.replace("-", "");
								 	prev_close_price = Double.parseDouble(value);
								 	prev_close_price = last_price + prev_close_price;
								// 	System.out.println(prev_close_price);	
							 	}else{
							 	String value = 	pc.replace("+", "");
							 	prev_close_price = Double.parseDouble(value);
							 	prev_close_price = last_price - prev_close_price;
							 	//System.out.println(prev_close_price);
							 	}
							 	
								//double last_price = Double.parseDouble(jObject.getString("l").replace(",", ""));
								//double prev_close_price = Double.parseDouble(jObject.getString("pcls_fix").replace(",", ""));
								double profitOrLoss = last_price - prev_close_price;
								String volume =jObject.getString("vo");
								volume=volume.replace(",", "");
								outputList.add(last_price+","+profitOrLoss+","+volume+","+line );
								}
							}
						} 
						catch (JSONException e) {
							e.printStackTrace();
						}
					}
				}
			}
			
			String testPath = prop.getProperty("file.summary.path")+"/TEST_RESULT/"+LocalDate.now();
			
		OutputCSVWriter.writeToCsvTestResultSummary(testPath, outputList,"thirtymin_intraday_summary");
		AmumUtil.executionTime(startTime);
		System.out.println("Execution Completed......");
	}


	private static String getJsonObjectInfo(String symbol) throws IOException {
		String jsonString = null;
		if(symbol != null && symbol.length()>0){
			if(symbol.contains("&")){
				symbol = symbol.replace("&", "%26");
			}
			//RETIRED - String url ="http://www.google.com/finance/info?infotype=infoquoteall&q=NSE:"+symbol;
			String url ="https://finance.google.com/finance?output=json&q="+symbol;
			System.out.println("Executing>>>"+url);
			
			jsonString = downloadFileFromInternet(url);
			if(jsonString != null ){
				//jsonString =jsonString.replace("// [", "");
				//jsonString =jsonString.replace("]", "");	
				jsonString = jsonString.replaceAll("\\s+","");
				jsonString =jsonString.replace("//[{", "{");
				jsonString =jsonString.replace("}]}]", "}]}");
			}
			//System.out.println("jsonString111>>>"+jsonString);
		}
		return jsonString;
	}

	private static List<String>  getSymbol(Properties prop, String fileName) throws IOException {
		String outputPath=prop.getProperty("file.summary.path");
        String readFileName = outputPath+"/"+LocalDate.now()+"/"+fileName;
        List<String> list = new ArrayList<>();
        try (Stream<String> stream = Files.lines(Paths.get(readFileName))) {

        	list = stream
					.filter(line -> !line.startsWith("SYMBOL"))
					.map(String::toUpperCase)
					.collect(Collectors.toList());

		} catch (IOException e) {
			e.printStackTrace();
		}
        return list;
		}


	private static String downloadFileFromInternet(String httpUrl) throws IOException {
		String response = null;
			URL url = new URL(httpUrl);
			 HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			 System.out.println("conn.getResponseCode()>>>>"+conn.getResponseCode());
			 if (conn.getResponseCode() == 200) {
				 try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
					 response = reader.lines().collect(Collectors.joining("\n"));
				 }
			 }
		return response;
	}
}
