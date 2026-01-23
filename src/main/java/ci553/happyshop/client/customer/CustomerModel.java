package ci553.happyshop.client.customer;

import ci553.happyshop.catalogue.Order;
import ci553.happyshop.catalogue.Product;
import ci553.happyshop.storageAccess.DatabaseRW;
import ci553.happyshop.orderManagement.OrderHub;
import ci553.happyshop.utility.StorageLocation;
import ci553.happyshop.utility.ProductListFormatter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.*;

/**
 * TODO
 * You can either directly modify the CustomerModel class to implement the required tasks,
 * or create a subclass of CustomerModel and override specific methods where appropriate.
 */
public class CustomerModel {
    public CustomerView cusView;
    public DatabaseRW databaseRW; //Interface type, not specific implementation
                                  //Benefits: Flexibility: Easily change the database implementation.

    private Product theProduct =null; // product found from search
    private ArrayList<Product> trolley =  new ArrayList<>(); // a list of products in trolley

    // Four UI elements to be passed to CustomerView for display updates.
    private String imageName = "imageHolder.jpg";                // Image to show in product preview (Search Page)
    private String displayLaSearchResult = "No Product was searched yet"; // Label showing search result message (Search Page)
    private String displayTaTrolley = "";                                // Text area content showing current trolley items (Trolley Page)
    private String displayTaReceipt = "";                                // Text area content showing receipt after checkout (Receipt Page)

    //SELECT productID, description, image, unitPrice,inStock quantity
    void search() throws SQLException {
        String productId = cusView.tfId.getText().trim();
        if(!productId.isEmpty()){
            theProduct = databaseRW.searchByProductId(productId); //search database
            if(theProduct != null && theProduct.getStockQuantity()>0){
                double unitPrice = theProduct.getUnitPrice();
                String description = theProduct.getProductDescription();
                int stock = theProduct.getStockQuantity();

                String baseInfo = String.format("Product_Id: %s\n%s,\nPrice: £%.2f", productId, description, unitPrice);
                String quantityInfo = stock < 100 ? String.format("\n%d units left.", stock) : "";
                displayLaSearchResult = baseInfo + quantityInfo;
                System.out.println(displayLaSearchResult);
            }
            else{
                theProduct=null;
                displayLaSearchResult = "No Product was found with ID " + productId;
                System.out.println("No Product was found with ID " + productId);
            }
        }else{
            theProduct=null;
            displayLaSearchResult = "Please type ProductID";
            System.out.println("Please type ProductID.");
        }
        updateView();
    }

    void addToTrolley(){
        if(theProduct!= null){

            // trolley.add(theProduct) — Product is appended to the end of the trolley.
            // To keep the trolley organized, add code here or call a method that:
            //TODO
            // 1. Merges items with the same product ID (combining their quantities).
            // 2. Sorts the products in the trolley by product ID.
            //trolley.add(theProduct);
            organisedTrolley();
            displayTaTrolley = ProductListFormatter.buildString(trolley); //build a String for trolley so that we can show it
        }
        else{
            displayLaSearchResult = "Please search for an available product before adding it to the trolley";
            System.out.println("must search and get an available product before add to trolley");
        }
        displayTaReceipt=""; // Clear receipt to switch back to trolleyPage (receipt shows only when not empty)
        updateView();
    }

    void organisedTrolley()
    {
        for (Product p : trolley)
        {
            //Check if current product has the same ID as the product being added
            if (p.getProductId().equals(theProduct.getProductId()))
            {
                //If product already exists in trolley, increase product quantity instead of adding duplicates
                p.setOrderedQuantity(p.getOrderedQuantity() + theProduct.getOrderedQuantity());
                return;
            }
        }
        //If the product was not found in the trolley, create new product object using the details of theProduct
        Product pNew = new Product(theProduct.getProductId(), theProduct.getProductDescription(),
                theProduct.getProductImageName(), theProduct.getUnitPrice(), theProduct.getStockQuantity());
        trolley.add(pNew);
        //Sort trolley by productID in ascending order
        //Comparator.comparing used to keep sorting logic clear
        trolley.sort(Comparator.comparing(Product::getProductId));
    }

    //------Checkout helpers methods
    //Checkout only valid if trolley contains at least one item
    public boolean isCheckoutValid() {
        return !trolley.isEmpty();
    }

    //Calculates total number of items in the trolley
    public int getTotalItemCount() {
        int total = 0;
        for (Product p : trolley) {
            total += p.getOrderedQuantity();
        }
        return total;
    }

