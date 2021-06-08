package com.id.gastromanager;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.hash.Hashing;
import com.id.gastromanager.model.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

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
				select d.*, m.diet, m.allergens, m.price, c.category_name
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

	public static Map<Integer, Map<Integer, Integer>> getDishIngredientQuantityMap() throws SQLException {
		Statement statement = connection.createStatement();

		// language=SQL
		String query = """
				select * from dish_ingredients;
				""";

		ResultSet resultSet = statement.executeQuery(query);
		Map<Integer, Map<Integer, Integer>> dishIngredientMap = new HashMap<>();
		while (resultSet.next()) {
			dishIngredientMap.computeIfAbsent(resultSet.getInt("dish_id"), k -> new HashMap<>())
					.put(resultSet.getInt("ingredient_id"), resultSet.getInt("quantity"));
		}
		statement.close();
		return dishIngredientMap;
	}

	public static Map<Integer, Integer> getStock(Restaurant restaurant) throws SQLException {
		Statement statement = connection.createStatement();

		// language=SQL
		String query = """
				select * from stock where restaurant_id = %d
				""".formatted(restaurant.getRestaurantId());

		ResultSet resultSet = statement.executeQuery(query);
		Map<Integer, Integer> stock = new HashMap<>();
		while (resultSet.next()) {
			stock.put(resultSet.getInt("ingredient_id"), resultSet.getInt("quantity"));
		}
		statement.close();
		return stock;
	}

	public static double getDiscountPrice(Dish dish, Customer customer) throws SQLException {
		Statement statement = connection.createStatement();

		// language=SQL
		String query = """
				select dish_discounted_price(%d, %d, null);
				""".formatted(dish.getDishId(), customer.getCustomerId());

		ResultSet resultSet = statement.executeQuery(query);
		try {
			resultSet.next();
			return resultSet.getDouble(1);
		} finally {
			statement.close();
		}
	}

	public static void submitOrder(Customer customer, Restaurant restaurant, List<MenuPosition> cartContents,
			boolean isDelivery) throws SQLException {
		Statement statement = connection.createStatement();

		ResultSet resultSet = statement.executeQuery("select max(order_id) from orders");

		int orderId = 1;
		if (resultSet.next()) {
			orderId = 1 + (int) resultSet.getObject(1);
		}

		// language=SQL
		StringBuilder sb = new StringBuilder("start transaction;\n");

		// language=SQL
		sb.append("""
				insert into orders (order_id, customer_id, restaurant_id, ordered_date, status, is_delivery)
				values (%d, %d, %d, now(), 'open', %s);
				""".formatted(orderId, customer.getCustomerId(), restaurant.getRestaurantId(), isDelivery));

		for (MenuPosition menuPosition : cartContents) {
			// language=SQL
			sb.append("""
					insert into order_details (order_id, dish_id, quantity)
					values (%d, %d, %d);
					""".formatted(orderId, menuPosition.getDishId(), menuPosition.numberInCartProperty.get()));
		}

		// language=SQL
		sb.append("commit;");

		String transaction = sb.toString();
		try {
			statement.execute(transaction);
		} catch (SQLException e) {
			statement.executeQuery("rollback;");
			throw e;
		}
		statement.close();
	}

//	public static boolean canDeleteRestaurant(int restaurantId) throws SQLException{
//		Statement statement = connection.createStatement();
//
//		// language=SQL
//		String query = """
//				    select count(*) as cnt from orders where restaurant_id = %d
//				""".formatted(restaurantId);
//
//		ResultSet resultSet = statement.executeQuery(query);
//		resultSet.next();
//		return resultSet.getInt("cnt")==0;
//	}

	//	public static boolean canDeleteIngredient(int ingredientId) throws SQLException{
