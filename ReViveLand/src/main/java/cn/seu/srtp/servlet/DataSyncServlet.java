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
            System.out.println("文件数量大于4了");
            result = "fail";
            processInfo = "file number is bigger than 4";
            JSONObject responseInfo = new JSONObject();
            responseInfo.put("result", result);
            responseInfo.put("info", processInfo);

            PrintWriter writer = response.getWriter();

            writer.write(JSON.toJSONString(responseInfo));
            return;
        }
        System.out.println("当前文件数量为：" + fileCount);

        // 检查是否全部都是文件上传请求
        isMultipart = ServletFileUpload.isMultipartContent(request);

//        response.setContentType("text/html;charset=utf-8");
        if (!isMultipart) {
            System.out.println("not multi part");
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
                    System.out.println("form field");
                    System.out.println(name);
                    System.out.println(value);
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

        if(fileCount == 4 && !full){
            full = true;
            System.out.println("我是进程"+num+"号，我进入了处理阶段，考虑杀死我？");
            System.out.println("文件准备完成，开始分析");
            //利用步行文件获取maxDate
            String walkingFile = uploadPath + newNames.get(oldNames.indexOf("Activities-walk.csv"));
            String line = "";
            String cvsSplitBy = ",";
            String maxDateStr = "";
            //获取当前时间
            Date curDate = new Date(System.currentTimeMillis());
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

                Date maxDate = dft1.parse(maxDateStr);
                maxDate = dft2.parse(dft2.format(maxDate));

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
                    HealthDataItem item = new HealthDataItem();
                    //表中的项：
                    //时间 用户 步行距离 步行时间 跑步时间 呼吸训练时间 睡眠时间
                    if(dft2.format(lastUpdate).equals("2020-01-01")){
                        System.out.println("新用户更新");
                        //新用户按照walk表的时间为基准进行更新，时间只取walk表里面的时间
                        //超出的就不统计了

                        boolean isFirst = true;//是否为第一条数据

                        br.close();
                        br = new BufferedReader(new FileReader(walkingFile));
                        //第一行是标题
                        line = br.readLine();
                        //记录上次记录的时间
                        Date lastRecordDate = dft2.parse("2020-01-01");
                        while ((line = br.readLine()) != null) {
                            // use comma as separator
                            String[] data = line.split(cvsSplitBy);
                            data[4] = data[4].replace("\"", "");
                            data[6] = data[6].replace("\"", "");
                            System.out.println(data[1]+" "+data[4]+" "+data[6]);
                            //如果本行的记录时间与上一次记录的时间不一样，需要先插入上一个对象，再初始化一个新的对象
                            if(!dft2.format(dft2.parse(data[1])).equals(dft2.format(lastRecordDate))){
                                System.out.println("开始或者插入");
                                //如果不是第一条记录
                                if(!isFirst){
                                    //之前记录的天数跟现在处理的天数不同，肯定要把现在的插入再说
                                    dataList.add(item);
                                    item = new HealthDataItem();
                                    //新的记录的时间与上一条记录时间中间是不是只差了一天，如果中间隔了一天的话要新增加一天
                                    System.out.println("两天间隔为"+(lastRecordDate.getTime() - (dft2.parse(data[1]).getTime())));
                                    //如果间隔大于1天，先插入之前记录的天数，再补上差的天数，再接上后面的天数
                                    while(lastRecordDate.getTime() - (dft2.parse(data[1]).getTime()) > 1000 * 60 * 60 * 24){
                                        System.out.println("中间间隔了一天，插入");
                                        HealthDataItem tempItem = new HealthDataItem();
                                        tempItem.setTime(new Date(lastRecordDate.getTime() - 1000 * 60 * 60 * 24));
                                        tempItem.setWalkTime(0);
                                        tempItem.setWalkingDistance(0);
                                        dataList.add(tempItem);
                                        lastRecordDate.setTime(lastRecordDate.getTime() - 1000 * 60 * 60 * 24);
                                    }
                                }else{
                                    //如果是第一条数据,处理之后就不是第一条了
                                    isFirst = false;
                                }
                                System.out.println("插入后更新");
                                item.setTime(dft2.parse(data[1]));//设置时间
                                item.setWalkingDistance(Double.parseDouble(data[4]));//设置步行距离
                                item.setWalkTime(StringToMinute(data[6]));//设置步行时间
                            }else{
                                //直接在原来基础上增加即可
                                System.out.println("在原来基础上增加");
                                item.addWalkingDistance(Double.parseDouble(data[4]));
                                item.addWalkTime(StringToMinute(data[6]));
                            }
                            //更新最近一次记录的时间
                            lastRecordDate = dft2.parse(data[1]);
                            System.out.println("记录上次的时间：" + dft2.format(lastRecordDate));
                        }
                        //最后还要把最后一个对象插入
                        dataList.add(item);
                        Date minDate = item.getTime();

                        //之后依次完成跑步表，呼吸训练表和睡眠表的更新
                        //因为更新了用户表，可以知道目前记录的时间段在什么范围之内
                        //从而对剩余三个表中需要记录的数据进行判断是否需要插入表中
                        //同时也可以简单的根据跑步表中最后的时间和其他数据的时间计算出在二维数组中的索引

                        //*****************利用跑步表进行更新*******************//
                        String runningFile = uploadPath + newNames.get(oldNames.indexOf("Activities-run.csv"));
                        br.close();
                        br = new BufferedReader(new FileReader(runningFile));
                        //第一行是标题行，不管
                        line = br.readLine();
                        while ((line = br.readLine()) != null) {
                            // use comma as separator
                            String[] data = line.split(cvsSplitBy);
                            data[6] = data[6].replace("\"", "");

                            int index = getListIndex(maxDate, minDate, dft2.parse(data[1]));
                            if(index != -1){
                                dataList.get(index).addRunTime(StringToMinute(data[6]));
                            }
                        }
                        //******************利用呼吸训练表进行更新******************//
                        String breathFile = uploadPath + newNames.get(oldNames.indexOf("Activities-breath.csv"));
                        br.close();
                        br = new BufferedReader(new FileReader(breathFile));
                        //第一行是标题行，不管
                        line = br.readLine();
                        while ((line = br.readLine()) != null) {
                            // use comma as separator
                            String[] data = line.split(cvsSplitBy);
                            data[4] = data[4].replace("\"", "");

                            int index = getListIndex(maxDate, minDate, dft2.parse(data[1]));
                            if(index != -1){
                                dataList.get(index).addBreathExTime(StringToMinute(data[4]));
                            }
                        }
                        //利用睡眠表进行更新
                        //1. 获取睡眠表中的准确数据（按照二维数组中的月和日进行匹配，获得准确日期）
                        //也就是把"X月 X"转换成"xx-xx"的形式
                        //      把"xx:xx 小时"转化成分钟数的形式

                        //=====注意提前把双引号去掉====
                        String sleepFile = uploadPath + newNames.get(oldNames.indexOf("睡眠.csv"));
                        br.close();
                        br = new BufferedReader(new FileReader(sleepFile));
                        //第一行是标题行，不管
                        line = br.readLine();

                        int index = -1;
                        //因为睡眠表从上往下时间是递增的，我们先找到最小的能和list中时间匹配的数据项在list中的下标
                        while ((line = br.readLine()) != null) {
                            // use comma as separator
                            String[] data = line.split(cvsSplitBy);
                            data[0] = data[0].replace("\"", "");
                            data[1] = data[1].replace("\"", "");

                            index = getListIndex_Sleep(maxDate, minDate, SleepDateTransfer(data[0]));
                            System.out.println("找到睡眠表的index:"+index);
                            if(index != -1) {
                                //先插入当前匹配好的项
                                dataList.get(index--).addSleepTime(SleepHourToMinute(data[1]));
                                break;
                            }
                        }
                        if(index != -1){
                            //找到匹配项，可以进行匹配
                            //双指针算法：注意睡眠表的遍历是让时间逐渐增大，所以list表应该反着遍历
                            if((line = br.readLine()) != null){
                                String[] data = line.split(cvsSplitBy);
                                data[0] = data[0].replace("\"", "");
                                data[1] = data[1].replace("\"", "");
                                SimpleDateFormat dft3 = new SimpleDateFormat("MM-dd");
                                for(;index >= 0 ; index--){
                                    if(line == null) break;

                                    while(dft3.format(dataList.get(index).getTime()).equals(SleepDateTransfer(data[0]))){
                                        //找到匹配的，插入睡眠数据
                                        dataList.get(index).addSleepTime(SleepHourToMinute(data[1]));
                                        System.out.println("在"+dft2.format(dataList.get(index).getTime())+"更新睡眠时间"+SleepHourToMinute(data[1]));
                                        if((line = br.readLine()) != null){
                                            data = line.split(cvsSplitBy);
                                            data[0] = data[0].replace("\"", "");
                                            data[1] = data[1].replace("\"", "");
                                        }else{break;}
                                    }
                                }
                            }
                        }
                        //否则剩下的睡眠时间全部为零，但是不会对分析结果产生影响，分析的时候会自动排除掉等于0的项

                        //分析数据（对于新用户不用分析数据，仅完成插入操作和更新字段的操作）

                        //更新用户的EXERCISE_STATUS和SLEEP_STATUS字段
                        //因为用户第一次更新，我们不知道用户以往的数据
                        //所以对于新用户只是简单的统计一下运动时间和睡眠时间在推荐值范围之内的天数占总天数的比率
                        // 大于 0.7 即还是认为用户是睡眠充足 / 运动充足的

                        //总时间
                        String sleepStatus = "";
                        String exerciseStatus = "";
                        int allDateCount = (int) ((maxDate.getTime() - minDate.getTime()) / 1000 / 60 / 60 / 24);
                        //如果allDateCount == 0 说明用户只上传了一天的数据，正常情况下我们不会记录
                        if(allDateCount != 0) {
                            int sleepWellDateCount = 0;
                            int exerciseWellDateCount = 0;
                            //统计睡眠(不统计索引0)
                            //运动分两种
                            //一种是步行，这个每个人每天都有，建议每天大于7,500步
                            //一种是跑步，这个不能看每天，要每一周每一周的看，健康建议一般是每一周达到xxx分钟的跑步
                            //一种是呼吸训练，建议时长在5-10分钟/天

                            //但是鉴于用户第一次上传数据，这里可以简单一些
                            //判断每天的运动时间 = 步行时间 * 1 + 跑步时间 * 1.5 + 呼吸训练时间 * 0.6是否大于30min

                            for (int i = 1; i < dataList.size(); i++) {
                                //7 h * 60 = 420 min
                                if(dataList.get(i).getSleepTime() >= 420) sleepWellDateCount++;

                                double exerciseTimePerDay =
                                        (double)(dataList.get(i).getWalkTime()) +
                                        (double)(dataList.get(i).getRunTime()) * 1.5 +
                                        (double)(dataList.get(i).getBreathExTime()) * 0.6;

                                if(exerciseTimePerDay >= 30) exerciseWellDateCount++;
                            }


                            System.out.println("统计"+allDateCount+"天，其中"+sleepWellDateCount+"天睡眠时间超过7h");
                            if((double)sleepWellDateCount/allDateCount >= judgeRatio) sleepStatus = "ENOUGH";//睡眠充足
                            else sleepStatus = "LACK";//睡眠缺乏
                            System.out.println("统计"+allDateCount+"天，其中"+exerciseWellDateCount+"天运动时间超过30min");
                            if((double)(exerciseWellDateCount) / allDateCount >= judgeRatio) exerciseStatus = "ENOUGH";//运动充足
                            else exerciseStatus = "LACK";//运动缺乏

                            //更新用户的LAST_UPDATE字段和状态字段
                            //1. 获取sqlSessionFactory对象
                            //2. 获取sqlSession对象
                            sqlSession = sqlSessionFactory.openSession();
                            //3. 获取对应Mapper接口的代理对象
                            userMapper = sqlSession.getMapper(USERMapper.class);
                            HEALTH_DATAMapper dataMapper = sqlSession.getMapper(HEALTH_DATAMapper.class);
                            //4. 执行对应的sql语句
                            userMapper.SetLastUpdate(LoginUUID, dft2.format(curDate));
                            userMapper.SetStatus(LoginUUID, sleepStatus, exerciseStatus);
                            //从后往前插入，日期从小到大
                            for(int i = dataList.size() - 1 ; i > 0 ; i--){
                                dataMapper.InsertHealthDataItem(LoginUUID, dataList.get(i));
                            }
                            sqlSession.commit();
                            //5. 释放资源
                            sqlSession.close();

                            //返回结果

                            result = "success";
                            processInfo = "data is update";
                            JSONObject responseInfo = new JSONObject();

                            responseInfo.put("result", result);
                            responseInfo.put("info", processInfo);

                            PrintWriter writer = response.getWriter();

                            writer.write(JSON.toJSONString(responseInfo));
                            return;
                        }

                    }else{
                        System.out.println("老用户更新");
                        //首先判断老用户上次是不是更新过了
                        //也就是maxDate和lastUpdate相同的情况
                        System.out.println("maxDate"+dft2.format(maxDate));
                        if(!dft2.format(maxDate).equals(dft2.format(lastUpdate))) {
                            System.out.println("老用户至少需要更新一天的数据");
                            //如果不相同，说明至少需要更新一天
                            //先生成对应行数的数组
                            //行数 = maxDate - lastUpdate + 1，但最后只上传maxDate - lastUpdate个
                            Date minDate = maxDate;
                            int itemCount =  (int) ((maxDate.getTime() - lastUpdate.getTime()) / 1000 / 60 / 60 / 24) + 1;
                            for(int i = 0 ; i < itemCount ; i++){
                                long tempTime = dft2.parse(dft2.format(maxDate)).getTime() - (long) i * 1000 * 60 * 60 * 24;
                                Date tempDate = new Date(tempTime);
                                item.setTime(tempDate);
                                dataList.add(item);
                                item = new HealthDataItem();
                                System.out.println("生成时间为"+dft2.format(tempDate)+"的数据项");
                                minDate = tempDate;//循环最后出来的一定是最小时间
                            }
                            //**************增加步行数据表*****************//
                            br.close();
                            br = new BufferedReader(new FileReader(walkingFile));
                            //第一行是标题行，不管
                            line = br.readLine();
                            while ((line = br.readLine()) != null) {
                                // use comma as separator
                                String[] data = line.split(cvsSplitBy);

                                data[4] = data[4].replace("\"", "");
                                data[6] = data[6].replace("\"", "");

                                int index = getListIndex(maxDate, minDate, dft2.parse(data[1]));
                                System.out.println("更新步行距离和时间"+dft2.format(dft2.parse(data[1]))+"的下标为"+index);
                                if(index != -1){
                                    dataList.get(index).addWalkingDistance(Double.parseDouble(data[4]));
                                    dataList.get(index).addWalkTime(StringToMinute(data[6]));
                                }
                            }
                            System.out.println("更新步行表完毕");
                            //================增加跑步数据表===================//
                            String runningFile = uploadPath + newNames.get(oldNames.indexOf("Activities-run.csv"));
                            br.close();
                            br = new BufferedReader(new FileReader(runningFile));
                            //第一行是标题行，不管
                            line = br.readLine();
                            while ((line = br.readLine()) != null) {
                                // use comma as separator
                                String[] data = line.split(cvsSplitBy);
                                data[6] = data[6].replace("\"", "");

                                int index = getListIndex(maxDate, minDate, dft2.parse(data[1]));
                                System.out.println("更新跑步时间"+dft2.format(dft2.parse(data[1]))+"的下标为"+index);
                                if(index != -1){
                                    dataList.get(index).addRunTime(StringToMinute(data[6]));
                                }
                            }
                            System.out.println("更新跑步表完毕");
                            //================增加呼吸数据表===================//
                            String breathFile = uploadPath + newNames.get(oldNames.indexOf("Activities-breath.csv"));
                            br.close();
                            br = new BufferedReader(new FileReader(breathFile));
                            //第一行是标题行，不管
                            line = br.readLine();
                            while ((line = br.readLine()) != null) {
                                // use comma as separator
                                String[] data = line.split(cvsSplitBy);
                                data[4] = data[4].replace("\"", "");

                                int index = getListIndex(maxDate, minDate, dft2.parse(data[1]));
                                System.out.println("更新呼吸时间"+dft2.format(dft2.parse(data[1]))+"的下标为"+index);
                                if(index != -1){
                                    dataList.get(index).addBreathExTime(StringToMinute(data[4]));
                                }
                            }
                            System.out.println("更新呼吸表完毕");
                            //================增加睡眠数据表===================//
                            //=====注意提前把双引号去掉====
                            String sleepFile = uploadPath + newNames.get(oldNames.indexOf("睡眠.csv"));
                            br.close();
                            br = new BufferedReader(new FileReader(sleepFile));
                            //第一行是标题行，不管
                            line = br.readLine();

                            int index = -1;
                            //因为睡眠表从上往下时间是递增的，我们先找到最小的能和list中时间匹配的数据项在list中的下标
                            while ((line = br.readLine()) != null) {
                                // use comma as separator
                                String[] data = line.split(cvsSplitBy);
                                data[0] = data[0].replace("\"", "");
                                data[1] = data[1].replace("\"", "");

                                index = getListIndex_Sleep(maxDate, minDate, SleepDateTransfer(data[0]));
                                System.out.println("找到睡眠表的index:"+index);
                                if(index != -1) {
                                    //先插入当前匹配好的项
                                    dataList.get(index--).addSleepTime(SleepHourToMinute(data[1]));
                                    break;
                                }
                            }
                            if(index != -1){
                                //找到匹配项，可以进行匹配
                                //双指针算法：注意睡眠表的遍历是让时间逐渐增大，所以list表应该反着遍历
                                if((line = br.readLine()) != null){
                                    String[] data = line.split(cvsSplitBy);
                                    data[0] = data[0].replace("\"", "");
                                    data[1] = data[1].replace("\"", "");
                                    SimpleDateFormat dft3 = new SimpleDateFormat("MM-dd");
                                    for(;index >= 0 ; index--){
                                        if(line == null) break;

                                        while(dft3.format(dataList.get(index).getTime()).equals(SleepDateTransfer(data[0]))){
                                            //找到匹配的，插入睡眠数据
                                            dataList.get(index).addSleepTime(SleepHourToMinute(data[1]));
                                            System.out.println("在"+dft2.format(dataList.get(index).getTime())+"更新睡眠时间"+SleepHourToMinute(data[1]));
                                            if((line = br.readLine()) != null){
                                                data = line.split(cvsSplitBy);
                                                data[0] = data[0].replace("\"", "");
                                                data[1] = data[1].replace("\"", "");
                                            }else{break;}
                                        }
                                    }
                                }
                            }
                            System.out.println("更新睡眠表完毕");
                            //否则剩下的睡眠时间全部为零，但是不会对分析结果产生影响，分析的时候会自动排除掉等于0的项

                            System.out.println("数据插入完毕");
                            for(int i = 0 ; i < dataList.size() ; i++){
                                System.out.println(dataList.get(i));
                            }

                            //数据插入完毕，接下来是分析的过程
                            //从dataList的最后一行开始分析，分析到第二行，第一行不分析
                            //依次对运动 睡眠进行分析
                            //预测值与真实值

                            //先获取过去的数据，用于预测
                            //2. 获取sqlSession对象
                            sqlSession = sqlSessionFactory.openSession();
                            //3. 获取对应Mapper接口的代理对象
                            HEALTH_DATAMapper healthDataMapper = sqlSession.getMapper(HEALTH_DATAMapper.class);
                            //4. 执行对应的sql语句
                            // 按照日期降序取出，这样能保证取到离分析的天数最近的100天数据
                            List<Double> walkingDistList = healthDataMapper.SelectRecentWalkingDistance(LoginUUID);
                            List<Integer> walkingTimeList = healthDataMapper.SelectRecentWalkingTime(LoginUUID);
                            List<Integer> runningTimeList = healthDataMapper.SelectRecentRunningTime(LoginUUID);
                            List<Integer> breathExTimeList = healthDataMapper.SelectRecentBreathExTime(LoginUUID);
                            List<Integer> sleepingTimeList = healthDataMapper.SelectRecentSleepingTime(LoginUUID);
                            //5. 释放资源
                            sqlSession.close();
                            // 但是这样不方面，我们插入更大的日期，因为add增加到末尾，而下标越大对应的日期也越大
                            // 所以我们进行翻转操作

                            Collections.reverse(walkingDistList);
                            Collections.reverse(walkingTimeList);
                            Collections.reverse(runningTimeList);
                            Collections.reverse(breathExTimeList);
                            Collections.reverse(sleepingTimeList);

                            //处理空值问题，五个数据中，需要处理空值问题的只有睡眠时间
                            //每天都要睡觉，只可能是晚上充电没有数据了
                            //其他的都可能为零
                            //对于睡眠中的空值，利用前面最近一天的睡眠值代替即可

                            //如果统计中的第一天为空，则取数据库中的时间最大的一条数据的睡眠时间代替
                            //可以这样做的原因是我们保证数据库中每一天的睡眠数据都不为空
                            if(dataList.get(dataList.size() - 1).getSleepTime() == 0){
                                System.out.println(dft2.format(dataList.get(dataList.size() - 1).getTime())+"的睡眠时间为零，用数据库中前一天的睡眠时间代替");
                                dataList.get(dataList.size() - 1).setSleepTime(sleepingTimeList.get(sleepingTimeList.size() - 1));
                            }
                            //daraList中：
                            //后一天 i - 1
                            //当天     i
                            //前一天 i + 1
                            for(int i = dataList.size() - 2 ; i > 0 ;  i--){
                                if(dataList.get(i).getSleepTime() == 0){
                                    dataList.get(i).setSleepTime(dataList.get(i + 1).getSleepTime());
                                }
                            }

                            // 此时下标小，对应的日期也小 大日期插到后面

                            int exerciseWellCount = 0;
                            int sleepWellCount = 0;
                            int allDateCount = dataList.size() - 1;

                            //获取用户得分情况
                            //2. 获取sqlSession对象
                            sqlSession = sqlSessionFactory.openSession();

                            //3. 获取对应Mapper接口的代理对象
                            userMapper = sqlSession.getMapper(USERMapper.class);

                            //4. 执行对应的sql语句
                            double treeScore = userMapper.GetTreeScore(LoginUUID);
                            double animalScore = userMapper.GetAnimalScore(LoginUUID);

                            //5. 释放资源
                            sqlSession.close();

                            for(int i = dataList.size() - 1 ; i > 0 ; i--){
                                //先分析运动情况
                                //分成四个部分：步行距离 步行时间 跑步时间 呼吸训练的时间
                                //四个部分都可以增加动物数量和树的数量
                                //最终确定状态是根据四个部分中 实际值超过预测值 的次数 占 总统计天数（dataList.size() - 1） * 4的比率确定

                                //数量大于
                                //====步行距离====//
                                //每天7500 * 0.75 = 5.25
                                double prediction = GetPrediction_Double(walkingDistList);
                                if(prediction > 5.25) prediction = 5.25;

                                double actual = dataList.get(i).getWalkingDistance();

                                if(actual > prediction){
                                    exerciseWellCount++;
                                    System.out.println(actual+"超过预测值"+prediction+" 锻炼健康次数"+exerciseWellCount);
                                    treeNum += Math.ceil(prediction * P_tree) +
                                            Math.floor((prediction - actual) * (P_tree + a) / 2);
                                    animalScore += (prediction * P_animal +
                                            (prediction - actual) * (P_animal + a)) / 2;
                                }else{
                                    treeScore += actual * P_tree / 2;
                                    animalScore += actual * P_animal;
                                }
                                System.out.println(dft2.format(dataList.get(i).getTime())+"步行距离预测："+prediction
                                +" 实际值："+ actual+" treeNum="+treeNum+" treeScore="+treeScore+"animalNum="+
                                        animalNum+" animalScore="+animalScore);
                                //分析结束，插入实际值
                                walkingDistList.add(actual);

                                //=====步行时间=====//
                                //分析步行时间 每周150 min 或者 每天 21 min
                                prediction = GetPrediction_Int(walkingTimeList);
                                actual = dataList.get(i).getWalkTime();
                                if(walkingTimeList.size() >= 6){
                                    //按照周目标计算
                                    for(int j = 1 ; j <= 6 ; j++) {
                                        prediction += walkingTimeList.get(walkingTimeList.size() - j);
                                        actual += walkingTimeList.get(walkingTimeList.size() - j);
                                    }

                                    if (prediction > 150) prediction = 150;

                                    if(actual > prediction){
                                        exerciseWellCount++;
                                        System.out.println(actual+"超过预测值"+prediction+" 锻炼健康次数"+exerciseWellCount);
                                        treeNum += Math.ceil((prediction * P_tree +
                                                (actual - prediction) * (P_tree + a)) / 20);
                                        animalScore += (prediction * P_animal +
                                                (actual - prediction) * (P_animal + a)) / 20;
                                    }else{
                                        treeScore += (actual * P_tree) / 40;
                                        animalScore += (actual * P_animal) / 10;
                                    }

                                }else{
                                    //按照每天目标计算
                                    if(prediction > 21) prediction = 21;

                                    if(actual > prediction){
                                        exerciseWellCount++;
                                        System.out.println(actual+"超过预测值"+prediction+" 锻炼健康次数"+exerciseWellCount);
                                        treeNum += Math.ceil((prediction * P_tree +
                                                (actual - prediction) * (P_tree + a)) / 8);

                                        animalScore += (prediction * P_animal +
                                                (actual - prediction) * (P_animal + a)) / 16;
                                    }
                                    else {
                                        treeScore += (actual * P_tree) / 20;
                                        animalScore += (actual * P_animal) / 12;
                                    }
                                }
                                System.out.println(dft2.format(dataList.get(i).getTime())+"步行时间预测："+prediction
                                        +" 实际值："+ actual+" treeNum="+treeNum+" treeScore="+treeScore+"animalNum="+
                                        animalNum+" animalScore="+animalScore);
                                //分析完成，插入实际值
                                walkingTimeList.add(dataList.get(i).getWalkTime());

                                //====跑步情况====
                                //每周40min | 每天 5min
                                prediction = GetPrediction_Int(runningTimeList);
                                actual = dataList.get(i).getRunTime();
                                if(runningTimeList.size() >= 6){
                                    //按照每周 40min 的推荐值计算
                                    for(int j = 1 ; j <= 6 ; j++) {
                                        prediction += runningTimeList.get(runningTimeList.size() - j);
                                        actual += runningTimeList.get(runningTimeList.size() - j);
                                    }

                                    if(prediction > 40) prediction = 40;

                                    if(actual > prediction){
                                        System.out.println(actual+"超过预测值"+prediction+" 锻炼健康次数"+exerciseWellCount);
                                        exerciseWellCount++;
                                        treeNum += Math.ceil((prediction * P_tree +
                                                (actual - prediction) * (P_tree + a)) / 12);

                                        animalScore += (prediction * P_animal +
                                                (actual - prediction) * (P_animal + a)) / 16;
                                    } else {
                                        treeScore += (actual * P_tree) / 40;
                                        animalScore += (actual * P_animal) / 6;
                                    }

                                } else {
                                    //按照每天 5min 的推荐值计算

                                    if (prediction > 5) prediction = 5;
                                    if(actual > prediction){
                                        System.out.println(actual+"超过预测值"+prediction+" 锻炼健康次数"+exerciseWellCount);
                                        exerciseWellCount++;
                                        treeNum += Math.ceil((prediction * P_tree +
                                                (actual - prediction) * (P_tree + a)) / 8);

                                        animalScore += (prediction * P_animal +
                                                (actual - prediction) * (P_animal + a)) / 12;
                                    } else {
                                        treeScore += (actual * P_tree) / 6;
                                        animalScore += (actual * P_animal);
                                    }
                                }
                                System.out.println(dft2.format(dataList.get(i).getTime())+"跑步时间预测："+prediction
                                        +" 实际值："+ actual+" treeNum="+treeNum+" treeScore="+treeScore+"animalNum="+
                                        animalNum+" animalScore="+animalScore);
                                //分析完成，插入实际值
                                runningTimeList.add(dataList.get(i).getRunTime());

                                //=====分析呼吸训练情况=====
                                //每周 20 min 或者 每天 3min
                                prediction = GetPrediction_Int(breathExTimeList);
                                actual = dataList.get(i).getBreathExTime();
                                if(breathExTimeList.size() >= 6){
                                    //按照每周 20min 的推荐值计算
                                    for(int j = 1 ; j <= 6 ; j++) {
                                        prediction += breathExTimeList.get(breathExTimeList.size() - j);
                                        actual += breathExTimeList.get(breathExTimeList.size() - j);
                                    }

                                    if(prediction > 20) prediction = 20;

                                    if(actual > prediction){
                                        System.out.println(actual+"超过预测值"+prediction+" 锻炼健康次数"+exerciseWellCount);
                                        exerciseWellCount++;
                                        treeNum += Math.ceil((prediction * P_tree +
                                                (actual - prediction) * (P_tree + a)) / 8);

                                        animalScore += (prediction * P_animal +
                                                (actual - prediction) * (P_animal + a)) / 4;
                                    } else {
                                        treeScore += (actual * P_tree) / 8;
                                        animalScore += (actual * P_animal) / 4;
                                    }

                                } else {
                                    //按照每天 3min 的推荐值计算

                                    if (prediction > 3) prediction = 3;
                                    if(actual > prediction){
                                        System.out.println(actual+"超过预测值"+prediction+" 锻炼健康次数"+exerciseWellCount);
                                        exerciseWellCount++;
                                        treeNum += Math.ceil((prediction * P_tree +
                                                (actual - prediction) * (P_tree + a)) / 4);

                                        animalScore += (prediction * P_animal +
                                                (actual - prediction) * (P_animal + a)) / 10;
                                    } else {
                                        treeScore += (actual * P_tree) / 4;
                                        animalScore += (actual * P_animal) / 2;
                                    }
                                }
                                System.out.println(dft2.format(dataList.get(i).getTime())+"呼吸训练时间预测："+prediction
                                        +" 实际值："+ actual+" treeNum="+treeNum+" treeScore="+treeScore+"animalNum="+
                                        animalNum+" animalScore="+animalScore);
                                //分析完成，插入实际值
                                breathExTimeList.add(dataList.get(i).getBreathExTime());

                                //分析结束一天的运动类型


                                //再分析睡眠情况
                                //也可以增加树的数量和动物的数量

                                prediction = GetPrediction_Int(sleepingTimeList);
                                actual = dataList.get(i).getSleepTime();

                                //建议睡 8h
                                if(prediction > 480) prediction = 480;

                                if(actual > prediction){
                                    sleepWellCount++;
                                    System.out.println(actual+"超过预测值"+prediction+" 睡眠健康次数"+sleepWellCount);
                                    treeNum += Math.ceil((prediction * P_tree +
                                            (actual - prediction) * (P_tree + a)) / 200);
                                    animalScore += (prediction * P_animal +
                                            (actual - prediction) * (P_animal + a/4)) / 40;
                                } else {
                                    treeScore += actual * P_tree / 200;
                                    animalScore += actual * P_animal / 80;
                                }
                                System.out.println(dft2.format(dataList.get(i).getTime())+"睡眠时间预测："+prediction
                                        +" 实际值："+ actual+" treeNum="+treeNum+" treeScore="+treeScore+"animalNum="+
                                        animalNum+" animalScore="+animalScore);

                                //统计结束，插入实际值
                                sleepingTimeList.add(dataList.get(i).getSleepTime());

                            }
                            treeNum += Math.floor(treeScore);
                            treeScore -= Math.floor(treeScore);

                            animalNum += Math.floor(animalScore);
                            animalScore -= Math.floor(animalScore);
                            //没有用完score下次继续用
                            //2. 获取sqlSession对象
                            sqlSession = sqlSessionFactory.openSession();

                            //3. 获取对应Mapper接口的代理对象
                            userMapper = sqlSession.getMapper(USERMapper.class);

                            //4. 执行对应的sql语句
                            userMapper.SetScore(LoginUUID, treeScore, animalScore);
                            System.out.println("设置分数成功");
                            sqlSession.commit();
                            System.out.println("获得植物数："+treeNum+" 动物数："+animalNum+
                                    " 植物分数："+treeScore+" 动物分数："+animalScore);
                            //5. 释放资源
                            sqlSession.close();
                            //生成动物和植物，插入数据库中
                            sqlSession = sqlSessionFactory.openSession();
                            GenerateGameObjects(LoginUUID, treeNum, animalNum, sqlSession);
                            String exerciseStatus = "";
                            String sleepStatus = "";
                            //统计信息获取当前状态，插入状态
                            System.out.println("======分析用户运动状态=======");
                            System.out.println(exerciseWellCount+"/"+allDateCount+"/4"+"="+(double)exerciseWellCount / allDateCount / 4.0);
                            if((double)exerciseWellCount / allDateCount / 4.0 >= judgeRatio){
                                exerciseStatus = "ENOUGH";
                            }else{
                                exerciseStatus = "LACK";
                            }
                            System.out.println("=====分析用户睡眠状态=====");
                            System.out.println(sleepWellCount+"/"+allDateCount+"="+(double)sleepWellCount / allDateCount);
                            if((double)sleepWellCount / allDateCount >= judgeRatio){
                                sleepStatus = "ENOUGH";
                            }else{
                                sleepStatus = "LACK";
                            }
                            //插入用户新增的动物数量和植物数量
                            sqlSession = sqlSessionFactory.openSession();
                            userMapper = sqlSession.getMapper(USERMapper.class);
                            userMapper.AddGameObjectNumber(LoginUUID, treeNum, animalNum);
                            sqlSession.commit();
                            sqlSession.close();
                            System.out.println("数量设置成功");
                            sqlSession = sqlSessionFactory.openSession();
                            userMapper = sqlSession.getMapper(USERMapper.class);
                            userMapper.SetStatus(LoginUUID, sleepStatus, exerciseStatus);
                            sqlSession.commit();
                            sqlSession.close();
                            System.out.println("设置状态成功");
                            //插入分析完成之后的dataList
                            //从后往前插入，保证顺序
                            sqlSession = sqlSessionFactory.openSession();
                            healthDataMapper = sqlSession.getMapper(HEALTH_DATAMapper.class);
                            for(int i = dataList.size() - 1 ; i > 0 ; i--){
                                healthDataMapper.InsertHealthDataItem(LoginUUID, dataList.get(i));
                                sqlSession.commit();
                            }
                            sqlSession.close();
                            System.out.println("插入dataList成功");
                            //更新用户LastUpdate为curDate
                            sqlSession = sqlSessionFactory.openSession();
                            userMapper = sqlSession.getMapper(USERMapper.class);
                            userMapper.SetLastUpdate(LoginUUID, dft2.format(maxDate));
                            sqlSession.commit();
                            sqlSession.close();
                            System.out.println("更新lastUpdate成功");

                            result = "success";
                            processInfo = "finish analysis";
                            JSONObject responseInfo = new JSONObject();

                            responseInfo.put("result", result);
                            responseInfo.put("info", processInfo);
                            responseInfo.put("treeNum", treeNum);
                            responseInfo.put("animalNum", animalNum);

                            PrintWriter writer = response.getWriter();

                            writer.write(JSON.toJSONString(responseInfo));
                            return;

                        }else{
                            result = "fail";
                            processInfo = "data is newest";
                            JSONObject responseInfo = new JSONObject();

                            responseInfo.put("result", result);
                            responseInfo.put("info", processInfo);

                            PrintWriter writer = response.getWriter();

                            writer.write(JSON.toJSONString(responseInfo));
                            return;
                        }
                    }


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


            return forecast.pointEstimates().at(0)+a;
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

