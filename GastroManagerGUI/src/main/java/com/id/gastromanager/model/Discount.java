package com.id.gastromanager.model;

public class Discount {
    private int discountId;
    private String dateFrom;
    private String dateTo;
    private int discount;

	public Discount(int discountId, String dateFrom, String dateTo, int discount) {
		this.discountId = discountId;
		this.dateFrom = dateFrom;
		this.dateTo = dateTo;
		this.discount = discount;
	}

    public void setDateFrom(String dateFrom) {
        this.dateFrom = dateFrom;
    }

    public void setDateTo(String dateTo) {
        this.dateTo = dateTo;
    }

    public void setDiscount(int discount) {
        this.discount = discount;
    }

    public void setDiscountId(int discountId) {
        this.discountId = discountId;
    }

    public int getDiscount() {
        return discount;
    }

    public int getDiscountId() {
        return discountId;
    }

    public String getDateFrom() {
        return dateFrom;
    }

    public String getDateTo() {
        return dateTo;
    }
    @Override
    public String toString() {
        return "Discounts{" +
                "discountId=" + discountId +
                ", dateFrom='" + dateFrom + '\'' +
                ", dateTo='" + dateTo + '\'' +
                ", discount=" + discount +
                '}';
    }


}
