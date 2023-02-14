package cn.seu.srtp.pojo;

public class MyTree {
    private String PlayerName;
    private int type;
    private float pos_x;
    private float pos_y;
    private float pos_z;

    private int growDegree;


    public String getPlayerName() {
        return PlayerName;
    }

    public void setPlayerName(String playerName) {
        PlayerName = playerName;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public float getPos_x() {
        return pos_x;
    }

    public void setPos_x(float pos_x) {
        this.pos_x = pos_x;
    }

    public float getPos_y() {
        return pos_y;
    }

    public void setPos_y(float pos_y) {
        this.pos_y = pos_y;
    }

    public float getPos_z() {
        return pos_z;
    }

    public void setPos_z(float pos_z) {
        this.pos_z = pos_z;
    }

    public int getGrowDegree() {
        return growDegree;
    }

    public void setGrowDegree(int growDegree) {
        this.growDegree = growDegree;
    }

    @Override
    public String toString() {
        return "Tree{" +
                "PlayerName='" + PlayerName + '\'' +
                ", type=" + type +
                ", pos_x=" + pos_x +
                ", pos_y=" + pos_y +
                ", pos_z=" + pos_z +
                ", growDegree=" + growDegree +
                '}';
    }
}
