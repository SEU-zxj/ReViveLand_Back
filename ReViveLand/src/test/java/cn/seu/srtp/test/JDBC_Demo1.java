package cn.seu.srtp.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * JDBC 基本操作
 */
public class JDBC_Demo1 {

    public static void main(String[] args) throws Exception {
        //1. 注册驱动
        Class.forName("com.mysql.jdbc.Driver");

        //2. 获取连接
        String url = "jdbc:mysql://8.130.26.63:3306/ReViveLand";
        String username = "root";
        String password = "DNDX_srtp_097157";
        Connection connection = DriverManager.getConnection(url, username, password);

        //3. 定义sql
        String sql = "UPDATE USER SET EMAIL = \'7788@78.com\' WHERE USER_NAME = \'admin\'";

        //4. 获取执行sql的对象 Statement
        Statement stmt = connection.createStatement();

        //5. 执行sql
        int count = stmt.executeUpdate(sql);

        //6. 处理结果
        System.out.println(count);

        //7. 释放资源
        stmt.close();
        connection.close();
    }
}
