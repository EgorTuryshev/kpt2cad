package ru.rkomi.kpt2cad.xslt;

import org.locationtech.jts.geom.MultiPolygon;

public class GeometryFeature {
    private String srcFile;
    private String dateUpload;
    private String cadNum;
    private String cadQrtr;
    private String area;
    private String skId;
    private String category;
    private String permitUse;
    private String address;
    private MultiPolygon geometry;

    public GeometryFeature(String srcFile, String dateUpload, String cadNum, String cadQrtr, String area, String skId,
                           String category, String permitUse, String address, MultiPolygon geometry) {
        this.srcFile = srcFile;
        this.dateUpload = dateUpload;
        this.cadNum = cadNum;
        this.cadQrtr = cadQrtr;
        this.area = area;
        this.skId = skId;
        this.category = category;
        this.permitUse = permitUse;
        this.address = address;
        this.geometry = geometry;
    }

    public String getSrcFile() { return srcFile; }
    public void setSrcFile(String srcFile) { this.srcFile = srcFile; }

    public String getDateUpload() { return dateUpload; }
    public void setDateUpload(String dateUpload) { this.dateUpload = dateUpload; }

    public String getCadNum() { return cadNum; }
    public void setCadNum(String cadNum) { this.cadNum = cadNum; }

    public String getCadQrtr() { return cadQrtr; }
    public void setCadQrtr(String cadQrtr) { this.cadQrtr = cadQrtr; }

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }

    public String getSkId() { return skId; }
    public void setSkId(String skId) { this.skId = skId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getPermitUse() { return permitUse; }
    public void setPermitUse(String permitUse) { this.permitUse = permitUse; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public MultiPolygon getGeometry() { return geometry; }
    public void setGeometry(MultiPolygon geometry) { this.geometry = geometry; }

    @Override
    public String toString() {
        return "GeometryFeature{" +
                "srcFile='" + srcFile + '\'' +
                ", dateUpload='" + dateUpload + '\'' +
                ", cadNum='" + cadNum + '\'' +
                ", cadQrtr='" + cadQrtr + '\'' +
                ", area='" + area + '\'' +
                ", skId='" + skId + '\'' +
                ", category='" + category + '\'' +
                ", permitUse='" + permitUse + '\'' +
                ", address='" + address + '\'' +
                ", geometry=" + geometry +
                '}';
    }
}

