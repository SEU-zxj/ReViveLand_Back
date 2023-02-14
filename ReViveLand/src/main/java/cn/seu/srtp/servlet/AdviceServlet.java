package cn.seu.srtp.servlet;

import cn.seu.srtp.mapper.HEALTH_DATAMapper;
import cn.seu.srtp.mapper.USERMapper;
import cn.seu.srtp.pojo.Advice;
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
import java.util.List;

@WebServlet("/advice")
public class AdviceServlet extends MyHttpServlet{
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

        //JSON类型的数据只能通过流的方式读取
        //读取JSON类型数据
        BufferedReader reader = req.getReader();
        String line = reader.readLine();
        JSONObject jsonGetter =  JSONObject.parseObject(line);

        String uuid = jsonGetter.getString("uuid");
        //返回用户名，用户的睡眠状态和运动状态
        //返回对象数组，每个对象都是一条建议
        if(!uuid.equals(""))
        {
            //1. 获取sqlSessionFactory对象
            String resource = "mybatis-config.xml";
            InputStream inputStream = Resources.getResourceAsStream(resource);
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);

            //2. 获取sqlSession对象
            SqlSession sqlSession = sqlSessionFactory.openSession();

            //3. 获取对应Mapper接口的代理对象
            USERMapper userMapper = sqlSession.getMapper(USERMapper.class);
            HEALTH_DATAMapper healthyMapper = sqlSession.getMapper(HEALTH_DATAMapper.class);

            //4. 执行对应的sql语句
            String userName = userMapper.GetUserName(uuid);
            String sleepStatus = userMapper.GetSleepStatus(uuid);
            String exerciseStatus = userMapper.GetExerciseStatus(uuid);
            List<Advice> advices = healthyMapper.GetAdvice(uuid);

            //5. 释放资源
            sqlSession.close();

            responseInfo.put("result", "success");
            responseInfo.put("userName", userName);
            responseInfo.put("sleepStatus", sleepStatus);
            responseInfo.put("exerciseStatus", exerciseStatus);
            responseInfo.put("advices", advices);

            PrintWriter writer = res.getWriter();

            writer.write(JSON.toJSONString(responseInfo));

            return;
        }else{
            responseInfo.put("result", "fail");
            responseInfo.put("info", "uuid is empty");

            PrintWriter writer = res.getWriter();

            writer.write(JSON.toJSONString(responseInfo));
            return;
        }

    }
}
