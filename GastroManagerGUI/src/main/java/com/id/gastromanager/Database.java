package com.id.gastromanager;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import com.google.common.hash.Hashing;
import com.id.gastromanager.model.Customer;
import com.id.gastromanager.model.MenuPosition;
import com.id.gastromanager.model.Restaurant;

public final class Database {
	private static Connection connection;

	public static void initConnection(String host, String port, String database, String username, String password)
			throws Exception {
		if (connection != null) {
			throw new UnsupportedOperationException("Database connection already initialized");
		}
		Class.forName("org.postgresql.Driver");
		String url = String.format("jdbc:postgresql://%s:%s/%s", host, port, database);
		connection = DriverManager.getConnection(url, username, password);
		System.out.println("Connected to database " + database);
	}

	public static Customer getCustomer(String email) throws SQLException {
		Statement statement = connection.createStatement();

		// language=SQL
		String query = """
				select * from customer_details where email = '%s';
				""".formatted(email);

		ResultSet resultSet = statement.executeQuery(query);
		try {
			if (!resultSet.next()) {
				return null;
			}
			return new Customer(resultSet);
		} finally {
			statement.close();
		}
	}

	public static boolean isPasswordCorrect(String email, String password) throws SQLException {
		@SuppressWarnings("UnstableApiUsage")
		String hash = Hashing.sha256().hashString(password, StandardCharsets.UTF_8).toString();

		Statement statement = connection.createStatement();

		// language=SQL
		String query = """
				select * from customer_details where email = '%s' and password_hash = '%s';
				""".formatted(email, hash);

		ResultSet resultSet = statement.executeQuery(query);
		try {
			return resultSet.next();
		} finally {
			statement.close();
		}
	}

	public static boolean isSystemUser(String email) throws SQLException {
		Statement statement = connection.createStatement();

		// language=SQL
		String query = """
				select customer_id from customer_details where email = '%s';
				""".formatted(email);

		ResultSet resultSet = statement.executeQuery(query);
		try {
			return resultSet.next() && resultSet.getInt("customer_id") == 0;
		} finally {
			statement.close();
		}
	}

	public static void insertCustomer(String email, String name, String surname, String address, String city,
			String phone, String password) throws SQLException {
		Statement statement = connection.createStatement();

		ResultSet resultSet = statement.executeQuery("select max(customer_id) from customers");
		resultSet.next(); // mamy pewność, że w tabeli jest przynajmniej użytkownik systemowy
		int maxId = (int) resultSet.getObject(1);

		@SuppressWarnings("UnstableApiUsage")
		String hash = Hashing.sha256().hashString(password, StandardCharsets.UTF_8).toString();

		// language=SQL
		String insertion = """
				insert into customers values (%d);
				insert into customer_details values (%d, '%s', '%s','%s','%s','%s','%s','%s');
				""".formatted(maxId + 1, maxId + 1, email, name, surname, address, city, phone, hash);

		statement.execute(insertion);
		statement.close();
	}

	public static void changeAddressAndCity(Customer customer, String address, String city) throws SQLException {
		Statement statement = connection.createStatement();

		String update = """
				update customer_details set address = '%s' where customer_id = %d;
				update customer_details set city = '%s' where customer_id = %d;
				""".formatted(address, customer.getCustomerId(), city, customer.getCustomerId());

		statement.execute(update);
		customer.setAddress(address);
		customer.setCity(city);
		statement.close();
	}

	public static List<Restaurant> getRestaurants() throws SQLException {
		Statement statement = connection.createStatement();
		ResultSet resultSet = statement.executeQuery("select * from restaurants;");
		ArrayList<Restaurant> restaurants = new ArrayList<>();
		while (resultSet.next()) {
			restaurants.add(new Restaurant(resultSet));
		}
		statement.close();
		return restaurants;
	}

	public static List<MenuPosition> getMenuPostions(Restaurant restaurant) throws SQLException {
		Statement statement = connection.createStatement();

		// language=SQL
		String query = """
				    select d.*, m.diet, m.allergens, c.category_name
					from menu_positions m
				             join dishes d on m.dish_id = d.dish_id
				             join categories c on d.category_id = c.category_id
					where is_available = true and restaurant_id = %d
					order by d.dish_name;
				""".formatted(restaurant.getRestaurantId());

		ResultSet resultSet = statement.executeQuery(query);
		ArrayList<MenuPosition> menuPositions = new ArrayList<>();
		while (resultSet.next()) {
			menuPositions.add(new MenuPosition(resultSet));
		}
		statement.close();
		return menuPositions;
	}
}
