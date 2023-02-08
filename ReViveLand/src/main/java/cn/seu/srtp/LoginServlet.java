package cn.seu.srtp;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebServlet;

@WebServlet("/login")
public class LoginServlet extends MyHttpServlet{

    @Override
    protected void doGet(ServletRequest req, ServletResponse res) {
        System.out.println("get....");
    }

    @Override
    protected void doPost(ServletRequest req, ServletResponse res) {
        System.out.println("post");
    }
}
