package com.id.gastromanager.controller;

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

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

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
    private ComboBox<String> dishNameBox;
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
    private ComboBox<String> deleteDishIngredient_ingredientIdBox;
    @FXML
    private ComboBox<String> deleteDishIngredient_dishIdBox;
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
        else
            AlertFactory.showErrorAlert("obiekt o podanym id nie istnieje");
    }

    public void initialize(URL url, ResourceBundle resourceBundle){
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
            dishNameBox.setItems(Database.getDishNames());
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
            deleteDishIngredient_dishIdBox.setItems(Database.getDishNames());
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        deleteDishIngredient_ingredientIdBox.setOnMouseClicked(event-> {
            try {
                deleteDishIngredient_ingredientIdBox.getItems().clear();
                deleteDishIngredient_ingredientIdBox.setItems(Database.getIngredientNames(deleteDishIngredient_dishIdBox.getValue()));
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        });
        try {
            addIngredientAllergenBox.setItems(Database.getAlergens());
        } catch (SQLException throwables) {
            throwables.printStackTrace();
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
                    throwables.printStackTrace();
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
                IngredientsQuantity selectedItem = IngredientsTable.getSelectionModel().getSelectedItem();
                IngredientsTable.getItems().remove(selectedItem);
        });

        AddDishButton.setOnMouseClicked(event -> {
            if(dishPriceField == null || !dishPriceField.getText().matches("(\\d+\\.\\d+)")){
                AlertFactory.showErrorAlert("Wpisana cena ma niepoprawny format.");
            }
            else if(dishNameBox.getValue() == null || dishNameBox.getValue().equals("")){
                AlertFactory.showErrorAlert("Nazwa dania nie może być pusta");
            }
            else if(IngredientsTable.getItems().isEmpty()){
                AlertFactory.showErrorAlert("List składników nie może być pusta");
            }
            else {
                List <IngredientsQuantity> IngredientsList = IngredientsTable.getItems();
                String name = dishNameBox.getValue();
                System.out.println( Double.parseDouble(dishPriceField.getText()));
                Database.addDish(name,IngredientsList, Double.parseDouble(dishPriceField.getText()), " ");
                IngredientsTable.getItems().clear();
            }
        });

        deleteProductButton.setOnMouseClicked(event ->{
            String ingredientId = deleteProductBox.getValue();
            if(isId(ingredientId)) {
                try {
                    System.out.println("usun skladnik");
                    processResult(Database.deleteIngredient(Integer.parseInt(ingredientId)));
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
        });
        deleteDishButton.setOnMouseClicked(event ->{
            String dishId = deleteDishBox.getValue();
            if(isId(dishId)) {
                try {
                    processResult(Database.deleteDish(Integer.parseInt(dishId)));
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
        });
        deleteDishIngredientButton.setOnMouseClicked(event ->{
//            Database.deleteDishIngredient(deleteDishIngredientBox.getValue());
            String dishId = deleteDishIngredient_dishIdBox.getValue();
            String ingredientId = deleteDishIngredient_ingredientIdBox.getValue();
            //if both aren't correct double popup appears
            if(isId(dishId) && isId(ingredientId)) {
                try {
                    processResult(Database.deleteDishIngredient(Integer.parseInt(dishId),Integer.parseInt(ingredientId)));
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
        });
        deleteDiscountButton.setOnMouseClicked(event ->{
            String discountId = deleteDiscountBox.getValue();
            if(isId(discountId)) {
                try {
                    processResult(Database.deleteDiscount(Integer.parseInt(discountId)));
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
        });
        deleteSpecialDateButton.setOnMouseClicked(event ->{
            String specialDateId = deleteSpecialDateBox.getValue();
            if(isId(specialDateId)) {
                try {
                    processResult(Database.deleteSpecialDate(Integer.parseInt(specialDateId)));
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
        });
        deleteCategoryButton.setOnMouseClicked(event ->{
            String categoryId = deleteCategoryBox.getValue();
            if(isId(categoryId)) {
                try {
                    processResult(Database.deleteCategory(Integer.parseInt(categoryId)));
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
        });

        addRestButton.setOnMouseClicked(event -> Database.addRestaurant(addRestaurantAddressField.getText(), addRestaurantCityField.getText(), addPostalCodeField.getText(), addRestaurantPhoneField.getText()));
        addCategoryButton.setOnMouseClicked(event-> Database.addCategory(addCategoryField.getText()));

        addIngredientAllergensAddButton.setOnMouseClicked(event->{
            addIngredientAllergensList.getItems().addAll(addIngredientAllergenBox.getValue());
        });
        addIngredientsAllergensDeleteButton.setOnMouseClicked(event ->{
            String selectedItem = addIngredientAllergensList.getSelectionModel().getSelectedItem();
            addIngredientAllergensList.getItems().remove(selectedItem);
        });
        addIngredientButton.setOnMouseClicked(event-> Database.addIngredient(addIngredientNameField.getText(), addIngredientAllergensList.getItems(), addIngredientDietBox.getValue(), addIngredientUnitsField.getText()));

    }
}
