package cn.seu.srtp.mapper;

import cn.seu.srtp.pojo.Advice;
import cn.seu.srtp.pojo.HealthDataItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface HEALTH_DATAMapper {
    void InsertHealthDataItem(@Param("uuid") String uuid, @Param("item") HealthDataItem item);

    //查询数据库中某个用户近100天的步行距离数据
    List<Double> SelectRecentWalkingDistance(@Param("uuid") String uuid);
    //查询数据库中某个用户近100天的步行时间数据
    List<Integer> SelectRecentWalkingTime(@Param("uuid") String uuid);

    //查询数据库中某个用户近100天跑步时间数据
    List<Integer> SelectRecentRunningTime(@Param("uuid") String uuid);

    //查询数据库中某个用户近100天呼吸训练时间数据
    List<Integer> SelectRecentBreathExTime(@Param("uuid") String uuid);

    //查询数据库中某个用户近100天的睡眠时间数据
    List<Integer> SelectRecentSleepingTime(@Param("uuid") String uuid);

    List<Advice> GetAdvice(@Param("uuid") String uuid);

    /**
     * 获得用户近7天的健康数据(从大到小排序，所以获取到数据之后还要再逆转一次)
     * @return
     */
    List<HealthDataItem> GetHealthDataItems(@Param("uuid") String uuid);
}
