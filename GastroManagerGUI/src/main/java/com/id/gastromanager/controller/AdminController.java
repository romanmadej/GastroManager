package com.id.gastromanager.controller;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

import com.id.gastromanager.AlertFactory;
import com.id.gastromanager.Database;
import com.id.gastromanager.model.Diet;
import com.id.gastromanager.model.IngredientsQuantity;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

public class AdminController extends Controller implements Initializable{

    @FXML
    private Button deleteCustomerButton;
    @FXML
    private ComboBox<String> deleteCustomerBox;
    @FXML
    private Button deleteRestaurantButton;
    @FXML
    private ComboBox<String> deleteRestaurantBox;
    @FXML
	private TextField dishNameBox;
    @FXML
    private TextField dishPriceField;
    @FXML
    private ChoiceBox<String> CategoryName;
    @FXML
    private TableView<IngredientsQuantity> IngredientsTable;
    @FXML
    private TableColumn<IngredientsQuantity, String> IngredientsCol1;
    @FXML
    private TableColumn<IngredientsQuantity, Integer> IngredientsCol2;
    @FXML
    private ComboBox<String> AddIngredientBox;
    @FXML
    private TextField numberOfIngredients;
    @FXML
    private Button AddIngredientButton;
    @FXML
    private Button DeleteIngredientButton;
    @FXML
    private Button AddDishButton;
    @FXML
    private ComboBox<String> deleteProductBox;
    @FXML
    private Button deleteProductButton;
    @FXML
    private ComboBox<String> deleteDishBox;
    @FXML
    private Button deleteDishButton;
    @FXML
    private ComboBox<String> deleteDishIngredient_ingredientName;
    @FXML
    private ComboBox<String> deleteDishIngredient_dishName;
    @FXML
    private Button deleteDishIngredientButton;
    @FXML
    private ComboBox<String> deleteDiscountBox;
    @FXML
    private Button deleteDiscountButton;
    @FXML
    private ComboBox<String> deleteSpecialDateBox;
    @FXML
    private Button deleteSpecialDateButton;
    @FXML
    private ComboBox<String> deleteCategoryBox;
    @FXML
    private Button deleteCategoryButton;

    @FXML
    private TextField addRestaurantAddressField;
    @FXML
    private TextField addRestaurantCityField;
    @FXML
    private TextField addPostalCodeField;
    @FXML
    private TextField addRestaurantPhoneField;
    @FXML
    private Button addRestButton;
    @FXML
    private TextField addCategoryField;
    @FXML
    private Button addCategoryButton;
    @FXML
    private TextField addIngredientNameField;
    @FXML
    private ListView<String> addIngredientAllergensList;
    @FXML
    private ChoiceBox<String> addIngredientAllergenBox;
    @FXML
    private Button addIngredientAllergensAddButton;
    @FXML
    private Button addIngredientsAllergensDeleteButton;
    @FXML
    private ChoiceBox<Diet> addIngredientDietBox;
    @FXML
    private TextField addIngredientUnitsField;
    @FXML
    private Button addIngredientButton;

    boolean isId(String val){
        if(val == null || !val.matches("^\\d+$")){
            AlertFactory.showErrorAlert("Wpisane id ma niepoprawny format.");
            return false;
        }
        return true;
    }
    void processResult(int result){
        if (result == 0)
            AlertFactory.showErrorAlert("Operacja nieudana. najpierw usuń obiekty zależne");
        else if (result == 1)
            AlertFactory.showInformationAlert("Operacja zakończona sukcesem!");
        else if(result==2)
            AlertFactory.showErrorAlert("obiekt o podanym id nie istnieje");
    }

    boolean isIdentifier(String s){
        if(s==null || !s.matches("[A-Za-z0-9ĄĆĘŁŃÓŚŹŻąćęłńóśźż,\\- ]+")){
            AlertFactory.showErrorAlert("Niepoprawny identyfikator!");
            return false;
        }
        return true;
    }

