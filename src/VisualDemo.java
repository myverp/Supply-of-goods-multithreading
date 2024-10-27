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
        semaphore.acquire(amount); // Резервує місце для товарів
        items += amount;
        updateLabel();
        addToHistory("Постачальник додав " + amount + " товарів.");
        Thread.sleep(1000); // Імітація часу постачання
        semaphore.release(amount); // Вивільняє місце
    }

    public void removeGoods(int amount) throws InterruptedException {
        semaphore.acquire(amount); // Перевіряє наявність товару
        if (items >= amount) {
            items -= amount;
            updateLabel();
            addToHistory("Покупець забрав " + amount + " товарів.");
            Thread.sleep(1000); // Імітація часу вилучення
        } else {
            addToHistory("Недостатньо товару для вилучення.");
        }
        semaphore.release(amount); // Дозволяє іншим потокам використовувати склад
    }

    private void updateLabel() {
        SwingUtilities.invokeLater(() -> label.setText("Товарів на складі: " + items + "/" + capacity));
    }

    private void addToHistory(String message) {
        SwingUtilities.invokeLater(() -> {
            historyArea.append(message + "\n");
            historyArea.setCaretPosition(historyArea.getDocument().getLength()); // Прокрутка до кінця
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
                Thread.sleep(2000); // Затримка між постачанням
            }
        } catch (InterruptedException e) {
            System.out.println("Постачальник зупинений.");
        }
    }
}

class Consumer implements Runnable {
    private final Warehouse warehouse;
    private final JLabel statusLabel;
    private int simulatedHour = 0; // Початкове значення симульованої години

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
                    setStatus("Покупець активно купує товар. Час: " + simulatedHour + ":00");
                } else {
                    setStatus("Неробочі години. Покупець чекає. Час: " + simulatedHour + ":00");
                }

                // Додаємо 5 годин за кожну ітерацію
                simulatedHour += 5;
                if (simulatedHour >= 24) {
                    simulatedHour -= 24; // Повертаємо в діапазон 0-23
                }

                Thread.sleep(3000); // Затримка між покупками
            }
        } catch (InterruptedException e) {
            System.out.println("Покупець зупинений.");
        }
    }

    private boolean isWorkingHours(int hour) {
        return hour >= 9 && hour < 18; // Робочі години з 9 до 18
    }

    private void setStatus(String status) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(status));
    }
}

public class VisualDemo {
    public static void main(String[] args) {
        // Створюємо вікно для відображення стану
        JFrame frame = new JFrame("Візуальна демонстрація складу");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 300);

        // Панель для відображення кількості товарів
        JLabel itemLabel = new JLabel("Товарів на складі: 0/100");
        itemLabel.setHorizontalAlignment(SwingConstants.CENTER);
        itemLabel.setFont(new Font("Arial", Font.PLAIN, 18));

        // Панель для відображення статусу покупця
        JLabel statusLabel = new JLabel("Статус покупця");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 16));

        // Текстове поле для відображення історії операцій
        JTextArea historyArea = new JTextArea(10, 30);
        historyArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(historyArea);

        // Розміщуємо елементи на панелі
        frame.setLayout(new BorderLayout());
        frame.add(itemLabel, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(statusLabel, BorderLayout.SOUTH);

        // Ініціалізуємо склад
        Warehouse warehouse = new Warehouse(100, itemLabel, historyArea);

        // Створюємо потоки постачальника і покупця
        Thread supplierThread = new Thread(new Supplier(warehouse));
        Thread consumerThread = new Thread(new Consumer(warehouse, statusLabel));

        // Запускаємо потоки
        supplierThread.start();
        consumerThread.start();

        // Відображаємо вікно
        frame.setVisible(true);

        // Імітуємо роботу протягом 60 секунд
        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Зупиняємо потоки
        supplierThread.interrupt();
        consumerThread.interrupt();
    }
}
