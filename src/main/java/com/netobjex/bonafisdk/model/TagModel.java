package com.netobjex.bonafisdk.model;

public class TagModel {
    private String tag;
    private String hash;
    private String identification;
    private Double cost;
    private String manufacturer;
    private String serialNo;
    private String dateOfManufacture;
    private String authorizedStore;
    private String phone;
    private String dateOfFirstArrivalAtStore;
    private String dateOfFirstSold;
    private String originalOwnerRegistration;
    private String email;
    private String gift;

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getTag() {
        return tag;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getIdentification() {
        return identification;
    }

    public void setIdentification(String identification) {
        this.identification = identification;
    }

    public Double getCost() {
        return cost;
    }

    public void setCost(Double cost) {
        this.cost = cost;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getSerialNo() {
        return serialNo;
    }

    public void setSerialNo(String serialNo) {
        this.serialNo = serialNo;
    }

    public String getAuthorizedStore() {
        return authorizedStore;
    }

    public void setAuthorizedStore(String authorizedStore) {
        this.authorizedStore = authorizedStore;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getOriginalOwnerRegistration() {
        return originalOwnerRegistration;
    }

    public void setOriginalOwnerRegistration(String originalOwnerRegistration) {
        this.originalOwnerRegistration = originalOwnerRegistration;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDateOfManufacture() {
        return dateOfManufacture;
    }

    public void setDateOfManufacture(String dateOfManufacture) {
        this.dateOfManufacture = dateOfManufacture;
    }

    public String getDateOfFirstArrivalAtStore() {
        return dateOfFirstArrivalAtStore;
    }

    public void setDateOfFirstArrivalAtStore(String dateOfFirstArrivalAtStore) {
        this.dateOfFirstArrivalAtStore = dateOfFirstArrivalAtStore;
    }

    public String getDateOfFirstSold() {
        return dateOfFirstSold;
    }

    public void setDateOfFirstSold(String dateOfFirstSold) {
        this.dateOfFirstSold = dateOfFirstSold;
    }

    public String getGift() {
        return gift;
    }

    public void setGift(String gift) {
        this.gift = gift;
    }

    @Override
    public String toString() {
        return "TagModel [tag="+tag+"]";
    }
}
