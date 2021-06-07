package com.id.gastromanager.controller;

import com.id.gastromanager.AlertFactory;
import com.id.gastromanager.Database;
import com.id.gastromanager.model.IngredientsQuantity;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import javax.xml.crypto.Data;
import java.net.URL;
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
    private ComboBox<String> deleteDishIngredientBox;
    @FXML
    private ComboBox<String> deleteDishIngredientNameBox;
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
    private ChoiceBox<String> addIngredientDietBox;
    @FXML
    private TextField addIngredientUnitsField;
    @FXML
    private Button addIngredientButton;

    public void initialize(URL url, ResourceBundle resourceBundle){
        deleteCustomerButton.setOnMouseClicked(event ->{
            String CustomerId = deleteCustomerBox.getValue();
            if(CustomerId == null){
                AlertFactory.showErrorAlert("Wpisane id ma niepoprawny format.");
            }
            if(CustomerId.matches("^\\d+$")) {
                Database.deleteCustomer(Integer.parseInt(CustomerId));
            }
            else{
                AlertFactory.showErrorAlert("Wpisane id ma niepoprawny format.");
            }
        });
        deleteRestaurantButton.setOnMouseClicked(event ->{
            String RestaurantId = deleteRestaurantBox.getValue();
            if(RestaurantId == null){
                AlertFactory.showErrorAlert("Wpisane id ma niepoprawny format.");
            }
            else if(RestaurantId.matches("^\\d+$")){
                Database.deleteRestaurant(Integer.parseInt(RestaurantId));
            }
            else{
                AlertFactory.showErrorAlert("Wpisane id ma niepoprawny format.");
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
            Database.deleteIngredient(deleteProductBox.getValue());
        });
        deleteDishButton.setOnMouseClicked(event ->{
            Database.deleteDish(deleteDishBox.getValue());
        });
        deleteDishIngredientButton.setOnMouseClicked(event ->{
            Database.deleteDishIngredient(deleteDishIngredientBox.getValue());
        });
        deleteDiscountButton.setOnMouseClicked(event ->{
            Database.deleteDiscount(Integer.parseInt(deleteDiscountBox.getValue()));
        });
        deleteSpecialDateButton.setOnMouseClicked(event ->{
            Database.deleteSpecialDate(Integer.parseInt(deleteSpecialDateBox.getValue()));
        });
        deleteCategoryButton.setOnMouseClicked(event ->{
            Database.deleteCategory(deleteCategoryBox.getValue());
        });

        addRestButton.setOnMouseClicked(event ->{
            Database.addRestaurant(addRestaurantAddressField.getText(), addRestaurantCityField.getText(), addPostalCodeField.getText(), addRestaurantPhoneField.getText());
        });
        addCategoryButton.setOnMouseClicked(event->{
            Database.addCategory(addCategoryField.getText());
        });

        addIngredientAllergensAddButton.setOnMouseClicked(event->{
            addIngredientAllergensList.getItems().addAll("addIngredientAllergenBox.getValue()", "sads");
        });
        addIngredientsAllergensDeleteButton.setOnMouseClicked(event ->{
            String selectedItem = addIngredientAllergensList.getSelectionModel().getSelectedItem();
            addIngredientAllergensList.getItems().remove(selectedItem);
        });
        addIngredientButton.setOnMouseClicked(event->{
            Database.addIngredient(addIngredientNameField.getText(), addIngredientAllergensList.getItems(), addIngredientDietBox.getValue(), addIngredientUnitsField.getText());
        });

    }
}
