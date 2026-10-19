# TechHub Computer Accessories POS

This is a Java Swing POS application. It does **not** use Maven and does **not** create an application JAR. The `run.bat` launcher compiles the Java source into the `out` folder and starts the application using the dependency JARs in `lib`.

## Requirements

- Windows
- JDK 17 or later installed and available on `PATH` (`java` and `javac`)
- MySQL 8.x server running on this computer

## MySQL setup

Make sure the MySQL 8 server is running. On startup, the app automatically creates the configured database (the default name is `computer_accessories_pos`) and its tables. The MySQL account must have permission to create databases. To create the default database manually instead, open MySQL Workbench or a MySQL command prompt and run:

```sql
CREATE DATABASE computer_accessories_pos
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

By default, the app connects to MySQL as `root` at `localhost` and asks for its password securely when `run.bat` starts. The password is not saved in this project. To use a different MySQL account or connection URL, set `POS_DB_USER` or `POS_DB_URL` in the terminal before running `run.bat`.

The first time the app connects to a new database, it asks you to create an admin username and password. Use a password with at least 8 characters. The password is stored as a BCrypt hash. Add cashier accounts later from the admin **Cashiers** page.

## Run the application

1. Make sure the MySQL server is running. The app creates its database and tables automatically if they are missing.
2. Double-click `run.bat`, or open Command Prompt in this project folder and run:

   ```bat
   run.bat
   ```

3. Enter the MySQL password when prompted. On first use, create the admin account in the setup window.
4. Sign in. Admin users open the dashboard; cashier users open the barcode POS.

The launcher compiles into `out` each time. No `mvn` command or application JAR is needed.

## VS Code Java problems

The `.classpath` and `.vscode/settings.json` files register the local JARs in `lib` for Java editor diagnostics. Excel export and dashboard charts use built-in Java APIs. If VS Code still shows unresolved imports after opening this project folder, run **Java: Clean Java Language Server Workspace** from the Command Palette and let the Java project reload. The source can also be verified independently by running `run.bat`, which recompiles it against the JARs in `lib`.

## Dependency JARs

The dependency files are in the project `lib` folder:

- `lib\jbcrypt-0.4.jar` — BCrypt password hashing.
- `lib\mysql-connector-j-8.0.33.jar` — MySQL Connector/J 8.0 JDBC database connection.

Excel export uses the JDK and does not require Apache POI. `run.bat` loads all JARs in `lib`.

## Features

- Role-based single login for administrators and cashiers.
- Admin dashboard with two initial charts and an option to add more, styled Swing navigation, table, and card components.
- Inventory records with barcode, item name, photo path or URL, quantity, size, and price.
- Purchase records linked to supplier and inventory item; purchase changes update stock. Source can be `SHOP` or `FACTORY`.
- Sale records, supplier list, VIP member list, and cashier account management.
- Tax settings and local QR image paths for KBZ Pay, Wave Pay, and AYA Pay.
- Cash or mobile banking checkout. Mobile checkout displays the configured QR code and asks for payment confirmation.
- Excel `.xlsx` export to a workbook with separate sheets for the stored records and settings. The exporter writes the workbook format using the JDK, without requiring Apache POI.

For product photos, paste a file path or URL in the inventory photo field, or use **Browse** to select a local image. QR images are selected from local files in **Setting** and must be accessible on the cashier computer.
