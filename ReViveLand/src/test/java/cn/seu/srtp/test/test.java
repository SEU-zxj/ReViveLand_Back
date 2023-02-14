package cn.seu.srtp.test;

import com.github.signaflo.timeseries.TestData;
import com.github.signaflo.timeseries.TimeSeries;
import com.github.signaflo.timeseries.forecast.Forecast;
import com.github.signaflo.timeseries.model.arima.Arima;
import com.github.signaflo.timeseries.model.arima.ArimaOrder;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class test {
    /**
     * 两个Date相减
     */
    @Test
    public void TestJavaDate()
    {
        DateFormat dft = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date star = dft.parse("2023-02-10");//开始时间
            Date endDay=dft.parse("2023-02-11");//结束时间
            Long starTime=star.getTime();
            Long endTime=endDay.getTime();
            Long num=endTime-starTime;//时间戳相差的毫秒数
            System.out.println("相差天数为："+num/24/60/60/1000);//除以一天的毫秒数
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    /**
     * 测试时间序列分析ARIMA模型
     *
     */
    @Test
    public void TestARIMA(){
        TimeSeries timeSeries = TestData.debitcards;

        ArimaOrder modelOrder = ArimaOrder.order(0, 1, 1, 0, 1, 1); // Note that intercept fitting will automatically be turned off

        Arima model = Arima.model(timeSeries, modelOrder);

        System.out.println(model.aic()); // Get and display the model AIC
        System.out.println(model.coefficients()); // Get and display the estimated coefficients
        System.out.println(java.util.Arrays.toString(model.stdErrors()));

        Forecast forecast = model.forecast(12); // To specify the alpha significance level, add it as a second argument.
        System.out.println("forecast: "+forecast);
    }

    /**
     * 首先获取文件中最新的时间maxDate
     * 测试获取四个csv文件中的maxDate
     */
    @Test
    public void TestGetMaxDate() throws ParseException {
        //四个文件可以分为两大类：运动和睡眠
        //可以通过两个文件第一行第一列的字段来分析
        //运动类为“运动类型”，睡眠为“”
        //按照步行.scv文件中的第一行date最大date

        String csvFile = "src/test/resources/analyzeFiles/Activities-walk.csv";
        String line = "";
        String cvsSplitBy = ",";

        DateFormat dft1 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        String maxDateStr = "";

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {

            if ((line = br.readLine()) != null) {

                // use comma as separator
                String[] title = line.split(cvsSplitBy);
                System.out.println(title[1]);
            }

            if ((line = br.readLine()) != null) {

                // use comma as separator
                String[] data = line.split(cvsSplitBy);
                maxDateStr = data[1];
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        Date maxDate = dft1.parse(maxDateStr);

        SimpleDateFormat dft2 = new SimpleDateFormat("yyyy-MM-dd");
        Date date1 = new Date(System.currentTimeMillis());
        System.out.println(dft2.format(date1));
        Date date2 = new Date(System.currentTimeMillis() + 1000 * 60);
        System.out.println(dft2.format(date2));
        System.out.println(dft2.format(date1).equals(dft2.format(date2)));
    }

    @Test
    public void TestIndexof(){
        String time = "01:31:56";
        String[] split = time.split(":");
        int hour = Integer.parseInt(split[0]);
        int minute = Integer.parseInt(split[1]);

        System.out.println(hour * 60 + minute);

    }
}