    //Calculates total cost of all items in the trolley
    public double getTotalCost() {
        double total = 0.0;
        for (Product p : trolley) {
            total += p.getOrderedQuantity() * p.getUnitPrice();
        }
        return total;
    }

    void checkOut() throws IOException, SQLException {

        // 1 validation - prevent checkout if trolley is empty
        if (!isCheckoutValid()) {
            displayTaTrolley = "Your trolley is empty";
            cusView.showInfoMessage("Checkout blocked",
                    "Your trolley is empty. Add items before checking out.");
            updateView();
            return;
        }

        // 2 confirm dialog - shows total/item count
        int totalItems = getTotalItemCount();
        double totalCost = getTotalCost();

        //Ask user to confirm checkout
        boolean confirmed = cusView.showConfirmCheckoutDialog(totalItems, totalCost);
        if (!confirmed) {
            //User cancelled checkout
            return;
        }
            // Group the products in the trolley by productId to optimize stock checking
            // Check the database for sufficient stock for all products in the trolley.
            // If any products are insufficient, the update will be rolled back.
            // If all products are sufficient, the database will be updated, and insufficientProducts will be empty.
            // Note: If the trolley is already organized (merged and sorted), grouping is unnecessary.
            ArrayList<Product> groupedTrolley= groupProductsById(trolley);
            ArrayList<Product> insufficientProducts= databaseRW.purchaseStocks(groupedTrolley);

            if(insufficientProducts.isEmpty()){ // If stock is sufficient for all products
                //get OrderHub and tell it to make a new Order
                OrderHub orderHub =OrderHub.getOrderHub();
                Order theOrder = orderHub.newOrder(trolley);
               // trolley.clear();
                //displayTaTrolley ="";

                //A clear receipt for the customer
                int itemCount = 0;
                double totalCostReceipt = 0.0;
                for (Product p : theOrder.getProductList()) {
                    itemCount += p.getOrderedQuantity();
                    totalCostReceipt += p.getOrderedQuantity() * p.getUnitPrice();
                }
                displayTaReceipt = String.format(
                        "Checkout Successful!\n\n" + "Order_ID: %s\n" + "Ordered_Date_Time: %s\n\n" +
                                "Items: %d\n" + "Total: £%.2f\n" + "-----------------------------------\n" + "%s",
                        theOrder.getOrderId(), theOrder.getOrderedDateTime(), itemCount, totalCostReceipt,
                        ProductListFormatter.buildString(theOrder.getProductList())
                );
                //Clear trolley after successful checkout
                trolley.clear();
                displayTaTrolley ="";

                //Tell view to swap to receipt page immediately
                cusView.showReceiptPage(displayTaReceipt);
                updateView();
                return;

            }
            else{ // Some products have insufficient stock — build an error message to inform the customer
                StringBuilder errorMsg = new StringBuilder();
                for(Product p : insufficientProducts){
                    errorMsg.append("\u2022 "+ p.getProductId()).append(", ")
                            .append(p.getProductDescription()).append(" (Only ")
                            .append(p.getStockQuantity()).append(" available, ")
                            .append(p.getOrderedQuantity()).append(" requested)\n");
                }
                theProduct=null;

                //TODO
                // Add the following logic here:
                // 1. Remove products with insufficient stock from the trolley.
                // 2. Trigger a message window to notify the customer about the insufficient stock, rather than directly changing displayLaSearchResult.
                //You can use the provided RemoveProductNotifier class and its showRemovalMsg method for this purpose.
                //remember close the message window where appropriate (using method closeNotifierWindow() of RemoveProductNotifier class)

                // 3 remove products with insufficient stock from the trolley
                Set<String> insufficientIDs = new HashSet<>();
                for (Product p : insufficientProducts) {
                    insufficientIDs.add(p.getProductId());
                }

                //Remove any trolley items whose productID is in the insufficient list
                trolley.removeIf(p -> insufficientIDs.contains(p.getProductId()));

                //Rebuild trolley display after removing items
                displayTaTrolley = ProductListFormatter.buildString(trolley);

                // 4 notify customer using a message window
                RemoveProductNotifier notifier = new RemoveProductNotifier();
                notifier.cusView = cusView;
                notifier.showRemovalMsg("Checkout failed. These items were removed due to insufficient stock:\n\n" + errorMsg);

                displayLaSearchResult = "Checkout failed due to insufficient stock for the following products:\n" + errorMsg.toString();
                System.out.println("stock is not enough");
            }

            //Trolley already cleared earlier
            trolley.clear();
            displayTaTrolley = "";

        updateView();
    }

