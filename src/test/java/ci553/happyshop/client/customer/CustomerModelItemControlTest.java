package ci553.happyshop.client.customer;

import ci553.happyshop.catalogue.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CustomerModelItemControlTest {
    private CustomerModel model;

    @BeforeEach
    void setUp(){
        model = new CustomerModel();

        //Attach a dummy view so updateView() doesn't crash
        model.setViewForTest(new DummyCustomerView());
    }

    @Test
    void increase_addsNewItemToTrolley_ifNotAlreadyPresent()
    {
        Product p = new Product("0001", "TV", "0001.jpg", 269.00, 50);
        p.setOrderedQuantity(1);

        model.setTheProductForTest(p);

        model.increase();

        assertEquals(1, model.getTrolley().size());
        assertEquals("0001", model.getTrolley().get(0).getProductId());
        assertEquals(1, model.getTrolley().get(0).getOrderedQuantity());
    }

    @Test
    void increase_incrementsQuantity_ifItemAlreadyInTrolley()
    {
        Product p = new Product("0001", "TV", "0001.jpg", 269.00, 50);
        p.setOrderedQuantity(1);
        model.setTheProductForTest(p);

        model.increase(); // qty 1
        model.increase(); //qty 2

        assertEquals(1, model.getTrolley().size());
        assertEquals(2, model.getTrolley().get(0).getOrderedQuantity());
    }

    @Test
    void decrease_decrementsQuantity_whenMoreThanOne()
    {
        Product p = new Product("0001", "TV", "0001.jpg", 269.00, 50);
        p.setOrderedQuantity(1);
        model.setTheProductForTest(p);

        model.increase(); // qty 1
        model.increase(); //qty 2
        model.decrease(); // back to qty 1

        assertEquals(1, model.getTrolley().size());
        assertEquals(1, model.getTrolley().get(0).getOrderedQuantity());
    }

    @Test
    void decrease_removesItem_whenQuantityHitsZero()
    {
        Product p = new Product("0001", "TV", "0001.jpg", 269.00, 50);
        p.setOrderedQuantity(1);
        model.setTheProductForTest(p);

        model.increase(); // qty 1
        model.decrease(); // should remove item

        assertTrue(model.getTrolley().isEmpty(), "Item should be removed when quantity becomes 0");
    }

    @Test
    void remove_deletesItemRegardlessOfQuantity()
    {
        Product p = new Product("0001", "TV", "0001.jpg", 269.00, 50);
        p.setOrderedQuantity(1);
        model.setTheProductForTest(p);

        model.increase(); // qty 1
        model.increase(); //qty 2
        model.remove(); // remove item completely

        assertTrue(model.getTrolley().isEmpty(), "Remove should delete the item when completely");
    }
}