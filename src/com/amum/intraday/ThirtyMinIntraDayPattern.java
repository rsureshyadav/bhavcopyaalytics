package com.amum.intraday;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.amum.sma.FileWrite;
import com.amum.util.AmumUtil;

public class ThirtyMinIntraDayPattern {
	static DecimalFormat df = new DecimalFormat("###.##");

	public static void main(String[] args) throws IOException, InterruptedException {


		System.out.println("Execution Started......");
		long startTime = System.currentTimeMillis();
		Properties prop = new Properties();
		InputStream input = null;
		List<String> outputList = new ArrayList<>();
		//read the config file
		input = new FileInputStream("conf/config.properties");
		prop.load(input);
		List<String> symbolItems = Arrays.asList(prop.getProperty("symbol").split("\\s*,\\s*"));
		int period = Integer.parseInt(prop.getProperty("intraday.period"));
		String deliveryMode = prop.getProperty("delivery.mode");
		List<String> inputFileList = AmumUtil.getLatestInputFileList(period);
		String header="SYMBOL,STOCK_STATUS,NEWS_STATUS,BUY_PRICE,SELL_PRICE";
		outputList.add(header);
		int count=0;
		int fileRowSplitCout=1;
		boolean isFileRowSplitCount=false;

		int fileRowSplit= Integer.parseInt(prop.getProperty("file.row.split"));
		int splitCount=1;
		for(String symbol :  symbolItems){
			if(!symbol.contains("-")){
				System.out.println(LocalDateTime.now()+"==> Executing ("+(symbolItems.size() - count) +") ==> "+symbol);
				Map<String, List<Double>> lastTwoPriceMap  = IntradayEngine.getLastTwoPriceInfo(inputFileList,symbol,deliveryMode);
				List<Double> priceList = lastTwoPriceMap.get("CLOSE_PRICE");

				if(!priceList.isEmpty()){
					String stockStatus = IntradayEngine.stockPriceSentiment(priceList);
					String newsSentiment = IntradayEngine.getNewsSentiment(prop,symbol);
					Map<String,Double> buySellMap = IntradayEngine.getBuySellPrice(lastTwoPriceMap);
					outputList.add(symbol+","+stockStatus+","+newsSentiment+","+df.format(buySellMap.get("BUY_PRICE"))+","+df.format(buySellMap.get("SELL_PRICE")));
				}
				count++;
				if(symbolItems.size()>=fileRowSplit){
					if(fileRowSplitCout==fileRowSplit){
						isFileRowSplitCount=true;
					}
					if(isFileRowSplitCount){
						isFileRowSplitCount=false;
						fileRowSplitCout=0;
						FileWrite.executeIntradaySplit(prop, outputList,splitCount);
						splitCount++;
					}
					fileRowSplitCout++;
				}
			}
		}
		if(symbolItems.size()<=fileRowSplit){
			FileWrite.executeIntraday(prop, outputList);
		}else{
			FileWrite.executeIntraday(prop, splitCount-1);
		}
		AmumUtil.executionTime(startTime);
		System.out.println("Execution Completed......");
	}

}
