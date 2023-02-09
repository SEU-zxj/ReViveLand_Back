package cn.seu.srtp.jdbc;

import cn.seu.srtp.pojo.User;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC 查询操作，获取数据库中数据集合
 */
public class JDBC_Demo3_ResultSet {

    @Test
    public void testss() throws Exception {
        //1. 注册驱动
        Class.forName("com.mysql.jdbc.Driver");

        //2. 获取连接
        String url = "jdbc:mysql://8.130.26.63:3306/ReViveLand";
        String db_username = "root";
        String db_password = "DNDX_srtp_097157";
        Connection connection = DriverManager.getConnection(url, db_username, db_password);

        //3. 定义sql
        String sql = "SELECT * FROM USER";

        //4. 获取stmt对象
        Statement stmt = connection.createStatement();

        //5. 执行sql
        ResultSet resultSet = stmt.executeQuery(sql);

        List<User> list = new ArrayList<User>();

        //6. 遍历获取数据
        while(resultSet.next())
        {
            String userName = resultSet.getString(1);
            String phoneNumber = resultSet.getString(2);
            String email = resultSet.getString(3);
            String password = resultSet.getString(4);

            User user = new User(userName, phoneNumber, email, password);
            list.add(user);
        }

        System.out.println(list);

        //7. 释放资源
        resultSet.close();
        stmt.close();
        connection.close();
    }
}
