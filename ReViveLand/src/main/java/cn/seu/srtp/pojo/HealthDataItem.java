package cn.seu.srtp.pojo;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 数据库中健康数据表中的表项
 */
public class HealthDataItem {
    //表项记录的日期 yyyy-MM-dd
    private Date time;
    private String userName;
    private double walkingDistance;
    //时间都精确到分钟，小数点砍掉
    private int walkTime;

    private int runTime;
    private int breathExTime;
    private int sleepTime;

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public double getWalkingDistance() {
        return walkingDistance;
    }

    public void addWalkingDistance(double dist){
        this.walkingDistance += dist;
    }

    public void setWalkingDistance(double walkingDistance) {
        this.walkingDistance = walkingDistance;
    }

    public int getWalkTime() {
        return walkTime;
    }

    public void addWalkTime(int time){
        this.walkTime += time;
    }

    public void setWalkTime(int walkTime) {
        this.walkTime = walkTime;
    }

    public int getRunTime() {
        return this.runTime;
    }
    public void addRunTime(int time){
        this.runTime += time;
    }
    public void setRunTime(int runTime) {
        this.runTime = runTime;
    }

    public int getBreathExTime() {
        return this.breathExTime;
    }

    public void addBreathExTime(int time){
        this.breathExTime += time;
    }

    public void setBreathExTime(int breathExTime) {
        this.breathExTime = breathExTime;
    }

    public int getSleepTime() {
        return this.sleepTime;
    }
    public void addSleepTime(int time){
        this.sleepTime += time;
    }
    public void setSleepTime(int sleepTime) {
        this.sleepTime = sleepTime;
    }

    @Override
    public String toString() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return "HealthDataItem{" +
                "time=" + dateFormat.format(time) +
                ", userName='" + userName + '\'' +
                ", walkingDistance=" + walkingDistance +
                ", walkTime=" + walkTime +
                ", runTime=" + runTime +
                ", breathExTime=" + breathExTime +
                ", sleepTime=" + sleepTime +
                '}';
    }

}