    public void initialize(URL url, ResourceBundle resourceBundle){
        try {
            CategoryName.setItems(Database.getCategoriesName());
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        try {
            deleteCustomerBox.setItems(Database.getCustomers());
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        try{
            deleteRestaurantBox.setItems(Database.getRestaurantsId());
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        try {
            CategoryName.setItems(Database.getCategoriesName());
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        try {
            AddIngredientBox.setItems(Database.getIngredientNames());
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        try {
            deleteProductBox.setItems(Database.getIngredientNames());
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        try {
            deleteDishBox.setItems(Database.getDishNames());
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        try {
            deleteDiscountBox.setItems(Database.getDiscountId());
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        try {
            deleteSpecialDateBox.setItems(Database.getSpecialDateId());
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        try {
            deleteCategoryBox.setItems(Database.getCategoriesName());
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        try {
            deleteDishIngredient_dishName.setItems(Database.getDishNames());
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        deleteDishIngredient_ingredientName.setOnMouseClicked(event-> {
            try {
                deleteDishIngredient_ingredientName.getItems().clear();
                deleteDishIngredient_ingredientName.setItems(Database.getIngredientNames(deleteDishIngredient_dishName.getValue()));
            } catch (SQLException throwables) {
                AlertFactory.showErrorAlert("Wpisane dane są niepoprawne");
            }
        });
        try {
            addIngredientAllergenBox.setItems(Database.getAlergens());
        } catch (SQLException throwables) {
            AlertFactory.showErrorAlert("Wpisane dane są niepoprawne");
        }
        ObservableList<Diet> dietList= FXCollections.observableArrayList();
        dietList.addAll(Diet.vegetarian, Diet.vegan, Diet.standard);
        addIngredientDietBox.setItems(dietList);
        deleteCustomerButton.setOnMouseClicked(event ->{
            String customerId = deleteCustomerBox.getValue();
            if(isId(customerId)) {
                try {
                    processResult(Database.deleteCustomer(Integer.parseInt(customerId)));
                } catch (SQLException throwables) {
                    AlertFactory.showErrorAlert("Wpisane dane są niepoprawne");
                }
            }
        });
        deleteRestaurantButton.setOnMouseClicked(event ->{
            String restaurantId = deleteRestaurantBox.getValue();
            if(isId(restaurantId)){
                try{
                    int result = Database.deleteRestaurant(Integer.parseInt(restaurantId));
                    if(result==0)
                        AlertFactory.showErrorAlert("Usuwanie restauracji z zamówieniami zabronione!");
                    else if(result==1)
                        AlertFactory.showInformationAlert("Operacja zakończona sukcesem!");
                    else
                        AlertFactory.showErrorAlert("Restauracja o podanym id nie istnieje!");
                }
                catch(Exception e){
                    e.printStackTrace();
                    AlertFactory.showErrorAlert("Wystąpił nieoczekiwany błąd.");
                }
            }
        });
        IngredientsCol1.setCellValueFactory(new PropertyValueFactory<>("ingredients"));
        IngredientsCol2.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        AddIngredientButton.setOnMouseClicked(event ->{
            if(numberOfIngredients.getText() == null || !numberOfIngredients.getText().matches("^\\d+$")){
                AlertFactory.showErrorAlert("Wpisana liczba składników ma niepoprawny format.");
            }
            else if(AddIngredientBox.getValue() == null || AddIngredientBox.getValue().equals("")){
                AlertFactory.showErrorAlert("Nazwa składnika nie może być pusta.");
            }
            else {
                IngredientsTable.getItems()
                        .add(new IngredientsQuantity(AddIngredientBox.getValue(), Integer.parseInt(numberOfIngredients.getText())));
            }
        });
        DeleteIngredientButton.setOnMouseClicked(event -> {
        });

        AddDishButton.setOnMouseClicked(event -> {
			if (dishPriceField == null || !dishPriceField.getText().matches("(\\d+\\.\\d+)")) {
                AlertFactory.showErrorAlert("Wpisana cena ma niepoprawny format.");
			} else if (dishNameBox.getText() == null || dishNameBox.getText().equals("")) {
                AlertFactory.showErrorAlert("Nazwa dania nie może być pusta");
			} else if (IngredientsTable.getItems().isEmpty()) {
                AlertFactory.showErrorAlert("List składników nie może być pusta");
			} else {
				List<IngredientsQuantity> IngredientsList = IngredientsTable.getItems();
				String name = dishNameBox.getText();
                try {
					Database.addDish(name, IngredientsList, Double.parseDouble(dishPriceField.getText()), " ");
                } catch (SQLException throwables) {
                    AlertFactory.showErrorAlert("Wpisane dane są niepoprawne");
                }
                IngredientsTable.getItems().clear();
            }
        });

        deleteProductButton.setOnMouseClicked(event ->{
            String ingredientName = deleteProductBox.getValue();
            if(isIdentifier(ingredientName)) {
                try {
                    processResult(Database.deleteIngredient(ingredientName));
                } catch (SQLException throwables) {
                    AlertFactory.showErrorAlert("Wpisane dane są niepoprawne");
                }
            }
        });
        deleteDishButton.setOnMouseClicked(event ->{
            String dishName = deleteDishBox.getValue();
            if(isIdentifier(dishName)) {
                try {
                    processResult(Database.deleteDish(dishName));
                } catch (SQLException throwables) {
                    AlertFactory.showErrorAlert("Wpisane dane są niepoprawne");
                }
            }
        });
        deleteDishIngredientButton.setOnMouseClicked(event ->{
//            Database.deleteDishIngredient(deleteDishIngredientBox.getValue());
            String dishName = deleteDishIngredient_dishName.getValue();
            String ingredientName = deleteDishIngredient_ingredientName.getValue();

            //if both aren't correct double popup appears
            if(isIdentifier(dishName) && isIdentifier(ingredientName)) {
                try {
                    processResult(Database.deleteDishIngredient(dishName,ingredientName));
                } catch (SQLException throwables) {
                    AlertFactory.showErrorAlert("Wpisane dane są niepoprawne");
                }
            }
        });
        deleteDiscountButton.setOnMouseClicked(event ->{
            String discountId = deleteDiscountBox.getValue();
            if(isId(discountId)) {
                try {
                    processResult(Database.deleteDiscount(Integer.parseInt(discountId)));
                } catch (SQLException throwables) {
                    AlertFactory.showErrorAlert("Wpisane dane są niepoprawne");
                }
            }
        });
        deleteSpecialDateButton.setOnMouseClicked(event ->{
            String specialDateId = deleteSpecialDateBox.getValue();
            if(isId(specialDateId)) {
                try {
                    processResult(Database.deleteSpecialDate(Integer.parseInt(specialDateId)));
                } catch (SQLException throwables) {
                    AlertFactory.showErrorAlert("Wpisane dane są niepoprawne");
                }
            }
        });
        deleteCategoryButton.setOnMouseClicked(event ->{
            String categoryName = deleteCategoryBox.getValue();
            if(isIdentifier(categoryName)) {
                try {
                    processResult(Database.deleteCategory(categoryName));
                } catch (SQLException throwables) {
                    AlertFactory.showErrorAlert("Wpisane dane są niepoprawne");
                }
            }
        });

        addRestButton.setOnMouseClicked(event -> {
            try {
                Database.addRestaurant(addRestaurantAddressField.getText(), addRestaurantCityField.getText(), addPostalCodeField.getText(), addRestaurantPhoneField.getText());
            } catch (SQLException throwables) {
                AlertFactory.showErrorAlert("Wpisane dane są niepoprawne");
            }
        });addCategoryButton.setOnMouseClicked(event-> {
            try {
                Database.addCategory(addCategoryField.getText());
            } catch (SQLException throwables) {
                AlertFactory.showErrorAlert("Wpisane dane są niepoprawne");
            }
        });

        addIngredientAllergensAddButton.setOnMouseClicked(event-> addIngredientAllergensList.getItems().addAll(addIngredientAllergenBox.getValue()));
        addIngredientsAllergensDeleteButton.setOnMouseClicked(event ->{
            String selectedItem = addIngredientAllergensList.getSelectionModel().getSelectedItem();
            addIngredientAllergensList.getItems().remove(selectedItem);
        });
        addIngredientButton.setOnMouseClicked(event-> {
            try {
                Database.addIngredient(addIngredientNameField.getText(), addIngredientAllergensList.getItems(), addIngredientDietBox.getValue(), addIngredientUnitsField.getText());
            } catch (SQLException throwables) {
                AlertFactory.showErrorAlert("Wpisane dane są niepoprawne");
            }
        });

    }
}
