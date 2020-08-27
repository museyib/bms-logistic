package az.inci.bmslogistic;

import androidx.annotation.NonNull;

public class User {

    private String id;
    private String password;
    private String name;
    private String pickGroup;
    private boolean collectFlag;
    private boolean pickFlag;
    private boolean checkFlag;
    private boolean countFlag;
    private boolean locationFlag;
    private boolean packFlag;
    private boolean docFlag;
    private boolean loadingFlag;

    String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    String getPassword() {
        return password;
    }

    void setPassword(String password) {
        this.password = password;
    }

    boolean isCollect() {
        return collectFlag;
    }

    void setCollectFlag(boolean collectFlag) {
        this.collectFlag = collectFlag;
    }

    boolean isPick() {
        return pickFlag;
    }

    void setPickFlag(boolean pickFlag) {
        this.pickFlag = pickFlag;
    }

    boolean isCheck() {
        return checkFlag;
    }

    void setCheckFlag(boolean checkFlag) {
        this.checkFlag = checkFlag;
    }

    boolean isCount() {
        return countFlag;
    }

    void setCountFlag(boolean countFlag) {
        this.countFlag = countFlag;
    }

    boolean isLocation() {
        return locationFlag;
    }

    void setLocationFlag(boolean locationFlag) {
        this.locationFlag = locationFlag;
    }

    boolean isPack() {
        return packFlag;
    }

    void setPackFlag(boolean packFlag) {
        this.packFlag = packFlag;
    }

    boolean isDoc() {
        return docFlag;
    }

    void setDocFlag(boolean docFlag) {
        this.docFlag = docFlag;
    }

    String getPickGroup() {
        return pickGroup;
    }

    void setPickGroup(String pickGroup) {
        this.pickGroup = pickGroup;
    }

    String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    public boolean isLoading() {
        return loadingFlag;
    }

    public void setLoadingFlag(boolean loadingFlag) {
        this.loadingFlag = loadingFlag;
    }

    @NonNull
    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", password='" + password + '\'' +
                ", name='" + name + '\'' +
                ", pickGroup='" + pickGroup + '\'' +
                ", collectFlag=" + collectFlag +
                ", pickFlag=" + pickFlag +
                ", checkFlag=" + checkFlag +
                ", countFlag=" + countFlag +
                ", locationFlag=" + locationFlag +
                ", packFlag=" + packFlag +
                ", docFlag=" + docFlag +
                ", loadingFlag=" + loadingFlag +
                '}';
    }
}
