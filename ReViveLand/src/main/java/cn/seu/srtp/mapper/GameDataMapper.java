package cn.seu.srtp.mapper;

import cn.seu.srtp.pojo.Animal;
import cn.seu.srtp.pojo.MyTree;
import cn.seu.srtp.pojo.Player;
import org.apache.ibatis.annotations.Param;

public interface GameDataMapper {

    void InsertTree(@Param("uuid") String uuid, @Param("tree") MyTree myTree);

    void InsertAnimal(@Param("uuid") String uuid, @Param("animal") Animal animal);

    Player GetPlayer(@Param("uuid") String uuid);

}
