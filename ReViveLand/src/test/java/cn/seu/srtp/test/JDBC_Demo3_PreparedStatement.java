package cn.seu.srtp.test;

import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * JDBC 查询操作，获取数据库中数据集合
 */
public class JDBC_Demo3_PreparedStatement {

    @Test
    public void TestPreStatement() throws Exception {
        //1. 注册驱动
        Class.forName("com.mysql.jdbc.Driver");

        //2. 获取连接
        String url = "jdbc:mysql://8.130.26.63:3306/ReViveLand";
        String db_username = "root";
        String db_password = "DNDX_srtp_097157";
        Connection connection = DriverManager.getConnection(url, db_username, db_password);

        //3. 定义sql
        String sql = "SELECT * FROM USER WHERE USER_NAME = ? AND PASSWORD = ?";
        String userName = "admin";
        String password = "1234565";

        //4. 获取Preparedstatement对象
        PreparedStatement prepStmt = connection.prepareStatement(sql);

        //5. 设置sql中的参数
        prepStmt.setString(1, userName);
        prepStmt.setString(2, password);

        //6. 遍历获取数据

        ResultSet resultSet = prepStmt.executeQuery();

        if(resultSet.next()){
            System.out.println("success!");
        }else{
            System.out.println("fail!");
        }

        //7. 释放资源
        resultSet.close();
        prepStmt.close();
        connection.close();
    }
}
