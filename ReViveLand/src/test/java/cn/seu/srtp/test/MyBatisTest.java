package cn.seu.srtp.test;

import cn.seu.srtp.mapper.USERMapper;
import cn.seu.srtp.pojo.User;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class MyBatisTest {

    @Test
    public void TestSelectAllUser() throws IOException {
        //1. 获取sqlSessionFactory对象
        String resource = "mybatis-config.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);

        //2. 获取sqlSession对象
        SqlSession sqlSession = sqlSessionFactory.openSession();

        //3. 获取对应Mapper接口的代理对象
        USERMapper userMapper = sqlSession.getMapper(USERMapper.class);

        //4. 执行对应的sql语句
        List<User> users = userMapper.SelectAllUser();

        System.out.println(users);
        //5. 释放资源
        sqlSession.close();
    }

    @Test
    public void TestSelectByUserName() throws IOException {

        String name1 = "admin";
        String name2 = "failed";

        //1. 获取sqlSessionFactory对象
        String resource = "mybatis-config.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);

        //2. 获取sqlSession对象
        SqlSession sqlSession = sqlSessionFactory.openSession();

        //3. 获取对应Mapper接口的代理对象
        USERMapper userMapper = sqlSession.getMapper(USERMapper.class);

        //4. 执行对应的sql语句
        User user = userMapper.SelectByUserName(name2);

        System.out.println(user);
        //5. 释放资源
        sqlSession.close();
    }

    @Test
    public void TestLogin() throws IOException {

        String userName1 = "admin";
        String userName2 = "failed";
        String password = "123456";

        //1. 获取sqlSessionFactory对象
        String resource = "mybatis-config.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);

        //2. 获取sqlSession对象
        SqlSession sqlSession = sqlSessionFactory.openSession();

        //3. 获取对应Mapper接口的代理对象
        USERMapper userMapper = sqlSession.getMapper(USERMapper.class);

        //4. 执行对应的sql语句
        User user = userMapper.Login(userName2, password);

        System.out.println(user);
        //5. 释放资源
        sqlSession.close();
    }

    @Test
    public void TestRegister() throws IOException {

        String userName1 = "admin";
        String userName2 = "testUser";
        String phoneNumber = "15850661098";
        String email = "2720611814@qq.com";
        String password = "097157";

        User oldUser = new User(userName1, phoneNumber, email, password);
        User newUser = new User(userName2, phoneNumber, email, password);

        //1. 获取sqlSessionFactory对象
        String resource = "mybatis-config.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);

        //2. 获取sqlSession对象
        SqlSession sqlSession = sqlSessionFactory.openSession();

        //3. 获取对应Mapper接口的代理对象
        USERMapper userMapper = sqlSession.getMapper(USERMapper.class);

        //4. 执行对应的sql语句
        //开启事务
        User user = userMapper.SelectByUserName(userName2);
        if(user == null){
            //插入新的用户
            System.out.println("insert");
            int count = userMapper.Register(newUser);
            System.out.println(count);
            sqlSession.commit();
        }else{
            System.out.println("exist");
        }

        //5. 释放资源
        sqlSession.close();
    }
}
