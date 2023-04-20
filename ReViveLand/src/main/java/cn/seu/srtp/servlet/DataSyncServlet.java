package cn.seu.srtp.servlet;

import cn.seu.srtp.mapper.GameDataMapper;
import cn.seu.srtp.mapper.HEALTH_DATAMapper;
import cn.seu.srtp.mapper.USERMapper;
import cn.seu.srtp.pojo.Animal;
import cn.seu.srtp.pojo.HealthDataItem;
import cn.seu.srtp.pojo.MyTree;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.signaflo.math.stats.distributions.Normal;
import com.github.signaflo.timeseries.TimePeriod;
import com.github.signaflo.timeseries.TimeSeries;
import com.github.signaflo.timeseries.forecast.Forecast;
import com.github.signaflo.timeseries.model.arima.Arima;
import com.github.signaflo.timeseries.model.arima.ArimaOrder;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Test;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;


@WebServlet("/dataSync")
public class DataSyncServlet extends MyHttpServlet{

    private static final long serialVersionUID = -4187075130535308117L;
    private boolean isMultipart = false;

    //设置整个传输文件的请求包的大小
    private int maxFileSize = 1024 * 1024 * 10; //10MB
    //设置允许文件占用的最大空间（磁盘上）
    private int maxMemSize = 100 * 1024;        //100MB
    //记录已经存储的文件的数量，到4个之后再处理
    private int fileCount = 0;
    //文件数量是否已经到达了4个
    private boolean full = false;
    //存储uuid化之前的名称
    private List<String> oldNames = new ArrayList<>();
    //存储uuid化之后的名称
    private List<String> newNames = new ArrayList<>();

    private String LoginUUID = "";
    /**
     * 衡量用户睡眠情况和运动情况的比率
     * 新用户：达到推荐值的天数占总统计天数的比率
     * 老用户：达到预测值的天数占总统计天数的比率
     */
    private double judgeRatio = 0.7;

    private int processNum = 0;
    //对于老用户：新增的动物数量和树的数量
    private int animalNum = 0;
    private int treeNum = 0;
    //生成树和动物的概率
    private double P_tree = 0.1;
    private double P_animal = 0.01;
    //完成目标之后的奖励系数
    private double a = 0.2;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int num = processNum++;
        System.out.println("我是进程"+num+"号");
        //发送响应数据，注意处理跨域请求问题
        String scheme = request.getScheme();//返回前后端通信的协议（http,https,ftp...)
        String ip = request.getRemoteAddr();//返回发出请求的IP地址
        String host = request.getRemoteHost();//返回发出请求的客户机的主机名
        int port =request.getRemotePort();//返回发出请求的客户机的端口号

        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Allow-Origin", scheme+"://"+ip+":8081");
        response.setHeader("Content-type", "application/json");
        String result = "";
        String processInfo = "";

        if(full){
            result = "fail";
            processInfo = "file number is bigger than 5";
            JSONObject responseInfo = new JSONObject();
            responseInfo.put("result", result);
            responseInfo.put("info", processInfo);
            PrintWriter writer = response.getWriter();
            writer.write(JSON.toJSONString(responseInfo));
            return;
        }

        // 检查是否全部都是文件上传请求
        isMultipart = ServletFileUpload.isMultipartContent(request);

//        response.setContentType("text/html;charset=utf-8");
        if (!isMultipart) {
            result = "fail";
            processInfo = "not multipart";
            JSONObject responseInfo = new JSONObject();

            responseInfo.put("result", result);
            responseInfo.put("info", processInfo);

            PrintWriter writer = response.getWriter();

            writer.write(JSON.toJSONString(responseInfo));

            return;
        }
        DiskFileItemFactory factory = new DiskFileItemFactory();
        // 文件大小的最大值将被存储在内存中，最大允许100MB
        factory.setSizeThreshold(maxMemSize);

