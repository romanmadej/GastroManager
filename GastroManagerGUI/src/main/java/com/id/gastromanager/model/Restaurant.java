package com.id.gastromanager.model;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Restaurant {
    private int restaurantId;
    private String address;
    private String city;
	private String postalCode;
    private String phone;

	public Restaurant(int restaurantId, String address, String city, String postalCode, String phone) {
		this.restaurantId = restaurantId;
		this.address = address;
		this.city = city;
		this.postalCode = postalCode;
		this.phone = phone;
	}

	public Restaurant(ResultSet resultSet) throws SQLException {
		this.restaurantId = resultSet.getInt("restaurant_id");
		this.address = resultSet.getString("address");
		this.city = resultSet.getString("city");
		this.postalCode = resultSet.getString("postal_code");
		this.phone = resultSet.getString("phone");
	}

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPhone() {
        return phone;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCity() {
        return city;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
    }

	public String getPostalCode() {
		return postalCode;
    }

    public void setRestaurantId(int restaurantId) {
        this.restaurantId = restaurantId;
    }

    public int getRestaurantId() {
        return restaurantId;
    }

    @Override
    public String toString() {
        return "Restaurants{" +
                "restaurantId=" + restaurantId +
                ", address='" + address + '\'' +
                ", city='" + city + '\'' +
				", postal_code='" + postalCode + '\'' +
                ", phone='" + phone + '\'' +
                '}';
    }
}

