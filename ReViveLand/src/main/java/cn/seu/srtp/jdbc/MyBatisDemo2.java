package cn.seu.srtp.jdbc;

import cn.seu.srtp.mapper.USERMapper;
import cn.seu.srtp.pojo.User;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 *
 * MyBatis代理开发
 */
public class MyBatisDemo2 {

    public static void main (String[] args) throws IOException {
        //1. 加载mybatis核心配置文件，获取SqlSessionFactory
        String resource = "mybatis-config.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);

        //2. 获取SqlSession对象，执行sql语句
        SqlSession sqlSession = sqlSessionFactory.openSession();

//        List<User> users = sqlSession.selectList("cn.seu.srtp.mapper.USERMapper.SelectAllUser");

        //获取由Mabatis生成的对应接口的代理对象
        USERMapper userMapper = sqlSession.getMapper(USERMapper.class);
        List<User> users = userMapper.SelectAllUser();
        System.out.println(users);

        //3. 释放资源
        sqlSession.close();

    }
}
