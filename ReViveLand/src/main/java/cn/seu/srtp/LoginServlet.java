package cn.seu.srtp;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/login")
public class LoginServlet extends MyHttpServlet{

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String name = req.getParameter("name");
        String password = req.getParameter("password");

        System.out.println("Name is: " + name + ", and password is: " + password);

        //允许跨域请求，动态设置为发出请求的ip
        String scheme = req.getScheme();
        String ip = req.getRemoteAddr();//返回发出请求的IP地址
        String host = req.getRemoteHost();//返回发出请求的客户机的主机名
        int port =req.getRemotePort();//返回发出请求的客户机的端口号

        System.out.println("Scheme is: "+ scheme);
        System.out.println("host is" + host);
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
//        BufferedReader reader = req.getReader();
//        String line = reader.readLine();
//        JSONObject jsonTest =  JSONObject.parseObject(line);
//
//        System.out.println(jsonTest.getString("name"));
//        System.out.println(jsonTest.getString("password"));


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

//        String name = req.getParameter("name");
//        String password = req.getParameter("password");
//
//
//        System.out.println("Name is: " + name + ", and password is: " + password);

//        res.setHeader("content-type", "text/html;charset=utf-8");
//        res.getWriter().write("<h1>name is " + name + ", and password is " + password + "<h1>");
    }
}