        //设置小文件的存储路径
        String uploadPath = getServletContext().getRealPath("/uploadFiles") + "/";
        File uploadFile = new File(uploadPath);
        if(!uploadFile.exists()){
            uploadFile.mkdir();
        }

        // Location to save data that is larger than maxMemSize.
        String tempPath = getServletContext().getRealPath("/tempFiles") + "/";
        File tempFile = new File(tempPath);
        if(!tempFile.exists()){
            tempFile.mkdir();
        }
        //设置比较大的文件在磁盘中临时存放的路径
        factory.setRepository(tempFile);

        // System.out.println(path);
        // 创建一个文件上传处理对象
        ServletFileUpload upload = new ServletFileUpload(factory);
        // 允许上传的文件的最大值，10MB
        upload.setSizeMax(maxFileSize);


        try {
            // 解析请求，获取文件项
            List<FileItem> items = upload.parseRequest(request);
            // 处理上传的文件项
            Iterator<FileItem> iter = items.iterator();

            while (iter.hasNext()) {
                FileItem item = iter.next();
                if (item.isFormField()) {
                    //这里表示是表单类型数据上传，但我们这里明显上传的是文件
                    String name = item.getFieldName();
                    String value = item.getString();
                    LoginUUID = value;
                } else {
                    // 获取上传的文件

                    //======前端限制每次只能上传四个，文件名都规范=======
                    //1. 根据文件后缀确认是否进行后续处理
                    //      1.1 后缀为csv，进行后续步骤
                    //      1.2 后缀不是csv，continue下一个文件
                    //2. 将文件保存到指定的路径下，UUID文件重命名
                    //   记录文件路径和原来的名称；否则直接记录路径和原来的名字
                    //3. 保存文件
                    //4. 处理文件，并返回信息
                    String fieldName = item.getFieldName();
                    String oldFileName = item.getName();
                    String contentType = item.getContentType();
                    boolean isInMemory = item.isInMemory();
                    long sizeInBytes = item.getSize();

                    if(contentType.equals("text/csv")){
                        String newFileName = UUID.randomUUID().toString() + ".csv";
                        String uploadFilePath = uploadPath + newFileName;
                        File saveFile = new File(uploadFilePath);

                        item.write(saveFile);
                        item.delete();

                        //成功上传文件
                        fileCount++;
                        System.out.println("目前已经成功上传" + fileCount + "个文件");

                        oldNames.add(oldFileName);
                        newNames.add(newFileName);

                        System.out.println(uploadFilePath);
                        System.out.println(oldNames);
                        System.out.println(newNames);

                    }else{
                        result = "fail";
                        processInfo = "not csv";
                        JSONObject responseInfo = new JSONObject();

                        responseInfo.put("result", result);
                        responseInfo.put("info", processInfo);

                        PrintWriter writer = response.getWriter();

                        writer.write(JSON.toJSONString(responseInfo));
                        return;
                    }
                }
            }
        } catch (Exception ex) {
            System.out.println("ex:" + ex.getMessage());
            result = "exception";
            JSONObject responseInfo = new JSONObject();

            responseInfo.put("result", result);
            responseInfo.put("info", processInfo);

            PrintWriter writer = response.getWriter();

            writer.write(JSON.toJSONString(responseInfo));
            return;
        }

        //进行文件的处理
        //首先获取文件中最新的时间maxDate，当前系统时间curDate和用户上次更新时间lastUpdate
        //1. 限制maxDate = curDate 数据必须是最新的
        //2. maxDate 必须大于 lastDate，如果相等，说明用户的信息已经是最新数据，不需要再更新
        //3. 生成二维数组（行数=maxDate - lastDate,列数 = 日期，用户名，步行距离，步行时间，跑步时间，呼吸训练时间，睡眠时间）
        //  3.1 使用ARIMA模型进行预测得到当前date的预测值
        //  3.2 根据算法计算出获得的种子数量和动物数量，求和
        //  3.3 插入数据到HEALTH_DATA表
        //4. 插入更新的动物和植物
        //5. 统计运动和随眠信息，更新用户的睡眠状态和运动状态

