package ci553.happyshop.client.customer;

import ci553.happyshop.catalogue.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class CustomerModelOrganisedTrolley {
    private CustomerModel model;

    @BeforeEach
    void setUp()
    {
        model = new CustomerModel();

        //Attach a dummy view so updateView() doesn't crash
        model.setViewForTest(new DummyCustomerView());
    }

    @Test
    void addingSameProductTwice_mergesIntoOneLineWithQuantity()
    {
        Product p = new Product("0002", "DAB Radio", "0002.jpg", 29.99, 18);
        p.setOrderedQuantity(1);

        model.setTheProductForTest(p);
        model.increase(); // add once
        model.increase(); //add again (should merge)

        ArrayList<Product> trolley = model.getTrolley();
        assertEquals(1, trolley.size(), "Expected duplicates to merge into one trolley line");
        assertEquals("0002", trolley.get(0).getProductId());
        assertEquals(2, trolley.get(0).getOrderedQuantity(), "Expected merged quantity to be 2");
    }

    @Test
    void addingA_thenB_thenA_stillMergesCorrectly() {
        Product a = new Product("0001", "TV", "0001.jpg", 269.00, 50);
        a.setOrderedQuantity(1);

        Product b = new Product("0003", "Toaster", "0003.jpg", 19.99, 10);
        b.setOrderedQuantity(1);

        model.setTheProductForTest(a);
        model.increase(); // A

        model.setTheProductForTest(b);
        model.increase(); // B

        model.setTheProductForTest(a);
        model.increase(); // A again -> should merge with first A

        ArrayList<Product> trolley = model.getTrolley();
        assertEquals(2, trolley.size(), "Expected only 2 lines (A merged + B)");
        assertEquals("0001", trolley.get(0).getProductId());
        assertEquals(2, trolley.get(0).getOrderedQuantity(), "Expected A quantity to be 2 after merging");
        assertEquals("0003", trolley.get(1).getProductId());
        assertEquals(1, trolley.get(1).getOrderedQuantity());
    }

    @Test
    void trolleyDisplay_isSortedByProductIdAscending() {
        //add product 0002 first, then 0001
        Product p2 = new Product("0002", "DAB Radio", "0002.jpg", 29.99, 50);
        p2.setOrderedQuantity(1);

        Product p1 = new Product("0001", "TV", "0001.jpg", 269.00, 50);
        p1.setOrderedQuantity(1);


        //add in sorted order: 0001, 0002, 0003
        model.setTheProductForTest(p2);
        model.increase(); //add 0002

        model.setTheProductForTest(p1);
        model.increase(); //add 0001

        ArrayList<Product> trolley = model.getTrolley();

        assertEquals("0001", trolley.get(0).getProductId(), "First item should be the smallest product ID");
        assertEquals("0002", trolley.get(1).getProductId(), "Second item should be the larger product ID");

        //order is not based on quantity
        assertEquals(1, trolley.get(0).getOrderedQuantity());
        assertEquals(1, trolley.get(1).getOrderedQuantity());

    }


}