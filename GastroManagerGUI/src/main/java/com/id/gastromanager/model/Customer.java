package com.id.gastromanager.model;

import java.sql.ResultSet;
import java.sql.SQLException;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleStringProperty;

public class Customer {
    private int customerId;
    private String email;
    private String name;
    private String surname;
	private final SimpleStringProperty addressProperty = new SimpleStringProperty("");
	private final SimpleStringProperty cityProperty = new SimpleStringProperty("");
    private String phone;
	private String passwordHash;

	public Customer(int customerId, String email, String name, String surname, String address, String city,
			String phone, String passwordHash) {
		this.customerId = customerId;
		this.email = email;
		this.name = name;
		this.surname = surname;
		setAddress(address);
		setCity(city);
		this.phone = phone;
		this.passwordHash = passwordHash;
	}

	public Customer(ResultSet resultSet) throws SQLException {
		this.customerId = resultSet.getInt("customer_id");
		this.email = resultSet.getString("email");
		this.name = resultSet.getString("name");
		this.surname = resultSet.getString("surname");
		setAddress(resultSet.getString("address"));
		setCity(resultSet.getString("city"));
		this.phone = resultSet.getString("phone");
		this.passwordHash = resultSet.getString("password_hash");
	}

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getAddress() {
		return addressProperty.getValue();
    }

    public void setAddress(String address) {
		this.addressProperty.setValue(address);
    }

    public String getCity() {
		return cityProperty.getValue();
    }

    public void setCity(String city) {
		this.cityProperty.setValue(city);
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public ReadOnlyStringProperty getCityProperty() {
		return cityProperty;
	}

	public ReadOnlyStringProperty getAddressProperty() {
		return addressProperty;
	}

	public boolean isSystemUser() {
		return customerId == 0;
	}

    @Override
    public String toString() {
        return "customerDetails{" +
                "customerId=" + customerId +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", surname='" + surname + '\'' +
				", address='" + getAddress() + '\'' + ", city='" + getCity() + '\'' +
                ", phone='" + phone + '\'' +
                '}';
    }
}
