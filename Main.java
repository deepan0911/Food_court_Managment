import java.sql.*;
import java.util.*;

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
}

class Bill {
    private List<Order> orders;
    private int total;
    private Connection connection;

    public Bill(Connection connection) {
        orders = new ArrayList<>();
        total = 0;
        this.connection = connection;
    }

    public void addOrder(Order order, int customerId) {
        orders.add(order);
        total += order.calculateTotal();

        // Insert order details into the database
        String sql = "INSERT INTO orders (customer_id, item_name, quantity, total_price) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, customerId);
            pstmt.setString(2, order.getItemName());
            pstmt.setInt(3, order.getQuantity());
            pstmt.setInt(4, order.calculateTotal());
            pstmt.executeUpdate();
            System.out.println("Order details saved successfully!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getTotal() {
        return total;
    }

    public void generateBill(String customerName, String mobileNumber) {
        System.out.println();
        System.out.println("** Thank you for ordering ***");
        System.out.println("Your Bill is: " + total);

        // Additional logic to handle bill generation
    }
}

public class Main {
    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/cafe_db", "root", "")) {
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
                    System.out.print("Enter your mobile number: ");
                    String mobileNumber = sc.nextLine();

                    // Insert customer details into the database
                    int customerId = insertCustomer(conn, customerName, mobileNumber);

                    menu.displayMenu();
                    boolean ordering = true;
                    while (ordering) {
                        System.out.print("Enter Your Choice: ");
                        int choice = sc.nextInt();

                        if (choice == 0) {
                            bill.generateBill(customerName, mobileNumber);
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
                        bill.addOrder(order, customerId);
                        System.out.println("Order Added to Cart!");

                        System.out.print("Do you wish to order anything else (Y/N)? ");
                        String again = sc.next();
                        if (again.equalsIgnoreCase("N")) {
                            bill.generateBill(customerName, mobileNumber);
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

