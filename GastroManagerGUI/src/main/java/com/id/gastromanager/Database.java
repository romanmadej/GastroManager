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
		PreparedStatement statement = connection.prepareStatement("""
				update customer_details set address = ? where customer_id = ?;
				update customer_details set city = ? where customer_id = ?;
				""");

		statement.setString(1, address);
		statement.setInt(2, customer.getCustomerId());
		statement.setString(3, city);
		statement.setInt(4, customer.getCustomerId());

		statement.execute();
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

	public static boolean isOpen(Restaurant restaurant) throws SQLException {
		Statement statement = connection.createStatement();

		// language=SQL
		String query = """
				select is_open(%d);
				""".formatted(restaurant.getRestaurantId());

		ResultSet resultSet = statement.executeQuery(query);
		try {
			resultSet.next();
			return resultSet.getBoolean(1);
		} finally {
			statement.close();
		}
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

	public static int deleteCustomer(int customerId) throws SQLException {
// Procedure call.
		CallableStatement delete_customer = connection.prepareCall("{ ? = call delete_customer( ? ) }");
		delete_customer.registerOutParameter(1, Types.INTEGER);
		delete_customer.setInt(2, customerId);
		delete_customer.execute();
		int res = delete_customer.getInt(1);
		delete_customer.close();
		return res;

	}

	public static int deleteRestaurant(int restaurantId) throws SQLException {
// Procedure call.
		CallableStatement delete_restaurant = connection.prepareCall("{ ? = call delete_restaurant( ? ) }");
		delete_restaurant.registerOutParameter(1, Types.INTEGER);
		delete_restaurant.setInt(2, restaurantId);
		delete_restaurant.execute();
		int res = delete_restaurant.getInt(1);
		delete_restaurant.close();
		return res;
	}


	public static int deleteIngredient(String ingredientName) throws SQLException {
		Statement statement = connection.createStatement();

		String query = """
				select ingredient_id from ingredients where name='%s';
				""".formatted(ingredientName);

		ResultSet resultSet = statement.executeQuery(query);
		if (!resultSet.next()) {
			statement.close();
			return 2;
		}
		int ingredientId = resultSet.getInt("ingredient_id");
		statement.close();

// Procedure call.
		CallableStatement delete_ingredient = connection.prepareCall("{ ? = call delete_ingredient( ? ) }");
		delete_ingredient.registerOutParameter(1, Types.INTEGER);
		delete_ingredient.setInt(2, ingredientId);
		delete_ingredient.execute();
		int res = delete_ingredient.getInt(1);
		delete_ingredient.close();
		return res;

	}

	public static int deleteDish(String dishName) throws SQLException {
		Statement statement = connection.createStatement();

		String query = """
				select dish_id from dishes where dish_name='%s';
				""".formatted(dishName);

		ResultSet resultSet = statement.executeQuery(query);
		if (!resultSet.next()) {
			statement.close();
			return 2;
		}
		int dishId = resultSet.getInt("dish_id");
		statement.close();


// Procedure call.
		CallableStatement delete_dish = connection.prepareCall("{ ? = call delete_dish( ? ) }");
		delete_dish.registerOutParameter(1, Types.INTEGER);
		delete_dish.setInt(2, dishId);
		delete_dish.execute();
		int res = delete_dish.getInt(1);
		delete_dish.close();
		return res;
	}

	public static int deleteDishIngredient(String dishName, String ingredientName) throws SQLException {
		Statement statement = connection.createStatement();

		String dishQuery = """
				select dish_id from dishes where dish_name='%s';
				""".formatted(dishName);

		ResultSet dishResultSet = statement.executeQuery(dishQuery);
		if (!dishResultSet.next()) {
			statement.close();
			return 2;
		}
		int dishId = dishResultSet.getInt("dish_id");

		String ingredientQuery = """
				select ingredient_id from ingredients where name='%s';
				""".formatted(ingredientName);

		ResultSet ingredientResultSet = statement.executeQuery(ingredientQuery);
		if (!ingredientResultSet.next()) {
			statement.close();
			return 2;
		}
		int ingredientId = ingredientResultSet.getInt("ingredient_id");
		statement.close();

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

// Procedure call.
		CallableStatement delete_special_date = connection.prepareCall("{ ? = call delete_special_date( ? ) }");
		delete_special_date.registerOutParameter(1, Types.INTEGER);
		delete_special_date.setInt(2, specialDateId);
		delete_special_date.execute();
		int res = delete_special_date.getInt(1);
		delete_special_date.close();
		return res;
	}

	public static int deleteCategory(String categoryName) throws SQLException {
		Statement statement = connection.createStatement();

		String query = """
				select category_id from categories where category_name='%s';
				""".formatted(categoryName);

		ResultSet resultSet = statement.executeQuery(query);
		if (!resultSet.next()) {
			AlertFactory.showErrorAlert("obiekt o podanym id nie istnieje");
			statement.close();
			return 2;
		}
		int categoryId = resultSet.getInt("category_id");
		statement.close();

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
	public static void addIngredient(String name, List<String> AllergensList, Diet diet, String units) throws SQLException {
		Statement statement = connection.createStatement();

		ResultSet resultSet = statement.executeQuery("select MAX(ingredient_id) from ingredients");

		int ingredient_id = 1;
		if (resultSet.next()) {
			ingredient_id = 1 + (int) resultSet.getObject(1);
		}

		// language=SQL
		StringBuilder sb = new StringBuilder("start transaction;\n");

		// language=SQL
		sb.append("""
				insert into ingredients(ingredient_id, name, units, diet)
				values (%d, '%s', '%s', '%s');
				""".formatted(ingredient_id, name, units, diet));

		ObservableList <Integer> RestaurantId= FXCollections.observableArrayList();
		resultSet = statement.executeQuery("select restaurant_id from restaurants");
		while(resultSet.next()){
			RestaurantId.add(resultSet.getInt(1));
		}
		for (int id : RestaurantId) {
			// language=SQL
			sb.append("""
					insert into stock(ingredient_id, restaurant_id, quantity)
					values (%d, %d, %d);
					""".formatted(ingredient_id,id, 0));
		}
		//language=SQL
		for(String allergen : AllergensList){
			resultSet = statement.executeQuery("select allergen_id from allergens where name = '%s'".formatted(allergen));
			int allergenId = 1;
			if(resultSet.next()){
				allergenId = resultSet.getInt(1);
			}
			//language=SQL
			sb.append("""
    				insert into ingredients_allergens (ingredient_id, allergen_id)
    				values(%d, %d);
				""".formatted(ingredient_id, allergenId));
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

	public static void addDish(String name, List<IngredientsQuantity> ingredientsList, double price, String Category) throws SQLException {
		Statement statement = connection.createStatement();

		ResultSet resultSet = statement.executeQuery("select MAX(dish_id) from dishes");

		int dishId = 1;
		if (resultSet.next()) {
			dishId = 1 + (int) resultSet.getObject(1);
		}

		// language=SQL
		StringBuilder sb = new StringBuilder("start transaction;\n");

		resultSet = statement.executeQuery("select category_id from categories where category_name = '%s'".formatted(Category));
		int categoryId = 1;
		if(resultSet.next()){
			categoryId = resultSet.getInt(1);
		}
		// language=SQL
		sb.append("""
				insert into dishes (dish_id, dish_name, category_id)
				values (%d, '%s', %d);
				""".formatted(dishId, name, categoryId));

		for (IngredientsQuantity ingredient : ingredientsList) {
			//language=SQL
			resultSet = statement.executeQuery("select ingredient_id from ingredients where name = '%s'".formatted(ingredient.getIngredients()));
			int ingredientId = 1;
			if(resultSet.next()) {
				ingredientId = resultSet.getInt(1);
			}
			// language=SQL
			sb.append("""
					insert into dish_ingredients (dish_id, ingredient_id, quantity)
					values (%d, %d, %d);
					""".formatted(dishId, ingredientId, ingredient.getQuantity()));
		}
		//language=SQL
		sb.append("""
    			insert into price_history (dish_id, date, value)
    			values (%d, now(),""".formatted(dishId));
		sb.append("""
%f);
				""".formatted(price).replace(',', '.'));
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
	public static void addCategory(String category) throws SQLException {
		Statement statement = connection.createStatement();

		ResultSet resultSet = statement.executeQuery("select MAX(category_id) from categories");
		int categoryId = 1;
		if(resultSet.next()) categoryId = resultSet.getInt(1)+1;
		//language=SQL
		String query = """
    				insert into categories (category_id, category_name)
    				values (%d, '%s');
				""".formatted(categoryId, category);
		statement.execute(query);
		statement.close();
	}

	public static void addRestaurant(String address, String city, String postalCode, String phone) throws SQLException {
		Statement statement = connection.createStatement();

		ResultSet resultSet = statement.executeQuery("select MAX(restaurant_id) from restaurants");

		int RestaurantId = 1;
		if (resultSet.next()) {
			RestaurantId = 1 + (int) resultSet.getObject(1);
		}

		// language=SQL
		StringBuilder sb = new StringBuilder("start transaction;\n");

		// language=SQL
		sb.append("""
				insert into restaurants (restaurant_id, address, city, postal_code, phone)
				values (%d, '%s', '%s', '%s', '%s');
				""".formatted(RestaurantId, address, city, postalCode, phone));

		ObservableList <Integer> IngredientID= FXCollections.observableArrayList();
		resultSet = statement.executeQuery("select ingredient_id from ingredients");
		while(resultSet.next()){
			IngredientID.add(resultSet.getInt(1));
		}
		for (int id : IngredientID) {
			// language=SQL
			sb.append("""
					insert into stock(ingredient_id, restaurant_id, quantity)
					values (%d, %d, %d);
					""".formatted(id, RestaurantId, 0));
		}
		//language=SQL
		sb.append("""
    			insert into opening_hours (restaurant_id, day, opening_time, closing_time)
				values (%d, 1, '12:00', '21:00'),
					   (%d, 2, '11:00', '21:00'),
					   (%d, 3, '11:00', '21:00'),
					   (%d, 4, '11:00', '21:00'),
					   (%d, 5, '11:00', '21:00'),
					   (%d, 6, '11:00', '21:00'),
					   (%d, 7, '11:00', '21:00');
			""".formatted(RestaurantId,RestaurantId,RestaurantId,RestaurantId,RestaurantId,RestaurantId,RestaurantId));
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
}
