//package cn.seu.srtp.test;
//
//import cn.seu.srtp.mapper.GameDataMapper;
//import cn.seu.srtp.mapper.HEALTH_DATAMapper;
//import cn.seu.srtp.mapper.USERMapper;
//import cn.seu.srtp.pojo.*;
//import org.apache.ibatis.io.Resources;
//import org.apache.ibatis.session.SqlSession;
//import org.apache.ibatis.session.SqlSessionFactory;
//import org.apache.ibatis.session.SqlSessionFactoryBuilder;
//import org.junit.Test;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.text.ParseException;
//import java.text.SimpleDateFormat;
//import java.util.List;
//import java.util.UUID;
//
//public class MyBatisTest {
//
//    @Test
//    public void TestSelectAllUser() throws IOException {
//        //1. 获取sqlSessionFactory对象
//        String resource = "mybatis-config.xml";
//        InputStream inputStream = Resources.getResourceAsStream(resource);
//        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
//
//        //2. 获取sqlSession对象
//        SqlSession sqlSession = sqlSessionFactory.openSession();
//
//        //3. 获取对应Mapper接口的代理对象
//        USERMapper userMapper = sqlSession.getMapper(USERMapper.class);
//
//        //4. 执行对应的sql语句
//        List<User> users = userMapper.SelectAllUser();
//
//        System.out.println(users);
//        //5. 释放资源
//        sqlSession.close();
//    }
//
//    @Test
//    public void TestSelectByUserName() throws IOException {
//
//        String name1 = "admin";
//        String name2 = "failed";
//
//        //1. 获取sqlSessionFactory对象
//        String resource = "mybatis-config.xml";
//        InputStream inputStream = Resources.getResourceAsStream(resource);
//        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
//
//        //2. 获取sqlSession对象
//        SqlSession sqlSession = sqlSessionFactory.openSession();
//
//        //3. 获取对应Mapper接口的代理对象
//        USERMapper userMapper = sqlSession.getMapper(USERMapper.class);
//
//        //4. 执行对应的sql语句
//        User user = userMapper.SelectByUserName(name2);
//
//        System.out.println(user);
//        //5. 释放资源
//        sqlSession.close();
//    }
//
//    @Test
//    public void TestLogin() throws IOException {
//
//        String userName1 = "admin";
//        String userName2 = "failed";
//        String password = "123456";
//
//        //1. 获取sqlSessionFactory对象
//        String resource = "mybatis-config.xml";
//        InputStream inputStream = Resources.getResourceAsStream(resource);
//        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
//
//        //2. 获取sqlSession对象
//        SqlSession sqlSession = sqlSessionFactory.openSession();
//
//        //3. 获取对应Mapper接口的代理对象
//        USERMapper userMapper = sqlSession.getMapper(USERMapper.class);
//
//        //4. 执行对应的sql语句
//        User user = userMapper.Login(userName1, password);
//
//        System.out.println(user);
//        //5. 释放资源
//        sqlSession.close();
//    }
//
//    @Test
//    public void TestRegister() throws IOException {
//
//        String userName1 = "admin";
//        String userName2 = "testUser";
//        String phoneNumber = "15850661098";
//        String email = "2720611814@qq.com";
//        String password = "097157";
//
//        User oldUser = new User(userName1, phoneNumber, email, password);
//        User newUser = new User(userName2, phoneNumber, email, password);
//
//        //1. 获取sqlSessionFactory对象
//        String resource = "mybatis-config.xml";
//        InputStream inputStream = Resources.getResourceAsStream(resource);
//        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
//
//        //2. 获取sqlSession对象
//        SqlSession sqlSession = sqlSessionFactory.openSession();
//
//        //3. 获取对应Mapper接口的代理对象
//        USERMapper userMapper = sqlSession.getMapper(USERMapper.class);
//
//        //4. 执行对应的sql语句
//        //开启事务
//        User user = userMapper.SelectByUserName(userName2);
//        if(user == null){
//            //插入新的用户
//            System.out.println("insert");
//            int count = userMapper.Register(newUser);
//            System.out.println(count);
//            sqlSession.commit();
//        }else{
//            System.out.println("exist");
//        }
//
//        //5. 释放资源
//        sqlSession.close();
//    }
//
//    /**
//     * 用户修改密码
//     * 用户首先输入用户名和邮箱，获取验证码的时候，首先验证用户名与邮箱是否绑定
//     * 若有绑定关系，则获取原用户对象，修改其密码
//     * 否则不进行修改
//     * @throws IOException
//     */
//    @Test
//    public void TestChangePassword() throws IOException {
//
//        String inputUserName = "admin";
//        String inputEmail = "8888@88.com";
//
//        String newPassword = "097157";
//
//
//        //1. 获取sqlSessionFactory对象
//        String resource = "mybatis-config.xml";
//        InputStream inputStream = Resources.getResourceAsStream(resource);
//        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
//
//        //2. 获取sqlSession对象
//        SqlSession sqlSession = sqlSessionFactory.openSession();
//
//        //3. 获取对应Mapper接口的代理对象
//        USERMapper userMapper = sqlSession.getMapper(USERMapper.class);
//
//        //4. 执行对应的sql语句
//
//        //4.1 检查输入的用户名和邮箱
//        User user = userMapper.SelectByUserName(inputUserName);
//        if(user.getEmail().equals(inputEmail)){
//            //可以修改密码
//            int count = userMapper.ChangePassword(user.getUserName(), newPassword);
//            //提交事务
//            sqlSession.commit();
//            System.out.println("password changed! and count = " + count);
//        }else{
//            //不可以修改密码
//            System.out.println("change failed, email or username is wrong!");
//        }
//
//        //5. 释放资源
//        sqlSession.close();
//    }
//
//    @Test
//    public void TestInsertLoginRecord() throws IOException {
//
//        String inputUserName = "admin";
//        String uuid = UUID.randomUUID().toString();
//
//
//        //1. 获取sqlSessionFactory对象
//        String resource = "mybatis-config.xml";
//        InputStream inputStream = Resources.getResourceAsStream(resource);
//        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
//
//        //2. 获取sqlSession对象
//        SqlSession sqlSession = sqlSessionFactory.openSession();
//
//        //3. 获取对应Mapper接口的代理对象
//        USERMapper userMapper = sqlSession.getMapper(USERMapper.class);
//
//        //4. 执行对应的sql语句
//        int count = userMapper.InsertLoginRecord(uuid, inputUserName);
//        //提交事务
//        sqlSession.commit();
//
//        System.out.println(count);
//
//        //5. 释放资源
//        sqlSession.close();
//    }
//
//    @Test
//    public void TestGetLastUpdate() throws IOException {
//
//        String uuid = "2da2666e-d717-4141-8ec2-0209ddc62a98";
//        //老用户更新
////        String update = "2023-02-11";
//        //新用户更新
//        String update = "2020-01-01";
//        String sleep = "";
//        String exercise = "";
//
//
//        //1. 获取sqlSessionFactory对象
//        String resource = "mybatis-config.xml";
//        InputStream inputStream = Resources.getResourceAsStream(resource);
//        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
//
//        //2. 获取sqlSession对象
//        SqlSession sqlSession = sqlSessionFactory.openSession();
//
//        //3. 获取对应Mapper接口的代理对象
//        USERMapper userMapper = sqlSession.getMapper(USERMapper.class);
//
//        //4. 执行对应的sql语句
//        userMapper.SetLastUpdate(uuid, update);
////        userMapper.SetStatus(uuid, sleep, exercise);
//        sqlSession.commit();
//
//        //5. 释放资源
//        sqlSession.close();
//    }
//
//    @Test
//    public void TestInsertHealthData() throws IOException, ParseException {
//
//        String uuid = "2da2666e-d717-4141-8ec2-0209ddc62a98";
//        HealthDataItem item = new HealthDataItem();
//        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
//        item.setTime(format.parse("2023-01-24"));
//        item.setWalkingDistance(2.28);
//        item.setWalkTime(26);
//        item.setRunTime(12);
//        item.setBreathExTime(8);
//        item.setSleepTime(428);
//
//
//        //1. 获取sqlSessionFactory对象
//        String resource = "mybatis-config.xml";
//        InputStream inputStream = Resources.getResourceAsStream(resource);
//        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
//
//        //2. 获取sqlSession对象
//        SqlSession sqlSession = sqlSessionFactory.openSession();
//
//        //3. 获取对应Mapper接口的代理对象
//        HEALTH_DATAMapper healthDataMapper = sqlSession.getMapper(HEALTH_DATAMapper.class);
//
//        //4. 执行对应的sql语句
//        healthDataMapper.InsertHealthDataItem(uuid, item);
//        sqlSession.commit();
//
//        //5. 释放资源
//        sqlSession.close();
//    }
//
//    @Test
//    public void TestSelectDist() throws IOException, ParseException {
//
//        String uuid = "2da2666e-d717-4141-8ec2-0209ddc62a98";
//
//
//        //1. 获取sqlSessionFactory对象
//        String resource = "mybatis-config.xml";
//        InputStream inputStream = Resources.getResourceAsStream(resource);
//        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
//
//        //2. 获取sqlSession对象
//        SqlSession sqlSession = sqlSessionFactory.openSession();
//
//        //3. 获取对应Mapper接口的代理对象
//        HEALTH_DATAMapper healthDataMapper = sqlSession.getMapper(HEALTH_DATAMapper.class);
//
//        //4. 执行对应的sql语句
//        List<Double> distList = healthDataMapper.SelectRecentWalkingDistance(uuid);
//        List<Integer> walkingTimeList = healthDataMapper.SelectRecentWalkingTime(uuid);
//        List<Integer> runningTimeList = healthDataMapper.SelectRecentRunningTime(uuid);
//        List<Integer> breathExTimeList = healthDataMapper.SelectRecentBreathExTime(uuid);
//        List<Integer> sleepingTimeList = healthDataMapper.SelectRecentSleepingTime(uuid);
//
//        //5. 释放资源
//        sqlSession.close();
//
//        System.out.println(distList);
//        System.out.println(walkingTimeList);
//        System.out.println(runningTimeList);
//        System.out.println(breathExTimeList);
//        System.out.println(sleepingTimeList);
//
//    }
//
//    @Test
//    public void TestGetScore() throws IOException, ParseException {
//
//        String uuid = "2da2666e-d717-4141-8ec2-0209ddc62a98";
//
//
//        //1. 获取sqlSessionFactory对象
//        String resource = "mybatis-config.xml";
//        InputStream inputStream = Resources.getResourceAsStream(resource);
//        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
//
//        //2. 获取sqlSession对象
//        SqlSession sqlSession = sqlSessionFactory.openSession();
//
//        //3. 获取对应Mapper接口的代理对象
//        USERMapper userMapper = sqlSession.getMapper(USERMapper.class);
//
//        //4. 执行对应的sql语句
//        userMapper.SetScore(uuid, 0.4, 0.05);
//        sqlSession.commit();
//        //5. 释放资源
//        sqlSession.close();
//    }
//
//    @Test
//    public void TestInsertGameObjects() throws IOException, ParseException {
//
//        String uuid = "2da2666e-d717-4141-8ec2-0209ddc62a98";
//
//        MyTree tree = new MyTree();
//        Animal animal = new Animal();
//
//        tree.setType(1);
//        tree.setPos_x(109.87F);
//        tree.setPos_y(287.09F);
//        tree.setPos_z(826.92F);
//        tree.setGrowDegree(20);
//
//        animal.setType(5);
//
//
//        //1. 获取sqlSessionFactory对象
//        String resource = "mybatis-config.xml";
//        InputStream inputStream = Resources.getResourceAsStream(resource);
//        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
//
//        //2. 获取sqlSession对象
//        SqlSession sqlSession = sqlSessionFactory.openSession();
//
//        //3. 获取对应Mapper接口的代理对象
//        GameDataMapper gameMapper = sqlSession.getMapper(GameDataMapper.class);
//
//        //4. 执行对应的sql语句
//        gameMapper.InsertTree(uuid, tree);
//        gameMapper.InsertAnimal(uuid, animal);
//        sqlSession.commit();
//        //5. 释放资源
//        sqlSession.close();
//    }
//
//    @Test
//    public void TestAddGameObjects() throws IOException, ParseException {
//
//        String uuid = "2da2666e-d717-4141-8ec2-0209ddc62a98";
//
//        //1. 获取sqlSessionFactory对象
//        String resource = "mybatis-config.xml";
//        InputStream inputStream = Resources.getResourceAsStream(resource);
//        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
//
//        //2. 获取sqlSession对象
//        SqlSession sqlSession = sqlSessionFactory.openSession();
//
//        //3. 获取对应Mapper接口的代理对象
//        GameDataMapper gameMapper = sqlSession.getMapper(GameDataMapper.class);
//        HEALTH_DATAMapper healthyMapper = sqlSession.getMapper(HEALTH_DATAMapper.class);
//
//        //4. 执行对应的sql语句
//        Player player = gameMapper.GetPlayer(uuid);
//        List<HealthDataItem> dataItems = healthyMapper.GetHealthDataItems(uuid);
//
//        System.out.println(player);
//        System.out.println(dataItems);
//
//        //5. 释放资源
//        sqlSession.commit();
//        //5. 释放资源
//        sqlSession.close();
//    }
//}
