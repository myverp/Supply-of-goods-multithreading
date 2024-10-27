import javax.swing.*;
import java.awt.*;
import java.util.concurrent.Semaphore;

class Warehouse {
    private final Semaphore semaphore;
    private int items;
    private final int capacity;
    private final JLabel label;
    private final JTextArea historyArea;

    public Warehouse(int capacity, JLabel label, JTextArea historyArea) {
        this.semaphore = new Semaphore(capacity);
        this.items = 0;
        this.capacity = capacity;
        this.label = label;
        this.historyArea = historyArea;
    }

    public void addGoods(int amount) throws InterruptedException {
        semaphore.acquire(amount); // Reserve space for goods
        items += amount;
        updateLabel();
        addToHistory("Supplier added " + amount + " items.");
        Thread.sleep(1000); // Simulate delivery time
        semaphore.release(amount); // Free up space
    }

    public void removeGoods(int amount) throws InterruptedException {
        semaphore.acquire(amount); // Check for available items
        if (items >= amount) {
            items -= amount;
            updateLabel();
            addToHistory("Customer removed " + amount + " items.");
            Thread.sleep(1000); // Simulate removal time
        } else {
            addToHistory("Not enough items to remove.");
        }
        semaphore.release(amount); // Allow other threads to use the warehouse
    }

    private void updateLabel() {
        SwingUtilities.invokeLater(() -> label.setText("Items in stock: " + items + "/" + capacity));
    }

    private void addToHistory(String message) {
        SwingUtilities.invokeLater(() -> {
            historyArea.append(message + "\n");
            historyArea.setCaretPosition(historyArea.getDocument().getLength()); // Scroll to the end
        });
    }
}

class Supplier implements Runnable {
    private final Warehouse warehouse;

    public Supplier(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    @Override
    public void run() {
        try {
            while (true) {
                int amount = (int) (Math.random() * 10) + 1;
                warehouse.addGoods(amount);
                Thread.sleep(2000); // Delay between deliveries
            }
        } catch (InterruptedException e) {
            System.out.println("Supplier stopped.");
        }
    }
}

class Consumer implements Runnable {
    private final Warehouse warehouse;
    private final JLabel statusLabel;
    private int simulatedHour = 0; // Initial value of the simulated hour

    public Consumer(Warehouse warehouse, JLabel statusLabel) {
        this.warehouse = warehouse;
        this.statusLabel = statusLabel;
    }

    @Override
    public void run() {
        try {
            while (true) {
                if (isWorkingHours(simulatedHour)) {
                    int amount = (int) (Math.random() * 5) + 1;
                    warehouse.removeGoods(amount);
                    setStatus("Customer actively buying items. Time: " + simulatedHour + ":00");
                } else {
                    setStatus("Non-working hours. Customer is waiting. Time: " + simulatedHour + ":00");
                }

                // Add 5 hours for each iteration
                simulatedHour += 5;
                if (simulatedHour >= 24) {
                    simulatedHour -= 24; // Wrap around in the range 0-23
                }

                Thread.sleep(3000); // Delay between purchases
            }
        } catch (InterruptedException e) {
            System.out.println("Customer stopped.");
        }
    }

    private boolean isWorkingHours(int hour) {
        return hour >= 9 && hour < 18; // Working hours from 9 to 18
    }

    private void setStatus(String status) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(status));
    }
}

public class VisualDemo {
    public static void main(String[] args) {
        // Create a window to display the status
        JFrame frame = new JFrame("Warehouse Visual Demo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 300);

        // Panel to display the number of items
        JLabel itemLabel = new JLabel("Items in stock: 0/100");
        itemLabel.setHorizontalAlignment(SwingConstants.CENTER);
        itemLabel.setFont(new Font("Arial", Font.PLAIN, 18));

        // Panel to display the customer's status
        JLabel statusLabel = new JLabel("Customer Status");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 16));

        // Text area to display the history of operations
        JTextArea historyArea = new JTextArea(10, 30);
        historyArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(historyArea);

        // Arrange the elements on the panel
        frame.setLayout(new BorderLayout());
        frame.add(itemLabel, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(statusLabel, BorderLayout.SOUTH);

        // Initialize the warehouse
        Warehouse warehouse = new Warehouse(100, itemLabel, historyArea);

        // Create supplier and consumer threads
        Thread supplierThread = new Thread(new Supplier(warehouse));
        Thread consumerThread = new Thread(new Consumer(warehouse, statusLabel));

        // Start the threads
        supplierThread.start();
        consumerThread.start();

        // Display the window
        frame.setVisible(true);

        // Simulate operation for 60 seconds
        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Stop the threads
        supplierThread.interrupt();
        consumerThread.interrupt();
    }
}
