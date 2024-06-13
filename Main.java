import java.sql.*;
import java.awt.Font;
import java.util.*;
import java.awt.Graphics;
import java.awt.print.Printable;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import java.awt.print.PageFormat;
import java.awt.Graphics2D;
import java.awt.print.PrinterException;
import java.awt.FontMetrics;


class FoodItem {
    private String name;
    private int price;

    public FoodItem(String name, int price) {
        this.name = name;
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }
}

class Menu {
    private Connection connection;
    private Scanner sc;

    public Menu(Connection connection) {
        this.connection = connection;
        sc = new Scanner(System.in);
        createMenuTable(); // Create menu table if not exists
        displayMenu(); // Display menu from the database
    }

    private void createMenuTable() {
        String sql = "CREATE TABLE IF NOT EXISTS menu (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "name VARCHAR(255) NOT NULL," +
                "price INT NOT NULL" +
                ")";
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void displayMenu() {
        System.out.println("              ** Welcome To our Cafe **");
        System.out.println("=====================================================");
        String sql = "SELECT * FROM menu";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            int index = 1;
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                int price = rs.getInt("price");
                System.out.printf("           %d. %-25s %d/-\n", index++, name, price);
            }
            System.out.println("           0. Exit                       ");
            System.out.println("======================================================");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addItem() {
        System.out.print("Enter Name of New Item: ");
        String name = sc.nextLine();
        System.out.print("Enter Price of New Item: ");
        int price = sc.nextInt();

        String sql = "INSERT INTO menu (name, price) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setInt(2, price);
            pstmt.executeUpdate();
            System.out.println("New Item Added Successfully!");
            displayMenu(); // Display updated menu
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeItem() {
        System.out.print("Enter Index of Item to Remove: ");
        int index = sc.nextInt();
        String sql = "DELETE FROM menu WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, index);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Item Removed Successfully!");
                displayMenu(); // Display updated menu
            } else {
                System.out.println("Invalid Index");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void modifyPrice() {
        System.out.print("Enter Index of Item to Modify: ");
        int index = sc.nextInt();
        System.out.print("Enter New Price: ");
        int newPrice = sc.nextInt();
        String sql = "UPDATE menu SET price = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, newPrice);
            pstmt.setInt(2, index);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Price Modified Successfully!");
                displayMenu(); // Display updated menu
            } else {
                System.out.println("Invalid Index");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getItemCount() {
        String sql = "SELECT COUNT(*) AS count FROM menu";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public FoodItem getItem(int index) {
        String sql = "SELECT * FROM menu LIMIT ?, 1";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, index - 1); // Because LIMIT is zero-indexed
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String name = rs.getString("name");
                int price = rs.getInt("price");
                return new FoodItem(name, price);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void adminPanel() {
        // Authentication check
        if (!authenticateAdmin()) {
            System.out.println("Invalid Username or Password. Access Denied.");
            return;
        }

        while (true) {
            System.out.println("Admin Panel");
            System.out.println("1. Add Item");
            System.out.println("2. Remove Item");
            System.out.println("3. Modify Price");
            System.out.println("4. Exit");
            System.out.print("Enter your choice: ");
            int choice = sc.nextInt();
            sc.nextLine(); // consume the newline
            switch (choice) {
                case 1:
                    addItem();
                    break;
                case 2:
                    removeItem();
                    break;
                case 3:
                    modifyPrice();
                    break;
                case 4:
                    return;
                default:
                    System.out.println("Invalid choice");
            }
        }
    }

    private boolean authenticateAdmin() {
        System.out.print("Enter Admin Username: ");
        String username = sc.nextLine();
        System.out.print("Enter Admin Password: ");
        String password = sc.nextLine();

        String sql = "SELECT * FROM admins WHERE username = ? AND password = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            return rs.next(); // If a record is found, authentication is successful
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}

class Order {
    private FoodItem item;
    private int quantity;

    public Order(FoodItem item, int quantity) {
        this.item = item;
        this.quantity = quantity;
    }

    public int calculateTotal() {
        return item.getPrice() * quantity;
    }

    public String getItemName() {
        return item.getName();
    }

    public int getQuantity() {
        return quantity;
    }

    public int getPrice() {
        return item.getPrice();
    }

    public int getTotalPrice() {
        return calculateTotal();
    }
}

class Bill {
    List<Order> orders;
    int total;
    private Connection connection;
    String customerName;
    String mobileNumber;

    public Bill(Connection connection) {
        orders = new ArrayList<>();
        total = 0;
        this.connection = connection;
    }

    public void setCustomerDetails(String customerName, String mobileNumber) {
        this.customerName = customerName;
        this.mobileNumber = mobileNumber;
    }

    public void addOrder(Order order) {
        orders.add(order);
        total += order.calculateTotal();
    }

    public void generateBill() {
        Scanner sc = new Scanner(System.in);
        System.out.println();
        System.out.println(" ----------- SRI SHAKTHI CAFE & BAKES   -----------");
        System.out.println("          ** Thank you for ordering ***            ");
        System.out.println("Customer Name: " + customerName);
        System.out.println("Mobile Number: " + mobileNumber);
        System.out.println("======================================================");
        System.out.printf("%-25s %-10s %-10s %-10s\n", "Item", "Quantity", "Price", "Total");
        System.out.println("======================================================");

        for (Order order : orders) {
            System.out.printf("%-25s %-10d %-10d %-10d\n", order.getItemName(), order.getQuantity(), order.getPrice(), order.getTotalPrice());
        }

        System.out.println("======================================================");
        System.out.printf("Grand Total: %d\n", total);
        System.out.println("======================================================");

        System.out.print("Do you want to print the bill? (Y/N): ");
        String printChoice = sc.nextLine();
        if (printChoice.equalsIgnoreCase("Y")) {
            printBill();
        }
    }

    private void printBill() {
        // Get default printer
        PrintService defaultPrinter = PrintServiceLookup.lookupDefaultPrintService();

        if (defaultPrinter != null) {
            // Prepare attributes
            PrintRequestAttributeSet attributeSet = new HashPrintRequestAttributeSet();
            attributeSet.add(new Copies(1)); // set number of copies

            // Print bill
            try {
                // Create PrinterJob
                java.awt.print.PrinterJob job = java.awt.print.PrinterJob.getPrinterJob();
                job.setPrintService(defaultPrinter);

                // Set printable
                job.setPrintable(new PrintableBill(this));

                // Print dialog
                if (job.printDialog(attributeSet)) {
                    job.print(attributeSet);
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Failed to print the bill.");
            }
        } else {
            System.out.println("No printer found. Please connect a printer to print the bill.");
        }
    }
}

class PrintableBill implements Printable {
    private Bill bill;

    public PrintableBill(Bill bill) {
        this.bill = bill;
    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
        if (pageIndex > 0) {
            return NO_SUCH_PAGE;
        }

        Graphics2D g2d = (Graphics2D) graphics;
        g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

        // Drawing bill content
        drawBillContent(g2d, pageFormat);

        return PAGE_EXISTS;
    }

    private void drawBillContent(Graphics2D g2d, PageFormat pageFormat) {
        double width = pageFormat.getImageableWidth();
        double height = pageFormat.getImageableHeight();
        int x = (int) ((width - 400) / 2); // Center the bill horizontally
        int y = 100; // Adjust y-coordinate for better alignment

        // Set font for the header
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        drawCenteredString(g2d, "------------ SRI SHAKTHI CAFE & BAKES ------------", x, y, 400);
        y += 30;

        // Set font for the rest of the bill
        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        drawCenteredString(g2d, "           *** Thank you for ordering ***           ", x, y, 400);
        y += 20;
        drawCenteredString(g2d, "Customer Name: " + bill.customerName, x, y, 400);
        y += 20;
        drawCenteredString(g2d, "Mobile Number: " + bill.mobileNumber, x, y, 400);
        y += 20;
        drawCenteredString(g2d, "====================================================", x, y, 400);
        y += 20;
        drawCenteredString(g2d, String.format("%-25s %-10s %-10s %-10s", "Item", "Quantity", "Price", "Total"), x, y, 400);
        y += 20;
        drawCenteredString(g2d, "====================================================", x, y, 400);
        y += 20;

        for (Order order : bill.orders) {
            drawCenteredString(g2d, String.format("%-25s %-10d %-10d %-10d", order.getItemName(), order.getQuantity(), order.getPrice(), order.getTotalPrice()), x, y, 400);
            y += 20;
        }

        drawCenteredString(g2d, "====================================================", x, y, 400);
        y += 20;
        drawCenteredString(g2d, "Total: " + bill.total, x, y, 400);
        y += 20;
        drawCenteredString(g2d, "====================================================", x, y, 400);
    }

    private void drawCenteredString(Graphics2D g2d, String text, int x, int y, int width) {
        FontMetrics metrics = g2d.getFontMetrics(g2d.getFont());
        int stringWidth = metrics.stringWidth(text);
        int textX = x + (width - stringWidth) / 2;
        g2d.drawString(text, textX, y);
    }

}

public class Main {
    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/cafe_db", "root", "")) {
            createAdminTable(conn); // Ensure the admin table exists and contains initial data if necessary
            Menu menu = new Menu(conn);
            Bill bill = new Bill(conn);
            Scanner sc = new Scanner(System.in);

            while (true) {
                System.out.print("Enter Your Role (1. Customer / 2. Admin): ");
                int role = sc.nextInt();

                if (role == 1) {
                    sc.nextLine(); // consume the newline
                    System.out.print("Enter your name: ");
                    String customerName = sc.nextLine();
                    String mobileNumber = getValidatedMobileNumber(sc);

                    // Insert customer details into the database
                    int customerId = insertCustomer(conn, customerName, mobileNumber);

                    bill.setCustomerDetails(customerName, mobileNumber);

                    menu.displayMenu();
                    boolean ordering = true;
                    while (ordering) {
                        System.out.print("Enter Your Choice: ");
                        int choice = sc.nextInt();

                        if (choice == 0) {
                            bill.generateBill();
                            System.exit(0);
                        }

                        if (choice < 1 || choice > menu.getItemCount()) {
                            System.out.println("Invalid Choice");
                            continue;
                        }

                        FoodItem selectedItem = menu.getItem(choice);
                        System.out.println("You have selected " + selectedItem.getName());
                        System.out.print("Enter the desired Quantity: ");
                        int quantity = sc.nextInt();

                        Order order = new Order(selectedItem, quantity);
                        bill.addOrder(order);
                        System.out.println("Order Added to Cart!");

                        System.out.print("Do you wish to order anything else (Y/N)? ");
                        String again = sc.next();
                        if (again.equalsIgnoreCase("N")) {
                            bill.generateBill();
                            ordering = false;
                        }
                    }
                } else if (role == 2) {
                    menu.adminPanel();
                } else {
                    System.out.println("Invalid Role");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void createAdminTable(Connection connection) {
        String sql = "CREATE TABLE IF NOT EXISTS admins (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "username VARCHAR(255) NOT NULL," +
                "password VARCHAR(255) NOT NULL" +
                ")";
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);

            // Insert a default admin account if the table is empty
            sql = "INSERT INTO admins (username, password) SELECT 'admin', 'password' WHERE NOT EXISTS (SELECT * FROM admins)";
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static String getValidatedMobileNumber(Scanner sc) {
        String mobileNumber;
        while (true) {
            System.out.print("Enter your mobile number: ");
            mobileNumber = sc.nextLine();
            if (isValidMobileNumber(mobileNumber)) {
                break;
            } else {
                System.out.println("Invalid mobile number. It must be exactly 10 digits.");
            }
        }
        return mobileNumber;
    }

    private static boolean isValidMobileNumber(String mobileNumber) {
        return mobileNumber.matches("\\d{10}");
    }

    private static int insertCustomer(Connection connection, String customerName, String mobileNumber) throws SQLException {
        String sql = "INSERT INTO customers (name, mobile) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, customerName);
            pstmt.setString(2, mobileNumber);
            pstmt.executeUpdate();
            ResultSet generatedKeys = pstmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                return generatedKeys.getInt(1); // Return the auto-generated customer ID
            }
            throw new SQLException("Failed to insert customer, no ID obtained.");
        }
    }
}
