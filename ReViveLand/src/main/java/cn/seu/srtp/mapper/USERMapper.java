package cn.seu.srtp.mapper;

//根据mapper配置文件中的方法，在对应接口中写方法

//方法名称对应配置文件中sql语句的id

//返回值为sql语句中配置的返回类型

import cn.seu.srtp.pojo.User;
import org.apache.ibatis.annotations.Param;

import java.util.List;

//具体返回一个对象还是集合要根据sql的语义进行判断
public interface USERMapper {
    List<User> SelectAllUser();

    User SelectByUserName(String userName);

    /**
     * 条件查询
     *  *参数接收
     *      1. 散装参数：如果方法中有多个参数，需要使用@Param("SQL占位符的名称")
     *      2. 对象参数：
     *      3. map集合参数：
     */

    User Login(@Param("userName") String userName, @Param("password") String password);

    int Register(User newUser);
}