//		Statement statement = connection.createStatement();
//
//		// language=SQL
//		String query = """
//				    select count(*) as cnt from dish_ingredients where ingredient_id = %d
//				""".formatted(ingredientId);
//
//		ResultSet resultSet = statement.executeQuery(query);
//		resultSet.next();
//		return resultSet.getInt("cnt")==0;
//	}
	public static int deleteCustomer(int customerId)throws SQLException {
		connection.setAutoCommit(false);
// Procedure call.
		CallableStatement delete_customer = connection.prepareCall("{ ? = call delete_customer( ? ) }");
		delete_customer.registerOutParameter(1, Types.INTEGER);
		delete_customer.setInt(2, customerId);
		delete_customer.execute();
		int res = delete_customer.getInt(1);
		delete_customer.close();
		return res;

	}

	public static int deleteRestaurant(int restaurantId) throws SQLException{
		connection.setAutoCommit(false);
// Procedure call.
		CallableStatement delete_restaurant = connection.prepareCall("{ ? = call delete_restaurant( ? ) }");
		delete_restaurant.registerOutParameter(1, Types.INTEGER);
		delete_restaurant.setInt(2, restaurantId);
		delete_restaurant.execute();
		int res = delete_restaurant.getInt(1);
		delete_restaurant.close();
		return res;
	}

	public static void addDish(String name, List<IngredientsQuantity> ingredientsList, double price, String Category) {
	}


	public static int deleteIngredient(int ingredientId) throws SQLException{
		connection.setAutoCommit(false);
// Procedure call.
		CallableStatement delete_ingredient = connection.prepareCall("{ ? = call delete_ingredient( ? ) }");
		delete_ingredient.registerOutParameter(1, Types.INTEGER);
		delete_ingredient.setInt(2, ingredientId);
		delete_ingredient.execute();
		int res = delete_ingredient.getInt(1);
		delete_ingredient.close();
		return res;

	}

	public static int deleteDish(int dishId) throws SQLException {
		connection.setAutoCommit(false);
// Procedure call.
		CallableStatement delete_dish = connection.prepareCall("{ ? = call delete_dish( ? ) }");
		delete_dish.registerOutParameter(1, Types.INTEGER);
		delete_dish.setInt(2, dishId);
		delete_dish.execute();
		int res = delete_dish.getInt(1);
		delete_dish.close();
		return res;
	}

	public static int deleteDishIngredient(int dishId,int ingredientId) throws SQLException {
		connection.setAutoCommit(false);
// Procedure call.
		CallableStatement delete_dish_ingredient = connection.prepareCall("{ ? = call delete_dish_ingredient( ?,? ) }");
		delete_dish_ingredient.registerOutParameter(1, Types.INTEGER);
		delete_dish_ingredient.setInt(2, dishId);
		delete_dish_ingredient.setInt(3, ingredientId);
		delete_dish_ingredient.execute();
		int res = delete_dish_ingredient.getInt(1);
		delete_dish_ingredient.close();
		return res;
	}

	public static int deleteDiscount(int discountId) throws SQLException {
		connection.setAutoCommit(false);
// Procedure call.
		CallableStatement delete_discount = connection.prepareCall("{ ? = call delete_discount( ? ) }");
		delete_discount.registerOutParameter(1, Types.INTEGER);
		delete_discount.setInt(2, discountId);
		delete_discount.execute();
		int res = delete_discount.getInt(1);
		delete_discount.close();
		return res;
	}

	public static int deleteSpecialDate(int specialDateId) throws SQLException {
		connection.setAutoCommit(false);
// Procedure call.
		CallableStatement delete_special_date = connection.prepareCall("{ ? = call delete_special_date( ? ) }");
		delete_special_date.registerOutParameter(1, Types.INTEGER);
		delete_special_date.setInt(2, specialDateId);
		delete_special_date.execute();
		int res = delete_special_date.getInt(1);
		delete_special_date.close();
		return res;
	}

	public static int deleteCategory(int categoryId) throws SQLException{
		connection.setAutoCommit(false);
// Procedure call.
		CallableStatement delete_category = connection.prepareCall("{ ? = call delete_category( ? ) }");
		delete_category.registerOutParameter(1, Types.INTEGER);
		delete_category.setInt(2, categoryId);
		delete_category.execute();
		int res = delete_category.getInt(1);
		delete_category.close();
		return res;

	}
	public static ObservableList<String> getCustomers() throws SQLException {
		Statement statement = connection.createStatement();
		// language=SQL
		String query = """
   				select * from customers;
			""";
		ResultSet resultSet = statement.executeQuery(query);
		ObservableList <String> customers = FXCollections.observableArrayList();
		while(resultSet.next()){
			customers.add(String.valueOf(resultSet.getInt("customer_id")));
		}
		statement.close();
		return customers;
	}
	public static ObservableList<String> getRestaurantsId() throws SQLException {
		Statement statement = connection.createStatement();
		// language=SQL
		String query = """
   				select restaurant_id from restaurants;
			""";
		ResultSet resultSet = statement.executeQuery(query);
		ObservableList <String> restaurants = FXCollections.observableArrayList();
		while(resultSet.next()){
			restaurants.add(String.valueOf(resultSet.getInt("restaurant_id")));
		}
		statement.close();
		return restaurants;
	}
	public static ObservableList<String> getDishNames() throws SQLException{
		Statement statement = connection.createStatement();
		// language=SQL
		String query = """
   				select dish_name from dishes;
			""";
		ResultSet resultSet = statement.executeQuery(query);
		ObservableList <String> restaurants = FXCollections.observableArrayList();
		while(resultSet.next()){
			restaurants.add(resultSet.getString("dish_name"));
		}
		statement.close();
		return restaurants;
	}
	public static ObservableList<String> getIngredientNames() throws SQLException{
		Statement statement = connection.createStatement();
		// language=SQL
		String query = """
   				select name from ingredients;
			""";
		ResultSet resultSet = statement.executeQuery(query);
		ObservableList <String> restaurants = FXCollections.observableArrayList();
		while(resultSet.next()){
			restaurants.add(resultSet.getString("name"));
		}
		statement.close();
		return restaurants;
	}
	public static ObservableList<String> getIngredientNames(String dish) throws SQLException{
		Statement statement = connection.createStatement();
		// language=SQL
		String query = String.format("""
   				select ingredients.name from 
   				(dishes join dish_ingredients on dishes.dish_id = dish_ingredients.dish_id) as pom 
   				join ingredients on pom.ingredient_id = ingredients.ingredient_id
   				where pom.dish_name = '%s';
			""", dish);
		ResultSet resultSet = statement.executeQuery(query);
		ObservableList <String> restaurants = FXCollections.observableArrayList();
		while(resultSet.next()){
			restaurants.add(resultSet.getString("name"));
		}
		statement.close();
		return restaurants;
	}
	public static ObservableList<String> getDiscountId() throws SQLException{
		Statement statement = connection.createStatement();
		// language=SQL
		String query = """
   				select discount_id from discounts;
			""";
		ResultSet resultSet = statement.executeQuery(query);
		ObservableList <String> restaurants = FXCollections.observableArrayList();
		while(resultSet.next()){
			restaurants.add(String.valueOf(resultSet.getInt("discount_id")));
		}
		statement.close();
		return restaurants;
	}
	public static ObservableList<String> getSpecialDateId() throws SQLException{
		Statement statement = connection.createStatement();
		// language=SQL
		String query = """
   				select special_date_id from special_dates;
			""";
		ResultSet resultSet = statement.executeQuery(query);
		ObservableList <String> restaurants = FXCollections.observableArrayList();
		while(resultSet.next()){
			restaurants.add(String.valueOf(resultSet.getInt("special_date_id")));
		}
		statement.close();
		return restaurants;
	}
	public static ObservableList<String> getCategoriesName() throws SQLException{
		Statement statement = connection.createStatement();
		// language=SQL
		String query = """
   				select category_name from categories;
			""";
		ResultSet resultSet = statement.executeQuery(query);
		ObservableList <String> restaurants = FXCollections.observableArrayList();
		while(resultSet.next()){
			restaurants.add(resultSet.getString("category_name"));
		}
		statement.close();
		return restaurants;
	}

	public static ObservableList<String> getAlergens() throws SQLException {
		Statement statement = connection.createStatement();
		// language=SQL
		String query = """
   				select name from allergens;
			""";
		ResultSet resultSet = statement.executeQuery(query);
		ObservableList<String> allergens = FXCollections.observableArrayList();
		while(resultSet.next()){
			allergens.add(resultSet.getString("name"));
		}
		statement.close();
		return allergens;
	}
	public static void addRestaurant(String address, String city, String postalCode, String phone) {
	}

	public static void addCategory(String category) {
	}

	public static void addIngredient(String name, List<String> AllergensList, Diet diet, String units) {
	}
}