    /**
     * Groups products by their productId to optimize database queries and updates.
     * By grouping products, we can check the stock for a given `productId` once, rather than repeatedly
     */
     ArrayList<Product> groupProductsById(ArrayList<Product> proList) {
        Map<String, Product> grouped = new HashMap<>();
        for (Product p : proList) {
            String id = p.getProductId();
            if (grouped.containsKey(id)) {
                Product existing = grouped.get(id);
                existing.setOrderedQuantity(existing.getOrderedQuantity() + p.getOrderedQuantity());
            } else {
                // Make a shallow copy to avoid modifying the original trolley item
                //orderedQuantity must be copied explicitly to preserve checkout quantities
                // (important for stock checking)
                Product copy = new Product(
                        p.getProductId(), p.getProductDescription(),
                        p.getProductImageName(), p.getUnitPrice(),
                        p.getStockQuantity()
                );
                //Copy orderedQuantity to ensure stock validation uses the correct requested amount
                copy.setOrderedQuantity(p.getOrderedQuantity());
                grouped.put(id, copy);
            }
        }
        return new ArrayList<>(grouped.values());
    }

    void cancel(){
        trolley.clear();
        displayTaTrolley="";
        updateView();
    }
    void closeReceipt(){
        displayTaReceipt="";
    }

    void updateView() {
        if(theProduct != null){
            imageName = theProduct.getProductImageName();
            String relativeImageUrl = StorageLocation.imageFolder +imageName; //relative file path, eg images/0001.jpg
            // Get the full absolute path to the image
            Path imageFullPath = Paths.get(relativeImageUrl).toAbsolutePath();
            imageName = imageFullPath.toUri().toString(); //get the image full Uri then convert to String
            System.out.println("Image absolute path: " + imageFullPath); // Debugging to ensure path is correct
        }
        else{
            imageName = "imageHolder.jpg";
        }
        cusView.update(imageName, displayLaSearchResult, displayTaTrolley,displayTaReceipt);
    }
     // extra notes:
     //Path.toUri(): Converts a Path object (a file or a directory path) to a URI object.
     //File.toURI(): Converts a File object (a file on the filesystem) to a URI object

    //for test only
    public ArrayList<Product> getTrolley() {
        return trolley;
    }

    //For Item-level control, button behaviour
    //Increase quantity of an item in the trolley by productId
    void increase() {
         if (theProduct == null) {
             displayLaSearchResult = "Please search for a product first, then press + to add it to the trolley.";
             updateView();
             return;
         }
         //Reuse organisedTrolley logic (merge + sort)
        organisedTrolley();

         //Refresh trolley text area and clear any previous receipt text
         displayTaTrolley = ProductListFormatter.buildString(trolley);
         displayTaReceipt = "";
         updateView();

    }

    //Decrease quantity of an item in the trolley by productId
    void decrease() {
        if (theProduct == null) {
            displayLaSearchResult = "Please search for a product first, then press - to reduce it from the trolley.";
            updateView();
            return;
        }

        String targetId = theProduct.getProductId();

        for (int i = 0; i < trolley.size(); i++) {
            Product p = trolley.get(i);
            if (p.getProductId().equals(targetId)) {
                int newQty = p.getOrderedQuantity() - 1;

                //Consistent rule: if quantity hits 0, remove item
                if (newQty <= 0) {
                    trolley.remove(i); //remove if hits 0
                } else {
                    p.setOrderedQuantity(newQty);
                }
                displayTaTrolley = ProductListFormatter.buildString(trolley);
                updateView();
                return;
            }
        }
        displayLaSearchResult = "That product is not currently in your trolley.";
        updateView();
    }

    //remove quantity of an item in the trolley by productId
    //Removes the trolley line completely (regardless of quantity)
    void remove() {
        if (theProduct == null) {
            displayLaSearchResult = "Please search for a product first, then press Remove to delete it from the trolley.";
            updateView();
            return;
        }

        String targetId = theProduct.getProductId();

        boolean removed = trolley.removeIf(p-> p.getProductId().equals(targetId));
        if (!removed) {
            displayLaSearchResult = "That product is not currently in your trolley.";
        }

        displayTaTrolley = ProductListFormatter.buildString(trolley);
        updateView();
    }

    //test helper
    void setTheProductForTest(Product p) {
         this.theProduct = p;
    }

    void setViewForTest(CustomerView view) {
        this.cusView = view;
    }


}
