package cn.seu.srtp;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class MyHttpServlet implements Servlet {
    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        //根据请求方式的不同，使用不同的逻辑
        //get请求方式的参数在请求行中，而post请求方式的参数在请求体中
        //1. 获取请求参数
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        //2.判断
        String method = request.getMethod();
        System.out.println("I know the req!");
        System.out.println("your method is " + method);
        if("GET".equals(method)){
            //Get请求方式
            doGet(request, response);
        }else if("POST".equals(method)){
            //Post请求方式
            doPost(request, response);
        }else{
            //其他的请求方式

            //可能是OPTIONS请求
            //支持跨域访问
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Allow-Methods", "*");
            response.setHeader("Access-Control-Allow-Headers", "Content-Type,Access-Token");
            response.setHeader("Access-Control-Expose-Headers", "*");
            if (request.getMethod().equals("OPTIONS")) {
                response.setStatus(204);
            }
        }
    }

    //交给子类实现Get请求的逻辑
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {

    }
    //交给子类实现Post请求的逻辑
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {

    }

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {

    }

    @Override
    public ServletConfig getServletConfig() {
        return null;
    }

    @Override
    public String getServletInfo() {
        return null;
    }

    @Override
    public void destroy() {

    }
}
