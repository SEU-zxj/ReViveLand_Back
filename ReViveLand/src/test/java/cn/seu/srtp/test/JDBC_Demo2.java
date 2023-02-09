package cn.seu.srtp.test;

import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * JDBC 事务管理
 */
public class JDBC_Demo2 {

    @Test
    public void testss() throws Exception {
        //1. 注册驱动
        Class.forName("com.mysql.jdbc.Driver");

        //2. 获取连接
        String url = "jdbc:mysql://8.130.26.63:3306/ReViveLand";
        String username = "root";
        String password = "DNDX_srtp_097157";
        Connection connection = DriverManager.getConnection(url, username, password);

        //3. 定义sql
        String sql1 = "UPDATE USER SET EMAIL = \'8888@88.com\' WHERE USER_NAME = \'admin\'";

        String sql2 = "UPDATE USER SET PASSWORD = \'123456\' WHERE USER_NAME = \'admin\'";

        //4. 获取执行sql的对象 Statement
        Statement stmt = connection.createStatement();


        try {
            //====开启事务====//
            //设置手动提交
            connection.setAutoCommit(false);
            //5. 执行sql
            int count1 = stmt.executeUpdate(sql1);
            //6. 处理结果
            System.out.println(count1);
            //****************************//
//            int i = 3/0;
            //***************************//
            //5. 执行sql
            int count2 = stmt.executeUpdate(sql2);
            //6. 处理结果
            System.out.println(count2);

            //====提交事务====//
            connection.commit();
        } catch (Exception e) {
            //====回滚事务====//
            connection.rollback();
            throw new RuntimeException(e);
        }

        //7. 释放资源
        stmt.close();
        connection.close();
    }
}
