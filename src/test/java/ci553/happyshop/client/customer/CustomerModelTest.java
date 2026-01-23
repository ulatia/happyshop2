package ci553.happyshop.client.customer;

import ci553.happyshop.catalogue.Product;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class CustomerModelTest {

    @Test
    void groupProductsById_FIX() {
        CustomerModel model = new CustomerModel();

        //Product with stock 18, but customer requests 25
        Product p = new Product("0002", "DAB Radio", "0002.jpg", 29.99, 18);
        p.setOrderedQuantity(25);

        ArrayList<Product> trolley = new ArrayList<>();
        trolley.add(p);

        //Group products by productID
        ArrayList<Product> grouped = model.groupProductsById(trolley);

        //After the fix, orderedQuantity should be preserved correctly
        assertEquals(25, grouped.get(0).getOrderedQuantity(),
                "Grouped product should preserve orderedQuantity for correct stock validation");
    }
}