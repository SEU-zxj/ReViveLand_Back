package cn.seu.srtp.pojo;

public class Player {
    private int treeNum;
    private int animalNum;
    private double treeScore;
    private double animalScore;

    public int getTreeNum() {
        return treeNum;
    }

    public void setTreeNum(int treeNum) {
        this.treeNum = treeNum;
    }

    public int getAnimalNum() {
        return animalNum;
    }

    public void setAnimalNum(int animalNum) {
        this.animalNum = animalNum;
    }

    public double getTreeScore() {
        return treeScore;
    }

    public void setTreeScore(double treeScore) {
        this.treeScore = treeScore;
    }

    public double getAnimalScore() {
        return animalScore;
    }

    public void setAnimalScore(double animalScore) {
        this.animalScore = animalScore;
    }

    @Override
    public String toString() {
        return "Player{" +
                "treeNum=" + treeNum +
                ", animalNum=" + animalNum +
                ", treeScore=" + treeScore +
                ", animalScore=" + animalScore +
                '}';
    }
}