        if(fileCount == 5 && !full){
            full = true;
            System.out.println("我是进程"+num+"号，我进入了处理阶段，考虑杀死我？");
            System.out.println("文件准备完成，开始分析");
            //利用步行文件获取maxDate
            String walkingFile = uploadPath + newNames.get(oldNames.indexOf("Activities-walk.csv"));
            String line = "";
            String cvsSplitBy = ",";
            String maxDateStr = "";
            String minDateStr = "";
            //获取当前时间
            Date curDate = new Date(System.currentTimeMillis());
            Date maxDate, minDate;
            //df1用于初始化Date对象
            //df2用于存储时间和进行比较
            DateFormat dft1 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            DateFormat dft2 = new SimpleDateFormat("yyyy-MM-dd");
            //读取csv文件
            try{
                BufferedReader br = new BufferedReader(new FileReader(walkingFile));
                //第一行是标题行，不管
                line = br.readLine();

                if ((line = br.readLine()) != null) {
                    // use comma as separator
                    String[] data = line.split(cvsSplitBy);
                    maxDateStr = data[1];
                }
                maxDate = dft1.parse(maxDateStr);
                maxDate = dft2.parse(dft2.format(maxDate));

                minDateStr = maxDateStr;
                minDate = maxDate;
                while((line = br.readLine()) != null){
                    String[] data = line.split(cvsSplitBy);
                    minDateStr = data[1];
                }
                minDate = dft1.parse(minDateStr);
                minDate = dft2.parse(dft2.format(minDate));

                br.close();

                //比较当前日期与步行表中的日期是否是同一天
                if(dft2.format(curDate).equals(dft2.format(maxDate))){
                    //进行更新操作
                    System.out.println("更新文件的数据");
                    //1. 获取用户的lastUpdate数据
                    //2. 生成二维表格
                    //  2.1 若用户是新用户，则lastUpdate为默认2020-01-01，需要特殊考虑
                    //      从maxDate-1往前推，先把步行表弄完，其他的按照步行表的时间进行更新
                    //  2.2 若用户是老用户，直接生成lastUpdate至maxDate的表格即可（不包括maxDate）
                    //3. 插入数据库
                    //4. 进行分析

                    //查询lastUpDate
                    //1. 获取sqlSessionFactory对象
                    String resource = "mybatis-config.xml";
                    InputStream inputStream = Resources.getResourceAsStream(resource);
                    SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
                    //2. 获取sqlSession对象
                    SqlSession sqlSession = sqlSessionFactory.openSession();
                    //3. 获取对应Mapper接口的代理对象
                    USERMapper userMapper = sqlSession.getMapper(USERMapper.class);
                    //4. 执行对应的sql语句
                    Date lastUpdate = userMapper.GetLastUpdate(LoginUUID);
                    System.out.println(lastUpdate);
                    //5. 释放资源
                    sqlSession.close();

                    System.out.println("用户的lastUpdate是"+ dft2.format(lastUpdate));

                    //***************************************************************//
                    //根据用户是第一次上传数据（新用户）还是老用户，分别统计出一个二维表格
                    //为了方便之后更新数据库HEALTH_DATA表
                    List<HealthDataItem> dataList = new ArrayList<HealthDataItem>();
                    //表中的项：
                    //时间 用户 步行距离 步行时间 跑步时间 呼吸训练时间 睡眠时间

                    //分为两步处理：
                    // 1. 将数据插入程序中的数据表
                    // 2. 对数据进行分析

                    if(dft2.format(lastUpdate).equals("2020-01-01")){
                        //新用户：插入从minDate到maxDate的数据
                        InsertIntoDataList(dataList, minDate, maxDate);
                        AnalyzeData_NonPredict();
                        WriteListToDB(dataList);
                    }else{
                        //老用户（最近更新过）：插入从lastUpdate到maxDate的数据
                        if(lastUpdate.before(maxDate) && lastUpdate.after(minDate)){
                            InsertIntoDataList(dataList, lastUpdate, maxDate);
                            AnalyzeData_Predict();
                            WriteListToDB(dataList);
                        }else{
                            //老用户（很久之前更新）：插入从minDate到maxUpdate的数据
                            InsertIntoDataList(dataList, minDate, maxDate);
                            AnalyzeData_Predict();
                            WriteListToDB(dataList);
                        }
                    }

                    //返回结果
                    result = "success";
                    processInfo = "data is update";
                    JSONObject responseInfo = new JSONObject();

                    responseInfo.put("result", result);
                    responseInfo.put("info", processInfo);

                    PrintWriter writer = response.getWriter();

                    writer.write(JSON.toJSONString(responseInfo));
                    return;

                }else{
                    System.out.println("失败，时间不同步，理由如下：");
                    System.out.println(dft2.format(curDate));
                    System.out.println(dft2.format(maxDate));
                    result = "fail";
                    processInfo = "data is not real time";
                    JSONObject responseInfo = new JSONObject();

                    responseInfo.put("result", result);
                    responseInfo.put("info", processInfo);

                    PrintWriter writer = response.getWriter();

                    writer.write(JSON.toJSONString(responseInfo));
                    return;
                }
            } catch (Exception e) {
                result = "exception";
                JSONObject responseInfo = new JSONObject();

                responseInfo.put("result", result);
                responseInfo.put("info", processInfo);

                PrintWriter writer = response.getWriter();

                writer.write(JSON.toJSONString(responseInfo));
                return;
            }
        }


    }

    /**
     * 将从文件读入的健康数据写入数据库
     * @param dataList
     */
    private void WriteListToDB(List<HealthDataItem> dataList) throws IOException {
        //1. 获取sqlSessionFactory对象
        String resource = "mybatis-config.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        //2. 获取sqlSession对象
        SqlSession sqlSession = sqlSessionFactory.openSession();
        //3. 获取对应Mapper接口的代理对象
        HEALTH_DATAMapper dataMapper = sqlSession.getMapper(HEALTH_DATAMapper.class);
        //4. 执行对应的sql语句
        //从后往前插入，日期从小到大
        for(int i = dataList.size() - 1 ; i > 0 ; i--){
            dataMapper.InsertHealthDataItem(LoginUUID, dataList.get(i));
        }
        sqlSession.commit();
        //5. 释放资源
        sqlSession.close();
    }

    private void AnalyzeData_Predict() {
        System.out.println("基于预测的数据分析");
    }

    private void AnalyzeData_NonPredict() {
        System.out.println("基于非预测的数据分析");
    }

    private void InsertIntoDataList(List<HealthDataItem> dataList, Date lower, Date upper) throws IOException, ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        int itemCnt = (int) ((upper.getTime() - lower.getTime()) / 1000 / 24 / 60 / 60  + 1);
        for(int i = 0 ; i < itemCnt ; i++){
            HealthDataItem dataItem = new HealthDataItem();
            dataItem.setTime(new Date(upper.getTime() - (long) i * 1000 * 24 * 60 * 60));
            dataItem.setSleepTime(0);
            dataItem.setBreathExTime(0);
            dataItem.setRunTime(0);
            dataItem.setWalkTime(0);
            dataItem.setWalkingDistance(0.0);
            dataItem.setWalkSteps(0);
            dataList.add(dataItem);
        }

        String uploadPath = getServletContext().getRealPath("/uploadFiles") + "/";
        String cvsSplitBy = ",";

        String walkingFile = uploadPath + newNames.get(oldNames.indexOf("Activities-walk.csv"));
        String runningFile = uploadPath + newNames.get(oldNames.indexOf("Activities-run.csv"));
        String breathFile = uploadPath + newNames.get(oldNames.indexOf("Activities-breath.csv"));
        String sleepingFile = uploadPath + newNames.get(oldNames.indexOf("睡眠.csv"));
        String walkingStepsFile = uploadPath + newNames.get(oldNames.indexOf("步数.csv"));

        BufferedReader br = new BufferedReader(new FileReader(walkingFile));
        //======================walkingFile=======================//
        String line = br.readLine();
        while((line = br.readLine()) != null){
            String[] data = line.split(cvsSplitBy);
            data[4] = data[4].replace("\"", "");
            data[6] = data[6].replace("\"", "");
            int index = getListIndex(upper, lower, sdf.parse(data[1]));
            if(index != -1){
                dataList.get(index).addWalkingDistance(Double.parseDouble(data[4]));//设置步行距离
                dataList.get(index).addWalkTime(StringToMinute(data[6]));//设置步行时间
            }
        }
        br.close();
        //======================runningFile=======================//
        br = new BufferedReader(new FileReader(runningFile));
        //第一行是标题
        line = br.readLine();
        while((line = br.readLine()) != null){
            String[] data = line.split(cvsSplitBy);
            data[6] = data[6].replace("\"", "");
            int index = getListIndex(upper, lower, sdf.parse(data[1]));
            if(index != -1){
                dataList.get(index).addRunTime(StringToMinute(data[6]));
            }
        }
        br.close();
        //====================breathFile=============================//
        br = new BufferedReader(new FileReader(breathFile));
        //第一行是标题
        line = br.readLine();
        while((line = br.readLine()) != null){
            String[] data = line.split(cvsSplitBy);
            data[4] = data[4].replace("\"", "");
            int index = getListIndex(upper, lower, sdf.parse(data[1]));
            if(index != -1){
                dataList.get(index).addBreathExTime(StringToMinute(data[4]));
            }
        }
        br.close();
        //======================sleepingFile==========================//
        br = new BufferedReader(new FileReader(sleepingFile));
        //第一行是标题
        line = br.readLine();
        while((line = br.readLine()) != null){
            String[] data = line.split(cvsSplitBy);
            data[0] = data[0].replace("\"", "");
            data[1] = data[1].replace("\"", "");
            int index = getListIndex_Sleep(upper, lower, SleepDateTransfer(data[0]));
            if(index != -1){
                dataList.get(index).setSleepTime(SleepHourToMinute(data[1]));
            }
        }
        br.close();
        //======================walkingStepsFile=========================//
        br = new BufferedReader(new FileReader(walkingStepsFile));
        //第一行是标题
        line = br.readLine();
        while((line = br.readLine()) != null){
            String[] data = line.split(cvsSplitBy);
            data[0] = data[0].replace("\"", "");
            data[1] = data[1].replace("\"", "");
            int index = getListIndex_Sleep(upper, lower, SleepDateTransfer(data[0]));
            if(index != -1){
                dataList.get(index).setWalkSteps(Integer.parseInt(data[1]));
            }
        }
        br.close();
    }

    private int StringToMinute(String time){
        //我们只需要“xx:xx:xx”类型的，数据长度为8
        time = time.substring(0, 8);
        String[] split = time.split(":");
        int hour = Integer.parseInt(split[0]);
        int minute = Integer.parseInt(split[1]);
        int second = Integer.parseInt(split[2]);

        if(second >= 30)
            return hour * 60 + minute + 1;
        else
            return hour * 60 + minute;
    }

    /**
     * 根据最大日期和最短日期判断出当前计算日期的索引（假设最大日期索引为0）
     * @param maxDate
     * @param minDate
     * @param calcDate
     * @return 返回list中的索引，若超出范围，返回-1
     */
    private int getListIndex(Date maxDate, Date minDate, Date calcDate){
        if(calcDate.after(maxDate) || calcDate.before(minDate)){
            return -1;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return (int) ((maxDate.getTime() - calcDate.getTime()) / 1000 / 60 / 60 / 24);
    }

    /**
     * 将睡眠表中的时间转化为"xx-xx"的形式
     * @param oldDate
     * @return
     */
    private String SleepDateTransfer(String oldDate){
        String[] times = oldDate.split("月 ");
        String month = times[0];
        String day = times[1];
        if(Integer.parseInt(month) < 10) month = "0" + month;
        if(Integer.parseInt(day) < 10) day = "0" + day;
        return month + '-' + day;
    }

    /**
     * 将睡眠时间转化为分钟数
     * @param rawData
     * @return
     */
    private int SleepHourToMinute(String rawData){
        rawData = rawData.replace(" 小时", "");
        String[] split = rawData.split(":");
        int hour = Integer.parseInt(split[0]);
        int minute = Integer.parseInt(split[1]);

        return hour * 60 + minute;
    }

    /**
     * 对于时间缺少年份的睡眠数据，根据月和日期进行匹配，获取在List中的下标
     * @param maxDate
     * @param minDate
     * @param clacDate
     * @return 如果匹配成功，返回列表中的index 如果匹配失败，返回-1
     */
    private int getListIndex_Sleep(Date maxDate, Date minDate, String clacDate){
        int length = (int) ((maxDate.getTime() - minDate.getTime()) / 1000 / 60 / 60 / 24) + 1;
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd");
        int count;
        Date date;
        for(count = 0 ; count < length ; count ++){
            date = new Date(maxDate.getTime() - (long) count * 1000 * 60 * 60 * 24);
            if(dateFormat.format(date).equals(clacDate)){
                return count;
            }
        }
        //如果遍历完了都没找到，说明没有对应的项
        return -1;
    }

    /**
     * 给定一个数组，返回下一个预测值 如果数据量 >=14 将使用ARIMA模型 若 < 14 将使用指数平滑
     * @param list
     * @return
     */
    private Double GetPrediction_Int(List<Integer> list){

        double e = 0.5;//设置指数平滑的平滑系数

        if(list.size() < 15){
            //观测数据太少，无法用ARIMA预测
            //利用指数平滑法进行预测
            //越靠近预测点，对预测结果的影响权重就越大
            double res = 0.0;
            //反着计算
            //越靠近下标0的，越再前算，对结果的影响也就越小
            for (int i = 0 ; i <= list.size() - 1 ; i++) {
                res = e * res + (1 - e) * list.get(i);
            }
            System.out.println(res);
            return res + a;
        }else{
            //观测数据足够，可以尝试使用ARIMA模型进行预测

            //把观测值转为数组
            double[] observations = new double[list.size()];
            for (int i = 0 ; i <= list.size() - 1 ; i++) {
                observations[i] = list.get(i);
            }

            // Second, we'll create a daily time series from those values.
            TimePeriod day = TimePeriod.oneDay();

            TimeSeries series = TimeSeries.from(day, observations);

            // Third, we'll create an ArimaOrder with a seasonal component.
            ArimaOrder order = ArimaOrder.order(0, 0, 0, 1, 1, 1);

            // Fourth, we create an ARIMA model with the series, the order,
            // and the weekly seasonality.

            TimePeriod week = TimePeriod.oneWeek();

            Arima model = Arima.model(series, order, week);

            // Finally, generate a forecast for next week using the model

            Forecast forecast = model.forecast(1);

            return forecast.pointEstimates().at(0) + a;
        }
    }

    private Double GetPrediction_Double(List<Double> list){

        double e = 0.5;//设置指数平滑的平滑系数

        if(list.size() < 15){
            //观测数据太少，无法用ARIMA预测
            //利用指数平滑法进行预测
            //越靠近预测点，对预测结果的影响权重就越大
            double res = 0.0;
            //反着计算
            //越靠近下标0的，越再前算，对结果的影响也就越小
            //数据库中每条数据都是预测值！都要参与运算
            for (int i = 0 ; i <= list.size() - 1 ; i++) {
                res = e * res + (1 - e) * list.get(i);
            }
            System.out.println(res);
            return res + a;
        }else{
            //观测数据足够，可以尝试使用ARIMA模型进行预测

            //把观测值转为数组
            double[] observations = new double[list.size()];
            for (int i = 0 ; i <= list.size() - 1 ; i++) {
                observations[i] = list.get(i);
            }

            // Second, we'll create a daily time series from those values.
            TimePeriod day = TimePeriod.oneDay();

            TimeSeries series = TimeSeries.from(day, observations);

            // Third, we'll create an ArimaOrder with a seasonal component.
            ArimaOrder order = ArimaOrder.order(0, 0, 0, 1, 1, 1);

            // Fourth, we create an ARIMA model with the series, the order,
            // and the weekly seasonality.

            TimePeriod week = TimePeriod.oneWeek();

            Arima model = Arima.model(series, order, week);

            // Finally, generate a forecast for next week using the model

            Forecast forecast = model.forecast(1);


            return forecast.pointEstimates().at(0)+a;
        }
    }

    private void GenerateGameObjects(String uuid, int treeNum, int animalNum, SqlSession sqlSession){

        //3. 获取对应Mapper接口的代理对象
        GameDataMapper gameMapper = sqlSession.getMapper(GameDataMapper.class);

        Random r = new Random();
        if(treeNum != 0) {
            MyTree tree = new MyTree();
            for(int i = 0 ; i < treeNum ; i++){
                tree.setGrowDegree(0);
                tree.setPos_x(2000 * r.nextFloat() -1000);
                tree.setPos_y(2000 * r.nextFloat() -1000);
                tree.setPos_z(2000 * r.nextFloat() -1000);
                tree.setType(r.nextInt(9) + 1);
                //4. 执行对应的sql语句
                gameMapper.InsertTree(uuid, tree);
            }
        }

        if(animalNum != 0){
            Animal animal = new Animal();
            for(int i = 0 ; i < animalNum ; i++){
                animal.setType(r.nextInt(4) + 1);
                //4. 执行对应的sql语句
                gameMapper.InsertAnimal(uuid, animal);
            }
        }
        sqlSession.commit();
        //5. 释放资源
        sqlSession.close();
    }

    @Test
    public void TestPrediction(){
        List<Integer> list = new ArrayList<Integer>();
        list.add(1);
        list.add(2);
        list.add(3);
        list.add(4);
        list.add(5);
        list.add(6);
        list.add(7);
        list.add(3);
        list.add(4);
        list.add(5);
        list.add(6);
        list.add(7);
        list.add(8);

        GetPrediction_Int(list);
    }

    @Test
    public void Test2(){
        Normal normal = new Normal(); // Create a normal distribution with mean 0 an sd of 1.

        double[] values = new double[14];
        for (int i = 0; i < values.length; i++) {
            values[i] = normal.rand();
        }

// Assumes Monday corresponds to 0.
        for (int fri = 4; fri < values.length; fri += 7) {
            values[fri] += 1.0;
            values[fri + 1] += 2.0;
            values[fri + 2] -= 1.0;
        }
// Second, we'll create a daily time series from those values.
        TimePeriod day = TimePeriod.oneDay();

        TimeSeries series = TimeSeries.from(day, values);

// Third, we'll create an ArimaOrder with a seasonal component.
        ArimaOrder order = ArimaOrder.order(0, 0, 0, 1, 1, 1);

// Fourth, we create an ARIMA model with the series, the order,
// and the weekly seasonality.

        TimePeriod week = TimePeriod.oneWeek();

        Arima model = Arima.model(series, order, week);

// Finally, generate a forecast for next week using the model

        Forecast forecast = model.forecast(10);
        System.out.println(forecast);
    }
}

