package cn.seu.srtp.pojo;

public class Animal {
    private String playerName;
    private int type;

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "Animal{" +
                "playerName='" + playerName + '\'' +
                ", type=" + type +
                '}';
    }
}
