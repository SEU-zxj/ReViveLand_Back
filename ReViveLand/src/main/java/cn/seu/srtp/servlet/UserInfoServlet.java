package cn.seu.srtp.servlet;

import cn.seu.srtp.mapper.GameDataMapper;
import cn.seu.srtp.mapper.HEALTH_DATAMapper;
import cn.seu.srtp.mapper.USERMapper;
import cn.seu.srtp.pojo.HealthDataItem;
import cn.seu.srtp.pojo.Player;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@WebServlet("/userInfo")
public class UserInfoServlet extends MyHttpServlet{
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        //生成对应的JSON数据，返回给前端
        //发送响应数据，注意处理跨域请求问题
        String scheme = req.getScheme();//返回前后端通信的协议（http,https,ftp...)
        String ip = req.getRemoteAddr();//返回发出请求的IP地址
        String host = req.getRemoteHost();//返回发出请求的客户机的主机名
        int port =req.getRemotePort();//返回发出请求的客户机的端口号

        res.setHeader("Access-Control-Allow-Credentials", "true");
        res.setHeader("Access-Control-Allow-Origin", scheme+"://"+ip+":8081");
        res.setHeader("Content-type", "application/json");

        JSONObject responseInfo = new JSONObject();

        //前端已经验证uuid不为空
        //首先读取uuid
        //JSON类型的数据只能通过流的方式读取
        //读取JSON类型数据
        BufferedReader reader = req.getReader();
        String line = reader.readLine();
        JSONObject jsonGetter =  JSONObject.parseObject(line);

        String uuid = jsonGetter.getString("uuid");

        //进行查询操作
        //1. 获取sqlSessionFactory对象
        String resource = "mybatis-config.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);

        //2. 获取sqlSession对象
        SqlSession sqlSession = sqlSessionFactory.openSession();

        //3. 获取对应Mapper接口的代理对象
        USERMapper userMapper = sqlSession.getMapper(USERMapper.class);
        GameDataMapper gameMapper = sqlSession.getMapper(GameDataMapper.class);
        HEALTH_DATAMapper healthyMapper = sqlSession.getMapper(HEALTH_DATAMapper.class);

        //4. 执行对应的sql语句
        String userName = userMapper.GetUserName(uuid);
        Player player = gameMapper.GetPlayer(uuid);
        List<HealthDataItem> dataItems = healthyMapper.GetHealthDataItems(uuid);
        //5. 释放资源
        sqlSession.commit();
        //5. 释放资源
        sqlSession.close();

        //记录时间的参数为 dateData
        //记录步行距离的数据为 distData
        //记录步行时间 跑步时间 呼吸训练时间 的数据 为 walkingTime runningTime breathExTime
        //记录睡眠时间的数据为 sleepingTime

        //逆转数组
        Collections.reverse(dataItems);

        SimpleDateFormat dft = new SimpleDateFormat("MM-dd");
        List<String> dateData = new ArrayList<String>();
        List<Double> distData = new ArrayList<Double>();
        List<Integer> walkingTime = new ArrayList<Integer>();
        List<Integer> runningTime = new ArrayList<Integer>();
        List<Integer> breathExTime = new ArrayList<Integer>();
        List<Integer> sleepingTime = new ArrayList<Integer>();

        DecimalFormat format = new DecimalFormat("0.00");


        for(HealthDataItem item : dataItems){
            dateData.add(dft.format(item.getTime()));
            try {
                distData.add((Double) format.parse(format.format(item.getWalkingDistance())));
            } catch (ParseException e) {
                responseInfo.put("result", "fail");
                responseInfo.put("info", "dist parse wrong");
                PrintWriter writer = res.getWriter();

                writer.write(JSON.toJSONString(responseInfo));
            }
            walkingTime.add(item.getWalkTime());
            runningTime.add(item.getRunTime());
            breathExTime.add(item.getBreathExTime());
            sleepingTime.add(item.getSleepTime());
        }

        responseInfo.put("result", "success");
        responseInfo.put("userName", userName);
        responseInfo.put("treeNum", player.getTreeNum());
        responseInfo.put("animalNum", player.getAnimalNum());
        responseInfo.put("treeScore", player.getTreeScore());
        responseInfo.put("animalScore", player.getAnimalScore());
        responseInfo.put("dateData", dateData);
        responseInfo.put("distData", distData);
        responseInfo.put("walkingTime", walkingTime);
        responseInfo.put("runningTime", runningTime);
        responseInfo.put("breathExTime", breathExTime);
        responseInfo.put("sleepingTime", sleepingTime);

        PrintWriter writer = res.getWriter();

        writer.write(JSON.toJSONString(responseInfo));
    }
}
