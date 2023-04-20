package cn.seu.srtp.servlet;

import cn.seu.srtp.mapper.USERMapper;
import cn.seu.srtp.pojo.User;
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
import java.util.UUID;

@WebServlet("/login")
public class LoginServlet extends MyHttpServlet{

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String name = req.getParameter("name");
        String password = req.getParameter("password");

        System.out.println("Name is: " + name + ", and password is: " + password);

        //允许跨域请求，动态设置为发出请求的ip
        String scheme = req.getScheme();//返回前后端通信的协议（http,https,ftp...)
        String ip = req.getRemoteAddr();//返回发出请求的IP地址
        String host = req.getRemoteHost();//返回发出请求的客户机的主机名
        int port =req.getRemotePort();//返回发出请求的客户机的端口号

        System.out.println("Scheme is: "+ scheme);
        System.out.println("host is " + host);
        System.out.println("ip is: "+ip);
        System.out.println("port is: "+ port);


        //允许跨域
        //************************************************************
        //测试时使用http://127.0.0.1:8081
        res.setHeader("Access-Control-Allow-Credentials", "true");
        res.setHeader("Access-Control-Allow-Origin", scheme+"://"+ip+":8081");

        res.setHeader("content-type", "text/html;charset=utf-8");
        res.getWriter().write("<h1>name 是 " + name + ", and password is " + password + "</h1>");

    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {

        //文件类型数据获取通过字节流getInputString()获取

        //读取JSON类型数据
        BufferedReader reader = req.getReader();
        String line = reader.readLine();
        JSONObject jsonTest =  JSONObject.parseObject(line);

        String inputName = jsonTest.getString("name");
        String inputPassword = jsonTest.getString("password");

        String scheme = req.getScheme();//返回前后端通信的协议（http,https,ftp...)
        String ip = req.getRemoteAddr();//返回发出请求的IP地址
        String host = req.getRemoteHost();//返回发出请求的客户机的主机名
        int port =req.getRemotePort();//返回发出请求的客户机的端口号

        //返回字段
        // result：表示登录结果
        // uuid: 表示本次登录的唯一标识
        String loginResult = "";
        String loginUUID = "";

        //将用户输入的用户名和密码与数据库进行比对
        //==数据库操作==//

        //1. 利用MyBatis，先获取sqlSessionFactory对象
        String resource = "mybatis-config.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        //2. 获取sqlSession对象
        SqlSession sqlSession = sqlSessionFactory.openSession();
        //3. 获取对应Mapper的代理对象
        USERMapper userMapper = sqlSession.getMapper(USERMapper.class);
        //4. 调用代理对象的方法，执行sql
        User user = userMapper.Login(inputName, inputPassword);

        if(user == null){
            //没有对应的用户，登陆失败
//            writer.write("fail");
            loginResult = "no user";

            res.setHeader("Access-Control-Allow-Credentials", "true");
            res.setHeader("Access-Control-Allow-Origin", scheme+"://"+ip+":8081");
            //返回给前端
            res.setHeader("Content-type", "application/json");

            JSONObject responseInfo = new JSONObject();
            responseInfo.put("result", loginResult);

            PrintWriter writer = res.getWriter();

            writer.write(JSON.toJSONString(responseInfo));

            //5. 释放资源
            sqlSession.commit();
            sqlSession.close();

        }else{
            //登陆成功
//            writer.write("success");

            loginUUID = UUID.randomUUID().toString();

            //将本次登陆信息存入数据库
            int count = userMapper.InsertLoginRecord(loginUUID, inputName);
            //5. 释放资源
            sqlSession.commit();
            sqlSession.close();

            if(count == 1){
                loginResult = "success";
            }else{
                loginResult = "insert fail";
            }
            //给前端返回相关数据
            //允许跨域
            //************************************************************
            //测试时使用http://127.0.0.1:8081
            res.setHeader("Access-Control-Allow-Credentials", "true");
            res.setHeader("Access-Control-Allow-Origin", scheme+"://"+ip+":8081");
            //返回给前端
            res.setHeader("Content-type", "application/json");

            JSONObject responseInfo = new JSONObject();
            responseInfo.put("result", loginResult);
            responseInfo.put("uuid", loginUUID);

            PrintWriter writer = res.getWriter();

            writer.write(JSON.toJSONString(responseInfo));
        }


        //通用的Map方法
//        Map<String, String[]> map = req.getParameterMap();
//        System.out.println(map);
//        for(String key: map.keySet()){
//            System.out.println("key: " + key);
//
//            String[] values = map.get(key);
//            for(String value : values){
//                System.out.println(value + " ");
//            }
//        }

    }
}
