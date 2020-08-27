package az.inci.bmslogistic;

public class ShipDoc {
    private String docNo;
    private String trxNo;
    private String trxDate;
    private String bpCode;
    private String bpName;
    private String sbeCode;
    private String sbeName;
    private String shipStatus;

    public String getDocNo() {
        return docNo;
    }

    public void setDocNo(String docNo) {
        this.docNo = docNo;
    }

    public String getTrxNo() {
        return trxNo;
    }

    public void setTrxNo(String trxNo) {
        this.trxNo = trxNo;
    }

    public String getTrxDate() {
        return trxDate;
    }

    public void setTrxDate(String trxDate) {
        this.trxDate = trxDate;
    }

    public String getBpCode() {
        return bpCode;
    }

    public void setBpCode(String bpCode) {
        this.bpCode = bpCode;
    }

    public String getBpName() {
        return bpName;
    }

    public void setBpName(String bpName) {
        this.bpName = bpName;
    }

    public String getSbeCode() {
        return sbeCode;
    }

    public void setSbeCode(String sbeCode) {
        this.sbeCode = sbeCode;
    }

    public String getSbeName() {
        return sbeName;
    }

    public void setSbeName(String sbeName) {
        this.sbeName = sbeName;
    }

    public String getShipStatus() {
        return shipStatus;
    }
}
